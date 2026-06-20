package com.example.trackifyv1.models

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackifyv1.data.repo.TrackifyRepository
import com.example.trackifyv1.notifications.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val repo = TrackifyRepository.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authUid = MutableStateFlow(auth.currentUser?.uid)

    private val authListener = FirebaseAuth.AuthStateListener { fa ->
        _authUid.value = fa.currentUser?.uid
        if (fa.currentUser != null) repo.startListening(fa.currentUser!!.uid)
        else repo.stopListening()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val subscriptions: StateFlow<List<SubscriptionModel>> =
        _authUid.flatMapLatest { uid -> uid?.let { repo.observeSubscriptions(it) } ?: flowOf(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        auth.addAuthStateListener(authListener)
        auth.currentUser?.let { _isLoading.value = true; repo.startListening(it.uid) }
        // Cache emits instantly once Room has rows, so loading just reflects "do we have anything yet".
        viewModelScope.launch {
            subscriptions.collect { if (it.isNotEmpty()) _isLoading.value = false }
        }
    }

    private fun uid() = auth.currentUser?.uid

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
        val uid = uid() ?: run { toast(context, "You must be logged in."); return }
        when {
            subscriptionName.isBlank()                        -> { toast(context, "Enter a subscription name."); return }
            subscriptionAmount.isBlank()                      -> { toast(context, "Enter an amount."); return }
            subscriptionAmount.toDoubleOrNull() == null       -> { toast(context, "Amount must be a valid number."); return }
            (subscriptionAmount.toDoubleOrNull() ?: 0.0) < 0 -> { toast(context, "Amount cannot be negative."); return }
        }
        val duplicate = subscriptions.value.any {
            it.subscriptionName.trim().equals(subscriptionName.trim(), ignoreCase = true)
        }
        if (duplicate) {
            toast(context, "${subscriptionName.trim()} already exists. Rename it to add separately.")
            return
        }
        val id = repo.newSubscriptionId()
        val sub = SubscriptionModel(
            id = id, subscriptionName = subscriptionName.trim(),
            subscriptionAmount = subscriptionAmount.trim(), subscriptionDate = subscriptionDate,
            expiryDate = expiryDate, reminderDate = reminderDate,
            category = category, billingCycle = billingCycle, isActive = true,
            isTrial = isTrial, trialEndDate = trialEndDate
        )
        viewModelScope.launch {
            repo.addOrUpdateSubscription(uid, sub)
            val helper = NotificationHelper(context)
            if (reminderDate.isNotBlank())
                helper.scheduleForSubscription(id, subscriptionName.trim(), reminderDate)
            if (isTrial && trialEndDate.isNotBlank())
                helper.scheduleTrialEndingNotification(id, subscriptionName.trim(), trialEndDate)
            toast(context, "\"${subscriptionName.trim()}\" added!")
            onSuccess()
        }
    }

    fun deleteSubscription(id: String, context: Context? = null) {
        if (id.isBlank()) return
        val uid = uid() ?: return
        val sub = subscriptions.value.find { it.id == id } ?: return
        context?.let { ctx ->
            NotificationHelper(ctx).cancelScheduledNotification(id.hashCode())
            NotificationHelper(ctx).cancelScheduledNotification(("trial_" + id).hashCode())
        }
        viewModelScope.launch { repo.deleteSubscription(uid, sub) }
    }

    fun restoreSubscription(deleted: DeletedSubscriptionModel, context: Context) {
        val uid = uid() ?: return
        val newId = repo.newSubscriptionId()
        viewModelScope.launch {
            repo.restoreSubscription(uid, deleted, newId)
            toast(context, "\"${deleted.subscriptionName}\" restored!")
        }
    }

    fun permanentlyDelete(deletedId: String, context: Context) {
        val uid = uid() ?: return
        viewModelScope.launch {
            repo.permanentlyDelete(uid, deletedId)
            toast(context, "Permanently deleted.")
        }
    }

    fun updateSubscription(sub: SubscriptionModel, context: Context? = null) {
        if (sub.id.isBlank()) return
        val uid = uid() ?: return
        viewModelScope.launch {
            repo.addOrUpdateSubscription(uid, sub)
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
        val today        = sdf.format(Calendar.getInstance().time)
        val priceChanged = newAmount != null && newAmount.isNotBlank() &&
            newAmount.toDoubleOrNull() != null &&
            newAmount.trim() != sub.subscriptionAmount.trim()
        val updatedHistory = if (priceChanged) sub.priceHistory + (today to sub.subscriptionAmount)
                             else sub.priceHistory
        updateSubscription(
            sub.copy(
                expiryDate         = advance(sub.expiryDate),
                reminderDate       = advance(sub.reminderDate),
                subscriptionAmount = if (priceChanged) newAmount!!.trim() else sub.subscriptionAmount,
                priceHistory       = updatedHistory
            ), context
        )
        if (priceChanged) {
            val oldAmt = sub.subscriptionAmount.toDoubleOrNull() ?: 0.0
            val newAmt = newAmount!!.toDoubleOrNull() ?: 0.0
            toast(context, "Price ${if (newAmt > oldAmt) "increased" else "decreased"}: KES ${sub.subscriptionAmount} → KES ${newAmount.trim()}")
        }
    }

    fun toggleActive(sub: SubscriptionModel, context: Context) =
        updateSubscription(sub.copy(isActive = !sub.isActive), context)

    fun clearAllSubscriptions(context: Context, onComplete: () -> Unit = {}) {
        val uid = uid() ?: return
        val current = subscriptions.value
        current.forEach { sub ->
            val h = NotificationHelper(context)
            h.cancelScheduledNotification(sub.id.hashCode())
            h.cancelScheduledNotification(("trial_" + sub.id).hashCode())
        }
        viewModelScope.launch {
            repo.clearAllSubscriptions(uid)
            toast(context, "All subscription data cleared.")
            onComplete()
        }
    }

    fun scheduleMonthlySummary(context: Context) {
        val activeSubs   = subscriptions.value.filter { it.isActive }
        val monthlyTotal = activeSubs.sumOf { sub ->
            val amt = sub.subscriptionAmount.toDoubleOrNull() ?: 0.0
            when (sub.billingCycle) {
                "Weekly"    -> amt * 4.33
                "Quarterly" -> amt / 3.0
                "Yearly"    -> amt / 12.0
                else        -> amt
            }
        }
        val now          = System.currentTimeMillis()
        val sevenDays    = 7L * 24 * 60 * 60 * 1000
        val renewingSoon = activeSubs.count { sub ->
            try {
                val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val date = sdf.parse(sub.expiryDate) ?: return@count false
                val diff = date.time - now
                diff in 0..sevenDays
            } catch (_: Exception) { false }
        }
        NotificationHelper(context).scheduleMonthlySummary(activeSubs.size, monthlyTotal, renewingSoon)
    }

    fun exportToJson(context: Context) {
        val subs = subscriptions.value
        if (subs.isEmpty()) { toast(context, "No subscriptions to backup."); return }
        viewModelScope.launch(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.append("[")
            subs.forEachIndexed { index, sub ->
                sb.append("{")
                sb.append("\"subscriptionName\":\"${sub.subscriptionName}\",")
                sb.append("\"subscriptionAmount\":\"${sub.subscriptionAmount}\",")
                sb.append("\"subscriptionDate\":\"${sub.subscriptionDate}\",")
                sb.append("\"expiryDate\":\"${sub.expiryDate}\",")
                sb.append("\"reminderDate\":\"${sub.reminderDate}\",")
                sb.append("\"category\":\"${sub.category}\",")
                sb.append("\"billingCycle\":\"${sub.billingCycle}\",")
                sb.append("\"isActive\":${sub.isActive},")
                sb.append("\"isTrial\":${sub.isTrial},")
                sb.append("\"trialEndDate\":\"${sub.trialEndDate}\"")
                sb.append("}")
                if (index < subs.lastIndex) sb.append(",")
            }
            sb.append("]")
            val fileName = "trackify_backup_${System.currentTimeMillis()}.json"
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = java.io.File(dir, fileName)
            file.writeText(sb.toString())
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "com.example.trackifyv1.provider", file)
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Trackify Backup")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Save backup"))
                val count = subs.size
                toast(context, "Backup of $count subscription${if (count != 1) "s" else ""} created!")
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) { toast(context, "Backup failed: ${ex.message}") }
        }
        }
    }

    fun restoreFromJson(context: Context, jsonText: String) {
        try {
            val trimmed = jsonText.trim()
            if (!trimmed.startsWith("[")) { toast(context, "Invalid backup file."); return }
            val entries = trimmed.removePrefix("[").removeSuffix("]")
                .split("},{").map { it.removePrefix("{").removeSuffix("}") }
            var imported = 0
            entries.forEach { entry ->
                fun field(key: String): String {
                    return Regex("\"$key\":\"([^\"]*)\"").find(entry)?.groupValues?.getOrNull(1) ?: ""
                }
                fun boolField(key: String) = entry.contains("\"$key\":true")
                val name = field("subscriptionName")
                if (name.isBlank()) return@forEach
                if (subscriptions.value.any { it.subscriptionName.trim().lowercase() == name.trim().lowercase() })
                    return@forEach
                addSubscription(
                    subscriptionName   = name,
                    subscriptionAmount = field("subscriptionAmount"),
                    subscriptionDate   = field("subscriptionDate"),
                    expiryDate         = field("expiryDate"),
                    reminderDate       = field("reminderDate"),
                    context            = context,
                    category           = field("category"),
                    billingCycle       = field("billingCycle").ifBlank { "Monthly" },
                    isTrial            = boolField("isTrial"),
                    trialEndDate       = field("trialEndDate")
                )
                imported++
            }
            toast(context, "Restored $imported subscription${if (imported != 1) "s" else ""}!")
        } catch (ex: Exception) {
            toast(context, "Restore failed: ${ex.message}")
        }
    }

    fun exportToCsv(context: Context) {
        val subs = subscriptions.value
        if (subs.isEmpty()) { toast(context, "No subscriptions to export."); return }
        viewModelScope.launch(Dispatchers.IO) {
        try {
            val sb = StringBuilder()
            sb.appendLine("Name,Amount,Cycle,Category,Start,Expiry,Status,Trial,Trial End,Price History")
            subs.forEach { sub ->
                val history = sub.priceHistory.entries.sortedByDescending { it.key }
                    .joinToString("; ") { "${it.key}:${it.value}" }
                val status = if (sub.isActive) "Active" else "Paused"
                val trial  = if (sub.isTrial) "Yes" else "No"
                sb.append("\"${sub.subscriptionName}\",")
                sb.append("${sub.subscriptionAmount},${sub.billingCycle},")
                sb.append("\"${sub.category}\",")
                sb.append("${sub.subscriptionDate},${sub.expiryDate},")
                sb.appendLine("$status,$trial,${sub.trialEndDate},\"$history\"")
            }
            val fileName = "trackify_${System.currentTimeMillis()}.csv"
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = java.io.File(dir, fileName)
            file.writeText(sb.toString())
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "com.example.trackifyv1.provider", file)
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Trackify Export")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV"))
                val count = subs.size
                toast(context, "Exported $count subscription${if (count != 1) "s" else ""}!")
            }
        } catch (ex: Exception) {
            withContext(Dispatchers.Main) { toast(context, "Export failed: ${ex.message}") }
        }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    private fun toast(ctx: Context, msg: String) =
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
