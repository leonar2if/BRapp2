package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.models.Appointment
import com.example.data.models.BlockedSlot
import com.example.data.models.Product
import com.example.data.models.Service
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SettingsRepository
import com.example.notifications.LocalNotificationScheduler
import com.example.service.BlockedSlotService
import com.example.utils.DateFormatter
import com.example.utils.ErrorTranslator
import com.example.utils.SlotSchedule
import com.example.utils.Validators
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "barberia_cache").fallbackToDestructiveMigration().build()
    private val productRepo = ProductRepository(db.serviceDao(), db.productDao())
    private val apptRepo = AppointmentRepository()
    private val settingsRepo = SettingsRepository(db.settingsDao())
    private val authRepo = AuthRepository(application)
    private val blockedSlotService = BlockedSlotService()
    private val clientNoteRepo = com.example.data.repository.ClientNoteRepository(db.clientNoteDao())

    fun getNotesForClient(phone: String): Flow<List<com.example.data.models.ClientNoteEntity>> =
        clientNoteRepo.getNotesForClient(phone)

    fun addClientNote(clientPhone: String, clientName: String, text: String) {
        viewModelScope.launch {
            clientNoteRepo.addNote(clientPhone, clientName, text)
        }
    }

    /**
     * Sube las notas pendientes antes de cerrar sesión (sección 6). Best-effort:
     * si no hay conexión, las notas quedan intactas localmente para la próxima
     * vez - el logout continúa de todos modos, no se bloquea esperando la red.
     */
    suspend fun uploadPendingNotesBeforeLogout() {
        val adminId = authRepo.userId.first()
        if (adminId.isNotEmpty()) {
            try {
                clientNoteRepo.uploadPendingNotes(adminId)
            } catch (e: Exception) {
                // best-effort, no bloquea el logout
            }
        }
    }

    val allServices: StateFlow<List<Service>> = productRepo.allServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<Product>> = productRepo.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val todayAppointments: StateFlow<List<Appointment>> = _todayAppointments

    private val _selectedDateAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val selectedDateAppointments: StateFlow<List<Appointment>> = _selectedDateAppointments

    private val _allAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val allAppointments: StateFlow<List<Appointment>> = _allAppointments

    // Turnos bloqueados por el admin para el día seleccionado en Agenda
    // (botón ⊘, sección 8). Se recargan junto con selectDate/refreshData.
    private val _selectedDateBlockedSlots = MutableStateFlow<List<BlockedSlot>>(emptyList())
    val selectedDateBlockedSlots: StateFlow<List<BlockedSlot>> = _selectedDateBlockedSlots

    private val _todayBlockedSlots = MutableStateFlow<List<BlockedSlot>>(emptyList())
    val todayBlockedSlots: StateFlow<List<BlockedSlot>> = _todayBlockedSlots

    private val _selectedDate = MutableStateFlow(DateFormatter.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate

    // Cronómetro independiente por cita (pantalla "Hoy" - vista de galería).
    // Cada tarjeta puede tener su propio cronómetro corriendo, identificado
    // por el id de la cita. elapsedByAppointment expone los segundos
    // transcurridos de cada uno para pintar el timer en su card.
    private val _elapsedByAppointment = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val elapsedByAppointment: StateFlow<Map<Long, Long>> = _elapsedByAppointment
    private val timerJobs = mutableMapOf<Long, Job>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _managerPhone = MutableStateFlow("34600000000")
    val managerPhone: StateFlow<String> = _managerPhone

    private val _managerName = MutableStateFlow("Gestor Rodríguez")
    val managerName: StateFlow<String> = _managerName

    private val _storeHours = MutableStateFlow("Lunes a Viernes 10:00 - 18:00")
    val storeHours: StateFlow<String> = _storeHours

    // Días laborables configurables (sección 16/17 del prompt maestro).
    // Se guarda en settings.working_days como CSV de códigos de 3 letras
    // (MON,TUE,WED,THU,FRI). Compatible con el JSON sembrado en
    // supabase_update_bloque1.sql: el parseo tolera ambos formatos.
    private val _workingDaysCsv = MutableStateFlow("MON,TUE,WED,THU,FRI")
    val workingDaysCsv: StateFlow<String> = _workingDaysCsv

    // Turnos configurables (pestaña Horario en Ajustes). Se guarda en
    // settings.slot_definitions como CSV de horas "HH:mm". Por defecto son
    // los 12 turnos oficiales (SlotSchedule.DEFAULT_SLOTS) hasta que el admin
    // los edite.
    private val _activeSlots = MutableStateFlow(SlotSchedule.DEFAULT_SLOTS)
    val activeSlots: StateFlow<List<String>> = _activeSlots

    fun saveActiveSlots(slots: List<String>) {
        viewModelScope.launch {
            val normalized = SlotSchedule.parseSlotDefinitionsCsv(slots.joinToString(","))
            _activeSlots.value = normalized
            settingsRepo.saveSetting("slot_definitions", SlotSchedule.slotDefinitionsToCsv(normalized))
        }
    }

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch { doRefresh() }
    }

    /** Igual que refreshData() pero awaitable, para pull-to-refresh (sección 1.1). */
    suspend fun refreshDataAwait() = doRefresh()

    private suspend fun doRefresh() {
        _isLoading.value = true
        val today = DateFormatter.getTodayDateString()
        val todayList = apptRepo.fetchAppointmentsByDate(today)
        _todayAppointments.value = todayList.sortedBy { it.appointmentTime }
        _todayBlockedSlots.value = blockedSlotService.getBlockedSlotsByDate(today)

        if (_selectedDate.value == today) {
            _selectedDateAppointments.value = _todayAppointments.value
            _selectedDateBlockedSlots.value = _todayBlockedSlots.value
        } else {
            _selectedDateAppointments.value = apptRepo.fetchAppointmentsByDate(_selectedDate.value)
            _selectedDateBlockedSlots.value = blockedSlotService.getBlockedSlotsByDate(_selectedDate.value)
        }

        val hist = apptRepo.fetchAllAppointments()
        _allAppointments.value = hist.sortedByDescending { it.appointmentDate }

        productRepo.refreshServices()
        productRepo.refreshProducts()
        settingsRepo.refreshSettings()

        _managerPhone.value = settingsRepo.getSettingValue("manager_phone", "34600000000")
        _managerName.value = settingsRepo.getSettingValue("manager_name", "Gestor Rodríguez")
        _storeHours.value = settingsRepo.getSettingValue("store_hours", "Lunes a Viernes 10:00 - 18:00")
        _workingDaysCsv.value = normalizeWorkingDaysValue(
            settingsRepo.getSettingValue("working_days", "MON,TUE,WED,THU,FRI")
        )
        _activeSlots.value = SlotSchedule.parseSlotDefinitionsCsv(
            settingsRepo.getSettingValue("slot_definitions", SlotSchedule.slotDefinitionsToCsv(SlotSchedule.DEFAULT_SLOTS))
        )
        
        _isLoading.value = false
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            _isLoading.value = true
            _selectedDateAppointments.value = apptRepo.fetchAppointmentsByDate(date)
            _selectedDateBlockedSlots.value = blockedSlotService.getBlockedSlotsByDate(date)
            _isLoading.value = false
        }
    }

    /**
     * Botón ⊘ "Libre el resto del día" (sección 8 del prompt maestro).
     * fromTime = null -> bloquea TODOS los turnos restantes desde ahora.
     * fromTime = "14:00" -> bloquea ese turno y todos los siguientes; los
     * turnos anteriores quedan intactos.
     * No bloquea turnos que ya tienen una cita confirmada (esos no se tocan;
     * el admin puede cancelarlos aparte si hace falta).
     */
    fun blockRestOfDay(date: String, fromTime: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val adminId = authRepo.userId.first()
            val allSlots = _activeSlots.value
            val nowTime = DateFormatter.getNowTimeString()
            val startFrom = fromTime ?: allSlots.firstOrNull { it > nowTime } ?: allSlots.last()
            val startIndex = allSlots.indexOf(startFrom).let { if (it == -1) 0 else it }
            val occupiedTimes = (if (date == DateFormatter.getTodayDateString()) _todayAppointments.value else _selectedDateAppointments.value)
                .filter { it.status != "canceled" }
                .map { it.appointmentTime.take(5) }
                .toSet()
            val slotsToBlock = allSlots.subList(startIndex, allSlots.size).filter { it !in occupiedTimes }

            if (slotsToBlock.isNotEmpty()) {
                blockedSlotService.blockSlots(date, slotsToBlock, adminId)
            }
            refreshData()
            _isLoading.value = false
        }
    }

    /** Revierte el bloqueo ⊘ del día indicado. */
    fun clearBlockedSlots(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            blockedSlotService.clearDayBlocks(date)
            refreshData()
            _isLoading.value = false
        }
    }

    /**
     * Reserva rápida del administrador desde la Agenda (sección 11 del
     * prompt maestro): a diferencia del flujo del cliente, el admin NO está
     * obligado a introducir datos personales. Nombre/teléfono son opcionales.
     */
    fun createQuickAdminAppointment(
        date: String,
        time: String,
        service: Service,
        name: String,
        phone: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val adminId = authRepo.userId.first()
            val cleanPhone = if (phone.isNotBlank() && Validators.isValidLocalPhone(phone)) {
                Validators.cleanPhoneNumber(phone)
            } else {
                ""
            }
            val appt = Appointment(
                clientId = "admin_walkin_$adminId",
                serviceId = service.id,
                fullName = name.ifBlank { "Cliente sin registrar" },
                phone = cleanPhone,
                isAnnexed = false,
                appointmentDate = date,
                appointmentTime = time,
                status = "confirmed",
                createdByAdmin = true
            )
            val res = apptRepo.createAppointment(appt, service.durationSlots, _activeSlots.value)
            if (res.isFailure) {
                _errorMessage.value = ErrorTranslator.toHumanMessage(res.exceptionOrNull())
            }
            refreshData()
            _isLoading.value = false
        }
    }

    /**
     * Cronómetro por card (pantalla "Hoy" - vista de galería). Cada cita
     * tiene su propio contador independiente identificado por su id, para
     * que el admin pueda iniciarlo desde cualquier turno del carrusel y ver
     * cuánto duró ese pelado en particular.
     */
    fun startCardTimer(appointmentId: Long) {
        timerJobs[appointmentId]?.cancel()
        timerJobs[appointmentId] = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _elapsedByAppointment.value
                _elapsedByAppointment.value = current + (appointmentId to (current[appointmentId] ?: 0L) + 1)
            }
        }
    }

    fun pauseCardTimer(appointmentId: Long) {
        timerJobs[appointmentId]?.cancel()
        timerJobs.remove(appointmentId)
    }

    fun isCardTimerRunning(appointmentId: Long): Boolean = timerJobs[appointmentId] != null

    /** Guarda la nota de una cita puntual (identificada por id), sin depender de un índice secuencial. */
    fun updateNotesForAppointment(appointmentId: Long, notes: String) {
        // Actualización optimista para que el campo de texto no "salte" mientras se guarda.
        _todayAppointments.value = _todayAppointments.value.map {
            if (it.id == appointmentId) it.copy(notes = notes) else it
        }
        viewModelScope.launch {
            apptRepo.updateNotes(appointmentId, notes)
        }
    }

    /** Marca como atendido y detiene su cronómetro si estaba corriendo (botón FINALIZADO de la card). */
    fun finalizeAppointment(appointmentId: Long) {
        pauseCardTimer(appointmentId)
        markAsAttended(appointmentId)
    }

    fun cancelAppointment(appointmentId: Long) {
        viewModelScope.launch {
            val adminId = authRepo.userId.first()
            val res = apptRepo.cancelAppointment(appointmentId, adminId)
            if (res.isSuccess) {
                LocalNotificationScheduler.cancelAppointmentReminders(getApplication(), appointmentId.toString())
            }
            refreshData()
        }
    }

    fun markAsAttended(appointmentId: Long) {
        viewModelScope.launch {
            val adminId = authRepo.userId.first()
            apptRepo.markAsAttended(appointmentId, adminId)
            refreshData()
        }
    }

    // SERVICES CRUD
    fun saveService(service: Service) {
        viewModelScope.launch {
            _isLoading.value = true
            if (service.id == 0L) {
                productRepo.createService(service)
            } else {
                productRepo.updateService(service.id, mapOf(
                    "name" to service.name,
                    "duration_minutes" to service.durationMinutes,
                    "duration_slots" to service.durationSlots,
                    "price" to service.price,
                    "is_active" to service.isActive
                ), service)
            }
            productRepo.refreshServices()
            _isLoading.value = false
        }
    }

    fun deleteService(serviceId: Long) {
        viewModelScope.launch {
            productRepo.deleteService(serviceId)
        }
    }

    // PRODUCTS CRUD
    fun saveProduct(product: Product, imageBytes1: ByteArray? = null, filename1: String? = null, imageBytes2: ByteArray? = null, filename2: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            if (product.id == 0L) {
                productRepo.createProduct(product, imageBytes1, filename1, imageBytes2, filename2)
            } else {
                var url1 = product.imageUrl1
                var url2 = product.imageUrl2
                if (imageBytes1 != null && filename1 != null) {
                    // Re-create to upload new image
                    productRepo.createProduct(product, imageBytes1, filename1, imageBytes2, filename2)
                } else {
                    productRepo.updateProduct(product.id, mapOf(
                        "name" to product.name,
                        "description" to product.description,
                        "price" to product.price,
                        "is_active" to product.isActive
                    ), product)
                }
            }
            productRepo.refreshProducts()
            _isLoading.value = false
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            productRepo.deleteProduct(productId)
        }
    }

    // SETTINGS
    fun saveManagerContact(name: String, phone: String) {
        viewModelScope.launch {
            settingsRepo.saveSetting("manager_name", name)
            settingsRepo.saveSetting("manager_phone", phone)
            _managerName.value = name
            _managerPhone.value = phone
        }
    }

    fun saveStoreHours(hours: String) {
        viewModelScope.launch {
            settingsRepo.saveSetting("store_hours", hours)
            _storeHours.value = hours
        }
    }

    /**
     * Guarda los días laborables elegidos por el admin (sección 16/17).
     * Recibe un Set de códigos ("MON","TUE",...) y lo persiste como CSV en
     * settings.working_days, reutilizando la misma tabla/patrón que el resto
     * de ajustes (manager_phone, store_hours), sin crear estructuras nuevas.
     */
    fun saveWorkingDays(days: Set<String>) {
        viewModelScope.launch {
            val csv = days.joinToString(",")
            settingsRepo.saveSetting("working_days", csv)
            _workingDaysCsv.value = csv
        }
    }

    /**
     * Acepta tanto el formato JSON sembrado por supabase_update_bloque1.sql
     * (ej. ["MON","TUE","WED","THU","FRI"]) como CSV simple (MON,TUE,...),
     * para no depender de cuál de los dos haya quedado guardado.
     */
    private fun normalizeWorkingDaysValue(raw: String): String {
        val cleaned = raw.trim()
        return if (cleaned.startsWith("[")) {
            cleaned.removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() }
                .joinToString(",")
        } else {
            cleaned
        }
    }

    fun markDayOff(date: String, sendNotification: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val appts = apptRepo.fetchAppointmentsByDate(date)
            val adminId = authRepo.userId.first()
            for (a in appts) {
                if (a.status == "confirmed" || a.status == "in_progress") {
                    val res = apptRepo.cancelAppointment(a.id, adminId)
                    if (res.isSuccess) {
                        LocalNotificationScheduler.cancelAppointmentReminders(getApplication(), a.id.toString())
                    }
                    if (sendNotification) {
                        // In real FCM, we trigger server push or local notification
                    }
                }
            }
            refreshData()
            _isLoading.value = false
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            authRepo.setDarkMode(enabled)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
        }
    }
}
