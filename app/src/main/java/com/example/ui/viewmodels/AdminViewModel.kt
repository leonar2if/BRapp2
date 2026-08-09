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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "barberia_cache").build()
    private val productRepo = ProductRepository(db.serviceDao(), db.productDao())
    private val apptRepo = AppointmentRepository()
    private val settingsRepo = SettingsRepository(db.settingsDao())
    private val authRepo = AuthRepository(application)

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

    private val _selectedDate = MutableStateFlow(DateFormatter.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate

    private val _currentTurnIndex = MutableStateFlow(0)
    val currentTurnIndex: StateFlow<Int> = _currentTurnIndex

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds
    private var timerJob: Job? = null

    private val _currentTurnNotes = MutableStateFlow("")
    val currentTurnNotes: StateFlow<String> = _currentTurnNotes

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

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            val today = DateFormatter.getTodayDateString()
            val todayList = apptRepo.fetchAppointmentsByDate(today)
            _todayAppointments.value = todayList.sortedBy { it.appointmentTime }
            
            if (_selectedDate.value == today) {
                _selectedDateAppointments.value = _todayAppointments.value
            } else {
                _selectedDateAppointments.value = apptRepo.fetchAppointmentsByDate(_selectedDate.value)
            }

            val hist = apptRepo.fetchAllAppointments()
            _allAppointments.value = hist.sortedByDescending { it.appointmentDate }

            productRepo.refreshServices()
            productRepo.refreshProducts()
            settingsRepo.refreshSettings()

            _managerPhone.value = settingsRepo.getSettingValue("manager_phone", "34600000000")
            _managerName.value = settingsRepo.getSettingValue("manager_name", "Gestor Rodríguez")
            _storeHours.value = settingsRepo.getSettingValue("store_hours", "Lunes a Viernes 10:00 - 18:00")
            
            _isLoading.value = false
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            _isLoading.value = true
            _selectedDateAppointments.value = apptRepo.fetchAppointmentsByDate(date)
            _isLoading.value = false
        }
    }

    fun startTurnExecution() {
        val list = _todayAppointments.value.filter { it.status == "confirmed" || it.status == "in_progress" }
        if (list.isNotEmpty()) {
            _currentTurnIndex.value = 0
            val currentAppt = list[0]
            _currentTurnNotes.value = currentAppt.notes ?: ""
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value += 1
            }
        }
    }

    fun finalizeCurrentTurn() {
        viewModelScope.launch {
            val list = _todayAppointments.value.filter { it.status == "confirmed" || it.status == "in_progress" }
            if (list.isNotEmpty() && _currentTurnIndex.value < list.size) {
                val current = list[_currentTurnIndex.value]
                val adminId = authRepo.userId.first()
                
                // Save notes
                apptRepo.updateNotes(current.id, _currentTurnNotes.value)
                // Mark attended
                apptRepo.markAsAttended(current.id, adminId)

                // Check next turn
                if (_currentTurnIndex.value + 1 < list.size) {
                    _currentTurnIndex.value += 1
                    val next = list[_currentTurnIndex.value]
                    _currentTurnNotes.value = next.notes ?: ""
                    startTimer()
                } else {
                    timerJob?.cancel()
                }
                refreshData()
            }
        }
    }

    fun updateCurrentNotes(notes: String) {
        _currentTurnNotes.value = notes
        viewModelScope.launch {
            val list = _todayAppointments.value.filter { it.status == "confirmed" || it.status == "in_progress" }
            if (list.isNotEmpty() && _currentTurnIndex.value < list.size) {
                apptRepo.updateNotes(list[_currentTurnIndex.value].id, notes)
            }
        }
    }

    fun rescheduleCurrentToNextMonth() {
        viewModelScope.launch {
            val list = _todayAppointments.value.filter { it.status == "confirmed" || it.status == "in_progress" }
            if (list.isNotEmpty() && _currentTurnIndex.value < list.size) {
                val current = list[_currentTurnIndex.value]
                val adminId = authRepo.userId.first()
                LocalNotificationScheduler.cancelAppointmentReminders(getApplication(), current.id.toString())
                val res = apptRepo.rescheduleNextMonth(current, adminId)
                val newAppt = res.getOrNull()
                if (res.isSuccess && newAppt != null) {
                    val serviceName = allServices.value.find { it.id == newAppt.serviceId }?.name ?: "servicio"
                    LocalNotificationScheduler.createNotificationChannel(getApplication())
                    LocalNotificationScheduler.scheduleAppointmentReminders(
                        context = getApplication(),
                        appointmentId = newAppt.id.toString(),
                        appointmentDate = newAppt.appointmentDate,
                        appointmentTime = newAppt.appointmentTime,
                        clientName = newAppt.fullName,
                        serviceName = serviceName
                    )
                }
                finalizeCurrentTurn()
            }
        }
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
