package com.example.trackifyv1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SubscriptionEntity::class,
        DeletedSubscriptionEntity::class,
        BudgetEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TrackifyDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun deletedSubscriptionDao(): DeletedSubscriptionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile private var instance: TrackifyDatabase? = null

        fun getInstance(context: Context): TrackifyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackifyDatabase::class.java,
                    "trackify.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
