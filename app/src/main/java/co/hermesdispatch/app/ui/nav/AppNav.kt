package co.hermesdispatch.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.hermesdispatch.app.data.repository.AuthRepository
import co.hermesdispatch.app.ui.chat.ChatScreen
import co.hermesdispatch.app.ui.chat.ChatViewModel
import co.hermesdispatch.app.ui.inbox.InboxItemScreen
import co.hermesdispatch.app.ui.inbox.InboxScreen
import co.hermesdispatch.app.ui.pairing.PairingScreen
import co.hermesdispatch.app.ui.scheduled.ScheduledScreen
import co.hermesdispatch.app.ui.settings.SettingsScreen
import co.hermesdispatch.app.ui.tasks.TasksScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

object Routes {
    const val PAIRING = "pairing"
    const val TASKS = "tasks"
    const val INBOX = "inbox"
    const val SCHEDULED = "scheduled"
    const val SETTINGS = "settings"
    const val INBOX_ITEM = "inboxItem" // inboxItem?id=…&title=…
    fun inboxItem(id: String, title: String?): String {
        val params = buildList {
            add("id=${android.net.Uri.encode(id)}")
            if (!title.isNullOrBlank()) add("title=${android.net.Uri.encode(title)}")
        }
        return "$INBOX_ITEM?${params.joinToString("&")}"
    }
    const val CHAT = "chat" // chat/{sessionId}?prompt=…&title=…; sessionId == "new" starts fresh
    fun chat(sessionId: String, prompt: String? = null, title: String? = null): String {
        val params = buildList {
            if (!prompt.isNullOrBlank()) add("prompt=${android.net.Uri.encode(prompt)}")
            if (!title.isNullOrBlank()) add("title=${android.net.Uri.encode(title)}")
        }
        return if (params.isEmpty()) "$CHAT/$sessionId" else "$CHAT/$sessionId?${params.joinToString("&")}"
    }
}

@HiltViewModel
class RootViewModel @Inject constructor(auth: AuthRepository) : ViewModel() {
    val startDestination: String = if (auth.isPaired()) Routes.TASKS else Routes.PAIRING
}

@Composable
fun AppNav(
    deepLinkSessionId: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    androidx.compose.runtime.LaunchedEffect(deepLinkSessionId) {
        if (!deepLinkSessionId.isNullOrBlank() && rootViewModel.startDestination != Routes.PAIRING) {
            navController.navigate(Routes.chat(deepLinkSessionId))
            onDeepLinkHandled()
        }
    }

    val showBars = currentRoute == Routes.TASKS ||
        currentRoute == Routes.INBOX ||
        currentRoute == Routes.SCHEDULED ||
        currentRoute == Routes.SETTINGS

    Scaffold(
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    val tabs = listOf(
                        Triple(Routes.TASKS, "Tasks", Icons.AutoMirrored.Filled.ListAlt),
                        Triple(Routes.INBOX, "Inbox", Icons.Filled.Inbox),
                        Triple(Routes.SCHEDULED, "Scheduled", Icons.Filled.Schedule),
                        Triple(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
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
        // Top inset is handled by each screen's own TopAppBar. The bottom inset
        // (nav bar) is applied ONLY to the tabbed screens — the full-screen chat
        // manages its own bottom inset (navigationBars + ime), so padding it here
        // would double the gap above the keyboard / nav bar.
        val tabModifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = rootViewModel.startDestination,
        ) {
            composable(Routes.PAIRING) {
                PairingScreen(onPaired = {
                    navController.navigate(Routes.TASKS) {
                        popUpTo(Routes.PAIRING) { inclusive = true }
                    }
                })
            }
            composable(Routes.TASKS) {
                Box(tabModifier) {
                    TasksScreen(
                        onTaskClick = { id, title -> navController.navigate(Routes.chat(id, title = title)) },
                        onNewTask = { prompt -> navController.navigate(Routes.chat(ChatViewModel.NEW, prompt)) },
                    )
                }
            }
            composable(Routes.INBOX) {
                Box(tabModifier) {
                    InboxScreen(
                        onOpen = { id, title -> navController.navigate(Routes.inboxItem(id, title)) },
                    )
                }
            }
            composable(Routes.SCHEDULED) { Box(tabModifier) { ScheduledScreen() } }
            composable(Routes.SETTINGS) {
                Box(tabModifier) {
                    SettingsScreen(onSignedOut = {
                        navController.navigate(Routes.PAIRING) {
                            popUpTo(0) { inclusive = true }
                        }
                    })
                }
            }
            composable(
                route = "${Routes.INBOX_ITEM}?id={id}&title={title}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("title") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                InboxItemScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.CHAT}/{sessionId}?prompt={prompt}&title={title}",
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("prompt") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                enterTransition = enterPush(),
                exitTransition = exitPush(),
                popEnterTransition = enterPop(),
                popExitTransition = exitPop(),
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
