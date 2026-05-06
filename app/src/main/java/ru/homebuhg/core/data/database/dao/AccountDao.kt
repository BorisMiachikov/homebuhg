package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.AccountEntity

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE householdId = :householdId AND isArchived = 0 ORDER BY name ASC")
    fun observe(householdId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: String): Flow<AccountEntity?>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("UPDATE accounts SET balanceMinor = :balanceMinor, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, balanceMinor: Long, updatedAt: Long)

    @Query("SELECT * FROM accounts WHERE householdId = :householdId AND updatedAt > :since")
    suspend fun getModifiedSince(householdId: String, since: Long): List<AccountEntity>
}
