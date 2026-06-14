package com.example.trackifyv1.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class SubscriptionModel(
    @get:Exclude val id: String = "",
    val subscriptionName: String = "",
    val subscriptionAmount: String = "",
    val subscriptionDate: String = "",
    val expiryDate: String = "",
    val reminderDate: String = "",
    val category: String = "",
    val billingCycle: String = "Monthly",
    val isActive: Boolean = true,
    val isTrial: Boolean = false,
    val trialEndDate: String = "",
    val priceHistory: Map<String, String> = emptyMap()
)

@IgnoreExtraProperties
data class DeletedSubscriptionModel(
    @get:Exclude val id: String = "",
    val subscriptionName: String = "",
    val subscriptionAmount: String = "",
    val subscriptionDate: String = "",
    val expiryDate: String = "",
    val reminderDate: String = "",
    val category: String = "",
    val billingCycle: String = "Monthly",
    val isTrial: Boolean = false,
    val trialEndDate: String = "",
    val priceHistory: Map<String, String> = emptyMap(),
    val deletedAt: Long = 0L
)

fun SubscriptionModel.toDeleted() = DeletedSubscriptionModel(
    id = id, subscriptionName = subscriptionName, subscriptionAmount = subscriptionAmount,
    subscriptionDate = subscriptionDate, expiryDate = expiryDate, reminderDate = reminderDate,
    category = category, billingCycle = billingCycle, isTrial = isTrial,
    trialEndDate = trialEndDate, priceHistory = priceHistory,
    deletedAt = System.currentTimeMillis()
)

fun DeletedSubscriptionModel.toSubscription() = SubscriptionModel(
    id = id, subscriptionName = subscriptionName, subscriptionAmount = subscriptionAmount,
    subscriptionDate = subscriptionDate, expiryDate = expiryDate, reminderDate = reminderDate,
    category = category, billingCycle = billingCycle, isActive = true,
    isTrial = isTrial, trialEndDate = trialEndDate, priceHistory = priceHistory
)
