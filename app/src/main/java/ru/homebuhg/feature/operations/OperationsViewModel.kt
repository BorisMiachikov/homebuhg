package ru.homebuhg.feature.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.domain.SessionManager
import javax.inject.Inject

@HiltViewModel
class OperationsViewModel @Inject constructor(
    sessionManager: SessionManager,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    data class UiState(
        val transactions: List<TransactionEntity> = emptyList(),
        val categoryMap: Map<String, CategoryEntity> = emptyMap(),
        val accountMap: Map<String, AccountEntity> = emptyMap(),
    )

    private val filterAccountId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UiState> = sessionManager.currentHouseholdId
        .flatMapLatest { hid ->
            combine(
                filterAccountId.flatMapLatest { acId ->
                    transactionRepository.observe(hid, accountId = acId)
                },
                categoryRepository.observe(hid),
                accountRepository.observe(hid)
            ) { txs, cats, accounts ->
                UiState(
                    transactions = txs,
                    categoryMap = cats.associateBy { it.id },
                    accountMap = accounts.associateBy { it.id }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setAccountFilter(accountId: String?) {
        filterAccountId.value = accountId
    }
}
