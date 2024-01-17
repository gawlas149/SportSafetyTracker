package com.example.sportsafetytracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class SensorActivity(
    private val mainViewModel: MainViewModel,
    context: Context,
    private val onDataChanged: (Triple<Float, Float, Float>) -> Unit) : SensorEventListener {
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    interface CrashDetectionListener {
        fun onCrashDetected()
        fun onCrashAvoided()
    }

    private var crashDetectionListener: CrashDetectionListener? = null

    fun setCrashDetectionListener(listener: CrashDetectionListener) {
        crashDetectionListener = listener
    }

    private var lastAcceleration : Double? = 10.0
    fun startTracking() {
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopTracking() {
        lastAcceleration = 10.0
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt((x*x + y*y + z*z).toDouble())

            val currentDelayTime : Long = mainViewModel.getDelayTime().toLong()

            if (mainViewModel.crashHappened.value == true
                && abs((lastAcceleration ?: 0.0) - acceleration) > 2
                && (mainViewModel.timerValue.value ?: 0) <= (currentDelayTime.toInt() + 5)) {
                crashDetectionListener?.onCrashAvoided()
            }

            if (abs((lastAcceleration ?: 0.0) - acceleration) > 8
                && (mainViewModel.crashHappened.value == false || mainViewModel.crashHappened.value == null)) {
                crashDetectionListener?.onCrashDetected()

                if (currentDelayTime > 0) {
                    mainViewModel.startCountdownTimer((currentDelayTime + 10)*1000)
                }
            }

            lastAcceleration = acceleration

            onDataChanged(Triple(x, y, z))
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if necessary
    }
}