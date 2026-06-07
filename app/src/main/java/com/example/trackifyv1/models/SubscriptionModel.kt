package com.example.trackifyv1.models

data class SubscriptionModel(
    val id: String = "",
    val subscriptionName: String = "",
    val subscriptionAmount: String = "",
    val subscriptionDate: String = "",
    val expiryDate: String = "",
    val reminderDate: String = "",
    val category: String = "",
    val billingCycle: String = "Monthly",  // Monthly | Quarterly | Yearly | Weekly | Custom
    val isActive: Boolean = true           // active / paused toggle
)
