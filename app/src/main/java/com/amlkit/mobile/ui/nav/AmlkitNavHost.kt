package com.amlkit.mobile.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amlkit.mobile.data.AmlkitRepository
import com.amlkit.mobile.data.AuthTokenStore
import com.amlkit.mobile.ui.admin.AdminScreen
import com.amlkit.mobile.ui.alerts.AlertsScreen
import com.amlkit.mobile.ui.audit.AuditScreen
import com.amlkit.mobile.ui.auth.LoginScreen
import com.amlkit.mobile.ui.auth.RegisterOrgScreen
import com.amlkit.mobile.ui.customers.CustomerDetailScreen
import com.amlkit.mobile.ui.customers.CustomerNewScreen
import com.amlkit.mobile.ui.customers.CustomersListScreen
import com.amlkit.mobile.ui.customers.EvidenceScreen
import com.amlkit.mobile.ui.dashboard.DashboardScreen
import com.amlkit.mobile.ui.home.AboutScreen
import com.amlkit.mobile.ui.home.HomeScreen
import com.amlkit.mobile.ui.more.MoreScreen
import com.amlkit.mobile.ui.reports.ReportBuilderScreen
import com.amlkit.mobile.ui.reports.ReportDetailScreen
import com.amlkit.mobile.ui.reports.ReportsListScreen
import com.amlkit.mobile.ui.screening.ScreeningScreen
import kotlinx.coroutines.launch

private val screenTitles = mapOf(
    Routes.CUSTOMER_NEW to "Customers",
    Routes.CUSTOMER_DETAIL to "Customers",
    "customers/{customerId}/evidence" to "Case file",
    Routes.ALERTS to "Home",
    Routes.AUDIT to "More",
    Routes.ADMIN to "More",
    Routes.REPORTS to "More",
    Routes.REPORT_NEW to "Reports",
    Routes.REPORT_DETAIL to "Reports",
    Routes.ABOUT to "Home",
)

@Composable
fun AmlkitApp(repository: AmlkitRepository, tokenStore: AuthTokenStore) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val token by tokenStore.token.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(token) {
        if (token == null && currentRoute != Routes.LOGIN && currentRoute != Routes.REGISTER) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val isTopLevel = currentRoute in bottomTabRoutes
    val isAuthRoute = currentRoute == Routes.LOGIN || currentRoute == Routes.REGISTER

    Scaffold(
        containerColor = com.amlkit.mobile.ui.theme.AmlBg,
        topBar = {
            if (!isAuthRoute && !isTopLevel) {
                val label = screenTitles[currentRoute] ?: ""
                AmlkitSubHeader(label = label, onBack = { navController.popBackStack() })
            }
        },
        bottomBar = {
            if (token != null && isTopLevel) {
                AmlkitBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (token != null) Routes.HOME else Routes.LOGIN,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    repository = repository,
                    onLoggedIn = { navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } } },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterOrgScreen(
                    repository = repository,
                    onRegistered = { navController.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } } },
                    onBackToLogin = { navController.popBackStack() },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    repository = repository,
                    tokenStore = tokenStore,
                    onScreenName = { navController.navigate(Routes.SCREENING) },
                    onNewCustomer = { navController.navigate(Routes.CUSTOMER_NEW) },
                    onAbout = { navController.navigate(Routes.ABOUT) },
                    onViewAlerts = { navController.navigate(Routes.ALERTS) },
                )
            }
            composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.DASHBOARD) {
                DashboardScreen(repository = repository, tokenStore = tokenStore, onOpenAlert = { navController.navigate(Routes.ALERTS) })
            }
            composable(Routes.SCREENING) { ScreeningScreen(repository = repository) }
            composable(Routes.CUSTOMERS) {
                CustomersListScreen(
                    repository = repository,
                    tokenStore = tokenStore,
                    onOpenCustomer = { id -> navController.navigate(Routes.customerDetail(id)) },
                    onNewCustomer = { navController.navigate(Routes.CUSTOMER_NEW) },
                )
            }
            composable(Routes.CUSTOMER_NEW) {
                CustomerNewScreen(repository = repository, onCreated = { id ->
                    navController.navigate(Routes.customerDetail(id)) { popUpTo(Routes.CUSTOMERS) }
                })
            }
            composable(
                route = Routes.CUSTOMER_DETAIL,
                arguments = listOf(androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType }),
            ) { entry ->
                val customerId = entry.arguments?.getInt("customerId") ?: return@composable
                CustomerDetailScreen(
                    repository = repository,
                    customerId = customerId,
                    onOpenEvidence = { navController.navigate("customers/$customerId/evidence") },
                )
            }
            composable(
                route = "customers/{customerId}/evidence",
                arguments = listOf(androidx.navigation.navArgument("customerId") { type = androidx.navigation.NavType.IntType }),
            ) { entry ->
                val customerId = entry.arguments?.getInt("customerId") ?: return@composable
                EvidenceScreen(repository = repository, customerId = customerId)
            }
            composable(Routes.ALERTS) { AlertsScreen(repository = repository) }
            composable(Routes.AUDIT) { AuditScreen(repository = repository) }
            composable(Routes.ADMIN) { AdminScreen(repository = repository) }
            composable(Routes.REPORTS) {
                ReportsListScreen(
                    repository = repository,
                    onOpenReport = { id -> navController.navigate(Routes.reportDetail(id)) },
                    onNewReport = { navController.navigate(Routes.REPORT_NEW) },
                )
            }
            composable(Routes.REPORT_NEW) {
                ReportBuilderScreen(repository = repository, onSaved = { id ->
                    navController.navigate(Routes.reportDetail(id)) { popUpTo(Routes.REPORTS) }
                })
            }
            composable(
                route = Routes.REPORT_DETAIL,
                arguments = listOf(androidx.navigation.navArgument("reportId") { type = androidx.navigation.NavType.IntType }),
            ) { entry ->
                val reportId = entry.arguments?.getInt("reportId") ?: return@composable
                ReportDetailScreen(repository = repository, reportId = reportId)
            }
            composable(Routes.MORE) {
                MoreScreen(
                    tokenStore = tokenStore,
                    onAlerts = { navController.navigate(Routes.ALERTS) },
                    onAudit = { navController.navigate(Routes.AUDIT) },
                    onAdmin = { navController.navigate(Routes.ADMIN) },
                    onReports = { navController.navigate(Routes.REPORTS) },
                    onLogout = { coroutineScope.launch { repository.logout() } },
                )
            }
        }
    }
}
