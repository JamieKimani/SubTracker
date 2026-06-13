package com.example.trackifyv1.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.trackifyv1.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Subscription Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming subscription renewals"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(title: String, message: String) {
        createChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun scheduleNotification(
        title: String,
        message: String,
        reminderTimeMillis: Long,
        requestCode: Int = title.hashCode()
    ) {

        if (reminderTimeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent
                    )
                } else {

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent
                    )
                    try {
                        val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(settingsIntent)
                    } catch (_: Exception) {
                        Toast.makeText(
                            context,
                            "Grant 'Alarms & Reminders' permission in Settings for accurate reminders.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent
                )
            } catch (_: Exception) {
                Toast.makeText(context, "Could not schedule reminder: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun scheduleForSubscription(
        subscriptionId: String,
        subscriptionName: String,
        reminderDateStr: String
    ): Boolean {
        if (reminderDateStr.isBlank()) return false
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(reminderDateStr) ?: return false
            val now  = Calendar.getInstance()
            val cal  = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (cal.timeInMillis <= now.timeInMillis) {

                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                val isPast  = cal.before(now)
                if (isToday || isPast) {
                    sendNotification(
                        title   = "Reminder: $subscriptionName",
                        message = "Your \"$subscriptionName\" subscription is due today!"
                    )
                    return true
                }
                return false
            }

            scheduleNotification(
                title              = "Reminder: $subscriptionName",
                message            = "Your \"$subscriptionName\" subscription is due soon!",
                reminderTimeMillis = cal.timeInMillis,
                requestCode        = subscriptionId.hashCode()
            )
            true
        } catch (_: Exception) { false }
    }

    fun scheduleTrialEndingNotification(
        subscriptionId: String,
        subscriptionName: String,
        trialEndDateStr: String
    ): Boolean {
        if (trialEndDateStr.isBlank()) return false
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(trialEndDateStr) ?: return false
            val now  = Calendar.getInstance()
            val cal  = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, TRIAL_REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)

            val requestCode = ("trial_" + subscriptionId).hashCode()

            if (cal.timeInMillis <= now.timeInMillis) {
                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                if (isToday) {
                    sendNotification(
                        title   = "⏳ Trial ending: $subscriptionName",
                        message = "Your free trial for \"$subscriptionName\" ends on $trialEndDateStr. Cancel now if you don't want to be charged!"
                    )
                    return true
                }
                return false
            }

            scheduleNotification(
                title              = "⏳ Trial ending: $subscriptionName",
                message            = "Your free trial for \"$subscriptionName\" ends tomorrow ($trialEndDateStr). Cancel now if you don't want to be charged!",
                reminderTimeMillis = cal.timeInMillis,
                requestCode        = requestCode
            )
            true
        } catch (_: Exception) { false }
    }

    fun cancelScheduledNotification(requestCode: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    companion object {
        const val CHANNEL_ID    = "trackify_reminders"
        const val EXTRA_TITLE   = "title"
        const val EXTRA_MESSAGE = "message"
        const val REMINDER_HOUR = 9
        const val TRIAL_REMINDER_HOUR = 9
    }
}
