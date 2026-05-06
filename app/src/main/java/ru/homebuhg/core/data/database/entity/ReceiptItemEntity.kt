package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_items", indices = [Index("transactionId")])
data class ReceiptItemEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val name: String,
    val priceMinor: Long,
    val qty: Double,
    val unit: String? = null,
    val fnsRaw: String? = null
)
