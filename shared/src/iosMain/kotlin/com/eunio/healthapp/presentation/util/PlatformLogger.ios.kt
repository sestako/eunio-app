package com.eunio.healthapp.presentation.util

import platform.Foundation.NSLog

actual fun logDebug(tag: String, message: String) {
    NSLog("[$tag] $message")
}
