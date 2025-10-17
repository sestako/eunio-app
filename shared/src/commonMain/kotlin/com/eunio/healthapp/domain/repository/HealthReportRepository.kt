package com.eunio.healthapp.domain.repository

import com.eunio.healthapp.domain.model.HealthReport
import com.eunio.healthapp.domain.model.ReportType
import com.eunio.healthapp.domain.model.DateRange
import com.eunio.healthapp.domain.model.ReportValidationResult
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

interface HealthReportRepository {
    
    /**
     * Validates if sufficient data exists for generating a meaningful report
     */
    suspend fun validateReportData(
        userId: String,
        dateRange: DateRange
    ): Result<ReportValidationResult>
    
    /**
     * Generates a comprehensive health report for the specified date range
     */
    suspend fun generateReport(
        userId: String,
        reportType: ReportType,
        dateRange: DateRange
    ): Result<HealthReport>
    
    /**
     * Saves a generated report to storage
     */
    suspend fun saveReport(report: HealthReport): Result<Unit>
    
    /**
     * Retrieves a specific report by ID
     */
    suspend fun getReport(reportId: String): Result<HealthReport?>
    
    /**
     * Gets all reports for a user
     */
    suspend fun getUserReports(userId: String): Result<List<HealthReport>>
    
    /**
     * Generates and saves a PDF version of the report
     */
    suspend fun generatePDF(report: HealthReport): Result<String>
    
    /**
     * Shares a report securely (generates shareable link)
     */
    suspend fun shareReport(reportId: String, expirationDays: Int = 7): Result<String>
    
    /**
     * Revokes access to a shared report
     */
    suspend fun revokeSharedAccess(reportId: String): Result<Unit>
    
    /**
     * Deletes a report and its associated files
     */
    suspend fun deleteReport(reportId: String): Result<Unit>
}