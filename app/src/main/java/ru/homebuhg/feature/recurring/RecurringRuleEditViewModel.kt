package ru.homebuhg.feature.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.serialization.json.Json
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.common.startOfDayMillis
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.RecurringRuleEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.RecurringRuleRepository
import ru.homebuhg.core.domain.RRuleParser
import ru.homebuhg.core.domain.SessionManager
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RecurringRuleEditViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val recurringRuleRepository: RecurringRuleRepository,
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    sealed interface Event {
        data object Saved : Event
        data object Deleted : Event
    }

    var type by mutableStateOf(TransactionType.EXPENSE)
        private set
    var amountText by mutableStateOf("")
        private set
    var selectedCategoryId by mutableStateOf<String?>(null)
        private set
    var selectedAccountId by mutableStateOf<String?>(null)
        private set
    var note by mutableStateOf("")
        private set
    var freq by mutableStateOf("MONTHLY")
        private set
    var interval by mutableIntStateOf(1)
        private set
    var byMonthDay by mutableStateOf<Int?>(null)
        private set
    var amountError by mutableStateOf(false)
        private set
    var accountError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    private var householdId = ""
    private var existingRule: RecurringRuleEntity? = null
    private var initialized = false

    val accounts: StateFlow<List<AccountEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> accountRepository.observe(hid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = combine(
        sessionManager.currentHouseholdId,
        snapshotFlow { type }
    ) { hid, t -> hid to t }
        .flatMapLatest { (hid, t) ->
            val catType = if (t == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
            categoryRepository.observeByType(hid, catType)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun initialize(ruleId: String?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            householdId = sessionManager.currentHouseholdId.first()
            if (ruleId != null) {
                recurringRuleRepository.getById(ruleId)?.let { rule ->
                    existingRule = rule
                    val template = runCatching {
                        Json.decodeFromString<TransactionTemplate>(rule.templateJson)
                    }.getOrNull()
                    if (template != null) {
                        type = template.type
                        amountText = (template.amountMinor / 100.0).toBigDecimal().toPlainString()
                        selectedCategoryId = template.categoryId.takeIf { it.isNotBlank() }
                        selectedAccountId = template.accountId.takeIf { it.isNotBlank() }
                        note = template.note
                    }
                    val params = rule.rrule.split(";").associate { part ->
                        val eq = part.indexOf('=')
                        if (eq < 0) part.trim() to "" else part.substring(0, eq).trim() to part.substring(eq + 1).trim()
                    }
                    freq = params["FREQ"] ?: "MONTHLY"
                    interval = params["INTERVAL"]?.toIntOrNull() ?: 1
                    byMonthDay = params["BYMONTHDAY"]?.toIntOrNull()
                }
            }
        }
    }

    @JvmName("changeType")
    fun setType(t: TransactionType) { type = t; selectedCategoryId = null }
    fun setAmount(v: String) { amountText = v; amountError = false }
    fun setCategory(id: String) { selectedCategoryId = id }
    fun setAccount(id: String) { selectedAccountId = id; accountError = false }
    @JvmName("changeNote")
    fun setNote(v: String) { note = v }
    @JvmName("changeFreq")
    fun setFreq(f: String) { freq = f; if (f != "MONTHLY") byMonthDay = null }
    @JvmName("changeInterval")
    fun setInterval(v: Int) { interval = v.coerceAtLeast(1) }
    @JvmName("changeByMonthDay")
    fun setByMonthDay(v: Int?) { byMonthDay = v }

    fun save() {
        val amountMinor = amountText.replace(",", ".")
            .toBigDecimalOrNull()
            ?.multiply(100.toBigDecimal())
            ?.toLong() ?: -1L
        if (amountMinor <= 0) { amountError = true; return }
        val accountId = selectedAccountId
        if (accountId == null) { accountError = true; return }

        val template = TransactionTemplate(
            type = type,
            amountMinor = amountMinor,
            categoryId = selectedCategoryId ?: "",
            accountId = accountId,
            note = note.trim()
        )
        val rrule = RRuleParser.buildRRule(freq, interval, byMonthDay.takeIf { freq == "MONTHLY" })

        viewModelScope.launch {
            isLoading = true
            val now = nowMillis()
            val nextRunAt = existingRule?.nextRunAt ?: LocalDate.now().startOfDayMillis()
            val rule = RecurringRuleEntity(
                id = existingRule?.id ?: UUID.randomUUID().toString(),
                householdId = householdId,
                templateJson = Json.encodeToString(TransactionTemplate.serializer(), template),
                rrule = rrule,
                nextRunAt = nextRunAt,
                lastRunAt = existingRule?.lastRunAt,
                isActive = true,
                updatedAt = now
            )
            recurringRuleRepository.upsert(rule)
            isLoading = false
            _events.send(Event.Saved)
        }
    }

    fun delete() {
        val rule = existingRule ?: return
        viewModelScope.launch {
            recurringRuleRepository.deactivate(rule.id)
            _events.send(Event.Deleted)
        }
    }
}
