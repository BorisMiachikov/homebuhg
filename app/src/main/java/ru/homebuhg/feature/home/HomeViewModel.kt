package ru.homebuhg.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.domain.SessionManager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionManager: SessionManager,
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val totalBalanceMinor: Long = 0L,
        val accounts: List<AccountEntity> = emptyList(),
        val recentTransactions: List<TransactionEntity> = emptyList(),
    )

    val uiState: StateFlow<UiState> = sessionManager.currentHouseholdId
        .flatMapLatest { hid ->
            combine(
                accountRepository.observe(hid),
                transactionRepository.observe(hid).map { it.take(5) }
            ) { accounts, txs ->
                UiState(
                    isLoading = false,
                    totalBalanceMinor = accounts.sumOf { it.balanceMinor },
                    accounts = accounts,
                    recentTransactions = txs
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())
}
