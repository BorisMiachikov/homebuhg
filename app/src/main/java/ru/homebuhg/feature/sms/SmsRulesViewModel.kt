package ru.homebuhg.feature.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.data.repository.SmsRuleRepository
import javax.inject.Inject

@HiltViewModel
class SmsRulesViewModel @Inject constructor(
    private val repository: SmsRuleRepository
) : ViewModel() {

    val rules: StateFlow<List<SmsRuleEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(id: String, isActive: Boolean) {
        viewModelScope.launch { repository.setActive(id, isActive) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
