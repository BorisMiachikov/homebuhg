package ru.homebuhg.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.homebuhg.core.common.currentRange
import ru.homebuhg.core.data.database.entity.BudgetEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import ru.homebuhg.core.data.repository.BudgetRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.domain.SessionManager
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    data class BudgetProgress(
        val budget: BudgetEntity,
        val categoryName: String,
        val spentMinor: Long,
        val progress: Float,
    )

    data class UiState(
        val items: List<BudgetProgress> = emptyList(),
        val isLoading: Boolean = true,
    )

    val uiState: StateFlow<UiState> = sessionManager.currentHouseholdId
        .flatMapLatest { hid ->
            combine(
                budgetRepository.observe(hid),
                categoryRepository.observe(hid),
                transactionRepository.observe(hid)
            ) { budgets, categories, transactions ->
                val catMap = categories.associateBy { it.id }
                val items = budgets.map { budget ->
                    val (fromMs, toMs) = budget.period.currentRange()
                    val spent = transactions.filter { tx ->
                        tx.categoryId == budget.categoryId &&
                            tx.type == TransactionType.EXPENSE &&
                            !tx.isDeleted &&
                            tx.occurredAt in fromMs..toMs
                    }.sumOf { it.amountMinor }
                    BudgetProgress(
                        budget = budget,
                        categoryName = catMap[budget.categoryId]?.name ?: "—",
                        spentMinor = spent,
                        progress = if (budget.limitMinor > 0) spent.toFloat() / budget.limitMinor else 0f
                    )
                }
                UiState(items = items, isLoading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun delete(id: String) {
        viewModelScope.launch { budgetRepository.delete(id) }
    }
}
