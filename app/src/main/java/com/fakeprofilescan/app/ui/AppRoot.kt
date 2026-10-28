package com.fakeprofilescan.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fakeprofilescan.app.AppViewModel

/**
 * Route map
 *   Top level (bottom bar, Back returns to Assess, then exits): assess | history | learn | settings
 *   assess -> form -> result/{id}   (result replaces form, so Back from a result returns to Assess)
 *   history -> result/{id}          (Back returns to History)
 * Reselecting the current tab does nothing (single-top).
 */
object Routes {
    const val ASSESS = "assess"
    const val FORM = "form"
    const val RESULT = "result/{id}"
    const val HISTORY = "history"
    const val LEARN = "learn"
    const val SETTINGS = "settings"
    fun result(id: String) = "result/$id"
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Assess(Routes.ASSESS, "Assess", Icons.Outlined.Home),
    History(Routes.HISTORY, "History", Icons.AutoMirrored.Outlined.List),
    Learn(Routes.LEARN, "Safety", Icons.Outlined.Info),
    Settings(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }

    // One-shot: a finished assessment replaces the form on the back stack.
    LaunchedEffect(Unit) {
        vm.resultEvents.collect { id ->
            nav.navigate(Routes.result(id)) { popUpTo(Routes.FORM) { inclusive = true }; launchSingleTop = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (route) {
                            Routes.FORM -> "New assessment"
                            Routes.RESULT -> "Assessment"
                            Routes.ASSESS -> "Fake Profile Scan"
                            else -> tab?.label ?: "Fake Profile Scan"
                        }
                    )
                },
                navigationIcon = {
                    if (tab == null) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (tab != null) NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = Routes.ASSESS, modifier = Modifier.padding(pad)) {
            composable(Routes.ASSESS) {
                HomeScreen(vm, onStart = { vm.newAssessment(); nav.navigate(Routes.FORM) { launchSingleTop = true } },
                    onOpen = { nav.navigate(Routes.result(it)) { launchSingleTop = true } })
            }
            composable(Routes.FORM) { FormScreen(vm) }
            composable(Routes.RESULT, listOf(navArgument("id") { type = NavType.StringType })) {
                ResultScreen(vm, it.arguments?.getString("id").orEmpty(),
                    onNew = { vm.newAssessment(); nav.navigate(Routes.FORM) { popUpTo(Routes.ASSESS); launchSingleTop = true } },
                    onGone = { nav.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(vm, onOpen = { nav.navigate(Routes.result(it)) { launchSingleTop = true } })
            }
            composable(Routes.LEARN) { LearnScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
        }
    }
}

@Composable
internal fun <T> kotlinx.coroutines.flow.StateFlow<T>.state(): T = collectAsStateWithLifecycle().value
