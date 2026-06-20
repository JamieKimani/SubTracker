package com.example.trackifyv1.data.repo

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.trackifyv1.data.local.BudgetEntity
import com.example.trackifyv1.data.local.SyncOp
import com.example.trackifyv1.data.local.SyncQueueEntity
import com.example.trackifyv1.data.local.TrackifyDatabase
import com.example.trackifyv1.data.local.toEntity
import com.example.trackifyv1.data.local.toModel
import com.example.trackifyv1.data.sync.SyncWorker
import com.example.trackifyv1.models.DeletedSubscriptionModel
import com.example.trackifyv1.models.SubscriptionModel
import com.example.trackifyv1.models.toDeleted
import com.example.trackifyv1.models.toSubscription
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Single source of truth for subscription/budget/deleted-subscription data.
 *
 * Reads always come from Room (instant, works offline). Writes go to Room immediately
 * (optimistic), then a [SyncQueueEntity] row is enqueued and [SyncWorker] is scheduled to push
 * it to Firebase as soon as a connection is available — surviving app restarts because the
 * queue itself lives in Room. Remote snapshots from Firebase write through into Room, but skip
 * any row that still has a pending queue entry so an unsynced local edit is never clobbered by
 * a stale remote read.
 */
class TrackifyRepository private constructor(context: Context) {

    private val db = TrackifyDatabase.getInstance(context)
    private val subscriptionDao = db.subscriptionDao()
    private val deletedDao = db.deletedSubscriptionDao()
    private val budgetDao = db.budgetDao()
    private val queueDao = db.syncQueueDao()
    private val workManager = WorkManager.getInstance(context)

    /** Long-lived scope for write-through of Firebase listener callbacks into Room. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val rtdb = FirebaseDatabase
        .getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").reference

    private var subsListener: ValueEventListener? = null
    private var deletedListener: ValueEventListener? = null
    private var budgetsListener: ValueEventListener? = null
    private var listeningUid: String? = null

    // ---------- Reads (Room only) ----------

    fun observeSubscriptions(uid: String): Flow<List<SubscriptionModel>> =
        subscriptionDao.observeAll(uid).map { list -> list.map { it.toModel() } }

    fun observeDeleted(uid: String): Flow<List<DeletedSubscriptionModel>> =
        deletedDao.observeAll(uid).map { list -> list.map { it.toModel() } }

    fun observeBudgets(uid: String): Flow<Map<String, Double>> =
        budgetDao.observeAll(uid).map { list -> list.associate { it.category to it.amount } }

    fun observePendingSyncCount(uid: String): Flow<Int> = queueDao.observeQueueSize(uid)

    // ---------- Firebase write-through into Room ----------

    /** Starts mirroring this user's Firebase data into Room. Safe to call repeatedly. */
    fun startListening(uid: String) {
        if (listeningUid == uid && subsListener != null) return
        stopListening()
        listeningUid = uid

        val subsRef = rtdb.child("Subscriptions").child(uid)
        subsRef.keepSynced(true)
        subsListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val remote = snap.children.mapNotNull { child ->
                    child.getValue(SubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
                scope.launch {
                    subscriptionDao.clearSyncedRows(uid)
                    subscriptionDao.upsertAll(remote.map { it.toEntity(uid) })
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        subsRef.addValueEventListener(subsListener!!)

        val deletedRef = rtdb.child("DeletedSubscriptions").child(uid)
        deletedRef.keepSynced(true)
        deletedListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val remote = snap.children.mapNotNull { child ->
                    child.getValue(DeletedSubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
                scope.launch {
                    deletedDao.clearSyncedRows(uid)
                    deletedDao.upsertAll(remote.map { it.toEntity(uid) })
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        deletedRef.addValueEventListener(deletedListener!!)

        val budgetsRef = rtdb.child("budgets").child(uid)
        budgetsListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val remote = snap.children.associate { child ->
                    (child.key ?: "") to (child.getValue(Double::class.java) ?: 0.0)
                }
                scope.launch {
                    budgetDao.clearSyncedRows(uid)
                    remote.forEach { (category, amount) ->
                        budgetDao.upsert(BudgetEntity(category, uid, amount))
                    }
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        budgetsRef.addValueEventListener(budgetsListener!!)

        // Safety-net sync attempt whenever listening (re)starts, e.g. on sign-in or app foreground.
        requestSync()
    }

    fun stopListening() {
        val uid = listeningUid ?: return
        subsListener?.let { rtdb.child("Subscriptions").child(uid).removeEventListener(it) }
        deletedListener?.let { rtdb.child("DeletedSubscriptions").child(uid).removeEventListener(it) }
        budgetsListener?.let { rtdb.child("budgets").child(uid).removeEventListener(it) }
        subsListener = null; deletedListener = null; budgetsListener = null
        listeningUid = null
    }

    fun clearLocalCache() {
        val uid = listeningUid
        stopListening()
        if (uid != null) {
            scope.launch {
                subscriptionDao.clearAll(uid)
                queueDao.clearForUser(uid)
            }
        }
    }

    /** Firebase push() keys are generated client-side and work fine offline. */
    fun newSubscriptionId(): String = rtdb.child("Subscriptions").push().key ?: java.util.UUID.randomUUID().toString()

    // ---------- Mutations: write Room + enqueue sync ----------

    suspend fun addOrUpdateSubscription(uid: String, sub: SubscriptionModel) {
        subscriptionDao.upsert(sub.toEntity(uid))
        enqueue(uid, SyncOp.ADD_SUBSCRIPTION, sub.id, encodeSubscription(sub))
    }

    suspend fun deleteSubscription(uid: String, sub: SubscriptionModel) {
        subscriptionDao.delete(sub.id, uid)
        val deleted = sub.toDeleted()
        deletedDao.upsert(deleted.toEntity(uid))
        enqueue(uid, SyncOp.DELETE_SUBSCRIPTION, sub.id, encodeDeleted(deleted))
    }

    suspend fun restoreSubscription(uid: String, deleted: DeletedSubscriptionModel, newId: String) {
        deletedDao.delete(deleted.id, uid)
        val restored = deleted.toSubscription().copy(id = newId)
        subscriptionDao.upsert(restored.toEntity(uid))
        enqueue(uid, SyncOp.RESTORE_SUBSCRIPTION, newId, "${deleted.id}|${encodeSubscription(restored)}")
    }

    suspend fun permanentlyDelete(uid: String, deletedId: String) {
        deletedDao.delete(deletedId, uid)
        enqueue(uid, SyncOp.PERMANENT_DELETE, deletedId, "")
    }

    suspend fun setBudget(uid: String, category: String, amount: Double) {
        if (amount <= 0) budgetDao.delete(category, uid)
        else budgetDao.upsert(BudgetEntity(category, uid, amount))
        enqueue(uid, SyncOp.SET_BUDGET, category, amount.toString())
    }

    suspend fun clearAllSubscriptions(uid: String) {
        subscriptionDao.clearAll(uid)
        enqueue(uid, SyncOp.CLEAR_ALL, uid, "")
    }

    private suspend fun enqueue(uid: String, op: String, entityId: String, payload: String) {
        queueDao.enqueue(SyncQueueEntity(uid = uid, opType = op, entityId = entityId, payloadRaw = payload))
        requestSync()
    }

    /** Kicks off (or re-confirms) a sync attempt; cheap to call often, WorkManager dedupes via unique work name. */
    fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        workManager.enqueueUniqueWork("trackify_sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        @Volatile private var instance: TrackifyRepository? = null
        fun getInstance(context: Context): TrackifyRepository =
            instance ?: synchronized(this) {
                instance ?: TrackifyRepository(context.applicationContext).also { instance = it }
            }

        fun encodeSubscription(sub: SubscriptionModel): String = listOf(
            sub.subscriptionName, sub.subscriptionAmount, sub.subscriptionDate, sub.expiryDate,
            sub.reminderDate, sub.category, sub.billingCycle, sub.isActive.toString(),
            sub.isTrial.toString(), sub.trialEndDate,
            sub.priceHistory.entries.joinToString(";") { "${it.key}=${it.value}" }
        ).joinToString("\u0001")

        fun decodeSubscription(id: String, raw: String): SubscriptionModel {
            val p = raw.split("\u0001")
            val history = if (p.size > 10 && p[10].isNotBlank())
                p[10].split(";").mapNotNull {
                    val kv = it.split("=", limit = 2)
                    if (kv.size == 2) kv[0] to kv[1] else null
                }.toMap()
            else emptyMap()
            return SubscriptionModel(
                id = id, subscriptionName = p.getOrElse(0) { "" }, subscriptionAmount = p.getOrElse(1) { "" },
                subscriptionDate = p.getOrElse(2) { "" }, expiryDate = p.getOrElse(3) { "" },
                reminderDate = p.getOrElse(4) { "" }, category = p.getOrElse(5) { "" },
                billingCycle = p.getOrElse(6) { "Monthly" }, isActive = p.getOrElse(7) { "true" }.toBoolean(),
                isTrial = p.getOrElse(8) { "false" }.toBoolean(), trialEndDate = p.getOrElse(9) { "" },
                priceHistory = history
            )
        }

        fun encodeDeleted(d: DeletedSubscriptionModel): String =
            encodeSubscription(
                SubscriptionModel(
                    id = d.id, subscriptionName = d.subscriptionName, subscriptionAmount = d.subscriptionAmount,
                    subscriptionDate = d.subscriptionDate, expiryDate = d.expiryDate, reminderDate = d.reminderDate,
                    category = d.category, billingCycle = d.billingCycle, isActive = true, isTrial = d.isTrial,
                    trialEndDate = d.trialEndDate, priceHistory = d.priceHistory
                )
            ) + "\u0001${d.deletedAt}"

        fun decodeDeleted(id: String, raw: String): DeletedSubscriptionModel {
            val lastSep = raw.lastIndexOf('\u0001')
            val deletedAt = raw.substring(lastSep + 1).toLongOrNull() ?: System.currentTimeMillis()
            val subPart = raw.substring(0, lastSep)
            val sub = decodeSubscription(id, subPart)
            return DeletedSubscriptionModel(
                id = id, subscriptionName = sub.subscriptionName, subscriptionAmount = sub.subscriptionAmount,
                subscriptionDate = sub.subscriptionDate, expiryDate = sub.expiryDate, reminderDate = sub.reminderDate,
                category = sub.category, billingCycle = sub.billingCycle, isTrial = sub.isTrial,
                trialEndDate = sub.trialEndDate, priceHistory = sub.priceHistory, deletedAt = deletedAt
            )
        }
    }
}
