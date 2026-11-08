package com.mindhr.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- Loading ----------

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Result of [rememberLoad]: the current state plus a way to fetch again. */
class Loader<T>(val state: Load<T>, val reload: () -> Unit)

/**
 * Runs [block] once, again on reload, and again whenever the screen comes back into view
 * (e.g. after a form pushed on top of it is closed), exposing Loading / Ok / Failed.
 */
@Composable
fun <T> rememberLoad(vararg keys: Any?, block: suspend () -> T): Loader<T> {
    var state by remember { mutableStateOf<Load<T>>(Load.Loading) }
    var tick by remember { mutableIntStateOf(0) }
    var resumedBefore by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (resumedBefore) tick++ else resumedBefore = true
    }
    LaunchedEffect(tick, *keys) {
        if (state !is Load.Ok) state = Load.Loading
        state = runCatching { block() }.fold({ Load.Ok(it) }, { Load.Failed(Session.errorMessage(it)) })
    }
    return Loader(state) { tick++ }
}

/** Shows a spinner, an error with Retry, or [content] for loaded data. */
@Composable
fun <T> LoadContent(loader: Loader<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    when (val s = loader.state) {
        Load.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Load.Failed -> MessageState(icon = Icons.Outlined.CloudOff, message = s.message, modifier = modifier, actionLabel = "Retry", onAction = loader.reload)
        is Load.Ok -> content(s.data)
    }
}

/** Centered icon + message; used for empty lists and errors so no screen is ever blank. */
@Composable
fun MessageState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).background(MhColors.PrimarySubtle, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MhColors.Primary) }
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

// ---------- Layout ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit, actions: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MhColors.Navy,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MhColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) { Column(Modifier.padding(16.dp)) { content() } }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

// ---------- Status ----------

/** Colors for LeaveStatus / RequestStatus / AttendanceStatus / PayrollStatus values. */
fun statusColor(status: String?): Color = when (status) {
    "APPROVED", "PRESENT", "PAID", "FINALIZED" -> MhColors.Green
    "PENDING", "LATE", "HALF_DAY", "DRAFT" -> MhColors.Amber
    "REJECTED", "ABSENT" -> MhColors.Red
    "ON_LEAVE" -> MhColors.Blue
    else -> MhColors.Muted
}

@Composable
fun StatusChip(status: String?, label: String = humanize(status)) {
    val color = statusColor(status)
    Text(
        label,
        Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

// ---------- Formatting ----------

/** "SALARY_ADVANCE" -> "Salary advance". */
fun humanize(value: String?): String =
    value?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.titlecase() } ?: "—"

/** Shown for empty profile fields instead of a blank. */
fun orNotProvided(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "Not provided"

// Formatters are built per call (not cached) so they follow the current locale.
private fun pattern(p: String) = DateTimeFormatter.ofPattern(p, Locale.getDefault())

fun parseDate(iso: String?): LocalDate? = iso?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
fun parseDateTime(iso: String?): LocalDateTime? = iso?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

fun formatDate(iso: String?): String = parseDate(iso)?.format(pattern("d MMM yyyy")) ?: "—"
fun formatShortDate(date: LocalDate): String = date.format(pattern("EEE, d MMM"))
fun formatMonth(month: java.time.YearMonth): String = month.format(pattern("MMMM yyyy"))

/** Time of day in the user's chosen 12h / 24h format. */
fun formatTime(iso: String?): String {
    val t = parseDateTime(iso) ?: return "--:--"
    return t.format(pattern(if (Session.clock24h) "HH:mm" else "h:mm a"))
}

fun formatMinutes(minutes: Int): String = if (minutes <= 0) "0h" else "${minutes / 60}h ${minutes % 60}m"

fun formatMoney(amount: Double?): String = amount?.let { String.format(Locale.getDefault(), "%,.2f", it) } ?: "—"

fun formatDays(days: Double): String = if (days % 1.0 == 0.0) days.toInt().toString() else days.toString()
