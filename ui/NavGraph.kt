package com.makhp.pelukdiri.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.makhp.pelukdiri.features.analytics.AnalyticsScreen
import com.makhp.pelukdiri.features.dashboard.MainStatsScreen
import com.makhp.pelukdiri.features.settings.AboutScreen
import com.makhp.pelukdiri.features.settings.AdaptiveModeScreen
import com.makhp.pelukdiri.features.settings.AppsInterventionScreen
import com.makhp.pelukdiri.features.settings.ExportCsvScreen
import com.makhp.pelukdiri.features.settings.NotificationSettingsScreen
import com.makhp.pelukdiri.features.settings.PrivacyScreen
import com.makhp.pelukdiri.features.settings.SettingsScreen
import com.makhp.pelukdiri.features.settings.TermsScreen

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
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            MainStatsScreen(
                onProgressClick = { navController.navigate(Screen.Analytics.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onHomeClick = { navController.navigate(Screen.Home.route) },
                onProgressClick = { navController.navigate(Screen.Analytics.route) },
                onNavigateToAdaptiveMode = { navController.navigate(Screen.AdaptiveMode.route) },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
                onNavigateToExport = { navController.navigate(Screen.ExportCsv.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNavigateToTerms = { navController.navigate(Screen.Terms.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }
        composable(Screen.AdaptiveMode.route) {
            AdaptiveModeScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToApps = { navController.navigate(Screen.AppsIntervention.route) }
            )
        }
        composable(Screen.AppsIntervention.route) {
            AppsInterventionScreen(
                onBackClick = { navController.popBackStack() },
                onSaveClick = { navController.popBackStack() }
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
