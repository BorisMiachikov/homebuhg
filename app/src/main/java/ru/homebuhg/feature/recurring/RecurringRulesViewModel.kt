package ru.homebuhg.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ru.homebuhg.core.data.database.entity.RecurringRuleEntity
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.RecurringRuleRepository
import ru.homebuhg.core.domain.RRuleParser
import ru.homebuhg.core.domain.SessionManager
import javax.inject.Inject

@HiltViewModel
class RecurringRulesViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val recurringRuleRepository: RecurringRuleRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    data class RuleItem(
        val rule: RecurringRuleEntity,
        val title: String,
        val subtitle: String,
        val amountMinor: Long,
    )

    data class UiState(
        val items: List<RuleItem> = emptyList(),
        val isLoading: Boolean = true,
    )

    val uiState: StateFlow<UiState> = sessionManager.currentHouseholdId
        .flatMapLatest { hid ->
            combine(
                recurringRuleRepository.observe(hid),
                categoryRepository.observe(hid)
            ) { rules, categories ->
                val catMap = categories.associateBy { it.id }
                val items = rules.map { rule ->
                    val template = runCatching {
                        Json.decodeFromString<TransactionTemplate>(rule.templateJson)
                    }.getOrNull()
                    val categoryName = template?.categoryId?.let { catMap[it]?.name }.orEmpty()
                    val title = categoryName.ifBlank {
                        template?.note?.takeIf { it.isNotBlank() } ?: "Правило"
                    }
                    RuleItem(
                        rule = rule,
                        title = title,
                        subtitle = RRuleParser.freqLabel(rule.rrule),
                        amountMinor = template?.amountMinor ?: 0L
                    )
                }
                UiState(items = items, isLoading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun deactivate(id: String) {
        viewModelScope.launch { recurringRuleRepository.deactivate(id) }
    }
}
