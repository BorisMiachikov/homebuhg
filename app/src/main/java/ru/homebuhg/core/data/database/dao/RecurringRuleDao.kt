package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.RecurringRuleEntity

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_rules WHERE householdId = :householdId AND isActive = 1")
    fun observe(householdId: String): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE householdId = :householdId AND isActive = 1 AND nextRunAt <= :nowMs")
    suspend fun getDue(householdId: String, nowMs: Long): List<RecurringRuleEntity>

    @Upsert
    suspend fun upsert(rule: RecurringRuleEntity)

    @Query("UPDATE recurring_rules SET nextRunAt = :nextRunAt, lastRunAt = :lastRunAt WHERE id = :id")
    suspend fun updateSchedule(id: String, nextRunAt: Long, lastRunAt: Long)

    @Query("UPDATE recurring_rules SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String)
}
