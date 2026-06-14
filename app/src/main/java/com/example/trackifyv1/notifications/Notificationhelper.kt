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

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val renewalChannel = NotificationChannel(
                CHANNEL_RENEWALS, "Subscription Renewals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming subscription renewals"
                enableVibration(true); setShowBadge(true)
            }
            val trialChannel = NotificationChannel(
                CHANNEL_TRIALS, "Free Trial Endings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when free trials are about to end"
                enableVibration(true); setShowBadge(true)
            }
            notificationManager.createNotificationChannel(renewalChannel)
            notificationManager.createNotificationChannel(trialChannel)
        }
    }

    fun sendNotification(title: String, message: String, channel: String = CHANNEL_RENEWALS) {
        createChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        }
        val notification = NotificationCompat.Builder(context, channel)
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
        title: String, message: String,
        reminderTimeMillis: Long,
        requestCode: Int = title.hashCode(),
        channel: String = CHANNEL_RENEWALS
    ) {
        if (reminderTimeMillis <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CHANNEL, channel)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                    try {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    } catch (_: Exception) {
                        Toast.makeText(context, "Grant 'Alarms & Reminders' for accurate reminders.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent) }
            catch (_: Exception) { Toast.makeText(context, "Could not schedule reminder.", Toast.LENGTH_SHORT).show() }
        }
    }

    fun scheduleForSubscription(subscriptionId: String, subscriptionName: String, reminderDateStr: String): Boolean {
        if (reminderDateStr.isBlank()) return false
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(reminderDateStr) ?: return false
            val now  = Calendar.getInstance()
            val cal  = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis) {
                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                if (isToday) {
                    sendNotification("Reminder: $subscriptionName",
                        "Your \"$subscriptionName\" subscription is due today!", CHANNEL_RENEWALS)
                    return true
                }
                return false
            }
            scheduleNotification(
                "Reminder: $subscriptionName",
                "Your \"$subscriptionName\" subscription is due soon!",
                cal.timeInMillis, subscriptionId.hashCode(), CHANNEL_RENEWALS
            )
            true
        } catch (_: Exception) { false }
    }

    fun scheduleTrialEndingNotification(subscriptionId: String, subscriptionName: String, trialEndDateStr: String): Boolean {
        if (trialEndDateStr.isBlank()) return false
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(trialEndDateStr) ?: return false
            val now  = Calendar.getInstance()
            val cal  = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val requestCode = ("trial_" + subscriptionId).hashCode()
            if (cal.timeInMillis <= now.timeInMillis) {
                val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                if (isToday) {
                    sendNotification("⏳ Trial ending: $subscriptionName",
                        "Your free trial for \"$subscriptionName\" ends tomorrow. Cancel now if you don't want to be charged!",
                        CHANNEL_TRIALS)
                    return true
                }
                return false
            }
            scheduleNotification(
                "⏳ Trial ending: $subscriptionName",
                "Your free trial for \"$subscriptionName\" ends on $trialEndDateStr. Cancel now to avoid charges!",
                cal.timeInMillis, requestCode, CHANNEL_TRIALS
            )
            true
        } catch (_: Exception) { false }
    }

    fun cancelScheduledNotification(requestCode: Int) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pending = PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pending?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it); it.cancel()
        }
    }

    companion object {
        const val CHANNEL_RENEWALS  = "trackify_renewals"
        const val CHANNEL_TRIALS    = "trackify_trials"
        const val EXTRA_TITLE       = "title"
        const val EXTRA_MESSAGE     = "message"
        const val EXTRA_CHANNEL     = "channel"
        const val REMINDER_HOUR     = 9
    }
}
