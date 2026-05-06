package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.homebuhg.core.data.database.entity.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getById(uid: String): UserEntity?

    @Upsert
    suspend fun upsert(user: UserEntity)
}
