package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts", indices = [Index("householdId")])
data class AccountEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val type: AccountType,
    val currency: String = "RUB",
    val balanceMinor: Long = 0L,
    val creditLimitMinor: Long? = null,
    val gracePeriodDays: Int? = null,
    val paymentDueDay: Int? = null,
    val color: Int,
    val iconKey: String,
    val isArchived: Boolean = false,
    val updatedAt: Long
)

enum class AccountType { CARD_DEBIT, CARD_CREDIT, CASH }
