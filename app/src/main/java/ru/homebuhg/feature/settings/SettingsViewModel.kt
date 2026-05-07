package ru.homebuhg.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.homebuhg.core.auth.AuthRepository
import ru.homebuhg.core.data.datastore.PreferencesRepository
import ru.homebuhg.core.domain.SessionManager
import ru.homebuhg.core.sync.FirestoreSyncService
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncService: FirestoreSyncService,
    private val preferencesRepository: PreferencesRepository,
    sessionManager: SessionManager
) : ViewModel() {

    data class UiState(
        val isFirebaseAvailable: Boolean = false,
        val email: String? = null,
        val isSignedIn: Boolean = false,
        val lastSyncMs: Long = 0L,
        val isSyncing: Boolean = false,
        val error: String? = null
    )

    private val _isSyncing = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val householdIdFlow = sessionManager.currentHouseholdId

    val uiState: StateFlow<UiState> = combine(
        authRepository.currentUser,
        preferencesRepository.lastSyncMs,
        _isSyncing,
        _error
    ) { user, lastSync, syncing, error ->
        UiState(
            isFirebaseAvailable = authRepository.isFirebaseAvailable,
            email = user?.email,
            isSignedIn = user != null,
            lastSyncMs = lastSync,
            isSyncing = syncing,
            error = error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UiState(isFirebaseAvailable = authRepository.isFirebaseAvailable)
    )

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _error.value = null
        authRepository.signInWithEmail(email, password)
            .onFailure { _error.value = it.message }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _error.value = null
        authRepository.signUpWithEmail(email, password)
            .onFailure { _error.value = it.message }
    }

    fun signOut() {
        authRepository.signOut()
        _error.value = null
    }

    fun syncNow() = viewModelScope.launch {
        if (_isSyncing.value) return@launch
        _isSyncing.value = true
        _error.value = null
        try {
            val hid = householdIdFlow.first()
            syncService.sync(hid)
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isSyncing.value = false
        }
    }

    fun clearError() { _error.value = null }
}
