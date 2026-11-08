package com.mindhr.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.launch

private data class Slide(val icon: ImageVector, val title: String, val text: String)

private val slides = listOf(
    Slide(Icons.Outlined.LocationOn, "Check in from anywhere", "Mark your attendance with one tap. Your location confirms you are at your office."),
    Slide(Icons.Outlined.EventAvailable, "Leave made simple", "See your balance, apply for leave and follow its status without paperwork."),
    Slide(Icons.Outlined.TaskAlt, "Everything in one place", "Payslips, requests, approvals and notifications from your organization."),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == slides.lastIndex

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(MhColors.Navy, MhColors.PrimaryDark)))
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("mindHR", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!last) TextButton(onClick = onDone) { Text("Skip", color = Color.White) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            val s = slides[page]
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(140.dp).background(Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(s.icon, null, Modifier.size(64.dp), tint = MhColors.Green)
                }
                Text(s.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(s.text, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(slides.size) { i ->
                val active = i == pager.currentPage
                Box(
                    Modifier.padding(3.dp).height(8.dp).width(if (active) 24.dp else 8.dp)
                        .background(if (active) MhColors.Green else Color.White.copy(alpha = 0.4f), RoundedCornerShape(50)),
                )
            }
        }
        Button(
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (last) "Get started" else "Next") }
    }
}
