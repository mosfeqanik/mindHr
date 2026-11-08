package com.mindhr.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindhr.mobile.data.LeaveApplyRequest
import com.mindhr.mobile.data.LeaveType
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ApplyLeaveScreen(onBack: () -> Unit) {
    val loader = rememberLoad { Session.api.leaveTypes().filter { it.active } }
    var type by remember { mutableStateOf<LeaveType?>(null) }
    var from by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var to by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val f = from
    val t = to
    val days = if (f != null && t != null && !t.isBefore(f)) ChronoUnit.DAYS.between(f, t) + 1 else null
    val datesInvalid = f != null && t != null && t.isBefore(f)

    Scaffold(topBar = { BackTopBar("Apply for leave", onBack) }, containerColor = MhColors.Background) { padding ->
        LoadContent(loader, Modifier.padding(padding)) { types ->
            if (types.isEmpty()) {
                MessageState(message = "Your organization has no leave types yet. Ask HR to set them up.", modifier = Modifier.padding(padding))
                return@LoadContent
            }
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SelectField("Leave type", types, type, { it.name + if (it.paid) "" else " (unpaid)" }, { type = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField("From", from, { from = it; if (to?.isBefore(it) == true) to = it }, Modifier.weight(1f))
                    DateField("To", to, { to = it }, Modifier.weight(1f))
                }
                when {
                    datesInvalid -> Text("The end date must be on or after the start date.", color = MaterialTheme.colorScheme.error)
                    days != null -> SectionCard {
                        Text("$days calendar day(s)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "HR may count fewer if the range includes days off.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it }, label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !submitting && type != null && days != null && reason.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    onClick = {
                        submitting = true
                        error = null
                        scope.launch {
                            runCatching {
                                Session.api.applyLeave(LeaveApplyRequest(type!!.id, f.toString(), t.toString(), reason.trim()))
                            }
                                .onSuccess { onBack() }
                                .onFailure { error = Session.errorMessage(it) }
                            submitting = false
                        }
                    },
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Submit application")
                }
            }
        }
    }
}
