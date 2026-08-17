package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.models.Appointment
import com.example.data.models.Product
import com.example.data.models.Service
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.ProductRepository
import com.example.data.repository.SettingsRepository
import com.example.notifications.LocalNotificationScheduler
import com.example.utils.DateFormatter
import com.example.utils.ErrorTranslator
import com.example.utils.SlotSchedule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "barberia_cache").fallbackToDestructiveMigration().build()
    private val productRepo = ProductRepository(db.serviceDao(), db.productDao())
    private val apptRepo = AppointmentRepository()
    private val blockedSlotService = com.example.service.BlockedSlotService()
    private val settingsRepo = SettingsRepository(db.settingsDao())
    private val authRepo = AuthRepository(application)

    val activeServices: StateFlow<List<Service>> = productRepo.activeServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<Product>> = productRepo.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _clientAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val clientAppointments: StateFlow<List<Appointment>> = _clientAppointments

    private val _dayAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val dayAppointments: StateFlow<List<Appointment>> = _dayAppointments

    private val _isDaySlotsLoading = MutableStateFlow(false)
    val isDaySlotsLoading: StateFlow<Boolean> = _isDaySlotsLoading

    private val _selectedDate = MutableStateFlow(DateFormatter.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate

    private val _selectedTime = MutableStateFlow("")
    val selectedTime: StateFlow<String> = _selectedTime

    private val _selectedService = MutableStateFlow<Service?>(null)
    val selectedService: StateFlow<Service?> = _selectedService

    private val _bookingStep = MutableStateFlow(0) // 0=Calendar, 1=Times, 2=Services, 3=Confirm, 4=Ticket
    val bookingStep: StateFlow<Int> = _bookingStep

    private val _lastBookedAppointment = MutableStateFlow<Appointment?>(null)
    val lastBookedAppointment: StateFlow<Appointment?> = _lastBookedAppointment

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _managerPhone = MutableStateFlow("34600000000")
    val managerPhone: StateFlow<String> = _managerPhone

    private val _storeHours = MutableStateFlow("10:00 - 18:00")
    val storeHours: StateFlow<String> = _storeHours

    private val _userBirthday = MutableStateFlow<String?>(null)
    val userBirthday: StateFlow<String?> = _userBirthday

    fun saveBirthday(birthday: String?) {
        viewModelScope.launch {
            val res = authRepo.updateBirthday(birthday)
            if (res.isSuccess) {
                _userBirthday.value = birthday
            }
        }
    }

    // Días laborables configurables desde Ajustes -> Horarios (admin).
    // Por defecto lunes a viernes hasta que se cargue el valor real de settings.
    private val _workingDays = MutableStateFlow(SlotSchedule.DEFAULT_WORKING_DAYS)

    // Turnos configurables (misma fuente que el admin, sección Horario en Ajustes).
    private val _activeSlots = MutableStateFlow(SlotSchedule.DEFAULT_SLOTS)
    val activeSlots: StateFlow<List<String>> = _activeSlots

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch { doRefresh() }
    }

    /** Igual que refreshData() pero awaitable, para pull-to-refresh (sección 1.1). */
    suspend fun refreshDataAwait() = doRefresh()

    private val _cancellationNotice = MutableStateFlow<Appointment?>(null)
    val cancellationNotice: StateFlow<Appointment?> = _cancellationNotice
    private val shownCancellationIds = mutableSetOf<Long>()

    fun dismissCancellationNotice() {
        _cancellationNotice.value?.let { shownCancellationIds.add(it.id) }
        _cancellationNotice.value = null
    }

    private suspend fun doRefresh() {
        _isLoading.value = true
        val userId = authRepo.userId.first()
        if (userId.isNotEmpty()) {
            val appts = apptRepo.fetchClientAppointments(userId)
            _clientAppointments.value = appts
            // Turno cancelado por el admin al bloquear un turno/día (sección 6):
            // avisar con disculpa la primera vez que se detecta, sin repetir.
            val pending = appts.firstOrNull {
                it.status == "canceled" && it.cancelReason == "admin_block" && it.id !in shownCancellationIds
            }
            if (pending != null && _cancellationNotice.value == null) {
                _cancellationNotice.value = pending
            }
        }
        productRepo.refreshServices()
        productRepo.refreshProducts()
        settingsRepo.refreshSettings()
        
        val phone = settingsRepo.getSettingValue("manager_phone", "34600000000")
        _managerPhone.value = phone
        val hours = settingsRepo.getSettingValue("store_hours", "10:00 - 18:00")
        _storeHours.value = hours
        _workingDays.value = SlotSchedule.parseWorkingDaysCsv(
            settingsRepo.getSettingValue("working_days", "MON,TUE,WED,THU,FRI")
        )
        _activeSlots.value = SlotSchedule.parseSlotDefinitionsCsv(
            settingsRepo.getSettingValue("slot_definitions", SlotSchedule.slotDefinitionsToCsv(SlotSchedule.DEFAULT_SLOTS))
        )

        // Recién ahora _workingDays refleja la config real (antes de esto, si
        // el día por defecto era "hoy" y hoy no es laborable, se mostraba como
        // disponible por error - sección 7). Si el día seleccionado ya no es
        // válido, saltar automáticamente al próximo día laborable.
        if (!SlotSchedule.isWorkingDay(_selectedDate.value, _workingDays.value)) {
            _selectedDate.value = SlotSchedule.findNextValidDay(_selectedDate.value, _workingDays.value)
        }

        _userBirthday.value = authRepo.getCurrentProfile()?.birthday
        fetchDaySlots(_selectedDate.value)
        _isLoading.value = false
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedTime.value = ""
        fetchDaySlots(date)
    }

    private val _selectedDateBlockedTimes = MutableStateFlow<Set<String>>(emptySet())
    val selectedDateBlockedTimes: StateFlow<Set<String>> = _selectedDateBlockedTimes

    private fun fetchDaySlots(date: String) {
        viewModelScope.launch {
            _isDaySlotsLoading.value = true
            val appts = apptRepo.fetchAppointmentsByDate(date)
            _dayAppointments.value = appts
            _selectedDateBlockedTimes.value = blockedSlotService.getBlockedSlotsByDate(date).map { it.blockTime }.toSet()
            _isDaySlotsLoading.value = false
        }
    }

    fun selectTime(time: String) {
        _selectedTime.value = time
        _bookingStep.value = 2 // Move to services selector
    }

    fun selectService(service: Service) {
        // Antes de avanzar a confirmar, comprobar que el rango completo de
        // turnos que ocupa este servicio (no solo el turno elegido) siga
        // libre. Un servicio de N turnos debe bloquear los N, y esto evita
        // que el cliente llegue a confirmar una cita que en realidad choca
        // con otra reserva en el segundo/tercer turno. Sección 12.
        val range = com.example.utils.SlotSchedule.slotRangeFor(_selectedTime.value, service.durationSlots, _activeSlots.value)
        if (range == null) {
            _bookingError.value = "Este servicio no cabe en el horario restante del día. Elige otra hora."
            _selectedTime.value = ""
            _bookingStep.value = 1
            return
        }
        val occupiedInRange = range.any { isSlotOccupied(it) }
        if (occupiedInRange) {
            _bookingError.value = "Este servicio necesita ${service.durationSlots} turnos consecutivos y alguno ya está ocupado. Elige otra hora."
            _selectedTime.value = ""
            _bookingStep.value = 1
            return
        }
        _selectedService.value = service
        _bookingStep.value = 3 // Move to confirm screen
    }

    fun setBookingStep(step: Int) {
        _bookingStep.value = step
        _bookingError.value = null
    }

    fun confirmBooking(
        isForOther: Boolean,
        otherName: String,
        otherLastName1: String,
        otherLastName2: String,
        otherPhone: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _bookingError.value = null

            val userId = authRepo.userId.first()
            val userPhone = authRepo.userPhone.first()
            val userFullName = authRepo.userFullName.first()
            val service = _selectedService.value

            if (service == null || _selectedTime.value.isEmpty() || _selectedDate.value.isEmpty()) {
                _bookingError.value = "Datos de reserva incompletos."
                _isLoading.value = false
                return@launch
            }

            if (isForOther && (otherName.isBlank() || otherPhone.isBlank())) {
                _bookingError.value = "Por favor, completa el nombre y teléfono de la otra persona."
                _isLoading.value = false
                return@launch
            }

            if (isForOther && !com.example.utils.Validators.isValidLocalPhone(otherPhone)) {
                _bookingError.value = "El número de teléfono debe empezar por 5 y tener 8 dígitos."
                _isLoading.value = false
                return@launch
            }

            // El cliente solo puede tener 1 turno activo a su propio nombre a la
            // vez: si ya tiene uno pendiente (confirmed/in_progress) sin resolver,
            // no puede sacar otro hasta que ese se resuelva (venga o no venga).
            // No aplica a reservas para otra persona (isForOther).
            if (!isForOther) {
                val yaTieneActivo = _clientAppointments.value.any {
                    it.clientId == userId && (it.status == "confirmed" || it.status == "in_progress")
                }
                if (yaTieneActivo) {
                    _bookingError.value = "Ya tienes un turno pendiente. No puedes reservar otro hasta que ese se resuelva."
                    _isLoading.value = false
                    return@launch
                }
            }

            val appt = Appointment(
                clientId = if (isForOther) "annexed_$userId" else userId,
                serviceId = service.id,
                fullName = if (isForOther) otherName else userFullName,
                lastName1 = if (isForOther) otherLastName1 else null,
                lastName2 = if (isForOther) otherLastName2 else null,
                phone = if (isForOther) com.example.utils.Validators.cleanPhoneNumber(otherPhone) else userPhone,
                isAnnexed = isForOther,
                mainClientId = if (isForOther) userId else null,
                appointmentDate = _selectedDate.value,
                appointmentTime = _selectedTime.value,
                status = "confirmed",
                createdByAdmin = false
            )

            val res = apptRepo.createAppointment(appt, service.durationSlots, _activeSlots.value)
            _isLoading.value = false
            val booked = res.getOrNull()
            if (res.isSuccess && booked != null) {
                _lastBookedAppointment.value = booked
                _bookingStep.value = 4 // Ticket confirmation
                LocalNotificationScheduler.createNotificationChannel(getApplication())
                LocalNotificationScheduler.scheduleAppointmentReminders(
                    context = getApplication(),
                    appointmentId = booked.id.toString(),
                    appointmentDate = booked.appointmentDate,
                    appointmentTime = booked.appointmentTime,
                    clientName = booked.fullName,
                    serviceName = service.name
                )
                refreshData()
            } else {
                _bookingError.value = ErrorTranslator.toHumanMessage(res.exceptionOrNull())
            }
        }
    }

    fun updatePhone(newPhone: String) {
        viewModelScope.launch {
            authRepo.updatePhone(newPhone)
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

    fun resetBookingFlow() {
        _selectedTime.value = ""
        _selectedService.value = null
        _bookingStep.value = 0
        _lastBookedAppointment.value = null
        _bookingError.value = null
    }

    fun isSlotOccupied(time: String): Boolean {
        val appts = _dayAppointments.value
        val services = activeServices.value
        return appts.any { appt ->
            if (appt.status == "canceled") return@any false
            // Turnos que ocupa esta cita: su propio turno de inicio +
            // (durationSlots - 1) turnos siguientes, según el servicio
            // reservado. Así un servicio de 2+ turnos bloquea todos los
            // turnos que ocupa, no solo el primero (sección 12).
            val durationSlots = services.find { it.id == appt.serviceId }?.durationSlots ?: 1
            val range = com.example.utils.SlotSchedule.slotRangeFor(appt.appointmentTime.take(5), durationSlots, _activeSlots.value)
                ?: listOf(appt.appointmentTime.take(5))
            time.take(5) in range
        }
    }

    /** El admin dejó este turno sin disponibilidad (día completo/parcial, sección 5). */
    fun isSlotBlocked(time: String): Boolean = time.take(5) in _selectedDateBlockedTimes.value

    fun getDayStatus(dateStr: String): String { // "green"=free, "red"=full, "gray"=no activity/past/no laborable
        val today = DateFormatter.getTodayDateString()
        if (dateStr < today) return "gray"
        if (!SlotSchedule.isWorkingDay(dateStr, _workingDays.value)) return "gray" // fin de semana / día no laborable
        // Si es el día que está cargado en _dayAppointments (el seleccionado) y
        // TODOS sus turnos están bloqueados, se ve igual que un día no laborable.
        if (dateStr == _selectedDate.value && _activeSlots.value.isNotEmpty() &&
            _selectedDateBlockedTimes.value.containsAll(_activeSlots.value)
        ) return "gray"

        val dayAppts = _dayAppointments.value.filter { it.appointmentDate == dateStr && it.status != "canceled" }
        val allSlots = _activeSlots.value
        return when {
            dayAppts.size >= allSlots.size -> "red"
            dayAppts.isEmpty() -> "green"
            else -> "green"
        }
    }
}
