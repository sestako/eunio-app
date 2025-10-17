package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.Symptom
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

/**
 * Repository interface for daily health log data operations.
 * Handles logging, retrieval, and analysis of daily health data.
 */
interface LogRepository {
    
    /**
     * Saves a daily health log entry.
     * Creates a new entry or updates existing one for the same date.
     * @param log The daily log data to save
     */
    suspend fun saveDailyLog(log: DailyLog): Result<Unit>
    
    /**
     * Retrieves the daily log for a specific date.
     * Returns null if no log exists for that date.
     * @param userId The ID of the user
     * @param date The date to retrieve the log for
     */
    suspend fun getDailyLog(userId: String, date: LocalDate): Result<DailyLog?>
    
    /**
     * Retrieves daily logs within a specific date range.
     * @param userId The ID of the user
     * @param startDate The start of the date range (inclusive)
     * @param endDate The end of the date range (inclusive)
     */
    suspend fun getLogsInRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>>
    
    /**
     * Retrieves the most recent daily logs for the user.
     * @param userId The ID of the user
     * @param limit Maximum number of logs to retrieve
     */
    suspend fun getRecentLogs(userId: String, limit: Int = 30): Result<List<DailyLog>>
    
    /**
     * Deletes a daily log entry.
     * @param userId The ID of the user
     * @param date The date of the log to delete
     */
    suspend fun deleteDailyLog(userId: String, date: LocalDate): Result<Unit>
    
    /**
     * Retrieves logs that contain period flow data within a date range.
     * Used for cycle tracking and analysis.
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     */
    suspend fun getPeriodLogsInRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>>
    
    /**
     * Retrieves logs that contain BBT (Basal Body Temperature) data.
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     */
    suspend fun getBBTLogsInRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>>
    
    /**
     * Retrieves logs that contain fertility indicator data (cervical mucus, OPK results).
     * @param userId The ID of the user
     * @param startDate The start of the date range
     * @param endDate The end of the date range
     */
    suspend fun getFertilityLogsInRange(
        userId: String, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): Result<List<DailyLog>>
    
    /**
     * Counts the total number of logs for a user.
     * @param userId The ID of the user
     */
    suspend fun getLogCount(userId: String): Result<Int>
    
    /**
     * Retrieves logs that match specific symptoms.
     * @param userId The ID of the user
     * @param symptoms List of symptoms to search for
     * @param startDate Optional start date for the search range
     * @param endDate Optional end date for the search range
     */
    suspend fun getLogsBySymptoms(
        userId: String, 
        symptoms: List<Symptom>,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Result<List<DailyLog>>
}