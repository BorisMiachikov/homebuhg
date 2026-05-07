package ru.homebuhg.feature.sms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.SmsRuleRepository
import ru.homebuhg.core.domain.SessionManager
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SmsRuleEditViewModel @Inject constructor(
    private val repository: SmsRuleRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    var senderPattern by mutableStateOf("")
        private set
    var bodyRegex by mutableStateOf("")
        private set
    var amountGroup by mutableStateOf("amount")
        private set
    var merchantGroup by mutableStateOf("")
        private set
    var type by mutableStateOf(TransactionType.EXPENSE)
        private set
    var selectedAccountId by mutableStateOf<String?>(null)
        private set
    var isActive by mutableStateOf(true)
        private set

    private var ruleId: String? = null
    private var initialized = false

    private val _events = Channel<Unit>()
    val events = _events.receiveAsFlow()

    val accounts: StateFlow<List<AccountEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> accountRepository.observe(hid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun initialize(ruleId: String?) {
        if (initialized) return
        initialized = true
        this.ruleId = ruleId
        if (ruleId == null) return
        viewModelScope.launch {
            val rule = repository.observeAll().first().find { it.id == ruleId } ?: return@launch
            senderPattern = rule.senderPattern
            bodyRegex = rule.bodyRegex
            amountGroup = rule.amountGroup
            merchantGroup = rule.merchantGroup ?: ""
            type = rule.type
            selectedAccountId = rule.accountId
            isActive = rule.isActive
        }
    }

    fun setSenderPattern(v: String) { senderPattern = v }
    fun setBodyRegex(v: String) { bodyRegex = v }
    fun setAmountGroup(v: String) { amountGroup = v }
    fun setMerchantGroup(v: String) { merchantGroup = v }
    fun setType(t: TransactionType) { type = t }
    fun setAccount(id: String?) { selectedAccountId = id }

    fun save() {
        if (senderPattern.isBlank() || bodyRegex.isBlank() || amountGroup.isBlank()) return
        val id = ruleId ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.upsert(
                SmsRuleEntity(
                    id = id,
                    senderPattern = senderPattern.trim(),
                    bodyRegex = bodyRegex.trim(),
                    amountGroup = amountGroup.trim(),
                    merchantGroup = merchantGroup.trim().takeIf { it.isNotEmpty() },
                    type = type,
                    accountId = selectedAccountId,
                    isActive = isActive
                )
            )
            _events.send(Unit)
        }
    }
}
