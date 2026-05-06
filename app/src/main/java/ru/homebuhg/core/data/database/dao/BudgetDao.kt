package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.BudgetEntity

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE householdId = :householdId AND isDeleted = 0")
    fun observe(householdId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM budgets WHERE householdId = :householdId AND updatedAt > :since")
    suspend fun getModifiedSince(householdId: String, since: Long): List<BudgetEntity>
}
