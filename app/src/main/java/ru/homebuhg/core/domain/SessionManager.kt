package ru.homebuhg.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.CategoryDao
import ru.homebuhg.core.data.database.dao.HouseholdDao
import ru.homebuhg.core.data.database.dao.UserDao
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.CategoryType
import ru.homebuhg.core.data.database.entity.HouseholdEntity
import ru.homebuhg.core.data.database.entity.HouseholdMemberEntity
import ru.homebuhg.core.data.database.entity.MemberRole
import ru.homebuhg.core.data.database.entity.UserEntity
import ru.homebuhg.core.data.datastore.PreferencesRepository
import ru.homebuhg.core.di.IoDispatcher
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val preferences: PreferencesRepository,
    private val userDao: UserDao,
    private val householdDao: HouseholdDao,
    private val categoryDao: CategoryDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    val currentHouseholdId: Flow<String> = preferences.householdId.filterNotNull()
    val currentUserId: Flow<String> = preferences.userId.filterNotNull()

    suspend fun ensureLocalSession() = withContext(io) {
        if (preferences.householdId.first() != null) return@withContext

        val userId = "local"
        val householdId = UUID.randomUUID().toString()
        val now = nowMillis()

        userDao.upsert(UserEntity(uid = userId, displayName = "Я", email = "", photoUrl = null))
        householdDao.upsert(
            HouseholdEntity(id = householdId, name = "Мой кошелёк", ownerUid = userId)
        )
        householdDao.upsertMember(
            HouseholdMemberEntity(
                householdId = householdId, userUid = userId,
                role = MemberRole.OWNER, joinedAt = now
            )
        )
        seedCategories(householdId, now)

        preferences.setUserId(userId)
        preferences.setHouseholdId(householdId)
    }

    private suspend fun seedCategories(householdId: String, now: Long) {
        defaultExpenseCategories.forEachIndexed { i, seed ->
            categoryDao.upsert(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    name = seed.name,
                    type = CategoryType.EXPENSE,
                    color = seed.color,
                    iconKey = seed.icon,
                    sortOrder = i,
                    updatedAt = now
                )
            )
        }
        defaultIncomeCategories.forEachIndexed { i, seed ->
            categoryDao.upsert(
                CategoryEntity(
                    id = UUID.randomUUID().toString(),
                    householdId = householdId,
                    name = seed.name,
                    type = CategoryType.INCOME,
                    color = seed.color,
                    iconKey = seed.icon,
                    sortOrder = i,
                    updatedAt = now
                )
            )
        }
    }
}

private data class CategorySeed(val name: String, val icon: String, val color: Int)

private val defaultExpenseCategories = listOf(
    CategorySeed("Продукты", "shopping_cart", 0xFF4CAF50.toInt()),
    CategorySeed("Транспорт", "directions_car", 0xFF2196F3.toInt()),
    CategorySeed("Кафе и рестораны", "restaurant", 0xFFFF9800.toInt()),
    CategorySeed("ЖКХ", "home", 0xFF9C27B0.toInt()),
    CategorySeed("Здоровье", "local_hospital", 0xFFF44336.toInt()),
    CategorySeed("Развлечения", "theaters", 0xFFE91E63.toInt()),
    CategorySeed("Одежда", "checkroom", 0xFF00BCD4.toInt()),
    CategorySeed("Связь", "phone", 0xFF607D8B.toInt()),
    CategorySeed("Прочее", "more_horiz", 0xFF9E9E9E.toInt()),
)

private val defaultIncomeCategories = listOf(
    CategorySeed("Зарплата", "work", 0xFF4CAF50.toInt()),
    CategorySeed("Фриланс", "laptop", 0xFF2196F3.toInt()),
    CategorySeed("Кэшбэк", "card_giftcard", 0xFFFF9800.toInt()),
    CategorySeed("Прочее", "more_horiz", 0xFF9E9E9E.toInt()),
)
