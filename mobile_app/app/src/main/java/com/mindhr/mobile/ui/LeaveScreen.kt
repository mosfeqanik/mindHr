package com.mindhr.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mindhr.mobile.data.LeaveBalance
import com.mindhr.mobile.data.LeaveRequest
import com.mindhr.mobile.data.LeaveType
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class LeaveData(val types: Map<Long, LeaveType>, val balances: List<LeaveBalance>, val requests: List<LeaveRequest>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveScreen(nav: NavHostController) {
    val me = Session.employeeId
    val loader = rememberLoad {
        if (me == null) LeaveData(emptyMap(), emptyList(), emptyList())
        else coroutineScope {
            val types = async { Session.api.leaveTypes() }
            val balances = async { Session.api.leaveBalances(me) }
            val requests = async { Session.api.leaveRequests(me) }
            LeaveData(types.await().associateBy { it.id }, balances.await(), requests.await().sortedByDescending { it.fromDate })
        }
    }
    var cancelling by remember { mutableStateOf<LeaveRequest?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MhColors.Navy, titleContentColor = Color.White),
            )
        },
        floatingActionButton = {
            if (me != null) ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.APPLY_LEAVE) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Apply") },
                containerColor = MhColors.Primary, contentColor = Color.White,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MhColors.Background,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (me == null) {
                MessageState(icon = Icons.Outlined.EventBusy, message = "Your account has no employee profile, so leave is not available.")
                return@Box
            }
            LoadContent(loader) { data ->
                val year = LocalDate.now().year
                val balances = data.balances.filter { it.year == year }
                LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { BalanceCard(balances, data.types, year) }
                    item { SectionTitle("My applications", Modifier.padding(top = 8.dp)) }
                    if (data.requests.isEmpty()) {
                        item { SectionCard { Text("You have not applied for leave yet. Tap Apply to start.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                    items(data.requests, key = { it.id }) { r ->
                        LeaveRequestCard(r, data.types[r.leaveTypeId]?.name, onCancel = if (r.status == "PENDING") ({ cancelling = r }) else null)
                    }
                }
            }
        }
    }

    cancelling?.let { r ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("Cancel this leave?") },
            text = { Text("${formatDate(r.fromDate)} – ${formatDate(r.toDate)}") },
            confirmButton = {
                TextButton(onClick = {
                    cancelling = null
                    scope.launch {
                        runCatching { Session.api.cancelLeave(r.id) }
                            .onSuccess { loader.reload(); snackbar.showSnackbar("Leave cancelled") }
                            .onFailure { snackbar.showSnackbar(Session.errorMessage(it)) }
                    }
                }) { Text("Cancel leave", color = MhColors.Red) }
            },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun BalanceCard(balances: List<LeaveBalance>, types: Map<Long, LeaveType>, year: Int) {
    val entitled = balances.sumOf { it.entitledDays }
    val used = balances.sumOf { it.usedDays }
    val left = (entitled - used).coerceAtLeast(0.0)
    SectionCard {
        SectionTitle("Balance $year")
        if (balances.isEmpty()) {
            Text("No leave balance has been set for you this year. Ask HR to add it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Donut(used = used, total = entitled)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDays(left), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("days left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Legend(MhColors.Primary, "Used", formatDays(used))
                Legend(MhColors.Green, "Remaining", formatDays(left))
                Legend(MhColors.Muted, "Entitled", formatDays(entitled))
            }
        }
        Spacer(Modifier.height(12.dp))
        balances.forEach { b ->
            val name = types[b.leaveTypeId]?.name ?: "Leave"
            val progress = if (b.entitledDays > 0) (b.usedDays / b.entitledDays).toFloat().coerceIn(0f, 1f) else 0f
            Column(Modifier.padding(vertical = 6.dp)) {
                Row {
                    Text(name, Modifier.weight(1f))
                    Text("${formatDays(b.usedDays)} / ${formatDays(b.entitledDays)} used", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = MhColors.Primary, trackColor = MhColors.PrimarySubtle,
                )
            }
        }
    }
}

@Composable
private fun Donut(used: Double, total: Double) {
    val sweep = if (total > 0) (used / total * 360).toFloat().coerceIn(0f, 360f) else 0f
    Canvas(Modifier.size(120.dp)) {
        val stroke = 14.dp.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(MhColors.Green, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (sweep > 0f) drawArc(MhColors.Primary, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun Legend(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text("  $label: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LeaveRequestCard(r: LeaveRequest, typeName: String?, who: String? = null, onCancel: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                who?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Text(typeName ?: "Leave", fontWeight = if (who == null) FontWeight.SemiBold else FontWeight.Normal)
            }
            StatusChip(r.status)
        }
        Text(
            "${formatDate(r.fromDate)} – ${formatDate(r.toDate)} · ${formatDays(r.days)} day(s)",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
        )
        r.reason?.takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 4.dp)) }
        r.decisionNote?.takeIf { it.isNotBlank() }?.let {
            Text("Note: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        onCancel?.let { TextButton(onClick = it, modifier = Modifier.align(Alignment.End)) { Text("Cancel", color = MhColors.Red) } }
        actions()
    }
}
