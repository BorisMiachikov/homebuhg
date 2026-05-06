package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.MerchantDao
import ru.homebuhg.core.data.database.entity.MerchantEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantRepository @Inject constructor(
    private val merchantDao: MerchantDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(householdId: String): Flow<List<MerchantEntity>> =
        merchantDao.observe(householdId)

    fun search(householdId: String, query: String): Flow<List<MerchantEntity>> =
        merchantDao.search(householdId, query)

    suspend fun getById(id: String): MerchantEntity? =
        withContext(io) { merchantDao.getById(id) }

    suspend fun upsert(merchant: MerchantEntity) = withContext(io) {
        merchantDao.upsert(merchant)
    }

    suspend fun delete(id: String) = withContext(io) {
        merchantDao.softDelete(id, nowMillis())
    }

    suspend fun getModifiedSince(householdId: String, since: Long): List<MerchantEntity> =
        withContext(io) { merchantDao.getModifiedSince(householdId, since) }
}
