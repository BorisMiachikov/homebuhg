package ru.homebuhg.feature.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.homebuhg.core.common.formatRuDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    onClose: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    // null = all time, number = months back
    val quickRanges = listOf(
        "Месяц" to 1,
        "3 мес" to 3,
        "Год" to 12,
        "Всё время" to null
    )
    var selectedQuick by remember { mutableStateOf<Int?>(-1) } // -1 = custom

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.export(it, ExportFormat.CSV) } }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri -> uri?.let { viewModel.export(it, ExportFormat.XLSX) } }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = viewModel.fromMs)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setFrom(it) }
                    selectedQuick = -1
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = viewModel.toMs)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.setTo(it) }
                    selectedQuick = -1
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = state) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Экспорт данных") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            Text("Период", style = MaterialTheme.typography.titleSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickRanges.forEach { (label, months) ->
                    FilterChip(
                        selected = selectedQuick == months,
                        onClick = {
                            selectedQuick = months
                            viewModel.setQuickRange(months)
                        },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider()

            Text("Диапазон дат", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "С",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showFromPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (viewModel.fromMs == 0L) "Начало"
                            else viewModel.fromMs.formatRuDay()
                        )
                    }
                }
                Text("—", style = MaterialTheme.typography.bodyLarge)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "По",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showToPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(viewModel.toMs.formatRuDay())
                    }
                }
            }

            HorizontalDivider()

            Text("Формат", style = MaterialTheme.typography.titleSmall)

            if (viewModel.isExporting) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { csvLauncher.launch("homebuhg_export.csv") },
                        modifier = Modifier.weight(1f)
                    ) { Text("CSV") }
                    Button(
                        onClick = { xlsxLauncher.launch("homebuhg_export.xlsx") },
                        modifier = Modifier.weight(1f)
                    ) { Text("XLSX") }
                }
            }

            if (viewModel.exportedCount >= 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Экспортировано операций: ${viewModel.exportedCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "CSV открывается в Excel, Google Sheets и любом табличном редакторе. " +
                        "XLSX создаётся без дополнительных библиотек.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
