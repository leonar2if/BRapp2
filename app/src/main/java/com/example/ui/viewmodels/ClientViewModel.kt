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
import com.example.utils.ErrorMessages
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "barberia_cache").build()
    private val productRepo = ProductRepository(db.serviceDao(), db.productDao())
    private val apptRepo = AppointmentRepository()
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

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = authRepo.userId.first()
            if (userId.isNotEmpty()) {
                val appts = apptRepo.fetchClientAppointments(userId)
                _clientAppointments.value = appts
            }
            productRepo.refreshServices()
            productRepo.refreshProducts()
            settingsRepo.refreshSettings()
            
            val phone = settingsRepo.getSettingValue("manager_phone", "34600000000")
            _managerPhone.value = phone
            val hours = settingsRepo.getSettingValue("store_hours", "10:00 - 18:00")
            _storeHours.value = hours
            
            fetchDaySlots(_selectedDate.value)
            _isLoading.value = false
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _selectedTime.value = ""
        fetchDaySlots(date)
    }

    private fun fetchDaySlots(date: String) {
        viewModelScope.launch {
            val appts = apptRepo.fetchAppointmentsByDate(date)
            _dayAppointments.value = appts
        }
    }

    fun selectTime(time: String) {
        _selectedTime.value = time
        _bookingStep.value = 2 // Move to services selector
    }

    fun selectService(service: Service) {
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

            val appt = Appointment(
                clientId = if (isForOther) "annexed_$userId" else userId,
                serviceId = service.id,
                fullName = if (isForOther) otherName else userFullName,
                lastName1 = if (isForOther) otherLastName1 else null,
                lastName2 = if (isForOther) otherLastName2 else null,
                phone = if (isForOther) otherPhone else userPhone,
                isAnnexed = isForOther,
                mainClientId = if (isForOther) userId else null,
                appointmentDate = _selectedDate.value,
                appointmentTime = _selectedTime.value,
                status = "confirmed",
                createdByAdmin = false
            )

            val res = apptRepo.createAppointment(appt)
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
                _bookingError.value = ErrorMessages.humanize(res.exceptionOrNull())
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
        val prefix = time.take(5)
        return appts.any { 
            it.appointmentTime.startsWith(prefix) && it.status != "canceled"
        }
    }

    fun getDayStatus(dateStr: String): String { // "green"=free, "red"=full, "gray"=no activity/past
        val today = DateFormatter.getTodayDateString()
        if (dateStr < today) return "gray"
        if (DateFormatter.isWeekend(dateStr)) return "gray"

        // Check if store marked as day off in settings
        val dayAppts = _dayAppointments.value.filter { it.appointmentDate == dateStr && it.status != "canceled" }
        val allSlots = DateFormatter.OFFICIAL_TIME_SLOTS
        return when {
            dayAppts.size >= allSlots.size -> "red"
            dayAppts.isEmpty() -> "green"
            else -> "green"
        }
    }
}
