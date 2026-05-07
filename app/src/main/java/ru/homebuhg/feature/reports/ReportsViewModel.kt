package ru.homebuhg.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import ru.homebuhg.core.data.repository.CategoryRepository
import ru.homebuhg.core.data.repository.TransactionRepository
import ru.homebuhg.core.di.IoDispatcher
import ru.homebuhg.core.domain.SessionManager
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    @IoDispatcher private val io: CoroutineDispatcher
) : ViewModel() {

    enum class Period(val months: Int, val label: String) {
        THREE(3, "3 мес"),
        SIX(6, "6 мес"),
        YEAR(12, "Год")
    }

    data class MonthBar(val label: String, val incomeRub: Double, val expenseRub: Double)

    data class CategoryRow(
        val name: String,
        val colorArgb: Int,
        val amountMinor: Long,
        val fraction: Float
    )

    data class UiState(
        val period: Period = Period.SIX,
        val monthBars: List<MonthBar> = emptyList(),
        val categoryRows: List<CategoryRow> = emptyList(),
        val totalIncomeMinor: Long = 0L,
        val totalExpenseMinor: Long = 0L,
        val isLoading: Boolean = true
    )

    private val _period = MutableStateFlow(Period.SIX)

    val uiState: StateFlow<UiState> = sessionManager.currentHouseholdId
        .combine(_period) { hid, period -> hid to period }
        .flatMapLatest { (householdId, period) ->
            flow {
                emit(UiState(period = period, isLoading = true))

                val toMs = System.currentTimeMillis()
                val fromMs = LocalDate.now()
                    .minusMonths(period.months.toLong())
                    .withDayOfMonth(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val monthly = withContext(io) {
                    transactionRepository.monthlyTotals(householdId, fromMs, toMs)
                }
                val topCats = withContext(io) {
                    transactionRepository.topExpenseCategories(householdId, fromMs, toMs)
                }
                val categories = withContext(io) {
                    categoryRepository.observe(householdId).first()
                }

                val catMap = categories.associateBy { it.id }
                val monthMap = monthly.groupBy { it.month }
                val allMonths = buildMonthList(fromMs, toMs)

                val monthBars = allMonths.map { m ->
                    val entries = monthMap[m] ?: emptyList()
                    val income = entries.firstOrNull { it.type == "INCOME" }?.total ?: 0L
                    val expense = entries.firstOrNull { it.type == "EXPENSE" }?.total ?: 0L
                    MonthBar(
                        label = formatMonth(m),
                        incomeRub = income / 100.0,
                        expenseRub = expense / 100.0
                    )
                }

                val maxAmt = topCats.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1L
                val categoryRows = topCats.map { ct ->
                    val cat = catMap[ct.categoryId]
                    CategoryRow(
                        name = cat?.name ?: "Другое",
                        colorArgb = cat?.color ?: 0xFF9E9E9E.toInt(),
                        amountMinor = ct.total,
                        fraction = ct.total.toFloat() / maxAmt
                    )
                }

                emit(
                    UiState(
                        period = period,
                        monthBars = monthBars,
                        categoryRows = categoryRows,
                        totalIncomeMinor = monthly.filter { it.type == "INCOME" }.sumOf { it.total },
                        totalExpenseMinor = monthly.filter { it.type == "EXPENSE" }.sumOf { it.total },
                        isLoading = false
                    )
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun setPeriod(period: Period) {
        _period.value = period
    }

    private fun buildMonthList(fromMs: Long, toMs: Long): List<String> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
        var cur = Instant.ofEpochMilli(fromMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
        val end = Instant.ofEpochMilli(toMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)
        val result = mutableListOf<String>()
        while (!cur.isAfter(end)) {
            result.add(cur.format(fmt))
            cur = cur.plusMonths(1)
        }
        return result
    }

    private fun formatMonth(yyyyMM: String): String =
        YearMonth.parse(yyyyMM)
            .format(DateTimeFormatter.ofPattern("MMM yy", Locale("ru")))
}
