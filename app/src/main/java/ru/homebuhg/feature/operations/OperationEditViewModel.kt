package ru.homebuhg.feature.operations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.SourceType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.di.IoDispatcher
import ru.homebuhg.core.domain.SessionManager
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OperationEditViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    sealed interface Event {
        data object Saved : Event
        data object Deleted : Event
    }

    // --- form state ---
    var type by mutableStateOf(TransactionType.EXPENSE)
        private set
    var amountText by mutableStateOf("")
        private set
    var occurredAt by mutableLongStateOf(nowMillis())
        private set
    var selectedAccountId by mutableStateOf<String?>(null)
        private set
    var selectedToAccountId by mutableStateOf<String?>(null)
        private set
    var selectedCategoryId by mutableStateOf<String?>(null)
        private set
    var note by mutableStateOf("")
        private set
    var amountError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    private var householdId = ""
    private var userId = "local"
    private var existingTx: TransactionEntity? = null
    private var initialized = false

    // --- reactive lists ---
    val accounts: StateFlow<List<AccountEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> accountRepository.observe(hid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = combine(
        sessionManager.currentHouseholdId,
        snapshotFlow { type }
    ) { hid, t -> Pair(hid, t) }
        .flatMapLatest { (hid, t) ->
            val catType = if (t == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            categoryRepository.observeByType(hid, catType)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun initialize(operationId: String?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            householdId = sessionManager.currentHouseholdId.first()
            userId = sessionManager.currentUserId.first()
            if (operationId != null) {
                withContext(io) { transactionRepository.getById(operationId) }?.let { tx ->
                    existingTx = tx
                    type = tx.type
                    amountText = (tx.amountMinor / 100.0).toBigDecimal().toPlainString()
                    occurredAt = tx.occurredAt
                    selectedAccountId = tx.accountId
                    selectedToAccountId = tx.toAccountId
                    selectedCategoryId = tx.categoryId
                    note = tx.note ?: ""
                }
            }
        }
    }

    @JvmName("changeType")
    fun setType(t: TransactionType) { type = t; selectedCategoryId = null }
    fun setAmount(v: String) { amountText = v; amountError = false }
    fun setAccount(id: String) { selectedAccountId = id }
    fun setToAccount(id: String) { selectedToAccountId = id }
    fun setCategory(id: String) { selectedCategoryId = id }
    @JvmName("changeNote")
    fun setNote(v: String) { note = v }
    @JvmName("changeOccurredAt")
    fun setOccurredAt(ms: Long) { occurredAt = ms }

    fun save() {
        val amountMinor = amountText.replace(",", ".")
            .toBigDecimalOrNull()
            ?.multiply(100.toBigDecimal())
            ?.toLong()
        if (amountMinor == null || amountMinor <= 0L) { amountError = true; return }
        val accountId = selectedAccountId ?: return

        viewModelScope.launch {
            isLoading = true
            val now = nowMillis()
            val newTx = TransactionEntity(
                id = existingTx?.id ?: UUID.randomUUID().toString(),
                householdId = householdId,
                occurredAt = occurredAt,
                type = type,
                amountMinor = amountMinor,
                currency = "RUB",
                accountId = accountId,
                toAccountId = selectedToAccountId.takeIf { type == TransactionType.TRANSFER },
                categoryId = selectedCategoryId.takeIf { type != TransactionType.TRANSFER },
                merchantId = null,
                note = note.trim().ifEmpty { null },
                createdBy = userId,
                createdAt = existingTx?.createdAt ?: now,
                updatedAt = now,
                sourceType = SourceType.MANUAL
            )
            withContext(io) {
                existingTx?.let { reverseBalance(it) }
                transactionRepository.upsert(newTx)
                applyBalance(newTx)
            }
            isLoading = false
            _events.send(Event.Saved)
        }
    }

    fun delete() {
        val tx = existingTx ?: return
        viewModelScope.launch {
            withContext(io) {
                reverseBalance(tx)
                transactionRepository.delete(tx.id)
            }
            _events.send(Event.Deleted)
        }
    }

    private suspend fun reverseBalance(tx: TransactionEntity) {
        when (tx.type) {
            TransactionType.EXPENSE -> adjustBalance(tx.accountId, +tx.amountMinor)
            TransactionType.INCOME -> adjustBalance(tx.accountId, -tx.amountMinor)
            TransactionType.TRANSFER -> {
                adjustBalance(tx.accountId, +tx.amountMinor)
                tx.toAccountId?.let { adjustBalance(it, -tx.amountMinor) }
            }
        }
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
