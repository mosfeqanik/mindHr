package com.mindhr.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mindhr.mobile.data.Attendance
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One row of the month log: a recorded day, or a day with no record (weekend or missing punch). */
private data class DayRow(val date: LocalDate, val record: Attendance?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(nav: NavHostController) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    val me = Session.employeeId
    val loader = rememberLoad { if (me == null) emptyList() else Session.api.attendance(me) }
    var correcting by remember { mutableStateOf<LocalDate?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My attendance") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MhColors.Navy, titleContentColor = Color.White),
        )
        Row(
            Modifier.fillMaxWidth().background(MhColors.Surface).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { month = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            Text(
                formatMonth(ym),
                Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { month = ym.plusMonths(1).toString() }, enabled = ym < YearMonth.now()) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
            }
        }
        if (me == null) {
            MessageState(icon = Icons.Outlined.EventBusy, message = "Your account has no employee profile, so there is no attendance to show.")
            return@Column
        }
        LoadContent(loader) { records ->
            val byDate = records.associateBy { parseDate(it.workDate) }
            val today = LocalDate.now()
            val last = if (ym == YearMonth.from(today)) today.dayOfMonth else ym.lengthOfMonth()
            val rows = (last downTo 1).map { d -> ym.atDay(d) }.map { DayRow(it, byDate[it]) }
            val present = rows.count { it.record?.status in setOf("PRESENT", "LATE", "HALF_DAY") }
            val late = rows.count { it.record?.status == "LATE" }
            val worked = rows.sumOf { it.record?.workedMinutes ?: 0 }

            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    SectionCard {
                        Row {
                            Stat("Present", present.toString(), MhColors.Green, Modifier.weight(1f))
                            Stat("Late", late.toString(), MhColors.Amber, Modifier.weight(1f))
                            Stat("Worked", formatMinutes(worked), MhColors.Primary, Modifier.weight(1f))
                        }
                    }
                }
                if (rows.isEmpty()) item { MessageState(message = "No days to show for this month yet.") }
                items(rows, key = { it.date.toString() }) { row -> DayCard(row) { correcting = row.date } }
                item {
                    Text(
                        "Tap a day to ask HR to correct a missed or wrong check-in.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    correcting?.let { date ->
        AlertDialog(
            onDismissRequest = { correcting = null },
            title = { Text("Request a correction?") },
            text = { Text("Send HR a correction request for ${formatShortDate(date)}. You can describe the right times on the next screen.") },
            confirmButton = {
                TextButton(onClick = {
                    correcting = null
                    nav.navigate(Routes.newRequest("ATTENDANCE_CORRECTION", date.toString()))
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { correcting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayCard(row: DayRow, onClick: () -> Unit) {
    val r = row.record
    // The backend's payroll rules treat Friday as the weekly day off.
    val weekend = r == null && row.date.dayOfWeek == DayOfWeek.FRIDAY
    val status = r?.status ?: if (weekend) "WEEKEND" else "NO_RECORD"
    val accent = if (r != null) statusColor(r.status) else MhColors.Border
    Row(
        Modifier.fillMaxWidth().background(MhColors.Surface, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).height(64.dp).background(accent, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(formatShortDate(row.date), fontWeight = FontWeight.SemiBold)
            Text(
                if (r == null) (if (weekend) "Weekend" else "No check-in recorded")
                else "In ${formatTime(r.checkInAt)}  ·  Out ${formatTime(r.checkOutAt)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.padding(end = 12.dp)) { StatusChip(status) }
    }
}
