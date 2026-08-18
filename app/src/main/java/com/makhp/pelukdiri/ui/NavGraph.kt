package com.makhp.pelukdiri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.makhp.pelukdiri.features.analytics.AnalyticsScreen
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
    object Terms : Screen("terms")
    object AllApps : Screen("all_apps")
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
                onViewAllClick = { navController.navigate(Screen.AllApps.route) },
                onOnboardingClick = { navController.navigate(Screen.Onboarding.route) }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = { navController.popBackStack() }
            )
        }
        composable(Screen.AllApps.route) {
            AllAppsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onViewAllClick = { navController.navigate(Screen.AllApps.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onProgressClick = { navController.navigate(Screen.Analytics.route) },
                onNavigateToAdaptiveMode = { navController.navigate(Screen.AdaptiveMode.route) },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                onNavigateToApps = { navController.navigate(Screen.AppsIntervention.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
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
        composable(Screen.Terms.route) {
            TermsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
