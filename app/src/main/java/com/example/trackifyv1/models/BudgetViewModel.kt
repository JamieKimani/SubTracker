package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase
        .getInstance("https://trackify-aab65-default-rtdb.firebaseio.com")
        .reference

    private val _budgets = MutableStateFlow<Map<String, Double>>(emptyMap())
    val budgets: StateFlow<Map<String, Double>> = _budgets.asStateFlow()

    private var listener: ValueEventListener? = null

    init { attach() }

    private fun budgetsRef() = auth.currentUser?.uid?.let { db.child("budgets").child(it) }

    private fun attach() {
        val ref = budgetsRef() ?: return
        listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                _budgets.value = snap.children.associate { child ->
                    (child.key ?: "") to (child.getValue(Double::class.java) ?: 0.0)
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(listener!!)
    }

    fun setBudget(category: String, amount: Double, context: Context) {
        if (amount <= 0) {
            budgetsRef()?.child(category)?.removeValue()
        } else {
            budgetsRef()?.child(category)?.setValue(amount)
        }
        Toast.makeText(context, if (amount <= 0) "Budget removed for $category" else "Budget set: KES ${"%.0f".format(amount)}/mo for $category", Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { budgetsRef()?.removeEventListener(it) }
    }
}
