package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schedules local notifications for appointment reminders
 * Notifications are sent 24 hours before and 1 hour before the appointment
 */
object LocalNotificationScheduler {
    
    const val CHANNEL_ID = "barberia_appointments"
    private const val CHANNEL_NAME = "Recordatorios de Citas"
    
    // Notification IDs (24h and 1h before)
    private const val NOTIFICATION_ID_24H = 1000
    private const val NOTIFICATION_ID_1H = 2000
    
    // Intent actions
    const val ACTION_24H_REMINDER = "com.example.ACTION_24H_REMINDER"
    const val ACTION_1H_REMINDER = "com.example.ACTION_1H_REMINDER"
    const val EXTRA_APPOINTMENT_ID = "appointment_id"
    const val EXTRA_CLIENT_NAME = "client_name"
    const val EXTRA_APPOINTMENT_TIME = "appointment_time"
    const val EXTRA_SERVICE_NAME = "service_name"
    
    /**
     * Create notification channel for Android 8+
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de citas de la barbería"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Schedule notifications for an appointment
     * @param appointmentId Unique ID of the appointment
     * @param appointmentDate Date of appointment (format: yyyy-MM-dd)
     * @param appointmentTime Time of appointment (format: HH:mm)
     * @param clientName Name of the client
     * @param serviceName Name of the service
     */
    fun scheduleAppointmentReminders(
        context: Context,
        appointmentId: String,
        appointmentDate: String,
        appointmentTime: String,
        clientName: String,
        serviceName: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Parse appointment datetime
        val dateTime = parseDateTime(appointmentDate, appointmentTime)
        if (dateTime == null) return
        
        val calendar24h = Calendar.getInstance().apply {
            time = dateTime
            add(Calendar.HOUR_OF_DAY, -24) // 24 hours before
        }
        
        val calendar1h = Calendar.getInstance().apply {
            time = dateTime
            add(Calendar.HOUR_OF_DAY, -1) // 1 hour before
        }
        
        val now = System.currentTimeMillis()
        
        // Schedule 24h notification if in the future
        if (calendar24h.timeInMillis > now) {
            scheduleNotification(
                context,
                alarmManager,
                NOTIFICATION_ID_24H + appointmentId.hashCode(),
                ACTION_24H_REMINDER,
                calendar24h,
                appointmentId,
                clientName,
                appointmentTime,
                serviceName
            )
        }
        
        // Schedule 1h notification if in the future
        if (calendar1h.timeInMillis > now) {
            scheduleNotification(
                context,
                alarmManager,
                NOTIFICATION_ID_1H + appointmentId.hashCode(),
                ACTION_1H_REMINDER,
                calendar1h,
                appointmentId,
                clientName,
                appointmentTime,
                serviceName
            )
        }
    }
    
    /**
     * Cancel notifications for an appointment
     */
    fun cancelAppointmentReminders(
        context: Context,
        appointmentId: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationId24h = NOTIFICATION_ID_24H + appointmentId.hashCode()
        val notificationId1h = NOTIFICATION_ID_1H + appointmentId.hashCode()
        
        cancelNotification(context, alarmManager, notificationId24h, ACTION_24H_REMINDER)
        cancelNotification(context, alarmManager, notificationId1h, ACTION_1H_REMINDER)
    }
    
    private fun scheduleNotification(
        context: Context,
        alarmManager: AlarmManager,
        notificationId: Int,
        action: String,
        calendar: Calendar,
        appointmentId: String,
        clientName: String,
        appointmentTime: String,
        serviceName: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
            putExtra(EXTRA_CLIENT_NAME, clientName)
            putExtra(EXTRA_APPOINTMENT_TIME, appointmentTime)
            putExtra(EXTRA_SERVICE_NAME, serviceName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
    
    private fun cancelNotification(
        context: Context,
        alarmManager: AlarmManager,
        notificationId: Int,
        action: String
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    private fun parseDateTime(date: String, time: String): Date? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            format.parse("$date $time")
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * BroadcastReceiver that fires when an alarm goes off
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LocalNotificationScheduler.createNotificationChannel(context)
        
        val clientName = intent.getStringExtra(LocalNotificationScheduler.EXTRA_CLIENT_NAME) ?: "Cliente"
        val appointmentTime = intent.getStringExtra(LocalNotificationScheduler.EXTRA_APPOINTMENT_TIME) ?: ""
        val serviceName = intent.getStringExtra(LocalNotificationScheduler.EXTRA_SERVICE_NAME) ?: "servicio"
        
        val (title, message) = when (intent.action) {
            LocalNotificationScheduler.ACTION_24H_REMINDER -> {
                val timeFormatted = formatTimeForDisplay(appointmentTime)
                "⏰ Recordatorio de Cita" to 
                "Hola $clientName! Mañana tienes cita a las $timeFormatted para $serviceName. ¡Te esperamos!"
            }
            LocalNotificationScheduler.ACTION_1H_REMINDER -> {
                val timeFormatted = formatTimeForDisplay(appointmentTime)
                val arrivalTime = calculateArrivalTime(appointmentTime)
                "🚨 Tu cita es pronto!" to 
                "$clientName, tu cita es a las $timeFormatted. Recuerda estar en el local a las $arrivalTime (5 min antes). ¡Te esperamos!"
            }
            else -> return
        }
        
        showNotification(context, title, message)
    }
    
    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(context, LocalNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun formatTimeForDisplay(time: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = inputFormat.parse(time)
            if (date != null) outputFormat.format(date) else time
        } catch (e: Exception) {
            time
        }
    }
    
    private fun calculateArrivalTime(appointmentTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = inputFormat.parse(appointmentTime)
            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                calendar.add(Calendar.MINUTE, -5)
                outputFormat.format(calendar.time)
            } else {
                appointmentTime
            }
        } catch (e: Exception) {
            appointmentTime
        }
    }
}
