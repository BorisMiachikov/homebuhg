package ru.homebuhg.core.sync

import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.AccountType
import ru.homebuhg.core.data.database.entity.BudgetEntity
import ru.homebuhg.core.data.database.entity.BudgetPeriod
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType

// ─── Transaction ─────────────────────────────────────────────────────────────

fun TransactionEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "householdId" to householdId,
    "occurredAt" to occurredAt,
    "type" to type.name,
    "amountMinor" to amountMinor,
    "currency" to currency,
    "accountId" to accountId,
    "toAccountId" to toAccountId,
    "categoryId" to categoryId,
    "merchantId" to merchantId,
    "note" to note,
    "createdBy" to createdBy,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "sourceType" to sourceType.name,
    "receiptId" to receiptId,
    "isDeleted" to isDeleted
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toTransactionEntity() = TransactionEntity(
    id = this["id"] as String,
    householdId = this["householdId"] as String,
    occurredAt = (this["occurredAt"] as Number).toLong(),
    type = TransactionType.valueOf(this["type"] as String),
    amountMinor = (this["amountMinor"] as Number).toLong(),
    currency = this["currency"] as? String ?: "RUB",
    accountId = this["accountId"] as String,
    toAccountId = this["toAccountId"] as? String,
    categoryId = this["categoryId"] as? String,
    merchantId = this["merchantId"] as? String,
    note = this["note"] as? String,
    createdBy = this["createdBy"] as? String ?: "",
    createdAt = (this["createdAt"] as Number).toLong(),
    updatedAt = (this["updatedAt"] as Number).toLong(),
    sourceType = SourceType.valueOf(this["sourceType"] as? String ?: "MANUAL"),
    receiptId = this["receiptId"] as? String,
    isDeleted = this["isDeleted"] as? Boolean ?: false
)

// ─── Account ──────────────────────────────────────────────────────────────────

fun AccountEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "householdId" to householdId,
    "name" to name,
    "type" to type.name,
    "currency" to currency,
    "balanceMinor" to balanceMinor,
    "creditLimitMinor" to creditLimitMinor,
    "gracePeriodDays" to gracePeriodDays,
    "paymentDueDay" to paymentDueDay,
    "color" to color,
    "iconKey" to iconKey,
    "isArchived" to isArchived,
    "updatedAt" to updatedAt
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toAccountEntity() = AccountEntity(
    id = this["id"] as String,
    householdId = this["householdId"] as String,
    name = this["name"] as String,
    type = AccountType.valueOf(this["type"] as String),
    currency = this["currency"] as? String ?: "RUB",
    balanceMinor = (this["balanceMinor"] as Number).toLong(),
    creditLimitMinor = (this["creditLimitMinor"] as? Number)?.toLong(),
    gracePeriodDays = (this["gracePeriodDays"] as? Number)?.toInt(),
    paymentDueDay = (this["paymentDueDay"] as? Number)?.toInt(),
    color = (this["color"] as Number).toInt(),
    iconKey = this["iconKey"] as String,
    isArchived = this["isArchived"] as? Boolean ?: false,
    updatedAt = (this["updatedAt"] as Number).toLong()
)

// ─── Category ─────────────────────────────────────────────────────────────────

fun CategoryEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "householdId" to householdId,
    "name" to name,
    "type" to type.name,
    "parentId" to parentId,
    "color" to color,
    "iconKey" to iconKey,
    "sortOrder" to sortOrder,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toCategoryEntity() = CategoryEntity(
    id = this["id"] as String,
    householdId = this["householdId"] as String,
    name = this["name"] as String,
    type = CategoryType.valueOf(this["type"] as String),
    parentId = this["parentId"] as? String,
    color = (this["color"] as Number).toInt(),
    iconKey = this["iconKey"] as String,
    sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0,
    updatedAt = (this["updatedAt"] as Number).toLong(),
    isDeleted = this["isDeleted"] as? Boolean ?: false
)

// ─── Budget ───────────────────────────────────────────────────────────────────

fun BudgetEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "householdId" to householdId,
    "categoryId" to categoryId,
    "period" to period.name,
    "limitMinor" to limitMinor,
    "currency" to currency,
    "startDate" to startDate,
    "isRolling" to isRolling,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toBudgetEntity() = BudgetEntity(
    id = this["id"] as String,
    householdId = this["householdId"] as String,
    categoryId = this["categoryId"] as String,
    period = BudgetPeriod.valueOf(this["period"] as String),
    limitMinor = (this["limitMinor"] as Number).toLong(),
    currency = this["currency"] as? String ?: "RUB",
    startDate = (this["startDate"] as Number).toLong(),
    isRolling = this["isRolling"] as? Boolean ?: false,
    updatedAt = (this["updatedAt"] as Number).toLong(),
    isDeleted = this["isDeleted"] as? Boolean ?: false
)
