package com.example.trackifyv1.notifications

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MonthlySummaryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        return try {
            val db   = FirebaseDatabase.getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").reference
            val snap = db.child("Subscriptions").child(uid).get().await()

            var totalMonthly = 0.0
            var activeCount  = 0
            var renewingSoon = 0

            snap.children.forEach { child ->
                val isActive  = child.child("isActive").getValue(Boolean::class.java) ?: true
                val amount    = child.child("subscriptionAmount").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                val cycle     = child.child("billingCycle").getValue(String::class.java) ?: "Monthly"
                val expiryStr = child.child("expiryDate").getValue(String::class.java) ?: ""

                if (isActive) {
                    activeCount++
                    val monthly = when (cycle) {
                        "Weekly"    -> amount * 4.33
                        "Quarterly" -> amount / 3.0
                        "Yearly"    -> amount / 12.0
                        else        -> amount
                    }
                    totalMonthly += monthly

                    if (expiryStr.isNotBlank()) {
                        try {
                            val sdf  = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            val date = sdf.parse(expiryStr)
                            if (date != null) {
                                val diff = (date.time - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                                if (diff in 0..7) renewingSoon++
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            if (activeCount > 0) {
                val message = buildString {
                    append("You have $activeCount active subscription${if (activeCount != 1) "s" else ""} ")
                    append("costing KES ${"%.0f".format(totalMonthly)}/mo")
                    if (renewingSoon > 0) append(". $renewingSoon renewing this week!")
                }
                NotificationHelper(applicationContext).sendNotification(
                    title   = "📊 Your monthly subscription summary",
                    message = message,
                    channel = NotificationHelper.CHANNEL_RENEWALS
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "monthly_summary"

        fun schedule(context: Context) {
            val now   = Calendar.getInstance()
            val next  = Calendar.getInstance().apply {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = next.timeInMillis - now.timeInMillis

            val request = OneTimeWorkRequestBuilder<MonthlySummaryWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<MonthlySummaryWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
