package ru.homebuhg.feature.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.AccountType
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.di.IoDispatcher
import ru.homebuhg.core.domain.SessionManager
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val accountRepository: AccountRepository,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    sealed interface Event {
        data object Saved : Event
        data object Deleted : Event
    }

    var name by mutableStateOf("")
        private set
    var type by mutableStateOf(AccountType.CARD_DEBIT)
        private set
    var initialBalanceText by mutableStateOf("0")
        private set
    var color by mutableIntStateOf(0xFF2196F3.toInt())
        private set
    var nameError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    private var householdId = ""
    private var existingAccount: AccountEntity? = null
    private var initialized = false

    fun initialize(accountId: String?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            householdId = sessionManager.currentHouseholdId.first()
            if (accountId != null) {
                withContext(io) { accountRepository.observeById(accountId).first() }?.let { acct ->
                    existingAccount = acct
                    name = acct.name
                    type = acct.type
                    initialBalanceText = (acct.balanceMinor / 100.0).toBigDecimal().toPlainString()
                    color = acct.color
                }
            }
        }
    }

    @JvmName("changeName")
    fun setName(v: String) { name = v; nameError = false }
    @JvmName("changeType")
    fun setType(t: AccountType) { type = t }
    fun setInitialBalance(v: String) { initialBalanceText = v }
    @JvmName("changeColor")
    fun setColor(c: Int) { color = c }

    fun save() {
        if (name.isBlank()) { nameError = true; return }
        val balanceMinor = initialBalanceText.replace(",", ".")
            .toBigDecimalOrNull()
            ?.multiply(100.toBigDecimal())
            ?.toLong() ?: 0L

        viewModelScope.launch {
            isLoading = true
            val now = nowMillis()
            val account = AccountEntity(
                id = existingAccount?.id ?: UUID.randomUUID().toString(),
                householdId = householdId,
                name = name.trim(),
                type = type,
                currency = "RUB",
                balanceMinor = if (existingAccount == null) balanceMinor else existingAccount!!.balanceMinor,
                color = color,
                iconKey = type.defaultIconKey(),
                updatedAt = now
            )
            withContext(io) { accountRepository.upsert(account) }
            isLoading = false
            _events.send(Event.Saved)
        }
    }

    fun delete() {
        val acct = existingAccount ?: return
        viewModelScope.launch {
            val archived = acct.copy(isArchived = true, updatedAt = nowMillis())
            withContext(io) { accountRepository.upsert(archived) }
            _events.send(Event.Deleted)
        }
    }
}

private fun AccountType.defaultIconKey() = when (this) {
    AccountType.CARD_DEBIT -> "credit_card"
    AccountType.CARD_CREDIT -> "credit_score"
    AccountType.CASH -> "payments"
}
