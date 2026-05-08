package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.data.database.dao.ReceiptItemDao
import ru.homebuhg.core.data.database.entity.ReceiptItemEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptItemRepository @Inject constructor(
    private val dao: ReceiptItemDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(transactionId: String): Flow<List<ReceiptItemEntity>> =
        dao.observe(transactionId)

    suspend fun allDistinctNames(): List<String> =
        withContext(io) { dao.allDistinctNames() }

    suspend fun getLastPriceForName(name: String): Long? =
        withContext(io) { dao.getLastPriceForName(name) }

    suspend fun replaceAll(transactionId: String, items: List<ReceiptItemEntity>) =
        withContext(io) {
            dao.deleteByTransaction(transactionId)
            if (items.isNotEmpty()) dao.upsertAll(items)
        }
}
