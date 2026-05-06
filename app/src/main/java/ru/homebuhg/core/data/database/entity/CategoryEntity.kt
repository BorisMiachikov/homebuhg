package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories", indices = [Index("householdId"), Index("parentId")])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val type: CategoryType,
    val parentId: String? = null,
    val color: Int,
    val iconKey: String,
    val sortOrder: Int = 0,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

enum class CategoryType { INCOME, EXPENSE }
