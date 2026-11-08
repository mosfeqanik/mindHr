package com.mindhr.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindhr.mobile.data.Attendance
import com.mindhr.mobile.data.DecisionRequest
import com.mindhr.mobile.data.LeaveRequest
import com.mindhr.mobile.data.LeaveType
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.data.WorkRequest
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Inbox(
    val names: Map<Long, String>,
    val types: Map<Long, LeaveType>,
    val leave: List<LeaveRequest>,
    val requests: List<WorkRequest>,
    val today: List<Attendance>,
)

/** What a pending decision dialog is about. */
private data class Decision(val approve: Boolean, val label: String, val run: suspend (DecisionRequest) -> Unit)

/** Manager inbox: pending leave and requests from the people in the approver's data scope, plus today's team attendance. */
@Composable
fun ApprovalsScreen(onBack: () -> Unit) {
    val me = Session.employeeId
    val loader = rememberLoad {
        coroutineScope {
            val api = Session.api
            val names = async { runCatching { api.employees().associate { it.id to it.fullName } }.getOrDefault(emptyMap()) }
            val types = async { runCatching { api.leaveTypes().associateBy { it.id } }.getOrDefault(emptyMap()) }
            val leave = async { if (Session.can("LEAVE_APPROVE")) api.leaveRequests() else emptyList() }
            val requests = async { if (Session.can("REQUEST_APPROVE")) api.requests() else emptyList() }
            val today = async { runCatching { api.attendance() }.getOrDefault(emptyList()) }
            val todayIso = LocalDate.now().toString()
            Inbox(
                names.await(), types.await(),
                leave.await().filter { it.status == "PENDING" && it.employeeId != me }.sortedBy { it.fromDate },
                requests.await().filter { it.status == "PENDING" && it.employeeId != me }.sortedBy { it.id },
                today.await().filter { it.workDate == todayIso && it.employeeId != me },
            )
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var decision by remember { mutableStateOf<Decision?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { BackTopBar("Approvals", onBack) }, snackbarHost = { SnackbarHost(snackbar) }, containerColor = MhColors.Background) { padding ->
        Column(Modifier.padding(padding)) {
            val counts = (loader.state as? Load.Ok)?.data
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MhColors.Surface) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Leave" + (counts?.leave?.size?.let { " ($it)" } ?: "")) })
                Tab(tab == 1, { tab = 1 }, text = { Text("Requests" + (counts?.requests?.size?.let { " ($it)" } ?: "")) })
                Tab(tab == 2, { tab = 2 }, text = { Text("Team today") })
            }
            LoadContent(loader) { inbox ->
                fun who(id: Long?) = id?.let { inbox.names[it] } ?: "Employee #${id ?: "?"}"
                when (tab) {
                    0 -> if (inbox.leave.isEmpty()) MessageState(icon = Icons.Outlined.TaskAlt, message = "No leave waiting for your approval.")
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(inbox.leave, key = { it.id }) { r ->
                            LeaveRequestCard(r, inbox.types[r.leaveTypeId]?.name, who = who(r.employeeId)) {
                                DecisionButtons(
                                    onReject = { decision = Decision(false, "leave for ${who(r.employeeId)}") { Session.api.rejectLeave(r.id, it) } },
                                    onApprove = { decision = Decision(true, "leave for ${who(r.employeeId)}") { Session.api.approveLeave(r.id, it) } },
                                )
                            }
                        }
                    }
                    1 -> if (inbox.requests.isEmpty()) MessageState(icon = Icons.Outlined.TaskAlt, message = "No requests waiting for your approval.")
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(inbox.requests, key = { it.id }) { r ->
                            WorkRequestCard(r, who = who(r.employeeId)) {
                                DecisionButtons(
                                    onReject = { decision = Decision(false, "${requestTypeLabel(r.type).lowercase()} for ${who(r.employeeId)}") { Session.api.rejectRequest(r.id, it) } },
                                    onApprove = { decision = Decision(true, "${requestTypeLabel(r.type).lowercase()} for ${who(r.employeeId)}") { Session.api.approveRequest(r.id, it) } },
                                )
                            }
                        }
                    }
                    else -> if (inbox.today.isEmpty()) MessageState(icon = Icons.Outlined.Groups, message = "No one in your team has checked in today yet.")
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(inbox.today, key = { it.id }) { a ->
                            SectionCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(who(a.employeeId), fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "In ${formatTime(a.checkInAt)}  ·  Out ${formatTime(a.checkOutAt)}",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    StatusChip(a.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    decision?.let { d ->
        var note by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!busy) decision = null },
            title = { Text(if (d.approve) "Approve ${d.label}?" else "Reject ${d.label}?") },
            text = {
                OutlinedTextField(
                    value = note, onValueChange = { if (it.length <= 500) note = it },
                    label = { Text(if (d.approve) "Note (optional)" else "Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                )
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    scope.launch {
                        runCatching { d.run(DecisionRequest(note.trim().ifBlank { null })) }
                            .onSuccess { loader.reload(); snackbar.showSnackbar(if (d.approve) "Approved" else "Rejected") }
                            .onFailure { snackbar.showSnackbar(Session.errorMessage(it)) }
                        decision = null
                    }
                }) { Text(if (d.approve) "Approve" else "Reject", color = if (d.approve) MhColors.Green else MhColors.Red) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { decision = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DecisionButtons(onReject: () -> Unit, onApprove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject", color = MhColors.Red) }
        Button(
            onClick = onApprove, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MhColors.Green),
        ) { Text("Approve") }
    }
}
