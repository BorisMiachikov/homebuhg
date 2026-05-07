package ru.homebuhg.feature.recurring

import kotlinx.serialization.Serializable
import ru.homebuhg.core.data.database.entity.TransactionType

@Serializable
data class TransactionTemplate(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountMinor: Long = 0L,
    val categoryId: String = "",
    val accountId: String = "",
    val toAccountId: String? = null,
    val note: String = ""
)
