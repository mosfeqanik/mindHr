package com.mindhr.mobile.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mindhr.mobile.data.Attendance
import com.mindhr.mobile.data.Branch
import com.mindhr.mobile.data.CheckInRequest
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.theme.MhColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private sealed interface Fix {
    data object Locating : Fix
    data object NoPermission : Fix
    data object Unavailable : Fix
    data class Found(val location: Location) : Fix
}

private fun hasLocationPermission(context: Context) =
    (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) ||
        (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)

@SuppressLint("MissingPermission") // only called after hasLocationPermission
private suspend fun currentLocation(context: Context): Location? =
    runCatching {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .await()
    }.getOrNull()

/**
 * Check-in / check-out: gets a GPS fix, shows how far the user is from their branch's geofence,
 * then posts it with optional remarks. The backend makes the final geofence decision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInSheet(checkingOut: Boolean, onDismiss: () -> Unit, onDone: (Attendance) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fix by remember { mutableStateOf<Fix>(Fix.Locating) }
    var attempt by remember { mutableIntStateOf(0) }
    var branch by remember { mutableStateOf<Branch?>(null) }
    var remarks by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(value = false) }
    var error by remember { mutableStateOf<String?>(null) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) attempt++ else fix = Fix.NoPermission
    }

    LaunchedEffect(attempt) {
        if (!hasLocationPermission(context)) {
            if (attempt == 0) {
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                fix = Fix.NoPermission
            }
            return@LaunchedEffect
        }
        fix = Fix.Locating
        fix = currentLocation(context)?.let { Fix.Found(it) } ?: Fix.Unavailable
    }
    LaunchedEffect(Unit) {
        Session.branchId?.let { id -> branch = runCatching { Session.api.branch(id) }.getOrNull() }
    }

    val title = if (checkingOut) "Check out" else "Check in"

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MhColors.Surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Now: " + LocalTime.now().format(DateTimeFormatter.ofPattern(if (Session.clock24h) "HH:mm" else "h:mm a")),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (val f = fix) {
                Fix.Locating -> LocationRow(Icons.Outlined.GpsFixed, MhColors.Primary, "Finding your location…", loading = true)
                Fix.NoPermission -> {
                    LocationRow(Icons.Outlined.LocationOff, MhColors.Red, "Location permission is needed to $title. Allow it in the app settings, then try again.")
                    TextButton(onClick = { attempt++ }) { Text("Try again") }
                }
                Fix.Unavailable -> {
                    LocationRow(Icons.Outlined.LocationOff, MhColors.Amber, "Could not get your location. Turn on GPS / Location and try again.")
                    TextButton(onClick = { attempt++ }) { Text("Try again") }
                }
                is Fix.Found -> GeofenceInfo(f.location, branch)
            }

            OutlinedTextField(
                value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks (optional)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(
                    enabled = !submitting && (fix is Fix.Found),
                    modifier = Modifier.weight(1f).height(48.dp),
                    onClick = {
                        val loc = (fix as Fix.Found).location
                        submitting = true
                        error = null
                        scope.launch {
                            val body = CheckInRequest(latitude = loc.latitude, longitude = loc.longitude, note = remarks.ifBlank { null })
                            runCatching { if (checkingOut) Session.api.checkOut(body) else Session.api.checkIn(body) }
                                .onSuccess(onDone)
                                .onFailure { error = Session.errorMessage(it) }
                            submitting = false
                        }
                    },
                ) {
                    if (submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text(title)
                }
            }
        }
    }
}

@Composable
private fun LocationRow(icon: ImageVector, color: Color, text: String, loading: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
            else Icon(icon, null, tint = color)
        }
        Text(text, Modifier.weight(1f))
    }
}

@Composable
private fun GeofenceInfo(location: Location, branch: Branch?) {
    val accuracy = "±${location.accuracy.roundToInt()} m"
    val lat = branch?.latitude
    val lng = branch?.longitude
    val radius = branch?.geofenceRadiusM
    if ((lat == null) || (lng == null) || (radius == null)) {
        LocationRow(Icons.Outlined.GpsFixed, MhColors.Green, "Location found ($accuracy).")
        return
    }
    val out = FloatArray(1)
    Location.distanceBetween(location.latitude, location.longitude, lat, lng, out)
    val distance = out[0].roundToInt()
    if (distance <= radius) {
        LocationRow(Icons.Outlined.GpsFixed, MhColors.Green, "You are at ${branch.name} ($distance m away, $accuracy).")
    } else {
        LocationRow(
            Icons.Outlined.WrongLocation, MhColors.Amber,
            "You are $distance m from ${branch.name}; the allowed area is $radius m. The server may refuse this.",
        )
    }
}
