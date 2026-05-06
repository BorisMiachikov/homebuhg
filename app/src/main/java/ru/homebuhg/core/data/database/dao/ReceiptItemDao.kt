package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.ReceiptItemEntity

@Dao
interface ReceiptItemDao {
    @Query("SELECT * FROM receipt_items WHERE transactionId = :transactionId")
    fun observe(transactionId: String): Flow<List<ReceiptItemEntity>>

    @Query("SELECT * FROM receipt_items WHERE transactionId = :transactionId")
    suspend fun getByTransaction(transactionId: String): List<ReceiptItemEntity>

    @Upsert
    suspend fun upsert(item: ReceiptItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<ReceiptItemEntity>)

    @Query("DELETE FROM receipt_items WHERE transactionId = :transactionId")
    suspend fun deleteByTransaction(transactionId: String)
}
