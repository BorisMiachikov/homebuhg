package ru.homebuhg.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.homebuhg.core.data.database.entity.BudgetPeriod

@Composable
fun BudgetEditScreen(
    budgetId: String?,
    onClose: () -> Unit,
    viewModel: BudgetEditViewModel = hiltViewModel()
) {
    LaunchedEffect(budgetId) { viewModel.initialize(budgetId) }
    LaunchedEffect(Unit) { viewModel.events.collect { onClose() } }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить бюджет?") },
            text = { Text("Бюджет будет удалён. История операций не затрагивается.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (budgetId == null) "Новый бюджет" else "Редактирование бюджета") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (budgetId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, "Удалить")
                        }
                    }
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp))
                    } else {
                        TextButton(onClick = viewModel::save) { Text("Сохранить") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                val selectedName = viewModel.expenseCategories
                    .find { it.id == viewModel.selectedCategoryId }?.name ?: ""
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория расходов") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    isError = viewModel.categoryError,
                    supportingText = if (viewModel.categoryError) { { Text("Выберите категорию") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    viewModel.expenseCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { viewModel.setCategory(cat.id); categoryExpanded = false }
                        )
                    }
                }
            }

            Column {
                Text("Период", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(
                            selected = viewModel.period == p,
                            onClick = { viewModel.setPeriod(p) },
                            label = { Text(p.label()) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.limitText,
                onValueChange = viewModel::setLimitText,
                label = { Text("Лимит") },
                suffix = { Text("₽") },
                isError = viewModel.limitError,
                supportingText = if (viewModel.limitError) { { Text("Введите положительное число") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Переходящий остаток", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Неиспользованный лимит переносится на следующий период",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = viewModel.isRolling,
                    onCheckedChange = viewModel::setRolling
                )
            }
        }
    }
}

private fun BudgetPeriod.label() = when (this) {
    BudgetPeriod.WEEK -> "Неделя"
    BudgetPeriod.MONTH -> "Месяц"
    BudgetPeriod.YEAR -> "Год"
}
