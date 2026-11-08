package com.mindhr.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mindhr.mobile.BuildConfig
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavHostController, onLogout: () -> Unit) {
    var clock24h by remember { mutableStateOf(Session.clock24h) }
    var confirmLogout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("More") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MhColors.Navy, titleContentColor = Color.White),
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionCard {
                MenuRow(Icons.Outlined.AccountCircle, "My profile") { nav.navigate(Routes.PROFILE) }
                HorizontalDivider()
                MenuRow(Icons.Outlined.ReceiptLong, "Payslips") { nav.navigate(Routes.PAYSLIPS) }
                HorizontalDivider()
                MenuRow(Icons.Outlined.RequestQuote, "My requests") { nav.navigate(Routes.REQUESTS) }
                HorizontalDivider()
                MenuRow(Icons.Outlined.Notifications, "Notifications") { nav.navigate(Routes.NOTIFICATIONS) }
                if (Session.isApprover) {
                    HorizontalDivider()
                    MenuRow(Icons.Outlined.AssignmentTurnedIn, "Approvals") { nav.navigate(Routes.APPROVALS) }
                }
            }
            SectionCard {
                SectionTitle("Settings")
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = MhColors.Primary)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("24-hour clock")
                        Text(if (clock24h) "Times show as 18:30" else "Times show as 6:30 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = clock24h, onCheckedChange = { clock24h = it; Session.clock24h = it })
                }
            }
            SectionCard {
                MenuRow(Icons.AutoMirrored.Outlined.Logout, "Sign out", color = MhColors.Red, chevron = false) { confirmLogout = true }
            }
            Text(
                "mindHR ${BuildConfig.VERSION_NAME} · signed in as ${Session.fullName}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sign out?") },
            confirmButton = { TextButton(onClick = onLogout) { Text("Sign out", color = MhColors.Red) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, color: Color = MhColors.Primary, chevron: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color)
        }
        Text(label, Modifier.weight(1f).padding(start = 14.dp), fontWeight = FontWeight.Medium, color = if (chevron) Color.Unspecified else color)
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
