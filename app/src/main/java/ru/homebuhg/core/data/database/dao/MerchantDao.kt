package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.MerchantEntity

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants WHERE householdId = :householdId AND isDeleted = 0 ORDER BY lastUsedAt DESC")
    fun observe(householdId: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE householdId = :householdId AND name LIKE '%' || :query || '%' AND isDeleted = 0 LIMIT 20")
    fun search(householdId: String, query: String): Flow<List<MerchantEntity>>

    @Query("SELECT * FROM merchants WHERE id = :id")
    suspend fun getById(id: String): MerchantEntity?

    @Upsert
    suspend fun upsert(merchant: MerchantEntity)

    @Query("UPDATE merchants SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM merchants WHERE householdId = :householdId AND updatedAt > :since")
    suspend fun getModifiedSince(householdId: String, since: Long): List<MerchantEntity>
}
