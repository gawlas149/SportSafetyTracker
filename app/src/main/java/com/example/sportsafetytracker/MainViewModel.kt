package com.example.sportsafetytracker

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.math.abs

@Suppress("DEPRECATION")
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _accelerometerData = MutableLiveData<Triple<Float, Float, Float>>()
    val accelerometerData: LiveData<Triple<Float, Float, Float>> = _accelerometerData

    private val _isTracking = MutableLiveData<Boolean>()
    val isTracking: LiveData<Boolean> = _isTracking

    private val _crashHappened = MutableLiveData<Boolean>()
    val crashHappened: LiveData<Boolean> = _crashHappened

    private val _messageSent = MutableLiveData<Boolean>()
    val messageSent: LiveData<Boolean> = _messageSent

    private var isOverlayDisplayed = false

    fun startIsTracking() {
        _isTracking.postValue(true)
        startTracking()
    }
    fun stopIsTracking() {
        _isTracking.postValue(false)
        stopTracking()
        crashAvoided()
    }

    fun messageSent() {
        _messageSent.postValue(true)
        stopIsTracking()
    }
    fun messageCanceled() {
        _messageSent.postValue(false)
    }

    private val sensorDataManager = SensorActivity(this, application) { data ->
        _accelerometerData.postValue(data)
    }

    init {
        val crashDetectionListener = object : SensorActivity.CrashDetectionListener {
            override fun onCrashDetected() {
                _crashHappened.postValue(true)
                checkCrashHappened(true)
            }
            override fun onCrashAvoided() {
                _crashHappened.postValue(false)
                checkCrashHappened(false)
                stopCountdownTimer()
                stopAlarmSound()
            }
        }

        sensorDataManager.setCrashDetectionListener(crashDetectionListener)
    }

    private val settingsManager = Settings(application)

    fun crashAvoided() {
        _crashHappened.postValue(false)
        checkCrashHappened(false)
        stopCountdownTimer()
        stopAlarmSound()
    }

    fun startTracking() {
        sensorDataManager.startTracking()
    }

    fun stopTracking() {
        sensorDataManager.stopTracking()
    }

    override fun onCleared() {
        super.onCleared()
        sensorDataManager.stopTracking()
        countdownTimer?.cancel()
    }

    fun loadDelayTime(): Flow<Int> {
        return settingsManager.loadDelayTime()
    }

    fun getDelayTime(): Int {
        val delayTime: Int
        runBlocking(Dispatchers.IO) {
            delayTime = loadDelayTime().first()
        }
        return delayTime
    }

    fun loadPhoneNumber(): Flow<String> {
        return settingsManager.loadPhoneNumber()
    }

    fun getPhoneNumber(): String {
        val phoneNumber: String
        runBlocking(Dispatchers.IO) {
            phoneNumber = loadPhoneNumber().first()
        }
        return phoneNumber
    }

    fun loadCustomMessage(): Flow<String> {
        return settingsManager.loadCustomMessage()
    }

    fun getCustomMessage(): String {
        val customMessage: String
        runBlocking(Dispatchers.IO) {
            customMessage = loadCustomMessage().first()
        }
        return customMessage
    }

     fun updateDelayTime(delayTime: Int) {
        runBlocking {
            launch {
                settingsManager.saveDelayTime(delayTime)
            }
        }
    }

    fun updatePhoneNumber(number: String) {
        runBlocking {
            launch {
                settingsManager.savePhoneNumber(number)
            }
        }
    }

    fun updateCustomMessage(message: String) {
        runBlocking {
            launch {
                settingsManager.saveCustomMessage(message)
            }
        }
    }

    fun isValidNumber(numberValue: String): Boolean {
        return settingsManager.isValidNumber(numberValue)
    }

    private val _timerValue = MutableLiveData<Long>()
    val timerValue: LiveData<Long> = _timerValue

    private var countdownTimer: CountDownTimer? = null

    fun startCountdownTimer(durationInMillis: Long) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.postValue(millisUntilFinished / 1000)
                updateTimerValue(millisUntilFinished / 1000)

                if (millisUntilFinished / 1000 <= getDelayTime()) {
                    playAlarmSound()
                }
            }

            override fun onFinish() {
                _timerValue.postValue(0)
                updateTimerValue(0)
                crashAvoided()
                fetchLocationAndSendSMS()
            }
        }.start()
    }

    fun stopCountdownTimer() {
        countdownTimer?.cancel()
    }

    private fun fetchLocationAndSendSMS() {
        try {
            val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager

            var locationListener: LocationListener? = null
            locationListener = LocationListener { location ->
                val context = getApplication<Application>().applicationContext
                val locationMessage = "\n\n" + context.getString(R.string.location_message_prefix) + " " + toDMS(location.latitude, location.longitude)
                sendSMS(locationMessage)

                locationListener?.let { listener ->
                    locationManager.removeUpdates(listener)
                }
            }

            if (ContextCompat.checkSelfPermission(getApplication<Application>().applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener)

                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, locationListener)
            } else {
                Toast.makeText(getApplication<Application>().applicationContext, "Location permission not granted", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("LocationDebug", "Failed to fetch location: ${e.message}")
        }
    }

    private fun sendSMS(locationMessage: String) {
        val context = getApplication<Application>().applicationContext
        val phoneNumber = getPhoneNumber()
        val customMessage = getCustomMessage()
        val defaultMessage = context.getString(R.string.default_message)
        val message: String

        val smsManager: SmsManager = SmsManager.getDefault()
        if (customMessage != "") {
            message = customMessage
        } else {
            message = defaultMessage
        }

        if (message.length <= 20) {
            val parts = smsManager.divideMessage(message + locationMessage)
            for (part in parts) {
                smsManager.sendTextMessage(phoneNumber, null, part, null, null)
            }
        }
        else {
            val parts = smsManager.divideMessage(message)
            for (part in parts) {
                smsManager.sendTextMessage(phoneNumber, null, part, null, null)
            }
            smsManager.sendTextMessage(phoneNumber, null, locationMessage, null, null)
        }

        Toast.makeText(context, "$phoneNumber: $message", Toast.LENGTH_LONG).show()

        Log.d("SMS", "Message sent successfully")

        messageSent()
    }

    private fun toDMS(latitude: Double, longitude: Double): String {
        val latDegree = abs(latitude).toInt()
        val latMinute = ((abs(latitude) - latDegree) * 60).toInt()
        val latSecond = ((abs(latitude) - latDegree - latMinute / 60.0) * 3600)

        val lonDegree = abs(longitude).toInt()
        val lonMinute = ((abs(longitude) - lonDegree) * 60).toInt()
        val lonSecond = ((abs(longitude) - lonDegree - lonMinute / 60.0) * 3600)

        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"

        return "${latDegree}°${latMinute}'${String.format(Locale.US, "%.2f", latSecond)}\"$latDirection ${lonDegree}°${lonMinute}'${String.format(
            Locale.US, "%.2f", lonSecond)}\"$lonDirection"
    }

    private var mediaPlayer: MediaPlayer? = null
    fun playAlarmSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(getApplication(), R.raw.severe_warning_alarm)
            mediaPlayer?.setVolume(1.0f, 1.0f)
        }
        mediaPlayer?.start()
    }

    fun stopAlarmSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }


    fun checkCrashHappened(crashHappened : Boolean) {
        if (crashHappened && !isOverlayDisplayed && MainActivity.isAppInBackground) {
            val intent = Intent(getApplication(), OverlayService::class.java)
            ContextCompat.startForegroundService(getApplication(), intent)
            isOverlayDisplayed = true
        } else if (!crashHappened && isOverlayDisplayed) {
            getApplication<Application>().stopService(Intent(getApplication(), OverlayService::class.java))
            isOverlayDisplayed = false
        }
    }

    fun updateTimerValue(newValue: Long) {
        SharedRepository.timerLiveData.postValue(newValue)
    }
}

object SharedRepository {
    val timerLiveData = MutableLiveData<Long>()
}