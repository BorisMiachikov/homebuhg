package ru.homebuhg.feature.export

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.homebuhg.core.common.nowMillis
import ru.homebuhg.core.common.startOfDayMillis
import ru.homebuhg.core.data.repository.AccountRepository
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.di.IoDispatcher
import ru.homebuhg.core.domain.SessionManager
import ru.homebuhg.core.export.CsvWriter
import ru.homebuhg.core.export.XlsxWriter
import java.time.LocalDate
import javax.inject.Inject

enum class ExportFormat { CSV, XLSX }

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    var fromMs by mutableLongStateOf(LocalDate.now().withDayOfMonth(1).startOfDayMillis())
        private set
    var toMs by mutableLongStateOf(nowMillis())
        private set
    var isExporting by mutableStateOf(false)
        private set
    var exportedCount by mutableIntStateOf(-1)
        private set

    fun setFrom(ms: Long) { fromMs = ms; exportedCount = -1 }
    fun setTo(ms: Long) { toMs = ms; exportedCount = -1 }

    fun setQuickRange(months: Int?) {
        toMs = nowMillis()
        fromMs = if (months == null) 0L
        else LocalDate.now().minusMonths(months.toLong()).withDayOfMonth(1).startOfDayMillis()
        exportedCount = -1
    }

    fun export(uri: Uri, format: ExportFormat) {
        viewModelScope.launch {
            isExporting = true
            withContext(io) {
                val hid = sessionManager.currentHouseholdId.first()
                val from = fromMs.takeIf { it > 0L }
                val transactions = transactionRepository
                    .observe(hid, fromMs = from, toMs = toMs)
                    .first()
                    .sortedBy { it.occurredAt }
                val cats = categoryRepository.observe(hid).first().associateBy { it.id }
                val accs = accountRepository.observe(hid).first().associateBy { it.id }

                appContext.contentResolver.openOutputStream(uri)?.use { output ->
                    when (format) {
                        ExportFormat.CSV -> output.write(
                            CsvWriter.write(transactions, cats, accs).toByteArray(Charsets.UTF_8)
                        )
                        ExportFormat.XLSX -> XlsxWriter.write(transactions, cats, accs, output)
                    }
                }
                exportedCount = transactions.size
            }
            isExporting = false
        }
    }
}
