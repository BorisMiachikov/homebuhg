package ru.homebuhg.core.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE householdId = :householdId AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observe(householdId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE householdId = :householdId AND type = :type AND isDeleted = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeByType(householdId: String, type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("SELECT * FROM categories WHERE householdId = :householdId AND updatedAt > :since")
    suspend fun getModifiedSince(householdId: String, since: Long): List<CategoryEntity>
}
