package com.example.expncetracker.exptkr.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
