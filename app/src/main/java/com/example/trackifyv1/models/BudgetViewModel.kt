package com.example.trackifyv1.models

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trackifyv1.data.repo.TrackifyRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val repo = TrackifyRepository.getInstance(application)

    private val _authUid = MutableStateFlow(auth.currentUser?.uid)
    private val authListener = FirebaseAuth.AuthStateListener { fa -> _authUid.value = fa.currentUser?.uid }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val budgets: StateFlow<Map<String, Double>> =
        _authUid.flatMapLatest { uid -> uid?.let { repo.observeBudgets(it) } ?: flowOf(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init { auth.addAuthStateListener(authListener) }

    fun setBudget(category: String, amount: Double, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repo.setBudget(uid, category, amount)
            Toast.makeText(
                context,
                if (amount <= 0) "Budget removed for $category"
                else "Budget set: KES ${"%.0f".format(amount)}/mo for $category",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
