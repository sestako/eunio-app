package com.eunio.healthapp.di

/**
 * Simple diagnostic logger for CI/CD tests
 */
object CICDDiagnosticLogger {
    
    private val logEntries = mutableListOf<LogEntry>()
    
    data class LogEntry(
        val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
        val level: LogLevel,
        val message: String
    )
    
    enum class LogLevel {
        INFO, SUCCESS, WARNING, FAILURE
    }
    
    data class LogSummary(
        val totalEntries: Int,
        val successCount: Int,
        val warningCount: Int,
        val errorCount: Int,
        val infoCount: Int
    )
    
    fun logTestStart(testName: String) {
        logEntries.add(LogEntry(level = LogLevel.INFO, message = "Test started: $testName"))
    }
    
    fun logSuccess(message: String) {
        logEntries.add(LogEntry(level = LogLevel.SUCCESS, message = message))
    }
    
    fun logWarning(message: String) {
        logEntries.add(LogEntry(level = LogLevel.WARNING, message = message))
    }
    
    fun logFailure(message: String) {
        logEntries.add(LogEntry(level = LogLevel.FAILURE, message = message))
    }
    
    fun logInfo(message: String) {
        logEntries.add(LogEntry(level = LogLevel.INFO, message = message))
    }
    
    fun getLogEntries(): List<LogEntry> = logEntries.toList()
    
    fun generateLogSummary(): LogSummary {
        val successCount = logEntries.count { it.level == LogLevel.SUCCESS }
        val warningCount = logEntries.count { it.level == LogLevel.WARNING }
        val errorCount = logEntries.count { it.level == LogLevel.FAILURE }
        val infoCount = logEntries.count { it.level == LogLevel.INFO }
        
        return LogSummary(
            totalEntries = logEntries.size,
            successCount = successCount,
            warningCount = warningCount,
            errorCount = errorCount,
            infoCount = infoCount
        )
    }
    
    fun clearLogs() {
        logEntries.clear()
    }
}