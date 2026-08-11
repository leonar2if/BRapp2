package com.example.data.repository

import com.example.data.models.Appointment
import com.example.service.AppointmentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppointmentRepository {
    private val appointmentService = AppointmentService()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    suspend fun fetchAppointmentsByDate(date: String): List<Appointment> {
        val list = appointmentService.getAppointmentsByDate(date)
        return list
    }

    suspend fun fetchAllAppointments(): List<Appointment> {
        val list = appointmentService.getAllAppointments()
        _appointments.value = list
        return list
    }

    suspend fun fetchClientAppointments(clientId: String): List<Appointment> {
        val list = appointmentService.getClientAppointments(clientId)
        _appointments.value = list
        return list
    }

    suspend fun createAppointment(appointment: Appointment, durationSlots: Int = 1): Result<Appointment> {
        val res = appointmentService.createAppointment(appointment, durationSlots)
        val newAppt = res.getOrNull()
        if (res.isSuccess && newAppt != null) {
            _appointments.value = _appointments.value + newAppt
        }
        return res
    }

    suspend fun cancelAppointment(appointmentId: Long, adminId: String): Result<Boolean> {
        val res = appointmentService.cancelAppointment(appointmentId, adminId)
        if (res.isSuccess) {
            _appointments.value = _appointments.value.map {
                if (it.id == appointmentId) it.copy(status = "canceled", canceledBy = adminId) else it
            }
        }
        return res
    }

    suspend fun markAsAttended(appointmentId: Long, adminId: String): Result<Boolean> {
        val res = appointmentService.markAsAttended(appointmentId, adminId)
        if (res.isSuccess) {
            _appointments.value = _appointments.value.map {
                if (it.id == appointmentId) it.copy(status = "attended") else it
            }
        }
        return res
    }

    suspend fun rescheduleNextMonth(appointment: Appointment, adminId: String): Result<Appointment> {
        return appointmentService.rescheduleNextMonth(appointment, adminId)
    }

    suspend fun updateNotes(appointmentId: Long, notes: String): Result<Boolean> {
        return appointmentService.updateAppointmentNotes(appointmentId, notes)
    }

    suspend fun isTimeAvailable(date: String, time: String, serviceId: Long): Boolean {
        return appointmentService.isTimeAvailable(date, time, serviceId)
    }
}
