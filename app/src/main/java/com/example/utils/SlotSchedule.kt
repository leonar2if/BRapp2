package com.example.utils

import java.util.Calendar

/**
 * Fuente única de verdad para:
 * - Los 12 turnos oficiales diarios (sección 4 del prompt maestro; turno 5 =
 *   13:00, no 13:30, según lo solicitado por el administrador).
 * - Qué días de la semana son laborables (sección 5 y 16).
 * - El rango navegable del calendario: mes actual + siguiente (sección 6).
 *
 * IMPORTANTE: esta lista está preparada para leerse en el futuro desde
 * Supabase (tabla `settings`, claves 'slot_definitions' / 'working_days',
 * ver supabase_update_bloque1.sql) sin cambiar la arquitectura — hoy se usa
 * un valor por defecto en código porque la app no tenía antes un mecanismo
 * de horarios configurables, pero DEFAULT_SLOTS / DEFAULT_WORKING_DAYS son
 * el único lugar que habría que sustituir por la config remota; nada más
 * del código depende de que sean exactamente estos 12 valores.
 */
object SlotSchedule {

    /** Los 12 turnos oficiales, en orden. NO agregar/quitar sin que el negocio lo pida. */
    val DEFAULT_SLOTS: List<String> = listOf(
        "10:00", "10:30", "11:00", "11:30",
        "13:00", "14:00", "14:30", "15:00",
        "16:00", "16:30", "17:00", "17:30"
    )

    /** Días laborables por defecto: lunes a viernes. Sábado y domingo, no laborables. */
    val DEFAULT_WORKING_DAYS: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
    )

    fun isWorkingDay(dateStr: String, workingDays: Set<Int> = DEFAULT_WORKING_DAYS): Boolean {
        val date = DateFormatter.stringToDate(dateStr) ?: return false
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(Calendar.DAY_OF_WEEK) in workingDays
    }

    private val DAY_CODE_TO_CALENDAR = mapOf(
        "SUN" to Calendar.SUNDAY, "MON" to Calendar.MONDAY, "TUE" to Calendar.TUESDAY,
        "WED" to Calendar.WEDNESDAY, "THU" to Calendar.THURSDAY, "FRI" to Calendar.FRIDAY,
        "SAT" to Calendar.SATURDAY
    )

    /**
     * Convierte el CSV guardado en settings.working_days (ej. "MON,TUE,WED,
     * THU,FRI") al Set<Int> de Calendar.DAY_OF_WEEK que usa isWorkingDay.
     * Si el CSV viene vacío o corrupto, cae en DEFAULT_WORKING_DAYS para no
     * dejar la app sin días laborables por un dato mal guardado.
     */
    fun parseWorkingDaysCsv(csv: String): Set<Int> {
        val parsed = csv.split(",")
            .map { it.trim().uppercase() }
            .mapNotNull { DAY_CODE_TO_CALENDAR[it] }
            .toSet()
        return if (parsed.isEmpty()) DEFAULT_WORKING_DAYS else parsed
    }

    /**
     * Rango de meses navegables: mes actual y el siguiente, nada más
     * (sección 6). Devuelve pares (año, mes 0-indexado) válidos.
     */
    fun navigableMonths(): List<Pair<Int, Int>> {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)
        val next = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }
        return listOf(
            currentYear to currentMonth,
            next.get(Calendar.YEAR) to next.get(Calendar.MONTH)
        )
    }

    fun isMonthNavigable(year: Int, month: Int): Boolean {
        return navigableMonths().any { it.first == year && it.second == month }
    }

    /**
     * Índices de slot [startIndex, startIndex + durationSlots) para un
     * servicio que empieza en `time`. Devuelve null si `time` no es un
     * turno válido o si el servicio se saldría del día.
     * No hardcodea "2 turnos": la duración viene de Service.durationSlots.
     */
    fun slotRangeFor(time: String, durationSlots: Int, slots: List<String> = DEFAULT_SLOTS): List<String>? {
        val startIndex = slots.indexOf(time)
        if (startIndex == -1) return null
        val endIndexExclusive = startIndex + durationSlots
        if (endIndexExclusive > slots.size) return null
        return slots.subList(startIndex, endIndexExclusive)
    }
}
