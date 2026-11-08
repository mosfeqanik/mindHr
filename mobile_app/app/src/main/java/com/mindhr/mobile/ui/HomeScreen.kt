package com.mindhr.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mindhr.mobile.data.Attendance
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Everything the dashboard shows, fetched in parallel. Optional parts degrade to null on failure. */
private data class Dashboard(
    val today: Attendance?,
    val leaveRemaining: Double?,
    val presentThisMonth: Int?,
    val myPending: Int?,
    val toApprove: Int?,
    val unread: Int,
)

private suspend fun loadDashboard(): Dashboard = coroutineScope {
    val me = Session.employeeId
    val api = Session.api
    val today = async { if (me == null) null else api.today().body() }
    val balances = async { if (me == null) null else runCatching { api.leaveBalances(me) }.getOrNull() }
    val attendance = async { if (me == null) null else runCatching { api.attendance(me) }.getOrNull() }
    val myLeave = async { if (me == null) null else runCatching { api.leaveRequests(me) }.getOrNull() }
    val myRequests = async { if (me == null) null else runCatching { api.requests(me) }.getOrNull() }
    val teamLeave = async { if (Session.can("LEAVE_APPROVE")) runCatching { api.leaveRequests() }.getOrNull() else null }
    val teamRequests = async { if (Session.can("REQUEST_APPROVE")) runCatching { api.requests() }.getOrNull() else null }
    val notes = async { runCatching { api.notifications() }.getOrDefault(emptyList()) }

    val year = LocalDate.now().year
    val month = YearMonth.now()
    Dashboard(
        today = today.await(),
        leaveRemaining = balances.await()?.filter { it.year == year }?.sumOf { it.entitledDays - it.usedDays },
        presentThisMonth = attendance.await()?.count {
            parseDate(it.workDate)?.let(YearMonth::from) == month && it.status in setOf("PRESENT", "LATE", "HALF_DAY")
        },
        myPending = listOfNotNull(
            myLeave.await()?.count { it.status == "PENDING" },
            myRequests.await()?.count { it.status == "PENDING" },
        ).takeIf { it.isNotEmpty() }?.sum(),
        toApprove = if (!Session.isApprover) null else
            (teamLeave.await()?.count { it.status == "PENDING" && it.employeeId != me } ?: 0) +
                (teamRequests.await()?.count { it.status == "PENDING" && it.employeeId != me } ?: 0),
        unread = notes.await().count { !it.read },
    )
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val loader = rememberLoad { loadDashboard() }
    var sheetFor by remember { mutableStateOf<Boolean?>(null) } // null = closed, true = check-out
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Header(unread = (loader.state as? Load.Ok)?.data?.unread ?: 0, onBell = { nav.navigate(Routes.NOTIFICATIONS) }, onAvatar = { nav.navigate(Routes.PROFILE) })
            LoadContent(loader) { d ->
                PullToRefreshBox(isRefreshing = false, onRefresh = loader.reload) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (Session.employeeId == null) {
                            SectionCard { Text("Your account has no employee profile, so attendance and leave are not available. Contact your HR admin.") }
                        } else {
                            TodayCard(d.today, onCheckIn = { sheetFor = false }, onCheckOut = { sheetFor = true })
                        }
                        SectionTitle("Summary")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiCard(Icons.Outlined.EventAvailable, d.leaveRemaining?.let(::formatDays) ?: "—", "Leave left", MhColors.Green, Modifier.weight(1f)) { nav.switchTab(Routes.LEAVE) }
                            KpiCard(Icons.Outlined.Today, d.presentThisMonth?.toString() ?: "—", "Present this month", MhColors.Primary, Modifier.weight(1f)) { nav.switchTab(Routes.ATTENDANCE) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            KpiCard(Icons.Outlined.PendingActions, d.myPending?.toString() ?: "—", "My pending", MhColors.Amber, Modifier.weight(1f)) { nav.navigate(Routes.REQUESTS) }
                            if (d.toApprove != null) {
                                KpiCard(Icons.Outlined.AssignmentTurnedIn, d.toApprove.toString(), "To approve", MhColors.Blue, Modifier.weight(1f)) { nav.navigate(Routes.APPROVALS) }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        SectionTitle("Quick actions")
                        val actions = buildList {
                            add(Triple(Icons.Outlined.CalendarMonth, "Apply leave", Routes.APPLY_LEAVE))
                            add(Triple(Icons.Outlined.RequestQuote, "New request", Routes.newRequest()))
                            add(Triple(Icons.Outlined.ReceiptLong, "Payslips", Routes.PAYSLIPS))
                            add(Triple(Icons.Outlined.TaskAlt, "My requests", Routes.REQUESTS))
                            if (Session.isApprover) add(Triple(Icons.Outlined.AssignmentTurnedIn, "Approvals", Routes.APPROVALS))
                            add(Triple(Icons.Outlined.AccountCircle, "Profile", Routes.PROFILE))
                        }
                        actions.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (icon, label, route) -> ModuleTile(icon, label, Modifier.weight(1f)) { nav.navigate(route) } }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    sheetFor?.let { out ->
        CheckInSheet(
            checkingOut = out,
            onDismiss = { sheetFor = null },
            onDone = { a ->
                sheetFor = null
                loader.reload()
                scope.launch {
                    snackbar.showSnackbar(if (out) "Checked out at ${formatTime(a.checkOutAt)}" else "Checked in at ${formatTime(a.checkInAt)}")
                }
            },
        )
    }
}

@Composable
private fun Header(unread: Int, onBell: () -> Unit, onAvatar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(MhColors.Navy, MhColors.PrimaryDark)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${greeting()},", color = Color.White.copy(alpha = 0.8f))
            Text(Session.fullName.ifBlank { "there" }, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onBell) {
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) {
                Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White)
            }
        }
        IconButton(onClick = onAvatar) { Icon(Icons.Outlined.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}

@Composable
private fun TodayCard(today: Attendance?, onCheckIn: () -> Unit, onCheckOut: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today · ${formatShortDate(LocalDate.now())}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            today?.status?.let { StatusChip(it) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeBox(Icons.Outlined.Login, "Check in", formatTime(today?.checkInAt), MhColors.Green, Modifier.weight(1f))
            TimeBox(Icons.Outlined.Logout, "Check out", formatTime(today?.checkOutAt), MhColors.Blue, Modifier.weight(1f))
        }
        if (today != null && today.lateMinutes > 0) {
            Text("Late by ${formatMinutes(today.lateMinutes)}", color = MhColors.Amber, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
        when {
            today?.checkInAt == null -> Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Check in") }
            today.checkOutAt == null -> Button(
                onClick = onCheckOut, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MhColors.Navy),
            ) { Text("Check out") }
            else -> Text(
                "Done for today · worked ${formatMinutes(today.workedMinutes)}",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MhColors.Green, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TimeBox(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Row(
        modifier.background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun KpiCard(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(34.dp).background(color.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModuleTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.background(MhColors.Surface, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(44.dp).background(MhColors.PrimarySubtle, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MhColors.Primary)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.width(88.dp))
    }
}
