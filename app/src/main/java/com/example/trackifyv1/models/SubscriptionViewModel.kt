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

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        if (fa.currentUser != null && listener == null) attach()
        else if (fa.currentUser == null) { detach(); _subscriptions.value = emptyList() }
    }

    init {
        auth.addAuthStateListener(authListener)
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
        isTrial: Boolean = false,
        trialEndDate: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val ref = userRef() ?: run { toast(context, "You must be logged in."); return }
        when {
            subscriptionName.isBlank()                       -> { toast(context, "Enter a subscription name."); return }
            subscriptionAmount.isBlank()                     -> { toast(context, "Enter an amount."); return }
            subscriptionAmount.toDoubleOrNull() == null      -> { toast(context, "Amount must be a valid number."); return }
            (subscriptionAmount.toDoubleOrNull() ?: 0.0) < 0 -> { toast(context, "Amount cannot be negative."); return }
        }
        val id  = ref.push().key ?: run { toast(context, "Connection error. Try again."); return }
        val sub = SubscriptionModel(
            id = id, subscriptionName = subscriptionName.trim(),
            subscriptionAmount = subscriptionAmount.trim(), subscriptionDate = subscriptionDate,
            expiryDate = expiryDate, reminderDate = reminderDate,
            category = category, billingCycle = billingCycle, isActive = true,
            isTrial = isTrial, trialEndDate = trialEndDate
        )
        ref.child(id).setValue(sub)
            .addOnSuccessListener {
                val helper = NotificationHelper(context)
                if (reminderDate.isNotBlank())
                    helper.scheduleForSubscription(id, subscriptionName.trim(), reminderDate)
                if (isTrial && trialEndDate.isNotBlank())
                    helper.scheduleTrialEndingNotification(id, subscriptionName.trim(), trialEndDate)
                toast(context, "\"${subscriptionName.trim()}\" added!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                toast(context, if (e.message?.contains("Permission denied", true) == true)
                    "Permission denied. Log out and back in." else "Failed to save. Check connection.")
            }
    }

    fun deleteSubscription(id: String, context: Context? = null) {
        if (id.isBlank()) return
        _subscriptions.value = _subscriptions.value.filter { it.id != id }
        context?.let { NotificationHelper(it).cancelScheduledNotification(id.hashCode()) }
        userRef()?.child(id)?.removeValue()
            ?.addOnFailureListener {
                context?.let { toast(it, "Could not delete from server. Try again.") }
            }
    }

    fun updateSubscription(sub: SubscriptionModel, context: Context? = null) {
        if (sub.id.isBlank()) return
        _subscriptions.value = _subscriptions.value.map { if (it.id == sub.id) sub else it }
        userRef()?.child(sub.id)?.setValue(sub)
            ?.addOnSuccessListener {
                context?.let { ctx ->
                    val h = NotificationHelper(ctx)
                    h.cancelScheduledNotification(sub.id.hashCode())
                    h.cancelScheduledNotification(("trial_" + sub.id).hashCode())
                    if (sub.reminderDate.isNotBlank())
                        h.scheduleForSubscription(sub.id, sub.subscriptionName, sub.reminderDate)
                    if (sub.isTrial && sub.trialEndDate.isNotBlank())
                        h.scheduleTrialEndingNotification(sub.id, sub.subscriptionName, sub.trialEndDate)
                    toast(ctx, "Updated!")
                }
            }
            ?.addOnFailureListener { context?.let { toast(it, "Could not update. Try again.") } }
    }

    fun renewSubscription(sub: SubscriptionModel, context: Context, newAmount: String? = null) {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        fun advance(d: String): String {
            if (d.isBlank()) return d
            return try {
                val cal = Calendar.getInstance().apply { time = sdf.parse(d)!! }
                when (sub.billingCycle) {
                    "Weekly"    -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    "Quarterly" -> cal.add(Calendar.MONTH, 3)
                    "Yearly"    -> cal.add(Calendar.YEAR, 1)
                    else        -> cal.add(Calendar.MONTH, 1)
                }
                sdf.format(cal.time)
            } catch (_: Exception) { d }
        }

        val today = sdf.format(Calendar.getInstance().time)
        val priceChanged = newAmount != null && newAmount.isNotBlank() &&
            newAmount.toDoubleOrNull() != null &&
            newAmount.trim() != sub.subscriptionAmount.trim()

        val updatedHistory = if (priceChanged) {
            sub.priceHistory + (today to sub.subscriptionAmount)
        } else sub.priceHistory

        updateSubscription(
            sub.copy(
                expiryDate         = advance(sub.expiryDate),
                reminderDate       = advance(sub.reminderDate),
                subscriptionAmount = if (priceChanged) newAmount!!.trim() else sub.subscriptionAmount,
                priceHistory       = updatedHistory
            ),
            context
        )

        if (priceChanged) {
            val old = sub.subscriptionAmount.toDoubleOrNull() ?: 0.0
            val new = newAmount!!.toDoubleOrNull() ?: 0.0
            val direction = if (new > old) "increased" else "decreased"
            toast(context, "Price $direction: KES ${sub.subscriptionAmount} → KES ${newAmount.trim()}")
        }
    }

    fun toggleActive(sub: SubscriptionModel, context: Context) =
        updateSubscription(sub.copy(isActive = !sub.isActive), context)

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        detach()
    }

    fun exportToCsv(context: Context) {
        val subs = _subscriptions.value
        if (subs.isEmpty()) { toast(context, "No subscriptions to export."); return }
        try {
            val fileName = "trackify_${System.currentTimeMillis()}.csv"
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            dir.mkdirs()
            val file   = java.io.File(dir, fileName)
            val sb     = StringBuilder()
            sb.appendLine("Name,Amount,Cycle,Category,Start,Expiry,Status,Trial,Trial End,Price History")
            subs.forEach { sub ->
                val history = sub.priceHistory.entries
                    .sortedByDescending { entry -> entry.key }
                    .joinToString("; ") { entry -> "${entry.key}:${entry.value}" }
                val status  = if (sub.isActive) "Active" else "Paused"
                val trial   = if (sub.isTrial) "Yes" else "No"
                sb.append("\"${sub.subscriptionName}\",")
                sb.append("${sub.subscriptionAmount},${sub.billingCycle},")
                sb.append("\"${sub.category}\",")
                sb.append("${sub.subscriptionDate},${sub.expiryDate},")
                sb.append("${status},${trial},${sub.trialEndDate},")
                sb.appendLine("\"${history}\"")
            }
            file.writeText(sb.toString())
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "com.example.trackifyv1.provider", file
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Trackify Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV"))
            val count = subs.size
            toast(context, "Exported $count subscription${if (count != 1) "s" else ""}!")
        } catch (ex: Exception) {
            toast(context, "Export failed: ${ex.message}")
        }
    }
    private fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
