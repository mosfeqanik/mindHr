package com.mindhr.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.data.WorkRequest
import com.mindhr.mobile.data.WorkRequestCreate
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Backend RequestType values with the labels employees see. */
val requestTypes = linkedMapOf(
    "SALARY_ADVANCE" to "Salary advance",
    "LOAN" to "Loan",
    "EXPENSE" to "Expense claim",
    "ATTENDANCE_CORRECTION" to "Attendance correction",
    "WORK_FROM_HOME" to "Work from home",
    "OVERTIME" to "Overtime",
    "LATE_ARRIVAL" to "Late arrival",
    "EARLY_EXIT" to "Early exit",
    "SHIFT_CHANGE" to "Shift change",
    "ASSET_REQUEST" to "Asset request",
)

/** Types that carry a money amount. */
private val moneyTypes = setOf("SALARY_ADVANCE", "LOAN", "EXPENSE")

fun requestTypeLabel(type: String) = requestTypes[type] ?: humanize(type)

@Composable
fun RequestsScreen(nav: NavHostController) {
    val me = Session.employeeId
    val loader = rememberLoad { if (me == null) emptyList() else Session.api.requests(me).sortedByDescending { it.id } }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var cancelling by remember { mutableStateOf<WorkRequest?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { BackTopBar("My requests", onBack = { nav.popBackStack() }) },
        floatingActionButton = {
            if (me != null) ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.newRequest()) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New request") },
                containerColor = MhColors.Primary, contentColor = Color.White,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MhColors.Background,
    ) { padding ->
        Column(Modifier.padding(padding)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(null, "PENDING", "APPROVED", "REJECTED", "CANCELLED")) { s ->
                    FilterChip(selected = filter == s, onClick = { filter = s }, label = { Text(s?.let(::humanize) ?: "All") })
                }
            }
            LoadContent(loader) { all ->
                val list = all.filter { filter == null || it.status == filter }
                if (list.isEmpty()) {
                    MessageState(icon = Icons.Outlined.RequestQuote, message = if (all.isEmpty()) "No requests yet. Tap New request to ask for an advance, a loan, an expense claim and more." else "No ${humanize(filter).lowercase()} requests.")
                    return@LoadContent
                }
                LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list, key = { it.id }) { r ->
                        WorkRequestCard(r, onCancel = if (r.status == "PENDING") ({ cancelling = r }) else null)
                    }
                }
            }
        }
    }

    cancelling?.let { r ->
        AlertDialog(
            onDismissRequest = { cancelling = null },
            title = { Text("Cancel this request?") },
            text = { Text("${requestTypeLabel(r.type)}: ${r.title}") },
            confirmButton = {
                TextButton(onClick = {
                    cancelling = null
                    scope.launch {
                        runCatching { Session.api.cancelRequest(r.id) }
                            .onSuccess { loader.reload(); snackbar.showSnackbar("Request cancelled") }
                            .onFailure { snackbar.showSnackbar(Session.errorMessage(it)) }
                    }
                }) { Text("Cancel request", color = MhColors.Red) }
            },
            dismissButton = { TextButton(onClick = { cancelling = null }) { Text("Keep") } },
        )
    }
}

@Composable
fun WorkRequestCard(r: WorkRequest, who: String? = null, onCancel: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                who?.let { Text(it, fontWeight = FontWeight.SemiBold) }
                Text(requestTypeLabel(r.type), color = MhColors.Primary, style = MaterialTheme.typography.labelLarge)
                Text(r.title, fontWeight = FontWeight.SemiBold)
            }
            StatusChip(r.status)
        }
        val meta = listOfNotNull(r.requestDate?.let(::formatDate), r.amount?.let(::formatMoney)).joinToString(" · ")
        if (meta.isNotEmpty()) Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        r.details?.takeIf { it.isNotBlank() }?.let { Text(it, Modifier.padding(top = 4.dp)) }
        r.decisionNote?.takeIf { it.isNotBlank() }?.let {
            Text("Note: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        onCancel?.let { TextButton(onClick = it, modifier = Modifier.align(Alignment.End)) { Text("Cancel", color = MhColors.Red) } }
        actions()
    }
}

@Composable
fun NewRequestScreen(initialType: String, initialDate: String, onBack: () -> Unit) {
    var type by rememberSaveable { mutableStateOf(initialType.takeIf { it in requestTypes }) }
    var date by remember { mutableStateOf(parseDate(initialDate) ?: LocalDate.now()) }
    var title by rememberSaveable {
        mutableStateOf(if (initialType == "ATTENDANCE_CORRECTION") "Attendance correction for ${formatShortDate(date)}" else "")
    }
    var details by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val needsAmount = type in moneyTypes
    val amountValue = amount.toDoubleOrNull()
    val valid = type != null && title.isNotBlank() && (!needsAmount || (amountValue != null && amountValue > 0))

    Scaffold(topBar = { BackTopBar("New request", onBack) }, containerColor = MhColors.Background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SelectField("Request type", requestTypes.keys.toList(), type, ::requestTypeLabel, { type = it })
            OutlinedTextField(
                value = title, onValueChange = { if (it.length <= 150) title = it }, label = { Text("Title") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            if (needsAmount) {
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            DateField("Date", date, { date = it })
            OutlinedTextField(
                value = details, onValueChange = { details = it },
                label = { Text(if (type == "ATTENDANCE_CORRECTION") "Correct times and reason (e.g. In 9:00, Out 18:00, forgot to check in)" else "Details") },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = valid && !submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        runCatching {
                            Session.api.createRequest(
                                WorkRequestCreate(type!!, title.trim(), details.trim().ifBlank { null }, if (needsAmount) amountValue else null, date.toString()),
                            )
                        }
                            .onSuccess { onBack() }
                            .onFailure { error = Session.errorMessage(it) }
                        submitting = false
                    }
                },
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Submit request")
            }
        }
    }
}
