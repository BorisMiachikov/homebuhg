package ru.homebuhg.feature.operations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.homebuhg.core.common.Money
import ru.homebuhg.core.common.format
import ru.homebuhg.core.common.formatRuDay
import ru.homebuhg.core.common.formatRuTime
import ru.homebuhg.core.common.toLocalDateTime
import ru.homebuhg.core.data.database.entity.AccountEntity
import ru.homebuhg.core.data.database.entity.CategoryEntity
import ru.homebuhg.core.data.database.entity.TransactionEntity
import ru.homebuhg.core.data.database.entity.TransactionType
import java.time.LocalDate

@Composable
fun OperationsScreen(
    viewModel: OperationsViewModel = hiltViewModel(),
    onAdd: () -> Unit,
    onEdit: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val grouped = remember(state.transactions) {
        state.transactions.groupBy { it.occurredAt.toLocalDateTime().toLocalDate() }
            .entries.sortedByDescending { it.key }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Операции") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (grouped.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Text(
                    "Нет операций",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                grouped.forEach { (date, txs) ->
                    stickyHeader(key = date.toString()) {
                        DateHeader(date, txs)
                    }
                    items(txs, key = { it.id }) { tx ->
                        TransactionListItem(
                            tx = tx,
                            category = state.categoryMap[tx.categoryId],
                            account = state.accountMap[tx.accountId],
                            toAccount = tx.toAccountId?.let { state.accountMap[it] },
                            onClick = { onEdit(tx.id) }
                        )
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    txs: List<TransactionEntity>,
) {
    val dayTotal = txs.fold(0L) { acc, tx ->
        when (tx.type) {
            TransactionType.EXPENSE -> acc - tx.amountMinor
            TransactionType.INCOME -> acc + tx.amountMinor
            TransactionType.TRANSFER -> acc
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(date.formatRuDay(), style = MaterialTheme.typography.labelLarge)
            val color = if (dayTotal >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            Text(
                Money(dayTotal).format(),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransactionListItem(
    tx: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    toAccount: AccountEntity?,
    onClick: () -> Unit
) {
    val (amountColor, sign) = when (tx.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error to "-"
        TransactionType.INCOME -> Color(0xFF4CAF50) to "+"
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.secondary to "→"
    }
    val subtitle = when (tx.type) {
        TransactionType.TRANSFER -> "${account?.name ?: "?"} → ${toAccount?.name ?: "?"}"
        else -> account?.name ?: ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category?.name ?: tx.note ?: tx.type.label(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            if (subtitle.isNotBlank()) {
                Text(
                    "$subtitle · ${tx.occurredAt.formatRuTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$sign${Money(tx.amountMinor).format()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = amountColor
            )
            tx.note?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

private fun TransactionType.label() = when (this) {
    TransactionType.EXPENSE -> "Расход"
    TransactionType.INCOME -> "Доход"
    TransactionType.TRANSFER -> "Перевод"
}
