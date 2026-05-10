package com.example.trackifyv1.models

data class SubscriptionModel(
    val id: String = "",
    val subscriptionName: String = "",
    val subscriptionAmount: String = "",
    val subscriptionDate: String = "",
    val expiryDate: String = "",
    val nextRenewalDate: String = "",
    val category: String = "",
    val reminderDate: String = ""
)