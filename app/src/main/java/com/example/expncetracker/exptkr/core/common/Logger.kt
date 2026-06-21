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
        Log.e(TAG, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    fun i(message: String) {
        if (DEBUG) Log.i(TAG, message)
    }
}
