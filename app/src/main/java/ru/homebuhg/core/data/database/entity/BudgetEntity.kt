package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budgets", indices = [Index("householdId"), Index("categoryId")])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val categoryId: String,
    val period: BudgetPeriod,
    val limitMinor: Long,
    val currency: String = "RUB",
    val startDate: Long,
    val isRolling: Boolean = false,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

enum class BudgetPeriod { WEEK, MONTH, YEAR }
