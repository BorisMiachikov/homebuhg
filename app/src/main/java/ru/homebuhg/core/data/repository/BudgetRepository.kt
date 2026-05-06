package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.BudgetDao
import ru.homebuhg.core.data.database.entity.BudgetEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(householdId: String): Flow<List<BudgetEntity>> =
        budgetDao.observe(householdId)

    suspend fun getById(id: String): BudgetEntity? =
        withContext(io) { budgetDao.getById(id) }

    suspend fun upsert(budget: BudgetEntity) = withContext(io) {
        budgetDao.upsert(budget)
    }

    suspend fun delete(id: String) = withContext(io) {
        budgetDao.softDelete(id, nowMillis())
    }

    suspend fun getModifiedSince(householdId: String, since: Long): List<BudgetEntity> =
        withContext(io) { budgetDao.getModifiedSince(householdId, since) }
}
