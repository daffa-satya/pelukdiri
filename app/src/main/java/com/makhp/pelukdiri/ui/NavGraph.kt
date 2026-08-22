package com.makhp.pelukdiri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.makhp.pelukdiri.features.analytics.AnalyticsScreen
import com.makhp.pelukdiri.features.analytics.AnalyticsPeriod
import com.makhp.pelukdiri.features.dashboard.AllAppsScreen
import com.makhp.pelukdiri.features.dashboard.MainStatsScreen
import com.makhp.pelukdiri.features.onboarding.OnboardingScreen
import com.makhp.pelukdiri.features.settings.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object AdaptiveMode : Screen("adaptive_mode")
    object AppsIntervention : Screen("apps_intervention")
    object NotificationSettings : Screen("notification_settings")
    object Privacy : Screen("privacy")
    object ExportCsv : Screen("export_csv")
    object About : Screen("about")
    object InformedConsent : Screen("informed_consent")
    object Terms : Screen("terms")
    object AllApps : Screen("all_apps/{date}/{period}") {
        fun route(date: java.time.LocalDate, period: AnalyticsPeriod) =
            "all_apps/$date/${period.name}"
    }
    object Onboarding : Screen("onboarding")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onExitApp: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainStatsScreen(
                onProgressClick = { navController.navigate(Screen.Analytics.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onViewAllClick = {
                    navController.navigate(Screen.AllApps.route(java.time.LocalDate.now(), AnalyticsPeriod.DAILY))
                },
                onOnboardingClick = { navController.navigate(Screen.Onboarding.route) }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AllApps.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("period") { type = NavType.StringType },
            ),
        ) { entry ->
            val date = java.time.LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            val period = AnalyticsPeriod.valueOf(requireNotNull(entry.arguments?.getString("period")))
            AllAppsScreen(
                selectedDate = date,
                selectedPeriod = period,
                onBackClick = { navController.popBackStack() },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onViewAllClick = { date, period ->
                    navController.navigate(Screen.AllApps.route(date, period))
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onProgressClick = { navController.navigate(Screen.Analytics.route) },
                onNavigateToAdaptiveMode = { navController.navigate(Screen.AdaptiveMode.route) },
                onNavigateToApps = { navController.navigate(Screen.AppsIntervention.route) },
                onNavigateToInformedConsent = { navController.navigate(Screen.InformedConsent.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onExitApp = onExitApp
            )
        }
        composable(Screen.AdaptiveMode.route) {
            val viewModel: AdaptiveModeViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AdaptiveModeScreen(
                onBackClick = { navController.popBackStack() },
                state = state,
                onAggressivenessChange = viewModel::setAggressiveness
            )
        }
        composable(Screen.AppsIntervention.route) {
            val viewModel: AppsInterventionViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AppsInterventionScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() },
                state = state,
                onQueryChange = viewModel::onQueryChange,
                onToggleApp = viewModel::toggleApp
            )
        }
        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Privacy.route) {
            PrivacyScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.ExportCsv.route) {
            ExportCsvScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.InformedConsent.route) {
            InformedConsentScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Screen.Terms.route) {
            TermsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
