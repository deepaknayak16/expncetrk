package com.example.expncetracker.exptkr.core.common

import android.util.Log
import com.example.expncetracker.exptkr.BuildConfig

object Logger {
    private const val TAG = "ExpnceTracker"
    private val DEBUG = BuildConfig.DEBUG

    fun d(message: String) {
        if (DEBUG) Log.d(TAG, message)
    }

    fun d(tag: String, message: String) {
        if (DEBUG) Log.d(tag, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            Log.e(TAG, message, throwable)
        } else {
            // TODO: Route to a crash reporter in production (e.g., Firebase Crashlytics)
            // if (throwable != null) FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (DEBUG) {
            Log.e(tag, message, throwable)
        } else {
            // TODO: Route to a crash reporter in production
        }
    }

    fun i(message: String) {
        if (DEBUG) Log.i(TAG, message)
    }
}
