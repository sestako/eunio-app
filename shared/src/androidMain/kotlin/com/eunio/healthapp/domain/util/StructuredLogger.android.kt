package com.eunio.healthapp.domain.util

import android.util.Log

/**
 * Android implementation of platform-specific logging using Log.d()
 * 
 * @param tag The log tag
 * @param message The formatted message to log
 */
internal actual fun platformLogDebug(tag: String, message: String) {
    Log.d(tag, message)
}
