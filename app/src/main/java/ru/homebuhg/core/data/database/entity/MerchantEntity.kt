package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "merchants", indices = [Index("householdId")])
data class MerchantEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val name: String,
    val defaultCategoryId: String? = null,
    val lastUsedAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
