package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_rules", indices = [Index("householdId")])
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val templateJson: String,
    val rrule: String,
    val nextRunAt: Long,
    val lastRunAt: Long? = null,
    val isActive: Boolean = true,
    val updatedAt: Long
)
