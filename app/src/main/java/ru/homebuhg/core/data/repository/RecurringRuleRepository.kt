package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.data.database.dao.RecurringRuleDao
import ru.homebuhg.core.data.database.entity.RecurringRuleEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleRepository @Inject constructor(
    private val recurringRuleDao: RecurringRuleDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(householdId: String): Flow<List<RecurringRuleEntity>> =
        recurringRuleDao.observe(householdId)

    suspend fun getById(id: String): RecurringRuleEntity? =
        withContext(io) { recurringRuleDao.getById(id) }

    suspend fun getDue(householdId: String, nowMs: Long): List<RecurringRuleEntity> =
        withContext(io) { recurringRuleDao.getDue(householdId, nowMs) }

    suspend fun upsert(rule: RecurringRuleEntity) = withContext(io) {
        recurringRuleDao.upsert(rule)
    }

    suspend fun updateSchedule(id: String, nextRunAt: Long, lastRunAt: Long) = withContext(io) {
        recurringRuleDao.updateSchedule(id, nextRunAt, lastRunAt)
    }

    suspend fun deactivate(id: String) = withContext(io) {
        recurringRuleDao.deactivate(id)
    }
}
