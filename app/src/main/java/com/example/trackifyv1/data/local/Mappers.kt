package com.example.trackifyv1.data.local

import com.example.trackifyv1.models.DeletedSubscriptionModel
import com.example.trackifyv1.models.SubscriptionModel

private fun Map<String, String>.toRaw(): String =
    entries.joinToString(",") { "${it.key}=${it.value}" }

private fun String.toPriceHistory(): Map<String, String> =
    if (isBlank()) emptyMap()
    else split(",").mapNotNull {
        val parts = it.split("=", limit = 2)
        if (parts.size == 2) parts[0] to parts[1] else null
    }.toMap()

fun SubscriptionModel.toEntity(uid: String) = SubscriptionEntity(
    id = id, uid = uid, subscriptionName = subscriptionName,
    subscriptionAmount = subscriptionAmount, subscriptionDate = subscriptionDate,
    expiryDate = expiryDate, reminderDate = reminderDate, category = category,
    billingCycle = billingCycle, isActive = isActive, isTrial = isTrial,
    trialEndDate = trialEndDate, priceHistoryRaw = priceHistory.toRaw()
)

fun SubscriptionEntity.toModel() = SubscriptionModel(
    id = id, subscriptionName = subscriptionName, subscriptionAmount = subscriptionAmount,
    subscriptionDate = subscriptionDate, expiryDate = expiryDate, reminderDate = reminderDate,
    category = category, billingCycle = billingCycle, isActive = isActive, isTrial = isTrial,
    trialEndDate = trialEndDate, priceHistory = priceHistoryRaw.toPriceHistory()
)

fun DeletedSubscriptionModel.toEntity(uid: String) = DeletedSubscriptionEntity(
    id = id, uid = uid, subscriptionName = subscriptionName,
    subscriptionAmount = subscriptionAmount, subscriptionDate = subscriptionDate,
    expiryDate = expiryDate, reminderDate = reminderDate, category = category,
    billingCycle = billingCycle, isTrial = isTrial, trialEndDate = trialEndDate,
    priceHistoryRaw = priceHistory.toRaw(), deletedAt = deletedAt
)

fun DeletedSubscriptionEntity.toModel() = DeletedSubscriptionModel(
    id = id, subscriptionName = subscriptionName, subscriptionAmount = subscriptionAmount,
    subscriptionDate = subscriptionDate, expiryDate = expiryDate, reminderDate = reminderDate,
    category = category, billingCycle = billingCycle, isTrial = isTrial,
    trialEndDate = trialEndDate, priceHistory = priceHistoryRaw.toPriceHistory(),
    deletedAt = deletedAt
)
