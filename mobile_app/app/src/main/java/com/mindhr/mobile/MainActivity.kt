package com.mindhr.mobile

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.mindhr.mobile.data.Session
import com.mindhr.mobile.ui.AppRoot
import com.mindhr.mobile.ui.theme.MindHrTheme

class MainActivity : ComponentActivity() {

    private val localNetwork = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Session.init(this)
        requestLocalNetworkIfNeeded()
        enableEdgeToEdge()
        setContent {
            MindHrTheme { AppRoot() }
        }
    }

    /**
     * Android 17 (API 37) blocks apps from local-network addresses such as the emulator's 10.0.2.2
     * unless ACCESS_LOCAL_NETWORK is granted. Only debug builds (declared in src/debug) talk to a
     * local backend, so only they ask.
     */
    private fun requestLocalNetworkIfNeeded() {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return
        if (checkSelfPermission(LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            localNetwork.launch(LOCAL_NETWORK)
        }
    }

    private companion object {
        const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    }
}
