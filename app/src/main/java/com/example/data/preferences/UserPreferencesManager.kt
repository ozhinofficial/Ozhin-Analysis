package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "finance_user_settings")

/**
 * Lightweight, secure DataStore Preferences manager for persistent user settings
 * including dark mode preference, primary base currency, and alert thresholds.
 */
class UserPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        val LOW_BALANCE_THRESHOLD = doublePreferencesKey("low_balance_threshold")
        val IS_BALANCE_HIDDEN = booleanPreferencesKey("is_balance_hidden")
        val IS_SCREEN_PRIVACY_ENABLED = booleanPreferencesKey("is_screen_privacy_enabled")
        val AUTO_LOCK_TIMEOUT_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("auto_lock_timeout_seconds")
    }

    val isDarkModeFlow: Flow<Boolean> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] ?: false
        }

    val baseCurrencyFlow: Flow<String> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] ?: "USD"
        }

    val lowBalanceThresholdFlow: Flow<Double> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LOW_BALANCE_THRESHOLD] ?: 500.0
        }

    val isBalanceHiddenFlow: Flow<Boolean> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_BALANCE_HIDDEN] ?: false
        }

    val isScreenPrivacyEnabledFlow: Flow<Boolean> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_SCREEN_PRIVACY_ENABLED] ?: false
        }

    val autoLockTimeoutSecondsFlow: Flow<Int> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT_SECONDS] ?: 0
        }

    suspend fun setDarkMode(isDark: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDark
        }
    }

    suspend fun setBaseCurrency(currencyCode: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] = currencyCode
        }
    }

    suspend fun setLowBalanceThreshold(threshold: Double) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.LOW_BALANCE_THRESHOLD] = threshold
        }
    }

    suspend fun setBalanceHidden(hidden: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_BALANCE_HIDDEN] = hidden
        }
    }

    suspend fun setScreenPrivacyEnabled(enabled: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SCREEN_PRIVACY_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockTimeoutSeconds(seconds: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_LOCK_TIMEOUT_SECONDS] = seconds
        }
    }
}
