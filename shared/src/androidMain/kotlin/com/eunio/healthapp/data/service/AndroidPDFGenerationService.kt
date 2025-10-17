package com.eunio.healthapp.data.service

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.service.PDFGenerationService
import com.eunio.healthapp.domain.service.PDFTemplate
import kotlinx.datetime.toJavaLocalDate
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Android implementation of PDF generation service
 * Uses Android's built-in PDF capabilities and Canvas API
 */
class AndroidPDFGenerationService(
    private val cacheDir: File
) : PDFGenerationService {

    override suspend fun generateReportPDF(report: HealthReport): String {
        return generateCustomPDF(report, PDFTemplate.STANDARD)
    }

    override suspend fun generateCustomPDF(report: HealthReport, templateType: PDFTemplate): String {
        if (!validateReportForPDF(report)) {
            throw IllegalArgumentException("Report does not contain sufficient data for PDF generation")
        }

        val fileName = "health_report_${report.id}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(cacheDir, fileName)

        try {
            // Create PDF document using Android PDF API
            val document = android.graphics.pdf.PdfDocument()
            
            when (templateType) {
                PDFTemplate.STANDARD -> generateStandardPDF(document, report)
                PDFTemplate.DETAILED -> generateDetailedPDF(document, report)
                PDFTemplate.SUMMARY -> generateSummaryPDF(document, report)
                PDFTemplate.MEDICAL_PROFESSIONAL -> generateMedicalPDF(document, report)
            }

            // Write to file
            FileOutputStream(pdfFile).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()

            return pdfFile.absolutePath
        } catch (e: Exception) {
            throw Exception("Failed to generate PDF: ${e.message}", e)
        }
    }

    override fun validateReportForPDF(report: HealthReport): Boolean {
        return report.cycleSummary.totalCycles > 0 || 
               report.symptomAnalysis.mostCommonSymptoms.isNotEmpty() ||
               report.insights.isNotEmpty()
    }

    private fun generateStandardPDF(document: android.graphics.pdf.PdfDocument, report: HealthReport) {
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Set up paint for text
        paint.textSize = 16f
        paint.color = android.graphics.Color.BLACK

        var yPosition = 50f
        val leftMargin = 50f
        val lineHeight = 25f

        // Title
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Health Report", leftMargin, yPosition, paint)
        yPosition += lineHeight * 2

        // Report info
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())}", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Report Period: ${report.dateRange.start} to ${report.dateRange.end}", leftMargin, yPosition, paint)
        yPosition += lineHeight * 2

        // Cycle Summary
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Cycle Summary", leftMargin, yPosition, paint)
        yPosition += lineHeight

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Total Cycles: ${report.cycleSummary.totalCycles}", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Average Cycle Length: ${"%.1f".format(report.cycleSummary.averageCycleLength)} days", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Average Luteal Phase: ${"%.1f".format(report.cycleSummary.averageLutealPhase)} days", leftMargin, yPosition, paint)
        yPosition += lineHeight
        canvas.drawText("Cycle Regularity: ${report.cycleSummary.cycleRegularity.name.replace("_", " ")}", leftMargin, yPosition, paint)
        yPosition += lineHeight * 2

        // Symptom Analysis
        if (report.symptomAnalysis.mostCommonSymptoms.isNotEmpty()) {
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Most Common Symptoms", leftMargin, yPosition, paint)
            yPosition += lineHeight

            paint.textSize = 14f
            paint.isFakeBoldText = false
            report.symptomAnalysis.mostCommonSymptoms.take(5).forEach { symptom ->
                val percentage = (symptom.frequency * 100).toInt()
                canvas.drawText("• ${symptom.symptom.replace("_", " ")}: $percentage%", leftMargin, yPosition, paint)
                yPosition += lineHeight
            }
            yPosition += lineHeight
        }

        // BBT Analysis
        report.symptomAnalysis.bbtAnalysis?.let { bbt ->
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Temperature Analysis", leftMargin, yPosition, paint)
            yPosition += lineHeight

            paint.textSize = 14f
            paint.isFakeBoldText = false
            canvas.drawText("Average Temperature: ${"%.2f".format(bbt.averageTemperature)}°F", leftMargin, yPosition, paint)
            yPosition += lineHeight
            canvas.drawText("Temperature Range: ${"%.2f".format(bbt.temperatureRange.min)}°F - ${"%.2f".format(bbt.temperatureRange.max)}°F", leftMargin, yPosition, paint)
            yPosition += lineHeight
            canvas.drawText("Ovulation Detection Rate: ${(bbt.ovulationDetectionRate * 100).toInt()}%", leftMargin, yPosition, paint)
            yPosition += lineHeight * 2
        }

        // Insights
        if (report.insights.isNotEmpty()) {
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("Key Insights", leftMargin, yPosition, paint)
            yPosition += lineHeight

            paint.textSize = 14f
            paint.isFakeBoldText = false
            report.insights.take(3).forEach { insight ->
                canvas.drawText("• ${insight.insightText}", leftMargin, yPosition, paint)
                yPosition += lineHeight
            }
        }

        // Medical disclaimer
        yPosition = pageInfo.pageHeight - 100f
        paint.textSize = 10f
        paint.color = android.graphics.Color.GRAY
        canvas.drawText("This report is for informational purposes only and should not replace professional medical advice.", 
                       leftMargin, yPosition, paint)

        document.finishPage(page)
    }

    private fun generateDetailedPDF(document: android.graphics.pdf.PdfDocument, report: HealthReport) {
        // More detailed version with charts and additional analysis
        generateStandardPDF(document, report) // For now, use standard template
    }

    private fun generateSummaryPDF(document: android.graphics.pdf.PdfDocument, report: HealthReport) {
        // Condensed version with key metrics only
        generateStandardPDF(document, report) // For now, use standard template
    }

    private fun generateMedicalPDF(document: android.graphics.pdf.PdfDocument, report: HealthReport) {
        // Professional format suitable for healthcare providers
        generateStandardPDF(document, report) // For now, use standard template
    }
}