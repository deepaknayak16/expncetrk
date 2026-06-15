package com.example.expncetracker.exptkr.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
val PERMISSION_RATIONALE_SHOWN_KEY = booleanPreferencesKey("permission_rationale_shown")
val SMS_PERMISSION_PERMANENTLY_DENIED_KEY = booleanPreferencesKey("sms_perm_denied")
val NOTIFICATION_PERMISSION_SHOWN_KEY = booleanPreferencesKey("notification_permission_shown")
val BUDGET_ALERTS_ENABLED_KEY = booleanPreferencesKey("budget_alerts_enabled")
val BUDGET_THRESHOLD_KEY = androidx.datastore.preferences.core.floatPreferencesKey("budget_threshold")
