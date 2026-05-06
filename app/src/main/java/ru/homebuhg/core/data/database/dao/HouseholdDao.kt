package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.HouseholdEntity
import ru.homebuhg.core.data.database.entity.HouseholdMemberEntity

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE id = :id")
    fun observe(id: String): Flow<HouseholdEntity?>

    @Upsert
    suspend fun upsert(household: HouseholdEntity)

    @Query("SELECT * FROM household_members WHERE householdId = :householdId")
    fun observeMembers(householdId: String): Flow<List<HouseholdMemberEntity>>

    @Upsert
    suspend fun upsertMember(member: HouseholdMemberEntity)

    @Query("DELETE FROM household_members WHERE householdId = :householdId AND userUid = :userUid")
    suspend fun removeMember(householdId: String, userUid: String)
}
