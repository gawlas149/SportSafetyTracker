package com.example.sportsafetytracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer

@Suppress("DEPRECATION")
class OverlayService : Service() {
    private lateinit var windowManager: WindowManager

    private var overlayView: View? = null
    private var timerValueObserver: Observer<Long>? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Overlay Service")
            .setContentText("Service is running.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val params: WindowManager.LayoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        params.gravity = Gravity.CENTER

        val localOverlayView = LayoutInflater.from(this).inflate(R.layout.tracker_overlay, null)

        val crashDetectedText: TextView = localOverlayView.findViewById(R.id.crashDetectedText)
        val messageCountdownText: TextView =
            localOverlayView.findViewById(R.id.messageCountdownText)
        val timerValueText: TextView = localOverlayView.findViewById(R.id.timerValueText)
        val cancelCrashButton: Button = localOverlayView.findViewById(R.id.cancelCrashButton)

        timerValueObserver = Observer<Long> { timerValue ->
            timerValueText.text = timerValue.toString()
        }

        crashDetectedText.visibility = View.VISIBLE
        messageCountdownText.visibility = View.VISIBLE
        timerValueText.visibility = View.VISIBLE
        cancelCrashButton.visibility = View.VISIBLE

        SharedRepository.timerLiveData.observeForever(timerValueObserver!!)

        cancelCrashButton.setOnClickListener {
            val intent = Intent("com.example.ACTION_CRASH_AVOIDED")
            sendBroadcast(intent)
        }

        overlayView = localOverlayView

        windowManager.addView(overlayView, params)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "OverlayServiceChannel"
    }

    override fun onDestroy() {
        super.onDestroy()
        val localOverlayView = overlayView

        if (localOverlayView != null) {

            val crashDetectedText: TextView = localOverlayView.findViewById(R.id.crashDetectedText)
            val timerValueText: TextView = localOverlayView.findViewById(R.id.timerValueText)
            val cancelCrashButton: Button = localOverlayView.findViewById(R.id.cancelCrashButton)

            crashDetectedText.visibility = View.GONE
            timerValueText.visibility = View.GONE
            cancelCrashButton.visibility = View.GONE
            timerValueObserver?.let {
                SharedRepository.timerLiveData.removeObserver(it)
                windowManager.removeView(overlayView)
            }
        }
    }
}
