package ru.homebuhg.feature.categories

import androidx.compose.runtime.Composable
import ru.homebuhg.core.designsystem.components.PlaceholderScreen

@Composable
fun CategoriesScreen(onClose: () -> Unit) {
    PlaceholderScreen(
        title = "Категории и магазины",
        description = "Управление категориями (Stage 2)",
        onBack = onClose
    )
}
