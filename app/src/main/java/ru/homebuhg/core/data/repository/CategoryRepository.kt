package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.CategoryDao
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(householdId: String): Flow<List<CategoryEntity>> =
        categoryDao.observe(householdId)

    fun observeByType(householdId: String, type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(householdId, type)

    suspend fun getById(id: String): CategoryEntity? =
        withContext(io) { categoryDao.getById(id) }

    suspend fun upsert(category: CategoryEntity) = withContext(io) {
        categoryDao.upsert(category)
    }

    suspend fun delete(id: String) = withContext(io) {
        categoryDao.softDelete(id, nowMillis())
    }

    suspend fun getModifiedSince(householdId: String, since: Long): List<CategoryEntity> =
        withContext(io) { categoryDao.getModifiedSince(householdId, since) }
}
