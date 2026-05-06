package ru.homebuhg.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_rules")
data class SmsRuleEntity(
    @PrimaryKey val id: String,
    val senderPattern: String,
    val bodyRegex: String,
    val accountId: String? = null,
    val amountGroup: String,
    val merchantGroup: String? = null,
    val type: TransactionType,
    val isActive: Boolean = true
)
