package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions
        WHERE householdId = :householdId AND isDeleted = 0
        AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:fromMs IS NULL OR occurredAt >= :fromMs)
        AND (:toMs IS NULL OR occurredAt <= :toMs)
        ORDER BY occurredAt DESC
    """)
    fun observe(
        householdId: String,
        accountId: String? = null,
        categoryId: String? = null,
        fromMs: Long? = null,
        toMs: Long? = null
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE householdId = :householdId AND categoryId = :categoryId
        AND type = 'EXPENSE' AND isDeleted = 0
        AND occurredAt >= :fromMs AND occurredAt <= :toMs
    """)
    suspend fun sumExpenseForCategory(
        householdId: String,
        categoryId: String,
        fromMs: Long,
        toMs: Long
    ): Long

    @Query("""
        SELECT COALESCE(SUM(amountMinor), 0) FROM transactions
        WHERE householdId = :householdId AND accountId = :accountId
        AND type != 'TRANSFER' AND isDeleted = 0
        AND occurredAt >= :fromMs AND occurredAt <= :toMs
    """)
    suspend fun sumForAccount(
        householdId: String,
        accountId: String,
        fromMs: Long,
        toMs: Long
    ): Long

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM transactions WHERE householdId = :householdId AND updatedAt > :since")
    suspend fun getModifiedSince(householdId: String, since: Long): List<TransactionEntity>
}
