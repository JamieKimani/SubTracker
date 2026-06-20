package com.example.trackifyv1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE uid = :uid ORDER BY subscriptionName COLLATE NOCASE")
    fun observeAll(uid: String): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id AND uid = :uid")
    suspend fun delete(id: String, uid: String)

    @Query("DELETE FROM subscriptions WHERE uid = :uid")
    suspend fun clearAll(uid: String)

    @Query("SELECT * FROM subscriptions WHERE id = :id AND uid = :uid LIMIT 1")
    suspend fun getById(id: String, uid: String): SubscriptionEntity?

    /** Replaces the full local cache for [uid] with the latest remote snapshot, but leaves alone
     * any row that still has an unsynced queue entry so an in-flight local edit isn't clobbered. */
    @Query(
        """DELETE FROM subscriptions WHERE uid = :uid AND id NOT IN
           (SELECT entityId FROM sync_queue WHERE uid = :uid)"""
    )
    suspend fun clearSyncedRows(uid: String)
}

@Dao
interface DeletedSubscriptionDao {
    @Query("SELECT * FROM deleted_subscriptions WHERE uid = :uid ORDER BY deletedAt DESC")
    fun observeAll(uid: String): Flow<List<DeletedSubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeletedSubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DeletedSubscriptionEntity>)

    @Query("DELETE FROM deleted_subscriptions WHERE id = :id AND uid = :uid")
    suspend fun delete(id: String, uid: String)

    @Query(
        """DELETE FROM deleted_subscriptions WHERE uid = :uid AND id NOT IN
           (SELECT entityId FROM sync_queue WHERE uid = :uid)"""
    )
    suspend fun clearSyncedRows(uid: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE uid = :uid")
    fun observeAll(uid: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE category = :category AND uid = :uid")
    suspend fun delete(category: String, uid: String)

    @Query(
        """DELETE FROM budgets WHERE uid = :uid AND category NOT IN
           (SELECT entityId FROM sync_queue WHERE uid = :uid)"""
    )
    suspend fun clearSyncedRows(uid: String)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE uid = :uid ORDER BY createdAt ASC")
    suspend fun getQueue(uid: String): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE uid = :uid")
    fun observeQueueSize(uid: String): Flow<Int>

    @Insert
    suspend fun enqueue(entity: SyncQueueEntity): Long

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun remove(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE uid = :uid")
    suspend fun clearForUser(uid: String)
}
