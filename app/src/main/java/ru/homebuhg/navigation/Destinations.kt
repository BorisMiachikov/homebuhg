package ru.homebuhg.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed interface Destination {
    @Serializable data object Home : Destination
    @Serializable data object Operations : Destination
    @Serializable data object Accounts : Destination
    @Serializable data object Reports : Destination
    @Serializable data object Settings : Destination

    @Serializable data class OperationEdit(
        val operationId: String? = null,
        val prefillAmountMinor: Long = 0L,
        val prefillDateMs: Long = 0L,
        val prefillNote: String = "",
        val initialType: String? = null
    ) : Destination
    @Serializable data class AccountEdit(val accountId: String? = null) : Destination
    @Serializable data object Categories : Destination
    @Serializable data object Budgets : Destination
    @Serializable data class BudgetEdit(val budgetId: String? = null) : Destination
    @Serializable data object RecurringRules : Destination
    @Serializable data class RecurringRuleEdit(val ruleId: String? = null) : Destination
    @Serializable data object Scanner : Destination
    @Serializable data object SmsRules : Destination
    @Serializable data class SmsRuleEdit(val ruleId: String? = null) : Destination
    @Serializable data object Export : Destination
}

data class TopLevelDestination(
    val route: Destination,
    val label: String,
    val icon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(Destination.Home, "Главная", Icons.Outlined.Home),
    TopLevelDestination(Destination.Operations, "Операции", Icons.Outlined.Receipt),
    TopLevelDestination(Destination.Accounts, "Счета", Icons.Outlined.AccountBalanceWallet),
    TopLevelDestination(Destination.Reports, "Отчёты", Icons.Outlined.BarChart),
    TopLevelDestination(Destination.Settings, "Настройки", Icons.Outlined.Settings)
)
