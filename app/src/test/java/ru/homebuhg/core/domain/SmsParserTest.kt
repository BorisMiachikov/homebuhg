package ru.homebuhg.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.data.database.entity.TransactionType

class SmsParserTest {

    private fun rule(
        sender: String = "Sberbank",
        body: String = """Покупка (?<amount>[\d ]+[.,]\d{2}) руб""",
        amount: String = "amount",
        merchant: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: String? = null
    ) = SmsRuleEntity(
        id = "test",
        senderPattern = sender,
        bodyRegex = body,
        amountGroup = amount,
        merchantGroup = merchant,
        type = type,
        accountId = accountId,
        isActive = true
    )

    @Test
    fun `match sender and extract amount`() {
        val rules = listOf(rule())
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 1 250,00 руб успешно",
            rules = rules
        )
        assertNotNull(result)
        assertEquals(125000L, result!!.amountMinor)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `match amount without space thousands separator`() {
        val rules = listOf(rule())
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 999,99 руб",
            rules = rules
        )
        assertNotNull(result)
        assertEquals(99999L, result!!.amountMinor)
    }

    @Test
    fun `no match when sender does not match`() {
        val rules = listOf(rule(sender = "Sberbank"))
        val result = SmsParser.parse(
            sender = "Tinkoff",
            body = "Покупка 1 000,00 руб",
            rules = rules
        )
        assertNull(result)
    }

    @Test
    fun `no match when body regex does not match`() {
        val rules = listOf(rule())
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Перевод выполнен",
            rules = rules
        )
        assertNull(result)
    }

    @Test
    fun `extract merchant group when present`() {
        val rules = listOf(
            rule(
                body = """(?<amount>[\d.,]+) руб в (?<merchant>\S+)""",
                merchant = "merchant"
            )
        )
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "500,00 руб в Pyaterochka",
            rules = rules
        )
        assertNotNull(result)
        assertEquals("Pyaterochka", result!!.merchant)
    }

    @Test
    fun `merchant is null when group not set`() {
        val rules = listOf(rule(merchant = null))
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 100,00 руб",
            rules = rules
        )
        assertNotNull(result)
        assertNull(result!!.merchant)
    }

    @Test
    fun `invalid sender regex does not crash, just skips rule`() {
        val badRule = rule(sender = "[invalid(regex")
        val goodRule = rule(sender = "Sberbank")
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 100,00 руб",
            rules = listOf(badRule, goodRule)
        )
        assertNotNull(result)
    }

    @Test
    fun `invalid body regex does not crash, just skips rule`() {
        val badRule = rule(body = "[invalid(regex")
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 100,00 руб",
            rules = listOf(badRule)
        )
        assertNull(result)
    }

    @Test
    fun `inactive rule is ignored`() {
        val inactive = rule().copy(isActive = false)
        val result = SmsParser.parse(
            sender = "Sberbank",
            body = "Покупка 100,00 руб",
            rules = listOf(inactive)
        )
        assertNull(result)
    }

    @Test
    fun `returns accountId from matching rule`() {
        val rules = listOf(rule(accountId = "acc-123"))
        val result = SmsParser.parse("Sberbank", "Покупка 100,00 руб", rules)
        assertEquals("acc-123", result?.accountId)
    }

    @Test
    fun `sender pattern is case insensitive`() {
        val rules = listOf(rule(sender = "SBERBANK"))
        val result = SmsParser.parse("sberbank", "Покупка 100,00 руб", rules)
        assertNotNull(result)
    }

    @Test
    fun `empty rules list returns null`() {
        assertNull(SmsParser.parse("Sberbank", "Покупка 100,00 руб", emptyList()))
    }
}
