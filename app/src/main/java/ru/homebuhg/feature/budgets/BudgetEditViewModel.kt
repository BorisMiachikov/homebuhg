package ru.homebuhg.feature.budgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.entity.BudgetEntity
import ru.homebuhg.core.data.database.entity.BudgetPeriod
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.repository.BudgetRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.domain.SessionManager
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BudgetEditViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    sealed interface Event {
        data object Saved : Event
        data object Deleted : Event
    }

    var expenseCategories by mutableStateOf<List<CategoryEntity>>(emptyList())
        private set
    var selectedCategoryId by mutableStateOf<String?>(null)
        private set
    var period by mutableStateOf(BudgetPeriod.MONTH)
        private set
    var limitText by mutableStateOf("")
        private set
    var isRolling by mutableStateOf(false)
        private set
    var limitError by mutableStateOf(false)
        private set
    var categoryError by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _events = Channel<Event>()
    val events = _events.receiveAsFlow()

    private var householdId = ""
    private var existingBudget: BudgetEntity? = null
    private var initialized = false

    fun initialize(budgetId: String?) {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            householdId = sessionManager.currentHouseholdId.first()
            expenseCategories = categoryRepository
                .observeByType(householdId, CategoryType.EXPENSE)
                .first()
            if (budgetId != null) {
                budgetRepository.getById(budgetId)?.let { budget ->
                    existingBudget = budget
                    selectedCategoryId = budget.categoryId
                    period = budget.period
                    limitText = (budget.limitMinor / 100.0).toBigDecimal().toPlainString()
                    isRolling = budget.isRolling
                }
            }
        }
    }

    fun setCategory(id: String) { selectedCategoryId = id; categoryError = false }
    @JvmName("changePeriod")
    fun setPeriod(p: BudgetPeriod) { period = p }
    @JvmName("changeLimitText")
    fun setLimitText(v: String) { limitText = v; limitError = false }
    @JvmName("changeRolling")
    fun setRolling(v: Boolean) { isRolling = v }

    fun save() {
        val catId = selectedCategoryId
        if (catId == null) { categoryError = true; return }
        val limitMinor = limitText.replace(",", ".")
            .toBigDecimalOrNull()
            ?.multiply(100.toBigDecimal())
            ?.toLong() ?: -1L
        if (limitMinor <= 0) { limitError = true; return }

        viewModelScope.launch {
            isLoading = true
            val now = nowMillis()
            val budget = BudgetEntity(
                id = existingBudget?.id ?: UUID.randomUUID().toString(),
                householdId = householdId,
                categoryId = catId,
                period = period,
                limitMinor = limitMinor,
                startDate = existingBudget?.startDate ?: now,
                isRolling = isRolling,
                updatedAt = now
            )
            budgetRepository.upsert(budget)
            isLoading = false
            _events.send(Event.Saved)
        }
    }

    fun delete() {
        val budget = existingBudget ?: return
        viewModelScope.launch {
            budgetRepository.delete(budget.id)
            _events.send(Event.Deleted)
        }
    }
}
