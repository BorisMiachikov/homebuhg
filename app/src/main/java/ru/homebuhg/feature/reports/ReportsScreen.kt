package ru.homebuhg.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import androidx.compose.ui.graphics.toArgb
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import ru.homebuhg.core.common.Money
import ru.homebuhg.core.common.format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelProducer = remember { CartesianChartModelProducer() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.monthBars) {
        val bars = uiState.monthBars
        if (bars.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(y = bars.map { it.incomeRub })
                    series(y = bars.map { it.expenseRub })
                }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Отчёты") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PeriodSelector(
                selected = uiState.period,
                onSelect = viewModel::setPeriod,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Динамика") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Расходы") }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else when (selectedTab) {
                0 -> DynamicsTab(uiState, modelProducer)
                1 -> CategoriesTab(uiState)
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: ReportsViewModel.Period,
    onSelect: (ReportsViewModel.Period) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportsViewModel.Period.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
private fun DynamicsTab(
    uiState: ReportsViewModel.UiState,
    modelProducer: CartesianChartModelProducer
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SummaryCards(uiState)

        if (uiState.monthBars.isEmpty()) {
            EmptyState("Нет операций за выбранный период")
        } else {
            val labels = uiState.monthBars.map { it.label }
            val xFormatter = remember(labels) {
                CartesianValueFormatter { _, value, _ ->
                    labels.getOrElse(value.toInt()) { "" }
                }
            }
            val yFormatter = remember {
                CartesianValueFormatter { _, value, _ ->
                    when {
                        value >= 1_000_000 -> "${(value / 1_000_000).toLong()}M ₽"
                        value >= 1_000 -> "${(value / 1_000).toLong()}k ₽"
                        else -> "${value.toLong()} ₽"
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ChartLegend()
                    Spacer(Modifier.height(8.dp))
                    val incomeColor = MaterialTheme.colorScheme.primary.toArgb()
                    val expenseColor = MaterialTheme.colorScheme.error.toArgb()
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(
                                ColumnCartesianLayer.ColumnProvider.series(
                                    rememberLineComponent(fill = Fill(incomeColor), thickness = 16.dp),
                                    rememberLineComponent(fill = Fill(expenseColor), thickness = 16.dp)
                                )
                            ),
                            startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(uiState: ReportsViewModel.UiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            label = "Доходы",
            amount = uiState.totalIncomeMinor,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Расходы",
            amount = uiState.totalExpenseMinor,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Итого",
            amount = uiState.totalIncomeMinor - uiState.totalExpenseMinor,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Money(amount).format(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ChartLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(label = "Доходы", color = MaterialTheme.colorScheme.primary)
        LegendItem(label = "Расходы", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CategoriesTab(uiState: ReportsViewModel.UiState) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.categoryRows.isEmpty()) {
            EmptyState("Нет расходов за выбранный период")
        } else {
            Text(
                text = "Топ категорий расходов",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.categoryRows.forEach { row ->
                        CategoryRowItem(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(row: ReportsViewModel.CategoryRow) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(row.colorArgb))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = Money(row.amountMinor).format(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { row.fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = Color(row.colorArgb),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
