package com.example.trackifyv1.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentlyDeletedViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase
        .getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").reference

    private val _deleted = MutableStateFlow<List<DeletedSubscriptionModel>>(emptyList())
    val deleted: StateFlow<List<DeletedSubscriptionModel>> = _deleted.asStateFlow()

    private var listener: ValueEventListener? = null
    private val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000

    init { attach() }

    private fun deletedRef() = auth.currentUser?.uid?.let { db.child("DeletedSubscriptions").child(it) }

    private fun attach() {
        val ref = deletedRef() ?: return
        listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val now  = System.currentTimeMillis()
                val list = snap.children.mapNotNull { child ->
                    child.getValue(DeletedSubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
                val valid   = list.filter { now - it.deletedAt < SEVEN_DAYS_MS }
                val expired = list.filter { now - it.deletedAt >= SEVEN_DAYS_MS }
                _deleted.value = valid.sortedByDescending { it.deletedAt }
                expired.forEach { deletedRef()?.child(it.id)?.removeValue() }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(listener!!)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { deletedRef()?.removeEventListener(it) }
    }
}
