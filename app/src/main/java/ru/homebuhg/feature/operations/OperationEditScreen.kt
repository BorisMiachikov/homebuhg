package ru.homebuhg.feature.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.homebuhg.core.common.formatRuDay
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.ReceiptItemEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperationEditScreen(
    operationId: String?,
    prefillAmountMinor: Long = 0L,
    prefillDateMs: Long = 0L,
    prefillNote: String = "",
    initialType: String? = null,
    onClose: () -> Unit,
    viewModel: OperationEditViewModel = hiltViewModel()
) {
    LaunchedEffect(operationId) { viewModel.initialize(operationId, prefillAmountMinor, prefillDateMs, prefillNote, initialType) }
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
    var showItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ReceiptItemEntity?>(null) }

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

    if (showItemDialog) {
        AddEditItemDialog(
            initial = editingItem,
            knownNames = viewModel.knownNames,
            onGetLastPrice = { name -> viewModel.getLastPriceForName(name) },
            onConfirm = { item ->
                if (editingItem == null) viewModel.addItem(item) else viewModel.updateItem(item)
                editingItem = null
                showItemDialog = false
            },
            onDismiss = { editingItem = null; showItemDialog = false }
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

            // Date — OutlinedTextField с иконкой, клик открывает пикер
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.occurredAt.formatRuDay(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата") },
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Выбрать дату")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                // Прозрачный оверлей перехватывает клики
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
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

            // Items section
            Column {
                Text("Позиции", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                // Шапка таблицы — всегда видна
                ItemsTableHeader()
                HorizontalDivider()

                if (viewModel.items.isEmpty()) {
                    Text(
                        "Нет позиций. Нажмите «+ Добавить позицию».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    viewModel.items.forEach { item ->
                        ItemRow(
                            item = item,
                            onEdit = {
                                editingItem = item
                                showItemDialog = true
                            },
                            onDelete = { viewModel.removeItem(item.id) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                TextButton(
                    onClick = { editingItem = null; showItemDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Добавить позицию") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isLoading
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                }
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun ItemsTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell("Товар", 3f)
        HeaderCell("Ед.", 1f)
        HeaderCell("Цена", 2f)
        HeaderCell("Кол-во", 1.5f)
        HeaderCell("Сумма", 2f)
        // placeholder for edit+delete icons width
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(weight),
        maxLines = 1
    )
}

@Composable
private fun ItemRow(
    item: ReceiptItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sum = (item.priceMinor / 100.0) * item.qty
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.name, modifier = Modifier.weight(3f), maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall)
        Text(item.unit ?: "—", modifier = Modifier.weight(1f), maxLines = 1,
            style = MaterialTheme.typography.bodySmall)
        Text("%.2f".format(item.priceMinor / 100.0), modifier = Modifier.weight(2f), maxLines = 1,
            style = MaterialTheme.typography.bodySmall)
        Text(formatQty(item.qty), modifier = Modifier.weight(1.5f), maxLines = 1,
            style = MaterialTheme.typography.bodySmall)
        Text("%.2f".format(sum), modifier = Modifier.weight(2f), maxLines = 1,
            style = MaterialTheme.typography.bodySmall)
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Редактировать",
                    modifier = Modifier.padding(2.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(2.dp))
            }
        }
    }
}

private val COMMON_UNITS = listOf("шт", "кг", "г", "л", "мл", "м", "упак", "пачка", "пара", "рул")

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString() else "%.3f".format(qty).trimEnd('0')

@Composable
private fun AddEditItemDialog(
    initial: ReceiptItemEntity?,
    knownNames: List<String>,
    onGetLastPrice: suspend (String) -> Long?,
    onConfirm: (ReceiptItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var nameText by remember { mutableStateOf(initial?.name ?: "") }
    var unitText by remember { mutableStateOf(initial?.unit ?: "шт") }
    var priceText by remember { mutableStateOf(if (initial != null) "%.2f".format(initial.priceMinor / 100.0) else "") }
    var qtyText by remember { mutableStateOf(if (initial != null) formatQty(initial.qty) else "1") }
    var nameExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val filteredNames = remember(nameText, knownNames) {
        if (nameText.isBlank()) knownNames
        else knownNames.filter { it.contains(nameText, ignoreCase = true) }
    }

    val priceDouble = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val qtyDouble = qtyText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val sum = priceDouble * qtyDouble

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая позиция" else "Редактировать позицию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Товар с autocomplete
                ExposedDropdownMenuBox(
                    expanded = nameExpanded && filteredNames.isNotEmpty(),
                    onExpandedChange = { nameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it; nameExpanded = true },
                        label = { Text("Товар") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = nameExpanded && filteredNames.isNotEmpty(),
                        onDismissRequest = { nameExpanded = false }
                    ) {
                        filteredNames.take(10).forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    nameText = suggestion
                                    nameExpanded = false
                                    scope.launch {
                                        val lastPrice = onGetLastPrice(suggestion)
                                        if (lastPrice != null && lastPrice > 0L) {
                                            priceText = "%.2f".format(lastPrice / 100.0)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Ед. изм. — справочник + свободный ввод
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it; unitExpanded = true },
                        label = { Text("Ед. изм.") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    val filteredUnits = COMMON_UNITS.filter {
                        unitText.isBlank() || it.startsWith(unitText, ignoreCase = true)
                    }
                    if (filteredUnits.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            filteredUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = { unitText = unit; unitExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Цена
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Цена, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Кол-во
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Кол-во") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Сумма (вычисляемая)
                Text(
                    "Сумма: ${"%.2f".format(sum)} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val priceMinor = (priceDouble * 100).toLong()
                    val qty = qtyDouble.takeIf { it > 0.0 } ?: 1.0
                    val item = ReceiptItemEntity(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        transactionId = initial?.transactionId ?: "",
                        name = nameText.trim(),
                        priceMinor = priceMinor,
                        qty = qty,
                        unit = unitText.trim().ifEmpty { null }
                    )
                    onConfirm(item)
                },
                enabled = nameText.isNotBlank() && priceDouble > 0.0
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
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
