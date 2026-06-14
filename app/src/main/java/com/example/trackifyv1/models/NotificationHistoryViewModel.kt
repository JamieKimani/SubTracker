package com.example.trackifyv1.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackifyv1.data.AppDatabase
import com.example.trackifyv1.data.NotificationHistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationHistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).notificationHistoryDao()

    val history = dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun log(title: String, message: String, channel: String) {
        viewModelScope.launch {
            dao.insert(NotificationHistoryEntity(title = title, message = message, channel = channel))
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            dao.deleteOlderThan(thirtyDaysAgo)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { dao.clearAll() }
    }
}
