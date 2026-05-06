package ru.homebuhg.feature.scanner

import androidx.compose.runtime.Composable
import ru.homebuhg.core.designsystem.components.PlaceholderScreen

@Composable
fun ScannerScreen(onClose: () -> Unit) {
    PlaceholderScreen(
        title = "Сканер QR ФНС",
        description = "Сканирование чеков (Stage 6)",
        onBack = onClose
    )
}
