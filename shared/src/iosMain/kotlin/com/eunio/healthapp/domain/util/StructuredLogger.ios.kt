package com.eunio.healthapp.domain.util

import platform.Foundation.NSLog

/**
 * iOS implementation of platform-specific logging using NSLog()
 * 
 * @param tag The log tag
 * @param message The formatted message to log
 */
internal actual fun platformLogDebug(tag: String, message: String) {
    NSLog("[$tag] $message")
}
