package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.data.database.dao.SmsRuleDao
import ru.homebuhg.core.data.database.entity.SmsRuleEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRuleRepository @Inject constructor(
    private val dao: SmsRuleDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observeAll(): Flow<List<SmsRuleEntity>> = dao.observeAll()
    fun observeActive(): Flow<List<SmsRuleEntity>> = dao.observeActive()

    suspend fun upsert(rule: SmsRuleEntity) = withContext(io) { dao.upsert(rule) }
    suspend fun setActive(id: String, isActive: Boolean) = withContext(io) { dao.setActive(id, isActive) }
    suspend fun delete(id: String) = withContext(io) { dao.delete(id) }
}
