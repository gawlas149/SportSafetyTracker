package com.example.sportsafetytracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class Settings(private val context: Context) {

    object PreferencesKeys {
        val DELAY_KEY = intPreferencesKey("delay_key")
        val PHONE_KEY = stringPreferencesKey(name = "phone_key")
        val MESSAGE_KEY = stringPreferencesKey(name = "custom_message_key")
    }

    //suspend fun saveSettings() {
    suspend fun saveDelayTime(delayTime: Int) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.DELAY_KEY] = (delayTime ?: 60) as Int
        }
    }

    fun loadDelayTime(): Flow<Int> {
        var delayTime: Flow<Int> = context.dataStore.data
            .map { preferences ->
                preferences[PreferencesKeys.DELAY_KEY] ?: 60
            }
        return delayTime
    }

    fun isValidNumber(numberValue: String): Boolean {
        val phonePattern = "\\(?\\+[0-9]{1,3}\\)? ?-?[0-9]{1,3} ?-?[0-9]{3,5} ?-?[0-9]{4}( ?-?[0-9]{3})? ?(\\w{1,10}\\s?\\d{1,6})?".toRegex()
        return numberValue.matches(phonePattern)
    }

    suspend fun savePhoneNumber(number: String) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.PHONE_KEY] = (number ?: "")
        }
    }

    fun loadPhoneNumber(): Flow<String> {
        var phoneNumber: Flow<String> = context.dataStore.data
            .map { preferences ->
                preferences[PreferencesKeys.PHONE_KEY] ?: ""
            }
        return phoneNumber
    }

    suspend fun saveCustomMessage(custom_message: String) {
        var custom_message = custom_message
        if (custom_message == "") custom_message = context.getString(R.string.default_message)
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.MESSAGE_KEY] = custom_message
        }
    }

    fun loadCustomMessage(): Flow<String> {
        var customMessage: Flow<String> = context.dataStore.data
            .map { preferences ->
                preferences[PreferencesKeys.MESSAGE_KEY] ?: context.getString(R.string.default_message)
            }
        return customMessage
    }
}