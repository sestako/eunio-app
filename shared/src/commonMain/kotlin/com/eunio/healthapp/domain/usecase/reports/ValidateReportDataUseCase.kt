package com.eunio.healthapp.domain.usecase.reports

import com.eunio.healthapp.domain.model.DateRange
import com.eunio.healthapp.domain.model.ReportValidationResult
import com.eunio.healthapp.domain.repository.HealthReportRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate

class ValidateReportDataUseCase(
    private val healthReportRepository: HealthReportRepository
) {
    
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<ReportValidationResult> {
        
        // Validate input parameters
        if (startDate >= endDate) {
            return Result.Success(
                ReportValidationResult(
                    isValid = false,
                    errors = listOf("Start date must be before end date"),
                    warnings = emptyList()
                )
            )
        }
        
        val dateRange = DateRange(startDate, endDate)
        return healthReportRepository.validateReportData(userId, dateRange)
    }
}