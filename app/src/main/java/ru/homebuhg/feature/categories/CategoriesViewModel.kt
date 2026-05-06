package ru.homebuhg.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.di.IoDispatcher
import ru.homebuhg.core.domain.SessionManager
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val categoryRepository: CategoryRepository,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    val expenseCategories: StateFlow<List<CategoryEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> categoryRepository.observeByType(hid, CategoryType.EXPENSE) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategories: StateFlow<List<CategoryEntity>> = sessionManager.currentHouseholdId
        .flatMapLatest { hid -> categoryRepository.observeByType(hid, CategoryType.INCOME) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val hid = sessionManager.currentHouseholdId.first()
            val existing = if (type == CategoryType.EXPENSE) expenseCategories.value else incomeCategories.value
            withContext(io) {
                categoryRepository.upsert(
                    CategoryEntity(
                        id = UUID.randomUUID().toString(),
                        householdId = hid,
                        name = name.trim(),
                        type = type,
                        color = 0xFF9E9E9E.toInt(),
                        iconKey = "label",
                        sortOrder = existing.size,
                        updatedAt = nowMillis()
                    )
                )
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            withContext(io) { categoryRepository.delete(id) }
        }
    }
}
