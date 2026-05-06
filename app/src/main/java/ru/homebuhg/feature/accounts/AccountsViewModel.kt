package ru.homebuhg.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.domain.SessionManager
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    sessionManager: SessionManager,
    accountRepository: AccountRepository,
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> accountRepository.observe(hid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
