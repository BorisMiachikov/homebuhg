package ru.homebuhg.feature.operations

import androidx.compose.runtime.Composable
import ru.homebuhg.core.designsystem.components.PlaceholderScreen

@Composable
fun OperationEditScreen(
    operationId: Long?,
    onClose: () -> Unit
) {
    val title = if (operationId == null) "Новая операция" else "Редактирование"
    PlaceholderScreen(
        title = title,
        description = "Форма создания/редактирования (Stage 2)",
        onBack = onClose
    )
}
