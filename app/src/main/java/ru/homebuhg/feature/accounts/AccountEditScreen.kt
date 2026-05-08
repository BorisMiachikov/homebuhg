package ru.homebuhg.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.homebuhg.core.data.database.entity.AccountType

private val accountColors = listOf(
    0xFF2196F3.toInt(), 0xFF4CAF50.toInt(), 0xFFF44336.toInt(), 0xFFFF9800.toInt(),
    0xFF9C27B0.toInt(), 0xFF00BCD4.toInt(), 0xFF607D8B.toInt(), 0xFF795548.toInt(),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountEditScreen(
    accountId: String?,
    onClose: () -> Unit,
    viewModel: AccountEditViewModel = hiltViewModel()
) {
    LaunchedEffect(accountId) { viewModel.initialize(accountId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { onClose() }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Архивировать счёт?") },
            text = { Text("Счёт будет скрыт из списка, но история операций сохранится.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete() }) {
                    Text("Архивировать", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (accountId == null) "Новый счёт" else "Редактирование счёта") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (accountId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, "Архивировать")
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
            // Name
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::setName,
                label = { Text("Название счёта") },
                isError = viewModel.nameError,
                supportingText = if (viewModel.nameError) { { Text("Введите название") } } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Type
            Column {
                Text("Тип счёта", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { type ->
                        FilterChip(
                            selected = viewModel.type == type,
                            onClick = { viewModel.setType(type) },
                            label = { Text(type.label()) }
                        )
                    }
                }
            }

            // Initial balance (only for new accounts)
            if (accountId == null) {
                OutlinedTextField(
                    value = viewModel.initialBalanceText,
                    onValueChange = viewModel::setInitialBalance,
                    label = { Text("Начальный баланс") },
                    suffix = { Text("₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Color picker
            Column {
                Text("Цвет", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    accountColors.forEach { colorInt ->
                        val isSelected = viewModel.color == colorInt
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setColor(colorInt) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
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

private fun AccountType.label() = when (this) {
    AccountType.CARD_DEBIT -> "Дебетовая"
    AccountType.CARD_CREDIT -> "Кредитная"
    AccountType.CASH -> "Наличные"
}
