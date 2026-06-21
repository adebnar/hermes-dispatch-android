package co.hermesdispatch.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.hermesdispatch.app.data.repository.AuthRepository
import co.hermesdispatch.app.ui.pairing.PairingScreen
import co.hermesdispatch.app.ui.scheduled.ScheduledScreen
import co.hermesdispatch.app.ui.tasks.TasksScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

object Routes {
    const val PAIRING = "pairing"
    const val TASKS = "tasks"
    const val SCHEDULED = "scheduled"
}

@HiltViewModel
class RootViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {
    val startDestination: String = if (auth.isPaired()) Routes.TASKS else Routes.PAIRING
}

@Composable
fun AppNav(rootViewModel: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBars = currentRoute == Routes.TASKS || currentRoute == Routes.SCHEDULED

    Scaffold(
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    val tabs = listOf(
                        Triple(Routes.TASKS, "Tasks", Icons.AutoMirrored.Filled.ListAlt),
                        Triple(Routes.SCHEDULED, "Scheduled", Icons.Filled.Schedule),
                    )
                    tabs.forEach { (route, label, icon) ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = rootViewModel.startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PAIRING) {
                PairingScreen(onPaired = {
                    navController.navigate(Routes.TASKS) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                })
            }
            composable(Routes.TASKS) { TasksScreen() }
            composable(Routes.SCHEDULED) { ScheduledScreen() }
        }
    }
}
