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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubscriptionViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").reference

    private val _subscriptions = MutableStateFlow<List<SubscriptionModel>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionModel>> = _subscriptions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var listener: ValueEventListener? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { fa ->
        if (fa.currentUser != null && listener == null) attach()
        else if (fa.currentUser == null) {
            detach()
            _subscriptions.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        if (auth.currentUser != null) attach()
    }

    private fun userRef() = auth.currentUser?.uid?.let { db.child("Subscriptions").child(it) }

    private fun attach() {
        val ref = userRef() ?: return
        if (listener != null) return
        ref.keepSynced(true)
        _isLoading.value = true
        listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                _subscriptions.value = snap.children.mapNotNull { child ->
                    child.getValue(SubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
                _isLoading.value = false
            }
            override fun onCancelled(e: DatabaseError) { _isLoading.value = false }
        }
        ref.addValueEventListener(listener!!)
    }

    private fun detach() {
        listener?.let { userRef()?.removeEventListener(it) }
        listener = null
    }

    fun addSubscription(
        subscriptionName: String,
        subscriptionAmount: String,
        subscriptionDate: String,
        expiryDate: String,
        reminderDate: String,
        context: Context,
        category: String = "",
        billingCycle: String = "Monthly",
        onSuccess: () -> Unit = {}
    ) {
        val ref = userRef() ?: run { toast(context, "You must be logged in."); return }
        when {
            subscriptionName.isBlank()              -> { toast(context, "Please enter a subscription name."); return }
            subscriptionAmount.isBlank()            -> { toast(context, "Please enter an amount."); return }
            subscriptionAmount.toDoubleOrNull() == null -> { toast(context, "Amount must be a valid number."); return }
            (subscriptionAmount.toDoubleOrNull() ?: 0.0) < 0 -> { toast(context, "Amount cannot be negative."); return }
        }
        val id  = ref.push().key ?: run { toast(context, "Connection error. Try again."); return }
        val sub = SubscriptionModel(
            id                 = id,
            subscriptionName   = subscriptionName.trim(),
            subscriptionAmount = subscriptionAmount.trim(),
            subscriptionDate   = subscriptionDate,
            expiryDate         = expiryDate,
            reminderDate       = reminderDate,
            category           = category,
            billingCycle       = billingCycle,
            isActive           = true
        )
        ref.child(id).setValue(sub)
            .addOnSuccessListener {
                if (reminderDate.isNotBlank())
                    NotificationHelper(context).scheduleForSubscription(id, subscriptionName.trim(), reminderDate)
                toast(context, "\"${subscriptionName.trim()}\" added!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                val msg = if (e.message?.contains("Permission denied", true) == true)
                    "Permission denied. Log out and log in again."
                else "Failed to save. Check your connection."
                toast(context, msg)
            }
    }

    fun deleteSubscription(id: String, context: Context? = null) {
        if (id.isBlank()) return
        context?.let { NotificationHelper(it).cancelScheduledNotification(id.hashCode()) }
        userRef()?.child(id)?.removeValue()
            ?.addOnSuccessListener { context?.let { toast(it, "Subscription deleted.") } }
            ?.addOnFailureListener { context?.let { toast(it, "Could not delete. Try again.") } }
    }

    fun updateSubscription(sub: SubscriptionModel, context: Context? = null) {
        if (sub.id.isBlank()) return
        userRef()?.child(sub.id)?.setValue(sub)
            ?.addOnSuccessListener {
                context?.let { ctx ->
                    val h = NotificationHelper(ctx)
                    h.cancelScheduledNotification(sub.id.hashCode())
                    if (sub.reminderDate.isNotBlank())
                        h.scheduleForSubscription(sub.id, sub.subscriptionName, sub.reminderDate)
                    toast(ctx, "Subscription updated!")
                }
            }
            ?.addOnFailureListener { context?.let { toast(it, "Could not update. Try again.") } }
    }

    fun renewSubscription(sub: SubscriptionModel, context: Context) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        fun advance(dateStr: String): String {
            if (dateStr.isBlank()) return dateStr
            return try {
                val cal = Calendar.getInstance().apply { time = sdf.parse(dateStr)!! }
                when (sub.billingCycle) {
                    "Weekly"    -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "Quarterly" -> cal.add(Calendar.MONTH, 3)
                    "Yearly"    -> cal.add(Calendar.YEAR, 1)
                    else        -> cal.add(Calendar.MONTH, 1)
                }
                sdf.format(cal.time)
            } catch (_: Exception) { dateStr }
        }
        updateSubscription(sub.copy(expiryDate = advance(sub.expiryDate), reminderDate = advance(sub.reminderDate)), context)
    }

    fun toggleActive(sub: SubscriptionModel, context: Context) =
        updateSubscription(sub.copy(isActive = !sub.isActive), context)

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        detach()
    }

    private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
