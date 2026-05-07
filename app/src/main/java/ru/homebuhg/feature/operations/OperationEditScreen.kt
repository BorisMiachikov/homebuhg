package ru.homebuhg.feature.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import ru.homebuhg.core.common.formatRuDay
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.TransactionType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperationEditScreen(
    operationId: String?,
    prefillAmountMinor: Long = 0L,
    prefillDateMs: Long = 0L,
    prefillNote: String = "",
    onClose: () -> Unit,
    viewModel: OperationEditViewModel = hiltViewModel()
) {
    LaunchedEffect(operationId) { viewModel.initialize(operationId, prefillAmountMinor, prefillDateMs, prefillNote) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OperationEditViewModel.Event.Saved,
                is OperationEditViewModel.Event.Deleted -> onClose()
            }
        }
    }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setOccurredAt(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить операцию?") },
            text = { Text("Это действие нельзя отменить. Баланс счёта будет скорректирован.") },
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
                title = { Text(if (operationId == null) "Новая операция" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (operationId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
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
            // Type tabs
            val tabs = listOf(
                TransactionType.EXPENSE to "Расход",
                TransactionType.INCOME to "Доход",
                TransactionType.TRANSFER to "Перевод"
            )
            val selectedTab = tabs.indexOfFirst { it.first == viewModel.type }
            TabRow(selectedTabIndex = selectedTab.coerceAtLeast(0)) {
                tabs.forEachIndexed { i, (type, label) ->
                    Tab(
                        selected = i == selectedTab,
                        onClick = { viewModel.setType(type) },
                        text = { Text(label) }
                    )
                }
            }

            // Amount
            OutlinedTextField(
                value = viewModel.amountText,
                onValueChange = viewModel::setAmount,
                label = { Text("Сумма") },
                suffix = { Text("₽") },
                isError = viewModel.amountError,
                supportingText = if (viewModel.amountError) {
                    { Text("Введите сумму больше нуля") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date
            TextButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Дата: ${viewModel.occurredAt.formatRuDay()}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // From account
            AccountDropdown(
                label = if (viewModel.type == TransactionType.TRANSFER) "Счёт (откуда)" else "Счёт",
                accounts = accounts,
                selectedId = viewModel.selectedAccountId,
                onSelect = viewModel::setAccount
            )

            // To account (only for transfer)
            if (viewModel.type == TransactionType.TRANSFER) {
                AccountDropdown(
                    label = "Счёт (куда)",
                    accounts = accounts.filter { it.id != viewModel.selectedAccountId },
                    selectedId = viewModel.selectedToAccountId,
                    onSelect = viewModel::setToAccount
                )
            }

            // Category chips (not for transfer)
            if (viewModel.type != TransactionType.TRANSFER && categories.isNotEmpty()) {
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

            // Note
            OutlinedTextField(
                value = viewModel.note,
                onValueChange = viewModel::setNote,
                label = { Text("Примечание") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.id == selectedId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text("${account.name} · ${account.balanceLabel()}") },
                    onClick = { onSelect(account.id); expanded = false }
                )
            }
        }
    }
}

private fun AccountEntity.balanceLabel(): String {
    val major = balanceMinor / 100.0
    return "%.2f ₽".format(major)
}

