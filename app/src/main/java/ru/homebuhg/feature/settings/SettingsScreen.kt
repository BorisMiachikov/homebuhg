package ru.homebuhg.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen(
    onOpenCategories: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurringRules: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Настройки") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Категории и магазины") },
                leadingContent = { Icon(Icons.Outlined.Category, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenCategories)
            )
            ListItem(
                headlineContent = { Text("Бюджеты и лимиты") },
                leadingContent = { Icon(Icons.Outlined.Savings, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenBudgets)
            )
            ListItem(
                headlineContent = { Text("Регулярные операции") },
                leadingContent = { Icon(Icons.Outlined.Repeat, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenRecurringRules)
            )
        }
    }
}
