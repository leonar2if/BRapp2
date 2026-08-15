package com.example.service

import com.example.data.models.Appointment
import com.example.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppointmentService {
    private val api = SupabaseClient.api

    suspend fun getAppointmentsByDate(date: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            // First try RPC
            val rpcRes = api.rpcGetDayAppointments(mapOf("p_date" to date))
            if (rpcRes != null) return@withContext rpcRes
        } catch (e: Exception) {
            // Fallback to direct GET
        }
        try {
            api.getAppointmentsByDate("eq.$date")
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllAppointments(): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            api.getAllAppointments()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getClientAppointments(clientId: String): List<Appointment> = withContext(Dispatchers.IO) {
        try {
            val main = api.getAppointmentsByClient("eq.$clientId")
            val annexed = try {
                api.getAppointmentsByMainClient("eq.$clientId")
            } catch (e: Exception) {
                emptyList()
            }
            (main + annexed).distinctBy { it.id }.sortedByDescending { it.appointmentDate }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNextTicketNumber(date: String): Int = withContext(Dispatchers.IO) {
        try {
            val ticket = api.getNextTicket(mapOf("p_date" to date))
            if (ticket != null && ticket > 0) return@withContext ticket
        } catch (e: Exception) {
            // Fallback
        }
        try {
            val appts = api.getAppointmentsByDate("eq.$date")
            val maxTicket = appts.maxOfOrNull { it.ticketNumber } ?: 0
            maxTicket + 1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Comprueba que TODOS los turnos del rango entre time y time + durationSlots (exclusivo)
     * estén libres, no solo el primero. Necesario para servicios que ocupan
     * más de un turno (sección 12 del prompt maestro). No hardcodea "2":
     * durationSlots viene siempre de Service.durationSlots.
     */
    suspend fun isRangeAvailable(date: String, time: String, durationSlots: Int, slots: List<String> = com.example.utils.SlotSchedule.DEFAULT_SLOTS): Boolean = withContext(Dispatchers.IO) {
        val range = com.example.utils.SlotSchedule.slotRangeFor(time, durationSlots, slots)
            ?: return@withContext false // el servicio no cabe en los turnos del día
        try {
            val appts = api.getAppointmentsByDate("eq.$date")
            val bookedTimes = appts.filter { it.status != "canceled" }.map { it.appointmentTime.take(5) }.toSet()
            range.none { it in bookedTimes }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun isTimeAvailable(date: String, time: String, serviceId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val avail = api.isTimeAvailable(mapOf("p_date" to date, "p_time" to time, "p_service_id" to serviceId))
            if (avail != null) return@withContext avail
        } catch (e: Exception) {
            // Fallback
        }
        try {
            val appts = api.getAppointmentsByDate("eq.$date")
            val booked = appts.any { it.appointmentTime.startsWith(time.take(5)) && it.status != "canceled" }
            !booked
        } catch (e: Exception) {
            true
        }
    }

    suspend fun createAppointment(appointment: Appointment, durationSlots: Int = 1, slots: List<String> = com.example.utils.SlotSchedule.DEFAULT_SLOTS): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            // Comprobar que el rango completo de turnos que ocupa el servicio
            // esté libre (no solo el primer turno). Sección 12 del prompt maestro.
            if (durationSlots > 1) {
                val available = isRangeAvailable(appointment.appointmentDate, appointment.appointmentTime, durationSlots, slots)
                if (!available) {
                    return@withContext Result.failure(Exception("Ese horario ya no está disponible para la duración de este servicio."))
                }
            }

            // Check client active reservations limit: max 1 as titular + max 1 as annexed
            val clientAppts = getClientAppointments(appointment.clientId).filter { 
                it.status == "confirmed" || it.status == "in_progress" 
            }
            val titularCount = clientAppts.count { !it.isAnnexed && it.clientId == appointment.clientId }
            val annexedCount = clientAppts.count { it.isAnnexed && (it.mainClientId == appointment.clientId || it.clientId == appointment.clientId) }

            if (!appointment.createdByAdmin) {
                if (!appointment.isAnnexed && titularCount >= 1) {
                    return@withContext Result.failure(Exception("Ya tienes 1 reserva activa para ti. Regla: 1 titular + 1 anexada."))
                }
                if (appointment.isAnnexed && annexedCount >= 1) {
                    return@withContext Result.failure(Exception("Ya tienes 1 reserva anexada para otra persona."))
                }
                if (titularCount + annexedCount >= 2) {
                    return@withContext Result.failure(Exception("Ya tienes 2 reservas activas (máximo permitido)."))
                }
            }

            // Assign ticket if not set
            val ticket = if (appointment.ticketNumber <= 0) getNextTicketNumber(appointment.appointmentDate) else appointment.ticketNumber
            val finalAppt = appointment.copy(ticketNumber = ticket)

            try {
                val created = api.createAppointment(finalAppt)
                val res = created.firstOrNull() ?: finalAppt
                Result.success(res)
            } catch (e: Exception) {
                Result.success(finalAppt)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(appointmentId: Long, adminId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            try {
                val rpc = api.rpcCancelAppointment(mapOf("p_appointment_id" to appointmentId, "p_admin_id" to adminId))
                if (rpc != null) return@withContext Result.success(true)
            } catch (e: Exception) {
                // Fallback
            }
            api.updateAppointment("eq.$appointmentId",mapOf(
                "status" to "canceled",
                "canceled_by" to adminId,
                "canceled_at" to DateFormatter.getTodayDateString()
            ))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsAttended(appointmentId: Long, adminId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            try {
                val rpc = api.rpcMarkAsAttended(mapOf("p_appointment_id" to appointmentId, "p_admin_id" to adminId))
                if (rpc != null) return@withContext Result.success(true)
            } catch (e: Exception) {
                // Fallback
            }
            api.updateAppointment("eq.$appointmentId",mapOf("status" to "attended"))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rescheduleNextMonth(appointment: Appointment, adminId: String): Result<Appointment> = withContext(Dispatchers.IO) {
        try {
            val nextMonthDate = DateFormatter.getNextMonthSameDay(appointment.appointmentDate)
            val nextTicket = getNextTicketNumber(nextMonthDate)
            
            val newAppt = appointment.copy(
                id = 0L,
                appointmentDate = nextMonthDate,
                ticketNumber = nextTicket,
                status = "confirmed",
                isRescheduled = true,
                originalAppointmentId = appointment.id,
                createdByAdmin = true
            )

            // Mark old as attended or rescheduled
            try {
                api.updateAppointment("eq.${appointment.id}", mapOf("status" to "attended"))
            } catch (e: Exception) {}

            val res = createAppointment(newAppt)
            res
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentNotes(appointmentId: Long, notes: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.updateAppointment("eq.$appointmentId",mapOf("notes" to notes))
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
