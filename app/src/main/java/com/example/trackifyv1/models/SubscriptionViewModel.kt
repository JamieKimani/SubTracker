package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionViewModel : ViewModel() {

    private val dbRef = FirebaseDatabase.getInstance().getReference("Subscriptions")

    private val _subscriptions = MutableStateFlow<List<SubscriptionModel>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionModel>> = _subscriptions.asStateFlow()

    init { fetchSubscriptions() }

    private fun fetchSubscriptions() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _subscriptions.value = snapshot.children.mapNotNull { child ->
                    child.getValue(SubscriptionModel::class.java)?.copy(id = child.key ?: "")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addSubscription(
        subscriptionName: String,
        subscriptionAmount: String,
        subscriptionDate: String,
        expiryDate: String,
        reminderDate: String,
        context: Context,
        category: String = ""
    ) {
        if (subscriptionName.isBlank()) {
            Toast.makeText(context, "Subscription name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (subscriptionAmount.isBlank()) {
            Toast.makeText(context, "Amount cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val id = dbRef.push().key ?: return
        val sub = SubscriptionModel(id, subscriptionName, subscriptionAmount, subscriptionDate, expiryDate, reminderDate, category)
        dbRef.child(id).setValue(sub)
            .addOnSuccessListener { Toast.makeText(context, "Subscription added!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    fun deleteSubscription(id: String) {
        if (id.isNotBlank()) dbRef.child(id).removeValue()
    }

    fun updateSubscription(subscription: SubscriptionModel) {
        if (subscription.id.isNotBlank()) dbRef.child(subscription.id).setValue(subscription)
    }
}