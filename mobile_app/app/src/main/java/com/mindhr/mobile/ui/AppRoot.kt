package com.mindhr.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors

/** Route names. Tabs show the bottom bar; the rest are pushed screens with a back arrow. */
object Routes {
    const val HOME = "home"
    const val ATTENDANCE = "attendance"
    const val LEAVE = "leave"
    const val MORE = "more"
    const val APPLY_LEAVE = "leave/apply"
    const val PAYSLIPS = "payslips"
    const val REQUESTS = "requests"
    const val NEW_REQUEST = "requests/new?type={type}&date={date}"
    const val APPROVALS = "approvals"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val CHANGE_PASSWORD = "profile/password"

    fun newRequest(type: String? = null, date: String? = null) =
        "requests/new?type=${type.orEmpty()}&date=${date.orEmpty()}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.ATTENDANCE, "Attendance", Icons.Filled.Fingerprint),
    Tab(Routes.LEAVE, "Leave", Icons.Filled.EventNote),
    Tab(Routes.MORE, "More", Icons.Filled.GridView),
)

@Composable
fun AppRoot() {
    var onboarded by rememberSaveable { mutableStateOf(Session.onboarded) }
    var loggedIn by rememberSaveable { mutableStateOf(Session.token != null) }
    Session.onUnauthorized = { loggedIn = false }

    when {
        !onboarded -> OnboardingScreen(onDone = { Session.onboarded = true; onboarded = true })
        !loggedIn -> LoginScreen(onLoggedIn = { loggedIn = true })
        else -> MainShell(onLogout = { Session.signOut(); loggedIn = false })
    }
}

@Composable
private fun MainShell(onLogout: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }

    Scaffold(
        containerColor = MhColors.Background,
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MhColors.Surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = { nav.switchTab(tab.route) },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MhColors.Primary,
                            selectedTextColor = MhColors.Primary,
                            indicatorColor = MhColors.PrimarySubtle,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // Pushed screens draw their own top bar; only the bottom inset from the bar applies here.
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Routes.HOME) { HomeScreen(nav) }
            composable(Routes.ATTENDANCE) { AttendanceScreen(nav) }
            composable(Routes.LEAVE) { LeaveScreen(nav) }
            composable(Routes.MORE) { MoreScreen(nav, onLogout) }
            composable(Routes.APPLY_LEAVE) { ApplyLeaveScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PAYSLIPS) { PayslipsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.REQUESTS) { RequestsScreen(nav) }
            composable(
                Routes.NEW_REQUEST,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "" },
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                NewRequestScreen(
                    initialType = e.arguments?.getString("type").orEmpty(),
                    initialDate = e.arguments?.getString("date").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.APPROVALS) { ApprovalsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PROFILE) { ProfileScreen(nav) }
            composable(Routes.CHANGE_PASSWORD) { ChangePasswordScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

/** Standard bottom-tab navigation: one copy of each tab, state kept when switching. */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
