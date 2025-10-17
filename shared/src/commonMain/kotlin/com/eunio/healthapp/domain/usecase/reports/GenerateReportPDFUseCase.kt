package com.eunio.healthapp.domain.usecase.reports

import com.eunio.healthapp.domain.repository.HealthReportRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError

class GenerateReportPDFUseCase(
    private val healthReportRepository: HealthReportRepository
) {
    
    suspend operator fun invoke(reportId: String): Result<String> {
        
        // Get the report first
        val reportResult = healthReportRepository.getReport(reportId)
        if (reportResult is Result.Error) {
            return reportResult
        }
        
        val report = (reportResult as Result.Success).data
            ?: return Result.Error(AppError.ValidationError("Report not found"))
        
        // Generate PDF
        return healthReportRepository.generatePDF(report)
    }
}