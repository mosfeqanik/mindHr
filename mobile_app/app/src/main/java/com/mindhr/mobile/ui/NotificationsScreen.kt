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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindhr.mobile.data.AppNotification
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

/** "just now", "5 min ago", "3 h ago", "2 days ago", else the date. */
private fun relative(iso: String?): String {
    val t = parseDateTime(iso) ?: return ""
    val d = Duration.between(t, LocalDateTime.now())
    return when {
        d.toMinutes() < 1 -> "just now"
        d.toHours() < 1 -> "${d.toMinutes()} min ago"
        d.toDays() < 1 -> "${d.toHours()} h ago"
        d.toDays() < 7 -> "${d.toDays()} day(s) ago"
        else -> formatDate(iso)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val loader = rememberLoad { Session.api.notifications() }
    // Ids marked read in this session, so the UI updates without waiting for a reload.
    val readNow = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    fun markRead(n: AppNotification) {
        if (n.read || n.id in readNow) return
        readNow.add(n.id)
        scope.launch { runCatching { Session.api.markRead(n.id) } }
    }

    Scaffold(
        topBar = {
            BackTopBar("Notifications", onBack) {
                val list = (loader.state as? Load.Ok)?.data.orEmpty()
                if (list.any { !it.read && it.id !in readNow }) {
                    TextButton(onClick = { list.forEach(::markRead) }) { Text("Mark all read", color = Color.White) }
                }
            }
        },
        containerColor = MhColors.Background,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            LoadContent(loader) { list ->
                PullToRefreshBox(isRefreshing = false, onRefresh = loader.reload, modifier = Modifier.fillMaxSize()) {
                    if (list.isEmpty()) {
                        MessageState(icon = Icons.Outlined.NotificationsNone, message = "You're all caught up. New notifications will show here.")
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(list, key = { it.id }) { n ->
                                val unread = !n.read && n.id !in readNow
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(if (unread) MhColors.PrimarySubtle else MhColors.Surface, RoundedCornerShape(14.dp))
                                        .clickable { markRead(n) }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(Modifier.size(40.dp).background(MhColors.Primary.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Notifications, null, tint = MhColors.Primary)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(n.subject ?: "Notification", fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal)
                                        n.body?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                        Text(relative(n.sentAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (unread) Box(Modifier.padding(top = 6.dp).size(8.dp).background(MhColors.Primary, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
