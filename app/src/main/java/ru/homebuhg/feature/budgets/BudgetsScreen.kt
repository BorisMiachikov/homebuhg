package ru.homebuhg.feature.budgets

import androidx.compose.runtime.Composable
import ru.homebuhg.core.designsystem.components.PlaceholderScreen

@Composable
fun BudgetsScreen(onClose: () -> Unit) {
    PlaceholderScreen(
        title = "Бюджеты и лимиты",
        description = "CRUD бюджетов (Stage 3)",
        onBack = onClose
    )
}
