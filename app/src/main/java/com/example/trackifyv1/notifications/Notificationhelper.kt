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

    /**
     * Schedule a notification for a subscription.
     * Uses [subscriptionId] as the unique request code so it can be reliably cancelled later.
     */
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
            context,
            requestCode,
            intent,
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
                    // Exact alarms not permitted — fall back to inexact and inform user once
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
                    Toast.makeText(
                        context,
                        "Note: Reminder may arrive slightly later than scheduled. " +
                                "Grant 'Alarms & Reminders' permission in Settings for precise timing.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent)
        }
    }

    /**
     * Schedule using a subscription's reminder date string (DD/MM/YYYY).
     * Returns true if successfully scheduled.
     */
    fun scheduleForSubscription(
        subscriptionId: String,
        subscriptionName: String,
        reminderDateStr: String
    ): Boolean {
        if (reminderDateStr.isBlank()) return false
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(reminderDateStr) ?: return false
            val cal  = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) return false
            scheduleNotification(
                title       = "Reminder: $subscriptionName",
                message     = "Your \"$subscriptionName\" subscription is due soon!",
                reminderTimeMillis = cal.timeInMillis,
                requestCode = subscriptionId.hashCode()
            )
            true
        } catch (_: Exception) { false }
    }

    /**
     * Cancel a previously scheduled notification by request code.
     */
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
    }
}
