package com.eunio.healthapp.domain.usecase.reports

import com.eunio.healthapp.domain.repository.HealthReportRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError

class ShareHealthReportUseCase(
    private val healthReportRepository: HealthReportRepository
) {
    
    suspend operator fun invoke(
        reportId: String,
        expirationDays: Int = 7
    ): Result<String> {
        
        // Validate expiration days
        if (expirationDays < 1 || expirationDays > 30) {
            return Result.Error(AppError.ValidationError("Expiration days must be between 1 and 30"))
        }
        
        // Verify report exists
        val reportResult = healthReportRepository.getReport(reportId)
        if (reportResult is Result.Error) {
            return reportResult
        }
        
        val report = (reportResult as Result.Success).data
            ?: return Result.Error(AppError.ValidationError("Report not found"))
        
        // Generate shareable link
        return healthReportRepository.shareReport(reportId, expirationDays)
    }
}