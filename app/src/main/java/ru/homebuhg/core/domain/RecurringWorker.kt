package ru.homebuhg.core.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.common.startOfDayMillis
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.datastore.PreferencesRepository
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.RecurringRuleRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.feature.recurring.TransactionTemplate
import java.time.LocalDate
import java.util.UUID

@HiltWorker
class RecurringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val recurringRuleRepository: RecurringRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val householdId = preferencesRepository.householdId.first() ?: return Result.success()
        val now = nowMillis()
        val dueRules = recurringRuleRepository.getDue(householdId, now)

        dueRules.forEach { rule ->
            runCatching {
                val template = Json.decodeFromString<TransactionTemplate>(rule.templateJson)
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    occurredAt = now,
                    type = template.type,
                    amountMinor = template.amountMinor,
                    currency = "RUB",
                    accountId = template.accountId,
                    toAccountId = template.toAccountId,
                    categoryId = template.categoryId.takeIf { it.isNotBlank() },
                    note = template.note.ifBlank { "Регулярная операция" },
                    createdBy = "recurring",
                    createdAt = now,
                    updatedAt = now,
                    sourceType = SourceType.MANUAL
                )
                transactionRepository.upsert(tx)
                applyBalance(tx)
                val nextDate = RRuleParser.nextOccurrence(rule.rrule, LocalDate.now())
                recurringRuleRepository.updateSchedule(rule.id, nextDate.startOfDayMillis(), now)
            }
        }
        return Result.success()
    }

    private suspend fun applyBalance(tx: TransactionEntity) {
        when (tx.type) {
            TransactionType.EXPENSE -> adjustBalance(tx.accountId, -tx.amountMinor)
            TransactionType.INCOME -> adjustBalance(tx.accountId, +tx.amountMinor)
            TransactionType.TRANSFER -> {
                adjustBalance(tx.accountId, -tx.amountMinor)
                tx.toAccountId?.let { adjustBalance(it, +tx.amountMinor) }
            }
        }
    }

    private suspend fun adjustBalance(accountId: String, delta: Long) {
        val acct = accountRepository.observeById(accountId).first() ?: return
        accountRepository.updateBalance(accountId, acct.balanceMinor + delta)
    }
}
