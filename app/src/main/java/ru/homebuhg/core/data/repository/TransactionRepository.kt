package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.TransactionDao
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(
        householdId: String,
        accountId: String? = null,
        categoryId: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null
    ): Flow<List<TransactionEntity>> =
        transactionDao.observe(householdId, accountId, categoryId, fromMs, toMs)

    fun observeById(id: String): Flow<TransactionEntity?> =
        transactionDao.observeById(id)

    suspend fun getById(id: String): TransactionEntity? =
        withContext(io) { transactionDao.getById(id) }

    suspend fun upsert(transaction: TransactionEntity) = withContext(io) {
        transactionDao.upsert(transaction)
    }

    suspend fun delete(id: String) = withContext(io) {
        transactionDao.softDelete(id, nowMillis())
    }

    suspend fun sumExpenseForCategory(
        householdId: String,
        categoryId: String,
        fromMs: Long,
        toMs: Long
    ): Long = withContext(io) {
        transactionDao.sumExpenseForCategory(householdId, categoryId, fromMs, toMs)
    }

    suspend fun getModifiedSince(householdId: String, since: Long): List<TransactionEntity> =
        withContext(io) { transactionDao.getModifiedSince(householdId, since) }
}
