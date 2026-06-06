package com.example.trackifyv1.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Firebase may not be fully initialised right after boot; catch any init error
        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) { null } ?: return

        val ref    = FirebaseDatabase.getInstance().getReference("Subscriptions").child(uid)
        val helper = NotificationHelper(context)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val id       = child.key ?: return@forEach
                    val name     = child.child("subscriptionName").getValue(String::class.java) ?: return@forEach
                    val reminder = child.child("reminderDate").getValue(String::class.java) ?: return@forEach
                    helper.scheduleForSubscription(id, name, reminder)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
