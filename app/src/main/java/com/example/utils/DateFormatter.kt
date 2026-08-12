package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val timeSecondsFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val displayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayMonthYear = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private val displayDayName = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
    private val time12hFormat = SimpleDateFormat("h:mm a", Locale("es", "ES"))

    /** "13:00" -> "1:00 PM", "09:30" -> "9:30 AM". Solo para mostrar; la lógica de
     * turnos sigue comparando el string original "HH:mm" en todas partes. */
    fun formatTimeForDisplay(time: String): String {
        return try {
            val parsed = timeFormat.parse(time.take(5)) ?: return time
            time12hFormat.format(parsed).uppercase(Locale("es", "ES"))
        } catch (e: Exception) {
            time
        }
    }

    fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun getNowTimeString(): String {
        return timeFormat.format(Date())
    }

    fun formatDateForDisplay(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr)
            if (date != null) displayDate.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDayName(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr)
            if (date != null) displayDayName.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatMonthYear(calendar: Calendar): String {
        return displayMonthYear.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    fun getDaysInMonth(year: Int, month: Int): List<Date> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val days = mutableListOf<Date>()
        for (day in 1..maxDays) {
            calendar.set(year, month, day)
            days.add(calendar.time)
        }
        return days
    }

    fun dateToString(date: Date): String {
        return dateFormat.format(date)
    }

    fun stringToDate(dateStr: String): Date? {
        return try {
            dateFormat.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getNextMonthSameDay(currentDateStr: String): String {
        return try {
            val date = dateFormat.parse(currentDateStr) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.WEEK_OF_YEAR, 4)
            dateFormat.format(calendar.time)
        } catch (e: Exception) {
            getTodayDateString()
        }
    }

    fun generateTimeSlots(startHour: Int = 10, endHour: Int = 18, intervalMinutes: Int = 30): List<String> {
        val slots = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val endCalendar = Calendar.getInstance()
        endCalendar.set(Calendar.HOUR_OF_DAY, endHour)
        endCalendar.set(Calendar.MINUTE, 0)
        endCalendar.set(Calendar.SECOND, 0)

        while (calendar.before(endCalendar)) {
            slots.add(timeFormat.format(calendar.time))
            calendar.add(Calendar.MINUTE, intervalMinutes)
        }
        return slots
    }
}
