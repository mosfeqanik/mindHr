package com.mindhr.mobile.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.mindhr.mobile.data.ChangePasswordRequest
import com.mindhr.mobile.data.Employee
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class ProfileData(val employee: Employee, val branch: String?, val department: String?)

@Composable
fun ProfileScreen(nav: NavHostController) {
    val loader = rememberLoad {
        coroutineScope {
            val e = Session.api.me()
            val branch = async { e.branchId?.let { runCatching { Session.api.branch(it).name }.getOrNull() } }
            val dept = async { e.departmentId?.let { runCatching { Session.api.department(it).name }.getOrNull() } }
            ProfileData(e, branch.await(), dept.await())
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    Scaffold(topBar = { BackTopBar("Profile", onBack = { nav.popBackStack() }) }, containerColor = MhColors.Background) { padding ->
        Box(Modifier.padding(padding)) {
            if (Session.employeeId == null) {
                MessageState(icon = Icons.Outlined.PersonOff, message = "Your account (${Session.fullName}) has no employee profile.")
                return@Box
            }
            LoadContent(loader) { (e, branch, dept) ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(64.dp).background(MhColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                                Text(initials(e.fullName), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Text(e.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(orNotProvided(e.position), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(e.employeeCode, style = MaterialTheme.typography.labelMedium, color = MhColors.Primary)
                            }
                            e.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                                FilledTonalIconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri())) }) {
                                    Icon(Icons.Outlined.Call, "Call")
                                }
                            }
                            e.email?.takeIf { it.isNotBlank() }?.let { email ->
                                FilledTonalIconButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$email".toUri())) }) {
                                    Icon(Icons.Outlined.Email, "Email")
                                }
                            }
                        }
                    }
                    PrimaryTabRow(selectedTabIndex = tab, containerColor = MhColors.Surface) {
                        listOf("Personal", "Official", "Emergency").forEachIndexed { i, label ->
                            Tab(tab == i, { tab = i }, text = { Text(label) })
                        }
                    }
                    SectionCard {
                        val rows = when (tab) {
                            0 -> listOf(
                                "Email" to e.email,
                                "Phone" to e.phone,
                                "National ID" to e.nationalIdSerial,
                                "Address" to e.address,
                            )
                            1 -> listOf(
                                "Employee code" to e.employeeCode,
                                "Position" to e.position,
                                "Department" to dept,
                                "Branch" to branch,
                                "Employment type" to e.employmentType?.let(::humanize),
                                "Joined" to e.joinedAt?.let(::formatDate),
                                "Status" to if (e.active) "Active" else "Inactive",
                            )
                            else -> listOf(
                                "Contact name" to e.emergencyContactName,
                                "Contact phone" to e.emergencyContactPhone,
                                "Relation" to e.emergencyContactRelation,
                            )
                        }
                        rows.forEachIndexed { i, (label, value) ->
                            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(orNotProvided(value), color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                        }
                    }
                    Text(
                        "To change these details, contact your HR admin.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { nav.navigate(Routes.CHANGE_PASSWORD) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Lock, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Change password")
                    }
                }
            }
        }
    }
}

private fun initials(name: String) =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val mismatch = confirm.isNotEmpty() && confirm != next
    val tooShort = next.isNotEmpty() && next.length < 8

    Scaffold(topBar = { BackTopBar("Change password", onBack) }, containerColor = MhColors.Background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (done) {
                MessageState(icon = Icons.Outlined.Lock, message = "Your password has been changed.", actionLabel = "Done", onAction = onBack)
                return@Column
            }
            PasswordField("Current password", current) { current = it }
            PasswordField("New password (at least 8 characters)", next, isError = tooShort) { next = it }
            PasswordField("Confirm new password", confirm, isError = mismatch) { confirm = it }
            if (mismatch) Text("The passwords do not match.", color = MaterialTheme.colorScheme.error)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !submitting && current.isNotEmpty() && next.length >= 8 && next == confirm,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    submitting = true
                    error = null
                    scope.launch {
                        runCatching {
                            val res = Session.api.changePassword(ChangePasswordRequest(current, next))
                            if (!res.isSuccessful) throw retrofit2.HttpException(res)
                        }
                            .onSuccess { done = true }
                            .onFailure { error = Session.errorMessage(it) }
                        submitting = false
                    }
                },
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Change password")
            }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, isError: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, isError = isError,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}
