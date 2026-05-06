package ru.homebuhg.core.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.data.database.dao.AccountDao
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    @IoDispatcher private val io: CoroutineDispatcher
) {
    fun observe(householdId: String): Flow<List<AccountEntity>> =
        accountDao.observe(householdId)

    fun observeById(id: String): Flow<AccountEntity?> =
        accountDao.observeById(id)

    suspend fun upsert(account: AccountEntity) = withContext(io) {
        accountDao.upsert(account)
    }

    suspend fun updateBalance(id: String, balanceMinor: Long) = withContext(io) {
        accountDao.updateBalance(id, balanceMinor, nowMillis())
    }

    suspend fun getModifiedSince(householdId: String, since: Long): List<AccountEntity> =
        withContext(io) { accountDao.getModifiedSince(householdId, since) }
}
