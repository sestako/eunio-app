package com.eunio.healthapp.domain.util

/**
 * Structured logging utility for consistent log formatting across platforms.
 * 
 * All logs follow the format:
 * [tag] OPERATION_NAME key1=value1, key2=value2, ...
 * 
 * Example:
 * [DailyLogSync] SAVE_START userId=user123, logId=2025-10-04, dateEpochDays=20259
 */
object StructuredLogger {
    
    /**
     * Log operation types for daily log sync operations
     */
    enum class LogOperation {
        SAVE_START,
        FIRESTORE_WRITE,
        LOAD_RESULT,
        SYNC_RESULT,
        SAVE_ERROR,
        LOAD_ERROR,
        SYNC_ERROR,
        SYNC_START,
        SYNC_SUCCESS,
        SYNC_FAILURE,
        SYNC_COMPLETE,
        RETRY_ATTEMPT,
        RETRY_SUCCESS,
        RETRY_EXHAUSTED
    }
    
    /**
     * Logs a structured message with consistent formatting
     * 
     * @param tag The log tag (e.g., "DailyLogSync")
     * @param operation The operation being logged
     * @param data Key-value pairs of data to log
     */
    fun logStructured(
        tag: String,
        operation: LogOperation,
        data: Map<String, Any?>
    ) {
        val message = buildStructuredMessage(operation, data)
        platformLogDebug(tag, message)
    }
    
    /**
     * Convenience method for logging with string operation name
     * 
     * @param tag The log tag (e.g., "DailyLogSync")
     * @param operation The operation name as a string
     * @param data Key-value pairs of data to log
     */
    fun logStructured(
        tag: String,
        operation: String,
        data: Map<String, Any?>
    ) {
        val message = buildStructuredMessage(operation, data)
        platformLogDebug(tag, message)
    }
    
    /**
     * Builds a structured log message from operation and data
     */
    private fun buildStructuredMessage(operation: LogOperation, data: Map<String, Any?>): String {
        return buildStructuredMessage(operation.name, data)
    }
    
    /**
     * Builds a structured log message from operation string and data
     */
    private fun buildStructuredMessage(operation: String, data: Map<String, Any?>): String {
        val dataString = data.entries.joinToString(", ") { (key, value) ->
            "$key=${formatValue(value)}"
        }
        return "$operation $dataString"
    }
    
    /**
     * Formats a value for logging, handling nulls and special types
     */
    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> value.toString()
        }
    }
}

/**
 * Platform-specific logging implementation.
 * This is an expect function that will be implemented differently on each platform.
 * 
 * @param tag The log tag
 * @param message The formatted message to log
 */
internal expect fun platformLogDebug(tag: String, message: String)
