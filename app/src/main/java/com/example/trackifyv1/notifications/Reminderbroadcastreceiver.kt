package com.example.trackifyv1.notifications


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper(context).sendNotification(
            intent.getStringExtra("title") ?: "Reminder",
            intent.getStringExtra("message") ?: "You have a reminder."
        )
    }
}