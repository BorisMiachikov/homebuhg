package ru.homebuhg.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_CURRENCY = stringPreferencesKey("default_currency")
        val KEY_LAST_SYNC_MS = androidx.datastore.preferences.core.longPreferencesKey("last_sync_ms")
    }

    val householdId: Flow<String?> = dataStore.data.map { it[KEY_HOUSEHOLD_ID] }
    val userId: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }
    val defaultCurrency: Flow<String> = dataStore.data.map { it[KEY_CURRENCY] ?: "RUB" }
    val lastSyncMs: Flow<Long> = dataStore.data.map { it[KEY_LAST_SYNC_MS] ?: 0L }

    suspend fun setHouseholdId(id: String) = dataStore.edit { it[KEY_HOUSEHOLD_ID] = id }
    suspend fun setUserId(id: String) = dataStore.edit { it[KEY_USER_ID] = id }
    suspend fun setLastSyncMs(ms: Long) = dataStore.edit { it[KEY_LAST_SYNC_MS] = ms }
}
