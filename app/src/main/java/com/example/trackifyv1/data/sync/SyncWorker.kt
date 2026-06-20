package com.example.trackifyv1.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trackifyv1.data.local.SyncOp
import com.example.trackifyv1.data.local.TrackifyDatabase
import com.example.trackifyv1.data.repo.TrackifyRepository
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Drains the [com.example.trackifyv1.data.local.SyncQueueEntity] table in FIFO order, pushing
 * each pending write to Firebase. A queue row is only removed once Firebase confirms the write,
 * so a kill or crash mid-sync just means the same row is retried next run. On a network failure
 * the worker stops and asks WorkManager to retry with backoff; on any other failure (e.g.
 * permission denied) that single row is dropped so it can't block everything behind it forever.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.success()
        val db = TrackifyDatabase.getInstance(applicationContext)
        val queueDao = db.syncQueueDao()
        val rtdb = FirebaseDatabase
            .getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").reference

        val pending = queueDao.getQueue(uid)
        for (item in pending) {
            try {
                when (item.opType) {
                    SyncOp.ADD_SUBSCRIPTION -> {
                        val sub = TrackifyRepository.decodeSubscription(item.entityId, item.payloadRaw)
                        rtdb.child("Subscriptions").child(uid).child(sub.id).setValue(sub).await()
                    }
                    SyncOp.DELETE_SUBSCRIPTION -> {
                        val deleted = TrackifyRepository.decodeDeleted(item.entityId, item.payloadRaw)
                        rtdb.child("Subscriptions").child(uid).child(item.entityId).removeValue().await()
                        rtdb.child("DeletedSubscriptions").child(uid).child(item.entityId).setValue(deleted).await()
                    }
                    SyncOp.RESTORE_SUBSCRIPTION -> {
                        val sepIdx = item.payloadRaw.indexOf('|')
                        val oldDeletedId = item.payloadRaw.substring(0, sepIdx)
                        val sub = TrackifyRepository.decodeSubscription(item.entityId, item.payloadRaw.substring(sepIdx + 1))
                        rtdb.child("Subscriptions").child(uid).child(sub.id).setValue(sub).await()
                        rtdb.child("DeletedSubscriptions").child(uid).child(oldDeletedId).removeValue().await()
                    }
                    SyncOp.PERMANENT_DELETE -> {
                        rtdb.child("DeletedSubscriptions").child(uid).child(item.entityId).removeValue().await()
                    }
                    SyncOp.SET_BUDGET -> {
                        val amount = item.payloadRaw.toDoubleOrNull() ?: 0.0
                        val ref = rtdb.child("budgets").child(uid).child(item.entityId)
                        if (amount <= 0) ref.removeValue().await() else ref.setValue(amount).await()
                    }
                    SyncOp.CLEAR_ALL -> {
                        rtdb.child("Subscriptions").child(uid).removeValue().await()
                    }
                }
                queueDao.remove(item.queueId)
            } catch (e: IOException) {
                // Network problem — stop here, leave remaining rows queued, retry with backoff.
                return@withContext Result.retry()
            } catch (e: Exception) {
                // Non-network failure (bad data, permission denied, etc.) — drop this row only.
                queueDao.remove(item.queueId)
            }
        }
        Result.success()
    }
}
