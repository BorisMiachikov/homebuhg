package ru.homebuhg.feature.accounts

import androidx.compose.runtime.Composable
import ru.homebuhg.core.designsystem.components.PlaceholderScreen

@Composable
fun AccountEditScreen(
    accountId: Long?,
    onClose: () -> Unit
) {
    val title = if (accountId == null) "Новый счёт" else "Редактирование счёта"
    PlaceholderScreen(
        title = title,
        description = "Форма создания/редактирования счёта (Stage 2)",
        onBack = onClose
    )
}
