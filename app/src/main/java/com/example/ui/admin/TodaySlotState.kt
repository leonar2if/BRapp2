package com.example.ui.admin

import com.example.data.models.Appointment
import com.example.data.models.Service
import com.example.utils.DateFormatter
import com.example.utils.SlotSchedule

/**
 * Un "ítem" de la vista Hoy activa: un turno del día (uno de los 12 slots
 * fijos de SlotSchedule), esté ocupado, libre o cancelado. La lista de
 * TodaySlotItem es la fuente única tanto para la vista de lista como para
 * el carrusel de detalle (galería) — ambas recorren exactamente los mismos
 * ítems en el mismo orden.
 */
data class TodaySlotItem(
    val time: String,
    val appointment: Appointment?, // null = libre. Si existe, su status ya NO es "canceled" salvo canceledAppointment.
    val canceledAppointment: Appointment?, // solo si el hueco tiene una cita cancelada (se muestra distinto de un libre real)
    val tag: TagKind,
    val icon: IconKind,
    val isCurrent: Boolean, // borde amarillo
    val showCheckButton: Boolean,
    val isContinuation: Boolean = false // true = este turno lo ocupa una cita de varios turnos que empezó antes (sección 3.3)
)

enum class TagKind { LIBRE, OCUPADO, ACTUAL, PASADO, CANCELADO }
enum class IconKind { NONE, PENDING_CLOCK, CONFIRMED_CHECK, CANCELED_X }

object TodaySlotBuilder {

    /**
     * Arma la lista de 12 ítems del día a partir de las citas de hoy.
     * "Actual" = el último slot cuya hora ya llegó (hora <= ahora). Si aún
     * no llega ningún slot (antes de que abra), no hay actual.
     */
    fun build(
        appointments: List<Appointment>,
        nowTime: String = DateFormatter.getNowTimeString(),
        services: List<Service> = emptyList()
    ): List<TodaySlotItem> {
        val slots = SlotSchedule.DEFAULT_SLOTS
        val currentSlot = slots.lastOrNull { it <= nowTime }

        // Para cada cita activa, calcula TODOS los turnos que ocupa según la
        // duración de su servicio (sección 3.3 / 12): no solo el turno de inicio.
        // slotOwner: turno -> (cita, esEsteElTurnoDeInicio)
        val slotOwner = mutableMapOf<String, Pair<Appointment, Boolean>>()
        appointments.filter { it.status != "canceled" }.forEach { appt ->
            val durationSlots = services.find { it.id == appt.serviceId }?.durationSlots ?: 1
            val range = SlotSchedule.slotRangeFor(appt.appointmentTime.take(5), durationSlots, slots)
                ?: listOf(appt.appointmentTime.take(5))
            range.forEachIndexed { index, s ->
                // Si dos citas se pisaran (no debería pasar, pero por seguridad no
                // pisamos un turno de inicio ya asignado con una continuación de otra).
                if (s !in slotOwner || index == 0) {
                    slotOwner[s] = appt to (index == 0)
                }
            }
        }

        return slots.map { slot ->
            val (active, isStart) = slotOwner[slot]?.let { it.first to it.second } ?: (null to true)
            val canceled = if (active == null) appointments.firstOrNull {
                it.status == "canceled" && it.appointmentTime.take(5) == slot
            } else null
            val isCurrent = slot == currentSlot
            val isPast = currentSlot != null && slot < currentSlot
            val isContinuation = active != null && !isStart

            when {
                isContinuation -> TodaySlotItem( // turno cubierto por una cita de varios turnos que empezó antes
                    time = slot,
                    appointment = active,
                    canceledAppointment = null,
                    tag = if (isCurrent) TagKind.ACTUAL else TagKind.OCUPADO,
                    icon = IconKind.NONE,
                    isCurrent = isCurrent,
                    showCheckButton = false,
                    isContinuation = true
                )
                active != null && active.status == "attended" -> TodaySlotItem(
                    time = slot,
                    appointment = active,
                    canceledAppointment = null,
                    tag = if (isCurrent) TagKind.ACTUAL else TagKind.PASADO,
                    icon = IconKind.CONFIRMED_CHECK,
                    isCurrent = isCurrent,
                    showCheckButton = false
                )
                active != null -> { // confirmed / in_progress, pendiente de atender
                    val tag = when {
                        isCurrent -> TagKind.ACTUAL
                        isPast -> TagKind.PASADO
                        else -> TagKind.OCUPADO
                    }
                    TodaySlotItem(
                        time = slot,
                        appointment = active,
                        canceledAppointment = null,
                        tag = tag,
                        icon = if (isCurrent || isPast) IconKind.PENDING_CLOCK else IconKind.NONE,
                        isCurrent = isCurrent,
                        showCheckButton = isCurrent || isPast
                    )
                }
                canceled != null -> TodaySlotItem(
                    time = slot,
                    appointment = null,
                    canceledAppointment = canceled,
                    tag = TagKind.CANCELADO,
                    icon = IconKind.CANCELED_X,
                    isCurrent = isCurrent,
                    showCheckButton = false
                )
                else -> TodaySlotItem( // libre real
                    time = slot,
                    appointment = null,
                    canceledAppointment = null,
                    tag = if (isPast) TagKind.PASADO else TagKind.LIBRE,
                    icon = IconKind.NONE,
                    isCurrent = isCurrent,
                    showCheckButton = false
                )
            }
        }
    }

    /** Texto "en 45 min" / "en 1h 30min" hasta la próxima cita reservada (no cancelada) a partir de un slot libre. */
    fun nextAppointmentLabel(items: List<TodaySlotItem>, fromSlot: String, nowTime: String = DateFormatter.getNowTimeString()): String {
        val next = items.firstOrNull {
            it.appointment != null && it.appointment.status != "attended" && it.time > fromSlot
        } ?: items.firstOrNull {
            it.appointment != null && it.appointment.status != "attended" && it.time > nowTime
        }
        if (next == null) return "No hay más turnos reservados por hoy"

        val diffMin = timeDiffMinutes(nowTime, next.time)
        return if (diffMin <= 0) {
            "Tu próximo turno es ahora — ${next.time}"
        } else if (diffMin < 60) {
            "Tu próximo turno es en $diffMin min (${next.time})"
        } else {
            val h = diffMin / 60
            val m = diffMin % 60
            if (m == 0) "Tu próximo turno es en ${h}h (${next.time})"
            else "Tu próximo turno es en ${h}h ${m}min (${next.time})"
        }
    }

    private fun timeDiffMinutes(from: String, to: String): Int {
        return try {
            val (fh, fm) = from.split(":").map { it.toInt() }
            val (th, tm) = to.split(":").map { it.toInt() }
            (th * 60 + tm) - (fh * 60 + fm)
        } catch (e: Exception) {
            0
        }
    }
}
