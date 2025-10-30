package com.eunio.healthapp.data.remote.firebase

import com.eunio.healthapp.bridge.*
import com.eunio.healthapp.domain.util.StructuredLogger
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume

/**
 * iOS implementation of FirebaseNativeBridge using typed cinterop bindings from EunioBridgeKit.
 * 
 * The concrete Firebase bridge implementation (FirebaseIOSBridge) is created
 * in the iOS app and injected via setSwiftBridge() at app startup.
 * 
 * This implementation uses proper typed cinterop bindings from the EunioBridgeKit framework,
 * providing type safety and better performance without any dynamic calls.
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseNativeBridge {
    
    companion object {
        private const val LOG_TAG = "FirebaseNativeBridge"
        
        /**
         * The Swift bridge instance, injected at runtime.
         * This is a typed FirebaseBridgeProtocol instance from the EunioBridgeKit cinterop bindings.
         */
        @Volatile
        private var bridgeInstance: FirebaseBridgeProtocol? = null
        
        /**
         * Set the Swift bridge instance.
         * This should be called from the iOS app during initialization.
         * 
         * @param bridge The FirebaseIOSBridge instance from Swift that conforms to FirebaseBridgeProtocol
         */
        fun setSwiftBridge(bridge: FirebaseBridgeProtocol) {
            bridgeInstance = bridge
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "BRIDGE_SET",
                data = mapOf("initialized" to true)
            )
        }
        
        /**
         * Get the bridge instance, throwing if not initialized.
         */
        private fun getBridge(): FirebaseBridgeProtocol {
            return bridgeInstance ?: throw IllegalStateException(
                "Firebase bridge not initialized. Call setSwiftBridge() from iOS app first."
            )
        }
    }
    
    /**
     * Test the bridge connectivity.
     */
    actual fun testConnection(): Boolean {
        val isConnected = bridgeInstance != null
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "TEST_CONNECTION",
            data = mapOf("connected" to isConnected)
        )
        return isConnected
    }
    
    /**
     * Save a daily log to Firestore using typed cinterop bindings.
     */
    actual suspend fun saveDailyLog(
        userId: String,
        logId: String,
        data: Map<String, Any>
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "SAVE_DAILY_LOG_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId
            )
        )
        
        try {
            val bridge = getBridge()
            val nsData = data.toNSDictionary()
            
            bridge.saveDailyLogWithUserId(
                userId = userId,
                logId = logId,
                data = nsData as Map<Any?, *>,
                completion = { error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "SAVE_DAILY_LOG_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "SAVE_DAILY_LOG_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "SAVE_DAILY_LOG_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Get a daily log from Firestore using typed cinterop bindings.
     */
    actual suspend fun getDailyLog(
        userId: String,
        logId: String
    ): Result<Map<String, Any>?> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_DAILY_LOG_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId
            )
        )
        
        try {
            val bridge = getBridge()
            
            bridge.getDailyLogWithUserId(
                userId = userId,
                logId = logId,
                completion = { nsData, error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_DAILY_LOG_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        val kotlinData = (nsData as? NSDictionary)?.toKotlinMap()
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_DAILY_LOG_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId,
                                "found" to (kotlinData != null)
                            )
                        )
                        continuation.resume(Result.success(kotlinData))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_DAILY_LOG_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Get a daily log by date using typed cinterop bindings.
     */
    actual suspend fun getDailyLogByDate(
        userId: String,
        epochDays: Long
    ): Result<Map<String, Any>?> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_DAILY_LOG_BY_DATE_START",
            data = mapOf(
                "userId" to userId,
                "epochDays" to epochDays
            )
        )
        
        try {
            val bridge = getBridge()
            
            bridge.getDailyLogByDateWithUserId(
                userId = userId,
                epochDays = epochDays,
                completion = { nsData, error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_DAILY_LOG_BY_DATE_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "epochDays" to epochDays,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        val kotlinData = (nsData as? NSDictionary)?.toKotlinMap()
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_DAILY_LOG_BY_DATE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "epochDays" to epochDays,
                                "found" to (kotlinData != null)
                            )
                        )
                        continuation.resume(Result.success(kotlinData))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_DAILY_LOG_BY_DATE_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "epochDays" to epochDays,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Get logs in a date range using typed cinterop bindings.
     */
    actual suspend fun getLogsInRange(
        userId: String,
        startEpochDays: Long,
        endEpochDays: Long
    ): Result<List<Map<String, Any>>> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_LOGS_IN_RANGE_START",
            data = mapOf(
                "userId" to userId,
                "startEpochDays" to startEpochDays,
                "endEpochDays" to endEpochDays
            )
        )
        
        try {
            val bridge = getBridge()
            
            bridge.getLogsInRangeWithUserId(
                userId = userId,
                startEpochDays = startEpochDays,
                endEpochDays = endEpochDays,
                completion = { nsArray, error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_LOGS_IN_RANGE_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "startEpochDays" to startEpochDays,
                                "endEpochDays" to endEpochDays,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        val kotlinList = (nsArray as? NSArray)?.toKotlinListOfMaps() ?: emptyList()
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_LOGS_IN_RANGE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "startEpochDays" to startEpochDays,
                                "endEpochDays" to endEpochDays,
                                "count" to kotlinList.size
                            )
                        )
                        continuation.resume(Result.success(kotlinList))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "GET_LOGS_IN_RANGE_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "startEpochDays" to startEpochDays,
                    "endEpochDays" to endEpochDays,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Delete a daily log using typed cinterop bindings.
     */
    actual suspend fun deleteDailyLog(
        userId: String,
        logId: String
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "DELETE_DAILY_LOG_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId
            )
        )
        
        try {
            val bridge = getBridge()
            
            bridge.deleteDailyLogWithUserId(
                userId = userId,
                logId = logId,
                completion = { error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "DELETE_DAILY_LOG_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "DELETE_DAILY_LOG_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "DELETE_DAILY_LOG_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "logId" to logId,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
    
    /**
     * Batch save daily logs using typed cinterop bindings.
     */
    actual suspend fun batchSaveDailyLogs(
        userId: String,
        logsData: List<Map<String, Any>>
    ): Result<Unit> = suspendCancellableCoroutine { continuation ->
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "BATCH_SAVE_DAILY_LOGS_START",
            data = mapOf(
                "userId" to userId,
                "count" to logsData.size
            )
        )
        
        try {
            val bridge = getBridge()
            val nsArray = logsData.toNSArrayOfDictionaries()
            
            bridge.batchSaveDailyLogsWithUserId(
                userId = userId,
                logsData = nsArray as List<*>,
                completion = { error ->
                    if (error != null) {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "BATCH_SAVE_DAILY_LOGS_ERROR",
                            data = mapOf(
                                "userId" to userId,
                                "count" to logsData.size,
                                "error" to (error.localizedDescription ?: "Unknown error")
                            )
                        )
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "BATCH_SAVE_DAILY_LOGS_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "count" to logsData.size
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
            )
        } catch (e: Exception) {
            StructuredLogger.logStructured(
                tag = LOG_TAG,
                operation = "BATCH_SAVE_DAILY_LOGS_EXCEPTION",
                data = mapOf(
                    "userId" to userId,
                    "count" to logsData.size,
                    "error" to (e.message ?: "Unknown exception")
                )
            )
            continuation.resume(Result.failure(e))
        }
    }
}

// MARK: - Extension Functions for Type Conversion

/**
 * Convert a Kotlin Map to NSDictionary.
 */
@OptIn(ExperimentalForeignApi::class)
private fun Map<String, Any>.toNSDictionary(): NSDictionary {
    val dict = NSMutableDictionary()
    forEach { (key, value) ->
        val nsValue: Any = when (value) {
            is String -> NSString.create(string = value)
            is Int -> NSNumber(int = value)
            is Long -> NSNumber(longLong = value)
            is Double -> NSNumber(double = value)
            is Boolean -> NSNumber(bool = value)
            is List<*> -> value.toNSArray()
            is Map<*, *> -> (value as Map<String, Any>).toNSDictionary()
            else -> NSNull()
        }
        val nsKey = NSString.create(string = key)
        dict.setObject(nsValue, forKey = nsKey)
    }
    return dict
}

/**
 * Convert a Kotlin List to NSArray.
 */
@OptIn(ExperimentalForeignApi::class)
private fun List<*>.toNSArray(): NSArray {
    val array = NSMutableArray()
    forEach { item ->
        val nsValue: Any = when (item) {
            is String -> NSString.create(string = item)
            is Int -> NSNumber(int = item)
            is Long -> NSNumber(longLong = item)
            is Double -> NSNumber(double = item)
            is Boolean -> NSNumber(bool = item)
            is Map<*, *> -> (item as Map<String, Any>).toNSDictionary()
            is List<*> -> item.toNSArray()
            else -> NSNull()
        }
        array.addObject(nsValue)
    }
    return array
}

/**
 * Convert a List of Maps to NSArray of NSDictionaries.
 */
@OptIn(ExperimentalForeignApi::class)
private fun List<Map<String, Any>>.toNSArrayOfDictionaries(): NSArray {
    val array = NSMutableArray()
    forEach { map ->
        array.addObject(map.toNSDictionary())
    }
    return array
}

/**
 * Convert an NSDictionary to Kotlin Map.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSDictionary.toKotlinMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val keys = this.allKeys as? List<*> ?: return result
    
    keys.forEach { key ->
        val keyStr = key.toString()
        val value = this.objectForKey(key)
        
        val kotlinValue: Any = when (value) {
            is NSString -> value.toString()
            is NSNumber -> {
                val objCType = value.objCType?.toKString()
                when {
                    objCType == "c" || objCType == "B" -> value.boolValue
                    objCType?.contains("f") == true || objCType?.contains("d") == true -> value.doubleValue
                    else -> value.longLongValue
                }
            }
            is NSArray -> value.toKotlinList()
            is NSDictionary -> value.toKotlinMap()
            is NSNull -> return@forEach
            else -> value?.toString() ?: return@forEach
        }
        
        result[keyStr] = kotlinValue
    }
    return result
}

/**
 * Convert an NSArray to Kotlin List.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSArray.toKotlinList(): List<Any> {
    val result = mutableListOf<Any>()
    for (i in 0 until this.count.toInt()) {
        val item = this.objectAtIndex(i.toULong())
        val kotlinItem: Any = when (item) {
            is NSString -> item.toString()
            is NSNumber -> {
                val objCType = item.objCType?.toKString()
                when {
                    objCType == "c" || objCType == "B" -> item.boolValue
                    objCType?.contains("f") == true || objCType?.contains("d") == true -> item.doubleValue
                    else -> item.longLongValue
                }
            }
            is NSArray -> item.toKotlinList()
            is NSDictionary -> item.toKotlinMap()
            is NSNull -> continue
            else -> item?.toString() ?: continue
        }
        result.add(kotlinItem)
    }
    return result
}

/**
 * Convert an NSArray of NSDictionaries to Kotlin List of Maps.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSArray.toKotlinListOfMaps(): List<Map<String, Any>> {
    val result = mutableListOf<Map<String, Any>>()
    for (i in 0 until this.count.toInt()) {
        val item = this.objectAtIndex(i.toULong())
        if (item is NSDictionary) {
            result.add(item.toKotlinMap())
        }
    }
    return result
}
