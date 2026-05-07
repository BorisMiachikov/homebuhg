package ru.homebuhg.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.SmsRuleRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.domain.SessionManager
import ru.homebuhg.core.domain.SmsParser
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsRuleRepository: SmsRuleRepository
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var accountRepository: AccountRepository
    @Inject lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rules = smsRuleRepository.observeActive().first()
                val result = SmsParser.parse(sender, body, rules) ?: return@launch

                val householdId = sessionManager.currentHouseholdId.first()
                val userId = sessionManager.currentUserId.first()
                val accountId = result.accountId
                    ?: accountRepository.observe(householdId).first().firstOrNull()?.id
                    ?: return@launch

                val now = nowMillis()
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    occurredAt = now,
                    type = result.type,
                    amountMinor = result.amountMinor,
                    accountId = accountId,
                    note = result.merchant,
                    createdBy = userId,
                    createdAt = now,
                    updatedAt = now,
                    sourceType = SourceType.SMS
                )
                transactionRepository.upsert(tx)

                val account = accountRepository.observeById(accountId).first() ?: return@launch
                val delta = if (result.type == TransactionType.EXPENSE) -result.amountMinor else +result.amountMinor
                accountRepository.updateBalance(accountId, account.balanceMinor + delta)
            } finally {
                pending.finish()
            }
        }
    }
}
