package ru.homebuhg.feature.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.homebuhg.core.data.database.entity.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsRuleEditScreen(
    ruleId: String?,
    onClose: () -> Unit,
    viewModel: SmsRuleEditViewModel = hiltViewModel()
) {
    LaunchedEffect(ruleId) { viewModel.initialize(ruleId) }
    LaunchedEffect(Unit) { viewModel.events.collect { onClose() } }

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ruleId == null) "Новое правило" else "Изменить правило") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = viewModel.senderPattern.isNotBlank() &&
                                viewModel.bodyRegex.isNotBlank() &&
                                viewModel.amountGroup.isNotBlank()
                    ) { Text("Сохранить") }
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
            val typeTabs = listOf(TransactionType.EXPENSE to "Расход", TransactionType.INCOME to "Доход")
            val selectedTab = typeTabs.indexOfFirst { it.first == viewModel.type }
            TabRow(selectedTabIndex = selectedTab.coerceAtLeast(0)) {
                typeTabs.forEachIndexed { i, (t, label) ->
                    Tab(
                        selected = i == selectedTab,
                        onClick = { viewModel.setType(t) },
                        text = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.senderPattern,
                onValueChange = viewModel::setSenderPattern,
                label = { Text("Паттерн отправителя") },
                placeholder = { Text("Sberbank|900") },
                supportingText = { Text("Regex для номера/имени отправителя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.bodyRegex,
                onValueChange = viewModel::setBodyRegex,
                label = { Text("Регулярное выражение") },
                placeholder = { Text("""(?<amount>[\d ]+[.,]\d{2}) руб""") },
                supportingText = { Text("Regex с именованными группами для тела SMS") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            OutlinedTextField(
                value = viewModel.amountGroup,
                onValueChange = viewModel::setAmountGroup,
                label = { Text("Группа суммы") },
                placeholder = { Text("amount") },
                supportingText = { Text("Имя именованной группы (?<name>...) для суммы") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.merchantGroup,
                onValueChange = viewModel::setMerchantGroup,
                label = { Text("Группа магазина (опционально)") },
                placeholder = { Text("merchant") },
                supportingText = { Text("Имя именованной группы для названия магазина") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (accounts.isNotEmpty()) {
                Text("Счёт по умолчанию", style = MaterialTheme.typography.labelLarge)
                var expanded by remember { mutableStateOf(false) }
                val selectedAccount = accounts.find { it.id == viewModel.selectedAccountId }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "Первый активный счёт",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Счёт") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Первый активный счёт") },
                            onClick = { viewModel.setAccount(null); expanded = false }
                        )
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = { viewModel.setAccount(account.id); expanded = false }
                            )
                        }
                    }
                }
            }

            Text(
                text = "Пример: Для Сбербанка используйте паттерн \"Sberbank|900\", " +
                        "регулярное выражение \"(?<amount>[\\d ]+[.,]\\d{2}) руб\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
