package com.eunio.healthapp.domain.service

import com.eunio.healthapp.domain.model.HealthReport

/**
 * Service interface for generating PDF reports from health data
 */
interface PDFGenerationService {
    
    /**
     * Generates a PDF document from a health report
     * @param report The health report to convert to PDF
     * @return URL or path to the generated PDF file
     */
    suspend fun generateReportPDF(report: HealthReport): String
    
    /**
     * Generates a PDF with custom template and styling
     * @param report The health report data
     * @param templateType The template style to use
     * @return URL or path to the generated PDF file
     */
    suspend fun generateCustomPDF(report: HealthReport, templateType: PDFTemplate): String
    
    /**
     * Validates that a report has sufficient data for PDF generation
     * @param report The health report to validate
     * @return True if the report can be converted to PDF
     */
    fun validateReportForPDF(report: HealthReport): Boolean
}

/**
 * Available PDF template types
 */
enum class PDFTemplate {
    STANDARD,
    DETAILED,
    SUMMARY,
    MEDICAL_PROFESSIONAL
}