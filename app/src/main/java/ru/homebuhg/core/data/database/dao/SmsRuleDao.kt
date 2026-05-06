package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.SmsRuleEntity

@Dao
interface SmsRuleDao {
    @Query("SELECT * FROM sms_rules WHERE isActive = 1")
    fun observeActive(): Flow<List<SmsRuleEntity>>

    @Query("SELECT * FROM sms_rules")
    fun observeAll(): Flow<List<SmsRuleEntity>>

    @Upsert
    suspend fun upsert(rule: SmsRuleEntity)

    @Query("UPDATE sms_rules SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: String, isActive: Boolean)

    @Query("DELETE FROM sms_rules WHERE id = :id")
    suspend fun delete(id: String)
}
