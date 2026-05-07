package ru.homebuhg.core.export

import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvWriter {

    private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val zone = ZoneId.systemDefault()

    fun write(
        transactions: List<TransactionEntity>,
        categories: Map<String, CategoryEntity>,
        accounts: Map<String, AccountEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Дата;Тип;Сумма;Счёт (откуда);Счёт (куда);Категория;Примечание;Источник")
        for (tx in transactions) {
            sb.append(tx.occurredAt.toDate()).append(';')
            sb.append(tx.type.label()).append(';')
            sb.append("%.2f".format(tx.amountMinor / 100.0)).append(';')
            sb.append(csv(accounts[tx.accountId]?.name ?: "")).append(';')
            sb.append(csv(tx.toAccountId?.let { accounts[it]?.name } ?: "")).append(';')
            sb.append(csv(categories[tx.categoryId]?.name ?: "")).append(';')
            sb.append(csv(tx.note ?: "")).append(';')
            sb.appendLine(tx.sourceType.name)
        }
        return sb.toString()
    }

    private fun Long.toDate(): String =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate().format(dateFmt)

    private fun TransactionType.label() = when (this) {
        TransactionType.INCOME -> "Доход"
        TransactionType.EXPENSE -> "Расход"
        TransactionType.TRANSFER -> "Перевод"
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(';') || escaped.contains('"') || escaped.contains('\n'))
            "\"$escaped\""
        else
            escaped
    }
}
