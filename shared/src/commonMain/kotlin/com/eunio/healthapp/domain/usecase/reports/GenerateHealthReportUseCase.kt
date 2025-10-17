package com.eunio.healthapp.domain.usecase.reports

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.HealthReportRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import kotlinx.datetime.LocalDate

class GenerateHealthReportUseCase(
    private val healthReportRepository: HealthReportRepository
) {
    
    suspend operator fun invoke(
        userId: String,
        reportType: ReportType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<HealthReport> {
        
        // Validate input parameters
        if (startDate >= endDate) {
            return Result.Error(AppError.ValidationError("Start date must be before end date"))
        }
        
        val dateRange = DateRange(startDate, endDate)
        
        // Validate that sufficient data exists
        val validation = healthReportRepository.validateReportData(userId, dateRange)
        if (validation is Result.Error) {
            return validation
        }
        
        val validationResult = (validation as Result.Success).data
        if (!validationResult.isValid) {
            return Result.Error(AppError.ValidationError("Insufficient data: ${validationResult.errors.joinToString(", ")}"))
        }
        
        // Generate the report
        val reportResult = healthReportRepository.generateReport(userId, reportType, dateRange)
        if (reportResult is Result.Error) {
            return reportResult
        }
        
        val report = (reportResult as Result.Success).data
        
        // Save the report
        val saveResult = healthReportRepository.saveReport(report)
        if (saveResult is Result.Error) {
            return saveResult
        }
        
        return Result.Success(report)
    }
}