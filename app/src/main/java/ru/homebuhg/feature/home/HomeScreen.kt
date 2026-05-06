package ru.homebuhg.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.homebuhg.core.common.Money
import ru.homebuhg.core.common.format
import ru.homebuhg.core.common.formatRuShort
import ru.homebuhg.core.common.formatRuTime
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.AccountType
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAddOperation: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Домашняя бухгалтерия") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddOperation,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Операция") }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TotalBalanceCard(state.totalBalanceMinor) }
            if (state.accounts.isNotEmpty()) {
                item { AccountsRow(state.accounts) }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(onClick = onAddOperation, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Расход")
                    }
                    FilledTonalButton(onClick = onOpenScanner, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.QrCodeScanner, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Скан чека")
                    }
                }
            }
            if (state.recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        "Последние операции",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(state.recentTransactions) { tx ->
                    RecentTransactionItem(tx)
                }
            } else {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        Alignment.Center
                    ) {
                        Text(
                            "Пока нет операций",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(balanceMinor: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Общий баланс", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = Money(balanceMinor).format(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AccountsRow(accounts: List<AccountEntity>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(accounts, key = { it.id }) { account ->
            ElevatedCard(modifier = Modifier.width(160.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        account.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        Money(account.balanceMinor).format(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        account.type.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(tx: TransactionEntity) {
    val (amountColor, sign) = when (tx.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error to "-"
        TransactionType.INCOME -> Color(0xFF4CAF50) to "+"
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary to "→"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                tx.note ?: tx.type.label(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                "${tx.occurredAt.formatRuShort()} ${tx.occurredAt.formatRuTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$sign${Money(tx.amountMinor).format()}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = amountColor
        )
    }
}

private fun AccountType.label() = when (this) {
    AccountType.CARD_DEBIT -> "Дебетовая"
    AccountType.CARD_CREDIT -> "Кредитная"
    AccountType.CASH -> "Наличные"
}
private fun TransactionType.label() = when (this) {
    TransactionType.EXPENSE -> "Расход"
    TransactionType.INCOME -> "Доход"
    TransactionType.TRANSFER -> "Перевод"
}
