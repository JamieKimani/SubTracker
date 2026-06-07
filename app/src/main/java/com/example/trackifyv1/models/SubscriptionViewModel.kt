package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.trackifyv1.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference

    private val _subscriptions = MutableStateFlow<List<SubscriptionModel>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionModel>> = _subscriptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listener: ValueEventListener? = null

    // Auth state listener — re-fetches subscriptions when user signs in
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null && listener == null) {
            fetchSubscriptions()
        } else if (firebaseAuth.currentUser == null) {
            // User signed out — clear data and detach listener
            listener?.let { db.child("Subscriptions").removeEventListener(it) }
            listener = null
            _subscriptions.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        // Also try immediately in case auth is already restored
        if (auth.currentUser != null) fetchSubscriptions()
    }

    private fun userRef() = auth.currentUser?.uid?.let { db.child("Subscriptions").child(it) }

    private fun fetchSubscriptions() {
        val ref = userRef() ?: return
        if (listener != null) return   // already attached
        _isLoading.value = true
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _subscriptions.value = snapshot.children.mapNotNull { child ->
                    child.getValue(SubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
                _isLoading.value = false
            }
            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
            }
        }
        ref.addValueEventListener(listener!!)
    }

    fun addSubscription(
        subscriptionName: String,
        subscriptionAmount: String,
        subscriptionDate: String,
        expiryDate: String,
        reminderDate: String,
        context: Context,
        category: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val ref = userRef()
        if (ref == null) {
            Toast.makeText(context, "You must be logged in to add subscriptions.", Toast.LENGTH_LONG).show()
            return
        }
        when {
            subscriptionName.isBlank() -> {
                Toast.makeText(context, "Please enter a subscription name.", Toast.LENGTH_SHORT).show(); return
            }
            subscriptionAmount.isBlank() -> {
                Toast.makeText(context, "Please enter an amount.", Toast.LENGTH_SHORT).show(); return
            }
            subscriptionAmount.toDoubleOrNull() == null -> {
                Toast.makeText(context, "Amount must be a valid number (e.g. 500 or 9.99).", Toast.LENGTH_SHORT).show(); return
            }
            subscriptionAmount.toDouble() < 0 -> {
                Toast.makeText(context, "Amount cannot be negative.", Toast.LENGTH_SHORT).show(); return
            }
        }

        val id  = ref.push().key ?: run {
            Toast.makeText(context, "Failed to generate ID. Check your connection.", Toast.LENGTH_LONG).show()
            return
        }
        // Store without id field to keep Firebase node clean
        val sub = mapOf(
            "id"                 to id,
            "subscriptionName"   to subscriptionName.trim(),
            "subscriptionAmount" to subscriptionAmount.trim(),
            "subscriptionDate"   to subscriptionDate,
            "expiryDate"         to expiryDate,
            "reminderDate"       to reminderDate,
            "category"           to category
        )

        ref.child(id).setValue(sub)
            .addOnSuccessListener {
                if (reminderDate.isNotBlank()) {
                    NotificationHelper(context).scheduleForSubscription(id, subscriptionName.trim(), reminderDate)
                }
                Toast.makeText(context, "\"${subscriptionName.trim()}\" added successfully!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Failed to save: ${e.message ?: "Check your connection and try again."}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun deleteSubscription(id: String, context: Context? = null) {
        if (id.isBlank()) return
        context?.let { NotificationHelper(it).cancelScheduledNotification(id.hashCode()) }
        userRef()?.child(id)?.removeValue()
            ?.addOnSuccessListener {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Subscription deleted.", Toast.LENGTH_SHORT).show()
                }
            }
            ?.addOnFailureListener {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Could not delete. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun updateSubscription(subscription: SubscriptionModel, context: Context? = null) {
        if (subscription.id.isBlank()) return
        userRef()?.child(subscription.id)?.setValue(subscription)
            ?.addOnSuccessListener {
                context?.let { ctx ->
                    val helper = NotificationHelper(ctx)
                    helper.cancelScheduledNotification(subscription.id.hashCode())
                    if (subscription.reminderDate.isNotBlank()) {
                        helper.scheduleForSubscription(
                            subscription.id,
                            subscription.subscriptionName,
                            subscription.reminderDate
                        )
                    }
                    Toast.makeText(ctx, "Subscription updated!", Toast.LENGTH_SHORT).show()
                }
            }
            ?.addOnFailureListener {
                context?.let { ctx ->
                    Toast.makeText(ctx, "Could not update. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        listener?.let { userRef()?.removeEventListener(it) }
    }
}
