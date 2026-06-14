package com.example.trackifyv1.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title   = intent.getStringExtra(NotificationHelper.EXTRA_TITLE)   ?: "Subscription Reminder"
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE) ?: "One of your subscriptions is due soon!"
        val channel = intent.getStringExtra(NotificationHelper.EXTRA_CHANNEL) ?: NotificationHelper.CHANNEL_RENEWALS
        NotificationHelper(context).sendNotification(title, message, channel)
    }
}
