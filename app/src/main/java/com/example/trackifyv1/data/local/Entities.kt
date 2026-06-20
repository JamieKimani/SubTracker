package com.example.trackifyv1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of a user's active subscriptions. Mirrors [com.example.trackifyv1.models.SubscriptionModel].
 * Primary key is the Firebase push id; rows are scoped to [uid] so multiple accounts can share one DB.
 */
@Entity(tableName = "subscriptions", primaryKeys = ["id", "uid"])
data class SubscriptionEntity(
    val id: String,
    val uid: String,
    val subscriptionName: String,
    val subscriptionAmount: String,
    val subscriptionDate: String,
    val expiryDate: String,
    val reminderDate: String,
    val category: String,
    val billingCycle: String,
    val isActive: Boolean,
    val isTrial: Boolean,
    val trialEndDate: String,
    /** [com.example.trackifyv1.models.SubscriptionModel.priceHistory] serialized as a flat "date1=amt1,date2=amt2" string. */
    val priceHistoryRaw: String
)

@Entity(tableName = "deleted_subscriptions", primaryKeys = ["id", "uid"])
data class DeletedSubscriptionEntity(
    val id: String,
    val uid: String,
    val subscriptionName: String,
    val subscriptionAmount: String,
    val subscriptionDate: String,
    val expiryDate: String,
    val reminderDate: String,
    val category: String,
    val billingCycle: String,
    val isTrial: Boolean,
    val trialEndDate: String,
    val priceHistoryRaw: String,
    val deletedAt: Long
)

@Entity(tableName = "budgets", primaryKeys = ["category", "uid"])
data class BudgetEntity(
    val category: String,
    val uid: String,
    val amount: Double
)

/**
 * A pending write made while offline (or before the sync worker has had a chance to run).
 * Drained in [createdAt] order by SyncWorker; each row is removed once the matching
 * Firebase write succeeds, so the queue is the durable source of truth for "what hasn't synced yet".
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val uid: String,
    val opType: String,
    val entityId: String,
    /** Flattened payload, format depends on opType (see SyncWorker). */
    val payloadRaw: String,
    val createdAt: Long = System.currentTimeMillis()
)

object SyncOp {
    const val ADD_SUBSCRIPTION = "ADD_SUBSCRIPTION"
    const val UPDATE_SUBSCRIPTION = "UPDATE_SUBSCRIPTION"
    const val DELETE_SUBSCRIPTION = "DELETE_SUBSCRIPTION"
    const val RESTORE_SUBSCRIPTION = "RESTORE_SUBSCRIPTION"
    const val PERMANENT_DELETE = "PERMANENT_DELETE"
    const val SET_BUDGET = "SET_BUDGET"
    const val CLEAR_ALL = "CLEAR_ALL"
}
