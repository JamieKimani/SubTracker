package com.example.trackifyv1.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackifyv1.data.repo.TrackifyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentlyDeletedViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val repo = TrackifyRepository.getInstance(application)
    private val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    private val _authUid = MutableStateFlow(auth.currentUser?.uid)
    private val authListener = FirebaseAuth.AuthStateListener { fa -> _authUid.value = fa.currentUser?.uid }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val deleted: StateFlow<List<DeletedSubscriptionModel>> =
        _authUid.flatMapLatest { uid -> uid?.let { repo.observeDeleted(it) } ?: flowOf(emptyList()) }
            .map { list ->
                val now = System.currentTimeMillis()
                val valid = list.filter { now - it.deletedAt < SEVEN_DAYS_MS }
                val expired = list.filter { now - it.deletedAt >= SEVEN_DAYS_MS }
                if (expired.isNotEmpty()) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) viewModelScope.launch {
                        expired.forEach { repo.permanentlyDelete(uid, it.id) }
                    }
                }
                valid.sortedByDescending { it.deletedAt }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { auth.addAuthStateListener(authListener) }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
