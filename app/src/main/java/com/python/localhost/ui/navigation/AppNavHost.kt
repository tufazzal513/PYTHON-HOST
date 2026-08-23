package com.python.localhost.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.screens.CreateProjectScreen
import com.python.localhost.ui.screens.EditorScreen
import com.python.localhost.ui.screens.EntryPointPickerScreen
import com.python.localhost.ui.screens.GitHubImportScreen
import com.python.localhost.ui.screens.GitScreen
import com.python.localhost.ui.screens.HomeScreen
import com.python.localhost.ui.screens.LogsScreen
import com.python.localhost.ui.screens.ProjectDashboardScreen
import com.python.localhost.ui.screens.ProjectsScreen
import com.python.localhost.ui.screens.RunningScreen
import com.python.localhost.ui.screens.RunConfigScreen
import com.python.localhost.ui.screens.ServerScreen
import com.python.localhost.ui.screens.SettingsScreen
import com.python.localhost.ui.screens.TerminalScreen

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav, container) }
        composable(Routes.PROJECTS) { ProjectsScreen(nav, container) }
        composable(Routes.RUNNING) { RunningScreen(nav, container) }
        composable(Routes.SETTINGS) { SettingsScreen(nav, container) }
        composable(Routes.CREATE_PROJECT) { CreateProjectScreen(nav, container) }
        composable(Routes.GITHUB_IMPORT) { GitHubImportScreen(nav, container) }
        composable(
            Routes.RUN_CONFIG,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            RunConfigScreen(nav, container, it.arguments!!.getString("projectId")!!)
        }
        composable(
            Routes.SERVER,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            ServerScreen(nav, container, it.arguments!!.getString("projectId")!!, it.arguments!!.getString("url") ?: "")
        }
        composable(
            Routes.DASHBOARD,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            ProjectDashboardScreen(
                nav, container, it.arguments!!.getString("projectId")!!
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("file") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            EditorScreen(
                nav, container,
                it.arguments!!.getString("projectId")!!,
                it.arguments!!.getString("file"),
            )
        }
        composable(
            Routes.TERMINAL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            TerminalScreen(nav, container, it.arguments!!.getString("projectId")!!)
        }
        composable(
            Routes.LOGS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            LogsScreen(nav, container, it.arguments!!.getString("projectId")!!)
        }
        composable(
            Routes.GIT,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            GitScreen(nav, container, it.arguments!!.getString("projectId")!!)
        }
        composable(
            Routes.ENTRY_PICKER,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) {
            EntryPointPickerScreen(nav, container, it.arguments!!.getString("projectId")!!)
        }
    }
}
