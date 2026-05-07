package ru.homebuhg.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.TransactionType

private val freqOptions = listOf(
    "DAILY" to "Каждый день",
    "WEEKLY" to "Каждую неделю",
    "MONTHLY" to "Каждый месяц",
    "YEARLY" to "Каждый год",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringRuleEditScreen(
    ruleId: String?,
    onClose: () -> Unit,
    viewModel: RecurringRuleEditViewModel = hiltViewModel()
) {
    LaunchedEffect(ruleId) { viewModel.initialize(ruleId) }
    LaunchedEffect(Unit) { viewModel.events.collect { onClose() } }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить правило?") },
            text = { Text("Правило будет деактивировано. Уже созданные операции сохранятся.") },
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
                title = { Text(if (ruleId == null) "Новое правило" else "Редактирование правила") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (ruleId != null) {
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
            val tabs = listOf(TransactionType.EXPENSE to "Расход", TransactionType.INCOME to "Доход")
            val selectedTab = tabs.indexOfFirst { it.first == viewModel.type }.coerceAtLeast(0)
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { i, (t, label) ->
                    Tab(selected = i == selectedTab, onClick = { viewModel.setType(t) }, text = { Text(label) })
                }
            }

            OutlinedTextField(
                value = viewModel.amountText,
                onValueChange = viewModel::setAmount,
                label = { Text("Сумма") },
                suffix = { Text("₽") },
                isError = viewModel.amountError,
                supportingText = if (viewModel.amountError) { { Text("Введите сумму больше нуля") } } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            AccountDropdown(
                accounts = accounts,
                selectedId = viewModel.selectedAccountId,
                isError = viewModel.accountError,
                onSelect = viewModel::setAccount
            )

            if (categories.isNotEmpty()) {
                Column {
                    Text("Категория", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = cat.id == viewModel.selectedCategoryId,
                                onClick = { viewModel.setCategory(cat.id) },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.note,
                onValueChange = viewModel::setNote,
                label = { Text("Примечание") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            Column {
                Text("Частота", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    freqOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = viewModel.freq == key,
                            onClick = { viewModel.setFreq(key) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (viewModel.freq != "DAILY") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.interval.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { viewModel.setInterval(it) } },
                        label = { Text("Каждые N") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    val freqUnit = when (viewModel.freq) {
                        "WEEKLY" -> "нед."
                        "MONTHLY" -> "мес."
                        "YEARLY" -> "лет"
                        else -> ""
                    }
                    if (freqUnit.isNotBlank()) {
                        Text(
                            freqUnit,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            if (viewModel.freq == "MONTHLY") {
                OutlinedTextField(
                    value = viewModel.byMonthDay?.toString() ?: "",
                    onValueChange = { v ->
                        val day = v.toIntOrNull()
                        viewModel.setByMonthDay(
                            if (day != null && day in 1..31) day else null
                        )
                    },
                    label = { Text("День месяца (1–31, необязательно)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun AccountDropdown(
    accounts: List<AccountEntity>,
    selectedId: String?,
    isError: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedId }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Счёт") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = isError,
            supportingText = if (isError) { { Text("Выберите счёт") } } else null,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name) },
                    onClick = { onSelect(account.id); expanded = false }
                )
            }
        }
    }
}
