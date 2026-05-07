package ru.homebuhg.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.homebuhg.feature.accounts.AccountEditScreen
import ru.homebuhg.feature.accounts.AccountsScreen
import ru.homebuhg.feature.budgets.BudgetEditScreen
import ru.homebuhg.feature.budgets.BudgetsScreen
import ru.homebuhg.feature.categories.CategoriesScreen
import ru.homebuhg.feature.home.HomeScreen
import ru.homebuhg.feature.operations.OperationEditScreen
import ru.homebuhg.feature.operations.OperationsScreen
import ru.homebuhg.feature.recurring.RecurringRuleEditScreen
import ru.homebuhg.feature.recurring.RecurringRulesScreen
import ru.homebuhg.feature.reports.ReportsScreen
import ru.homebuhg.feature.scanner.ScannerScreen
import ru.homebuhg.feature.settings.SettingsScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination.isTopLevel()) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { dest ->
                        navController.navigate(dest) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            modifier = Modifier.padding(padding)
        ) {
            composable<Destination.Home> {
                HomeScreen(
                    onAddOperation = { navController.navigate(Destination.OperationEdit()) },
                    onOpenScanner = { navController.navigate(Destination.Scanner) }
                )
            }
            composable<Destination.Operations> {
                OperationsScreen(
                    onAdd = { navController.navigate(Destination.OperationEdit()) },
                    onEdit = { id -> navController.navigate(Destination.OperationEdit(id)) }
                )
            }
            composable<Destination.Accounts> {
                AccountsScreen(
                    onAdd = { navController.navigate(Destination.AccountEdit()) },
                    onEdit = { id -> navController.navigate(Destination.AccountEdit(id)) }
                )
            }
            composable<Destination.Reports> { ReportsScreen() }
            composable<Destination.Settings> {
                SettingsScreen(
                    onOpenCategories = { navController.navigate(Destination.Categories) },
                    onOpenBudgets = { navController.navigate(Destination.Budgets) },
                    onOpenRecurringRules = { navController.navigate(Destination.RecurringRules) }
                )
            }
            composable<Destination.OperationEdit> { entry ->
                val args = entry.toRoute<Destination.OperationEdit>()
                OperationEditScreen(
                    operationId = args.operationId,
                    prefillAmountMinor = args.prefillAmountMinor,
                    prefillDateMs = args.prefillDateMs,
                    prefillNote = args.prefillNote,
                    onClose = { navController.popBackStack() }
                )
            }
            composable<Destination.AccountEdit> { entry ->
                val args = entry.toRoute<Destination.AccountEdit>()
                AccountEditScreen(
                    accountId = args.accountId,
                    onClose = { navController.popBackStack() }
                )
            }
            composable<Destination.Categories> {
                CategoriesScreen(onClose = { navController.popBackStack() })
            }
            composable<Destination.Budgets> {
                BudgetsScreen(
                    onClose = { navController.popBackStack() },
                    onAddBudget = { navController.navigate(Destination.BudgetEdit()) },
                    onEditBudget = { id -> navController.navigate(Destination.BudgetEdit(id)) }
                )
            }
            composable<Destination.BudgetEdit> { entry ->
                val args = entry.toRoute<Destination.BudgetEdit>()
                BudgetEditScreen(
                    budgetId = args.budgetId,
                    onClose = { navController.popBackStack() }
                )
            }
            composable<Destination.RecurringRules> {
                RecurringRulesScreen(
                    onClose = { navController.popBackStack() },
                    onAdd = { navController.navigate(Destination.RecurringRuleEdit()) },
                    onEdit = { id -> navController.navigate(Destination.RecurringRuleEdit(id)) }
                )
            }
            composable<Destination.RecurringRuleEdit> { entry ->
                val args = entry.toRoute<Destination.RecurringRuleEdit>()
                RecurringRuleEditScreen(
                    ruleId = args.ruleId,
                    onClose = { navController.popBackStack() }
                )
            }
            composable<Destination.Scanner> {
                ScannerScreen(
                    onClose = { navController.popBackStack() },
                    onScanned = { amountMinor, dateMs, note ->
                        navController.popBackStack()
                        navController.navigate(
                            Destination.OperationEdit(
                                prefillAmountMinor = amountMinor,
                                prefillDateMs = dateMs,
                                prefillNote = note
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (Destination) -> Unit
) {
    NavigationBar {
        topLevelDestinations.forEach { dest ->
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(dest.route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) }
            )
        }
    }
}

private fun NavDestination?.isTopLevel(): Boolean {
    if (this == null) return true
    return topLevelDestinations.any { dest -> hierarchy.any { it.hasRoute(dest.route::class) } }
}
