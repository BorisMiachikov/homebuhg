package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("householdId"),
        Index("occurredAt"),
        Index("accountId"),
        Index("categoryId")
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val occurredAt: Long,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String = "RUB",
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val merchantId: String? = null,
    val note: String? = null,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceType: SourceType = SourceType.MANUAL,
    val receiptId: String? = null,
    val isDeleted: Boolean = false
)

enum class TransactionType { INCOME, EXPENSE, TRANSFER }
enum class SourceType { MANUAL, SMS, QR, IMPORT }
