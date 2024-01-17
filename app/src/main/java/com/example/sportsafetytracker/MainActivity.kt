@file:Suppress("DEPRECATION")

package com.example.sportsafetytracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.sportsafetytracker.ui.theme.SportSafetyTrackerTheme


val LocalMainViewModel = compositionLocalOf<MainViewModel> {
    error("No ViewModel provided")
}
@Suppress("DEPRECATION")
class MainActivity : ComponentActivity(), LifecycleObserver {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!android.provider.Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
        else {
            requestPermissionsIfNeeded()
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun requestPermissionsIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), PERMISSIONS_REQUEST_SEND_SMS)
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_FINE_LOCATION)
        } else {
            setupContent()
        }
    }

    private fun setupContent() {
        setContent {
            SportSafetyTrackerTheme {
                CompositionLocalProvider(LocalMainViewModel provides mainViewModel) {
                    SafetyTrackerApp()
                }
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivityForResult(intent, REQUEST_CODE_SYSTEM_ALERT_WINDOW)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if ((requestCode == PERMISSIONS_REQUEST_SEND_SMS || requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded()
        } else {
            Toast.makeText(this, "SMS sending and location tracking permissions are required to run the app", Toast.LENGTH_LONG).show()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        isAppInBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isAppInBackground = true
    }

    companion object {
        private const val PERMISSIONS_REQUEST_SEND_SMS = 1
        private const val PERMISSIONS_REQUEST_FINE_LOCATION = 2
        private const val REQUEST_CODE_SYSTEM_ALERT_WINDOW = 3
        @JvmStatic
        var isAppInBackground = true
    }
    private var isReceiverRegistered = false

    private val crashAvoidedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mainViewModel.crashAvoided()
        }
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStart() {
        super.onStart()
        if (!isReceiverRegistered) {
            val filter = IntentFilter("com.example.ACTION_CRASH_AVOIDED")
            registerReceiver(crashAvoidedReceiver, filter)
            isReceiverRegistered = true
        }
    }
}