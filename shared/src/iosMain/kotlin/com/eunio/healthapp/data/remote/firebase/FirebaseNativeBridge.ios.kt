package com.eunio.healthapp.data.remote.firebase

import com.eunio.healthapp.domain.util.StructuredLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS implementation of FirebaseNativeBridge.
 * 
 * This implementation uses the Swift FirebaseBridgeWrapper class to interact with Firebase iOS SDK.
 * The Swift bridge is accessed through the iOS app's framework and is injected at runtime.
 * 
 * Note: The actual Swift bridge instance must be set before using this class.
 * This is typically done during app initialization in the iOS app.
 */
actual class FirebaseNativeBridge {
    
    companion object {
        private const val LOG_TAG = "FirebaseNativeBridge"
        
        /**
         * The Swift bridge instance.
         * This must be set from the iOS app before using the bridge.
         * 
         * Example (from Swift):
         * ```swift
         * FirebaseNativeBridge.companion.setSwiftBridge(FirebaseBridgeWrapper())
         * ```
         */
        @Volatile
        var swiftBridge: Any? = null
            private set
        
        /**
         * Set the Swift bridge instance.
         * This should be called from the iOS app during initialization.
         */
        fun setSwiftBridge(bridge: Any) {
            swiftBridge = bridge
        }
    }
    
    /**
     * Test the bridge connectivity.
     */
    actual fun testConnection(): Boolean {
        return swiftBridge != null
    }
    
    /**
     * Save a daily log to Firestore.
     */
    actual suspend fun saveDailyLog(
        userId: String,
        logId: String,
        data: Map<String, Any>
    ): Result<Unit> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "SAVE_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "operation" to "saveDailyLog"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                // Use dynamic invocation to call the Swift method
                // The Swift method signature: saveDailyLog(_:_:_:_:)
                val bridgeDynamic = bridge.asDynamic()
                
                // Create completion handler
                val completion: (NSError?) -> Unit = { error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "saveDailyLog")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "saveDailyLog",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "SAVE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
                
                // Call the Swift method dynamically
                bridgeDynamic.saveDailyLog(userId, logId, data, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(e, "saveDailyLog")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "saveDailyLog")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "saveDailyLog",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "logId" to logId
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
    
    /**
     * Get a daily log from Firestore.
     */
    actual suspend fun getDailyLog(
        userId: String,
        logId: String
    ): Result<Map<String, Any>?> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "operation" to "getDailyLog"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val bridgeDynamic = bridge.asDynamic()
                
                val completion: (Map<String, Any>?, NSError?) -> Unit = { data, error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "getDailyLog")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "getDailyLog",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId,
                                "found" to (data != null)
                            )
                        )
                        continuation.resume(Result.success(data))
                    }
                }
                
                bridgeDynamic.getDailyLog(userId, logId, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(e, "getDailyLog")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "getDailyLog")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "getDailyLog",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "logId" to logId
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
    
    /**
     * Get a daily log by date.
     */
    actual suspend fun getDailyLogByDate(
        userId: String,
        epochDays: Long
    ): Result<Map<String, Any>?> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_BY_DATE_START",
            data = mapOf(
                "userId" to userId,
                "epochDays" to epochDays,
                "operation" to "getDailyLogByDate"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val bridgeDynamic = bridge.asDynamic()
                
                val completion: (Map<String, Any>?, NSError?) -> Unit = { data, error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "getDailyLogByDate")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "getDailyLogByDate",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "epochDays" to epochDays
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_BY_DATE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "epochDays" to epochDays,
                                "found" to (data != null)
                            )
                        )
                        continuation.resume(Result.success(data))
                    }
                }
                
                bridgeDynamic.getDailyLogByDate(userId, epochDays, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(error, "getDailyLogByDate")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "getDailyLogByDate")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "getDailyLogByDate",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "epochDays" to epochDays
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
    
    /**
     * Get daily logs in a date range.
     */
    actual suspend fun getLogsInRange(
        userId: String,
        startEpochDays: Long,
        endEpochDays: Long
    ): Result<List<Map<String, Any>>> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "GET_RANGE_START",
            data = mapOf(
                "userId" to userId,
                "startEpochDays" to startEpochDays,
                "endEpochDays" to endEpochDays,
                "operation" to "getLogsInRange"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val bridgeDynamic = bridge.asDynamic()
                
                val completion: (List<Map<String, Any>>?, NSError?) -> Unit = { data, error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "getLogsInRange")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "getLogsInRange",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "startEpochDays" to startEpochDays,
                                "endEpochDays" to endEpochDays
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        val logs = data ?: emptyList()
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "GET_RANGE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "startEpochDays" to startEpochDays,
                                "endEpochDays" to endEpochDays,
                                "count" to logs.size
                            )
                        )
                        continuation.resume(Result.success(logs))
                    }
                }
                
                bridgeDynamic.getLogsInRange(userId, startEpochDays, endEpochDays, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(e, "getLogsInRange")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "getLogsInRange")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "getLogsInRange",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "startEpochDays" to startEpochDays,
                        "endEpochDays" to endEpochDays
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
    
    /**
     * Delete a daily log from Firestore.
     */
    actual suspend fun deleteDailyLog(
        userId: String,
        logId: String
    ): Result<Unit> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "DELETE_START",
            data = mapOf(
                "userId" to userId,
                "logId" to logId,
                "operation" to "deleteDailyLog"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val bridgeDynamic = bridge.asDynamic()
                
                val completion: (NSError?) -> Unit = { error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "deleteDailyLog")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "deleteDailyLog",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "DELETE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logId" to logId
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
                
                bridgeDynamic.deleteDailyLog(userId, logId, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(e, "deleteDailyLog")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "deleteDailyLog")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "deleteDailyLog",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "logId" to logId
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
    
    /**
     * Batch save multiple daily logs to Firestore using batch writes.
     * This is more efficient than individual writes and ensures atomicity.
     */
    actual suspend fun batchSaveDailyLogs(
        userId: String,
        logsData: List<Map<String, Any>>
    ): Result<Unit> {
        val bridge = swiftBridge ?: return Result.failure(
            IllegalStateException("Swift bridge not initialized. Call setSwiftBridge() first.")
        )
        
        StructuredLogger.logStructured(
            tag = LOG_TAG,
            operation = "BATCH_SAVE_START",
            data = mapOf(
                "userId" to userId,
                "logsCount" to logsData.size,
                "operation" to "batchSaveDailyLogs"
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val bridgeDynamic = bridge.asDynamic()
                
                val completion: (NSError?) -> Unit = { error ->
                    if (error != null) {
                        val appError = FirebaseErrorMapper.mapError(error, "batchSaveDailyLogs")
                        FirebaseErrorMapper.logError(
                            error = appError,
                            operation = "batchSaveDailyLogs",
                            additionalContext = mapOf(
                                "userId" to userId,
                                "logsCount" to logsData.size
                            )
                        )
                        continuation.resumeWithException(appError)
                    } else {
                        StructuredLogger.logStructured(
                            tag = LOG_TAG,
                            operation = "BATCH_SAVE_SUCCESS",
                            data = mapOf(
                                "userId" to userId,
                                "logsCount" to logsData.size
                            )
                        )
                        continuation.resume(Result.success(Unit))
                    }
                }
                
                bridgeDynamic.batchSaveDailyLogs(userId, logsData, completion)
                
            } catch (e: Exception) {
                val appError = if (e is NSError) {
                    FirebaseErrorMapper.mapError(e, "batchSaveDailyLogs")
                } else {
                    FirebaseErrorMapper.mapThrowable(e, "batchSaveDailyLogs")
                }
                
                FirebaseErrorMapper.logError(
                    error = appError,
                    operation = "batchSaveDailyLogs",
                    additionalContext = mapOf(
                        "userId" to userId,
                        "logsCount" to logsData.size
                    )
                )
                
                continuation.resumeWithException(appError)
            }
        }
    }
}
