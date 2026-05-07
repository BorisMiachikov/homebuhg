package ru.homebuhg.feature.scanner

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {

    sealed interface UiState {
        data object Scanning : UiState
        data object NoCameraPermission : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Scanning)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _scannedEvent = Channel<FnsQrParser.FnsReceipt>(Channel.CONFLATED)
    val scannedEvent: Flow<FnsQrParser.FnsReceipt> = _scannedEvent.receiveAsFlow()

    private val processed = mutableSetOf<String>()
    private var hasScanned = false

    fun onBarcodeDetected(rawValue: String) {
        if (hasScanned || rawValue in processed) return
        processed.add(rawValue)

        val receipt = FnsQrParser.parse(rawValue)
        if (receipt != null) {
            hasScanned = true
            _scannedEvent.trySend(receipt)
        } else {
            processed.remove(rawValue)
        }
    }

    fun onPermissionDenied() {
        _uiState.value = UiState.NoCameraPermission
    }

    fun reset() {
        hasScanned = false
        processed.clear()
        _uiState.value = UiState.Scanning
    }
}
