package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.data.database.AppDatabase
import com.example.data.database.PreferencesManager
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.AuthRepository
import com.example.utils.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reschedules appointment reminder alarms after a device reboot.
 *
 * AlarmManager alarms set via setExactAndAllowWhileIdle/setAndAllowWhileIdle do NOT
 * survive a reboot, so without this the 24h/1h reminders silently stop firing for
 * any appointment that was booked before the device restarted.
 *
 * This is a best-effort background refresh: it restores the persisted session,
 * pulls the user's upcoming appointments from Supabase, and re-schedules local
 * notifications for the ones still in the future. Any failure (offline, no saved
 * session, expired refresh token, request error) is swallowed on purpose - reminders
 * will simply be re-synced the next time the app is opened normally.
 */
class BootReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        // BOOT_COMPLETED receivers get a very short execution window before the
        // system may kill the process, so we use goAsync() + a hard timeout instead
        // of blocking onReceive() with a suspend call.
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                withTimeoutOrNull(9_000L) {
                    rescheduleReminders(appContext)
                }
            } catch (e: Exception) {
                // Best-effort only.
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleReminders(context: Context) {
        val prefs = PreferencesManager(context)
        val userId = prefs.userId.first()
        if (userId.isBlank()) return // nobody logged in, nothing to reschedule

        // The in-memory Supabase access token does not survive a reboot either,
        // so we need a valid token before we can call the API (see P1).
        val authRepo = AuthRepository(context)
        val sessionRestored = authRepo.restoreSession()
        if (!sessionRestored) return

        val role = prefs.userRole.first()
        val appointmentRepository = AppointmentRepository()
        val appointments = if (role == "admin") {
            appointmentRepository.fetchAllAppointments()
        } else {
            appointmentRepository.fetchClientAppointments(userId)
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, "barberia_cache").build()
        val serviceNamesById = try {
            db.serviceDao().getAllServices().first().associateBy({ it.id }, { it.name })
        } catch (e: Exception) {
            emptyMap()
        }

        val today = DateFormatter.getTodayDateString()
        appointments
            .filter { it.status == "confirmed" && it.appointmentDate >= today }
            .forEach { appointment ->
                LocalNotificationScheduler.scheduleAppointmentReminders(
                    context = context,
                    appointmentId = appointment.id.toString(),
                    appointmentDate = appointment.appointmentDate,
                    appointmentTime = appointment.appointmentTime,
                    clientName = appointment.fullName,
                    serviceName = serviceNamesById[appointment.serviceId] ?: "servicio"
                )
            }
    }
}
