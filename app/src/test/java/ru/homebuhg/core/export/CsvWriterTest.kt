package ru.homebuhg.core.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.AccountType
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType

class CsvWriterTest {

    private val account = AccountEntity(
        id = "acc-1",
        householdId = "hh",
        name = "Сбербанк",
        type = AccountType.CARD_DEBIT,
        balanceMinor = 0L,
        currency = "RUB",
        updatedAt = 0L
    )

    private val category = CategoryEntity(
        id = "cat-1",
        householdId = "hh",
        name = "Продукты",
        type = CategoryType.EXPENSE,
        color = 0,
        iconKey = "",
        sortOrder = 0,
        updatedAt = 0L
    )

    private fun tx(
        amountMinor: Long = 50000L,
        type: TransactionType = TransactionType.EXPENSE,
        note: String? = null
    ) = TransactionEntity(
        id = "tx-1",
        householdId = "hh",
        occurredAt = 1_700_000_000_000L,
        type = type,
        amountMinor = amountMinor,
        accountId = "acc-1",
        categoryId = "cat-1",
        note = note,
        createdBy = "local",
        createdAt = 0L,
        updatedAt = 0L,
        sourceType = SourceType.MANUAL
    )

    @Test
    fun `empty list produces only header row`() {
        val csv = CsvWriter.write(emptyList(), emptyMap(), emptyMap())
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("Дата;Тип;Сумма"))
    }

    @Test
    fun `amount is formatted with two decimal places`() {
        val csv = CsvWriter.write(listOf(tx(amountMinor = 123456L)), emptyMap(), emptyMap())
        assertTrue(csv.contains("1234.56"))
    }

    @Test
    fun `expense type renders as russian label`() {
        val csv = CsvWriter.write(listOf(tx(type = TransactionType.EXPENSE)), emptyMap(), emptyMap())
        assertTrue(csv.contains("Расход"))
    }

    @Test
    fun `income type renders as russian label`() {
        val csv = CsvWriter.write(listOf(tx(type = TransactionType.INCOME)), emptyMap(), emptyMap())
        assertTrue(csv.contains("Доход"))
    }

    @Test
    fun `account name resolved from map`() {
        val csv = CsvWriter.write(
            listOf(tx()),
            emptyMap(),
            mapOf("acc-1" to account)
        )
        assertTrue(csv.contains("Сбербанк"))
    }

    @Test
    fun `category name resolved from map`() {
        val csv = CsvWriter.write(
            listOf(tx()),
            mapOf("cat-1" to category),
            emptyMap()
        )
        assertTrue(csv.contains("Продукты"))
    }

    @Test
    fun `value containing semicolon is quoted`() {
        val csv = CsvWriter.write(
            listOf(tx(note = "note;with;semicolons")),
            emptyMap(),
            emptyMap()
        )
        assertTrue(csv.contains("\"note;with;semicolons\""))
    }

    @Test
    fun `value containing double quote is escaped`() {
        val csv = CsvWriter.write(
            listOf(tx(note = "say \"hello\"")),
            emptyMap(),
            emptyMap()
        )
        assertTrue(csv.contains("\"say \"\"hello\"\"\""))
    }

    @Test
    fun `null note produces empty field`() {
        val csv = CsvWriter.write(listOf(tx(note = null)), emptyMap(), emptyMap())
        val dataLine = csv.lines().first { it.isNotBlank() && !it.startsWith("Дата") }
        // note field is empty (7th field, 0-indexed = index 6)
        val fields = dataLine.split(";")
        assertEquals("", fields[6])
    }
}
