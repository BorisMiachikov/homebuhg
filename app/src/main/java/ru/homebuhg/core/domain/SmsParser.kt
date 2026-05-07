package ru.homebuhg.core.domain

import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.data.database.entity.TransactionType

object SmsParser {

    data class ParseResult(
        val amountMinor: Long,
        val type: TransactionType,
        val merchant: String?,
        val accountId: String?
    )

    fun parse(sender: String, body: String, rules: List<SmsRuleEntity>): ParseResult? {
        for (rule in rules) {
            if (!rule.isActive) continue
            val senderRe = runCatching {
                Regex(rule.senderPattern, RegexOption.IGNORE_CASE)
            }.getOrNull() ?: continue
            if (!senderRe.containsMatchIn(sender)) continue

            val bodyRe = runCatching { Regex(rule.bodyRegex) }.getOrNull() ?: continue
            val match = bodyRe.find(body) ?: continue

            val amountStr = runCatching { match.groups[rule.amountGroup]?.value }.getOrNull() ?: continue
            val amountMinor = amountStr.replace(" ", "").replace(",", ".")
                .toDoubleOrNull()?.times(100)?.toLong() ?: continue

            val merchant = rule.merchantGroup?.let {
                runCatching { match.groups[it]?.value }.getOrNull()
            }

            return ParseResult(
                amountMinor = amountMinor,
                type = rule.type,
                merchant = merchant,
                accountId = rule.accountId
            )
        }
        return null
    }
}
