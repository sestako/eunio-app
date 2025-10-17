package com.eunio.healthapp.data.repository

import com.eunio.healthapp.domain.model.*
import com.eunio.healthapp.domain.repository.*
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.domain.service.PDFGenerationService
import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.math.sqrt

class HealthReportRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val cycleRepository: CycleRepository,
    private val logRepository: LogRepository,
    private val insightRepository: InsightRepository,
    private val pdfGenerationService: PDFGenerationService
) : HealthReportRepository {

    override suspend fun validateReportData(
        userId: String,
        dateRange: DateRange
    ): Result<ReportValidationResult> {
        return try {
            val cycles = cycleRepository.getCycleHistory(userId, 50)
            val logs = logRepository.getLogsInRange(userId, dateRange.start, dateRange.end)
            
            when {
                cycles is Result.Error -> cycles
                logs is Result.Error -> logs
                else -> {
                    val cycleData = (cycles as Result.Success).data
                    val logData = (logs as Result.Success).data
                    
                    val bbtCount = logData.count { it.bbt != null }
                    val dateRangeDays = dateRange.start.daysUntil(dateRange.end)
                    
                    val validationResult = HealthReportValidation.validateReportRequest(
                        cycleCount = cycleData.size,
                        logCount = logData.size,
                        bbtCount = bbtCount,
                        dateRangeDays = dateRangeDays
                    )
                    
                    Result.Success(validationResult)
                }
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun generateReport(
        userId: String,
        reportType: ReportType,
        dateRange: DateRange
    ): Result<HealthReport> {
        return try {
            // Validate data first
            val validation = validateReportData(userId, dateRange)
            if (validation is Result.Error) return validation
            
            val validationResult = (validation as Result.Success).data
            if (!validationResult.isValid) {
                return Result.Error(AppError.ValidationError("Insufficient data for report generation: ${validationResult.errors.joinToString()}"))
            }

            // Fetch all required data
            val cycles = cycleRepository.getCycleHistory(userId, 50)
            val logs = logRepository.getLogsInRange(userId, dateRange.start, dateRange.end)
            val insights = insightRepository.getInsightHistory(userId, 100)

            when {
                cycles is Result.Error -> cycles
                logs is Result.Error -> logs
                insights is Result.Error -> insights
                else -> {
                    val cycleData = (cycles as Result.Success).data
                    val logData = (logs as Result.Success).data
                    val insightData = (insights as Result.Success).data

                    val report = HealthReport(
                        id = generateReportId(),
                        userId = userId,
                        generatedDate = Clock.System.now(),
                        reportType = reportType,
                        dateRange = dateRange,
                        cycleSummary = generateCycleSummary(cycleData),
                        symptomAnalysis = generateSymptomAnalysis(logData),
                        insights = insightData.filter { 
                            it.generatedDate >= dateRange.start.atStartOfDayIn(TimeZone.UTC) &&
                            it.generatedDate <= dateRange.end.atStartOfDayIn(TimeZone.UTC)
                        }
                    )

                    Result.Success(report)
                }
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun saveReport(report: HealthReport): Result<Unit> {
        return try {
            val result = firestoreService.saveHealthReport(report)
            when (result) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun getReport(reportId: String): Result<HealthReport?> {
        return try {
            firestoreService.getHealthReport(reportId)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun getUserReports(userId: String): Result<List<HealthReport>> {
        return try {
            firestoreService.getUserHealthReports(userId)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun generatePDF(report: HealthReport): Result<String> {
        return try {
            val pdfUrl = pdfGenerationService.generateReportPDF(report)
            
            // Update report with PDF URL
            val updatedReport = report.copy(pdfUrl = pdfUrl)
            saveReport(updatedReport)
            
            Result.Success(pdfUrl)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun shareReport(reportId: String, expirationDays: Int): Result<String> {
        return try {
            val shareUrlResult = firestoreService.createShareableLink(reportId, expirationDays)
            when (shareUrlResult) {
                is Result.Success -> {
                    // Update report sharing status
                    val report = getReport(reportId)
                    if (report is Result.Success && report.data != null) {
                        val updatedReport = report.data.copy(isShared = true)
                        saveReport(updatedReport)
                    }
                    Result.Success(shareUrlResult.data)
                }
                is Result.Error -> shareUrlResult
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun revokeSharedAccess(reportId: String): Result<Unit> {
        return try {
            val revokeResult = firestoreService.revokeShareableLink(reportId)
            when (revokeResult) {
                is Result.Success -> {
                    // Update report sharing status
                    val report = getReport(reportId)
                    if (report is Result.Success && report.data != null) {
                        val updatedReport = report.data.copy(isShared = false)
                        saveReport(updatedReport)
                    }
                    Result.Success(Unit)
                }
                is Result.Error -> revokeResult
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            // Revoke any shared access first
            revokeSharedAccess(reportId)
            
            // Delete the report
            firestoreService.deleteHealthReport(reportId)
        } catch (e: Exception) {
            Result.Error(AppError.UnknownError(e.message ?: "Unknown error"))
        }
    }

    private fun generateReportId(): String {
        return "report_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }

    private fun generateCycleSummary(cycles: List<Cycle>): CycleSummary {
        if (cycles.isEmpty()) {
            return CycleSummary(
                totalCycles = 0,
                averageCycleLength = 0.0,
                averageLutealPhase = 0.0,
                cycleRegularity = CycleRegularity.IRREGULAR,
                ovulationPatterns = OvulationPatterns(0.0, 0.0, 0.0)
            )
        }

        val completedCycles = cycles.filter { it.endDate != null && it.cycleLength != null }
        val cycleLengths = completedCycles.mapNotNull { it.cycleLength }
        val lutealPhaseLengths = completedCycles.mapNotNull { it.lutealPhaseLength }

        val avgCycleLength = if (cycleLengths.isNotEmpty()) cycleLengths.average() else 0.0
        val avgLutealPhase = if (lutealPhaseLengths.isNotEmpty()) lutealPhaseLengths.average() else 0.0

        val cycleRegularity = calculateCycleRegularity(cycleLengths)
        val ovulationPatterns = calculateOvulationPatterns(completedCycles)

        return CycleSummary(
            totalCycles = completedCycles.size,
            averageCycleLength = avgCycleLength,
            averageLutealPhase = avgLutealPhase,
            cycleRegularity = cycleRegularity,
            ovulationPatterns = ovulationPatterns
        )
    }

    private fun calculateCycleRegularity(cycleLengths: List<Int>): CycleRegularity {
        if (cycleLengths.size < 2) return CycleRegularity.IRREGULAR

        val mean = cycleLengths.average()
        val variance = cycleLengths.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = sqrt(variance)

        return when {
            standardDeviation <= 1.0 -> CycleRegularity.VERY_REGULAR
            standardDeviation <= 2.0 -> CycleRegularity.REGULAR
            standardDeviation <= 4.0 -> CycleRegularity.SOMEWHAT_IRREGULAR
            standardDeviation <= 7.0 -> CycleRegularity.IRREGULAR
            else -> CycleRegularity.VERY_IRREGULAR
        }
    }

    private fun calculateOvulationPatterns(cycles: List<Cycle>): OvulationPatterns {
        val cyclesWithOvulation = cycles.filter { 
            it.confirmedOvulationDate != null || it.predictedOvulationDate != null 
        }

        if (cyclesWithOvulation.isEmpty()) {
            return OvulationPatterns(0.0, 0.0, 0.0)
        }

        val ovulationDays = cyclesWithOvulation.mapNotNull { cycle ->
            val ovulationDate = cycle.confirmedOvulationDate ?: cycle.predictedOvulationDate
            ovulationDate?.let { 
                val startDate = cycle.startDate
                // Calculate day of cycle (simplified calculation)
                14.0 // Default ovulation day for now
            }
        }

        val avgOvulationDay = if (ovulationDays.isNotEmpty()) ovulationDays.average() else 0.0
        val confirmationRate = cycles.count { it.confirmedOvulationDate != null }.toDouble() / cycles.size
        val fertilityAccuracy = 0.85 // Placeholder - would be calculated based on actual predictions vs outcomes

        return OvulationPatterns(
            averageOvulationDay = avgOvulationDay,
            ovulationConfidenceRate = confirmationRate,
            fertilityWindowAccuracy = fertilityAccuracy
        )
    }

    private fun generateSymptomAnalysis(logs: List<DailyLog>): SymptomAnalysis {
        val symptomFrequencies = calculateSymptomFrequencies(logs)
        val symptomTrends = calculateSymptomTrends(logs)
        val moodPatterns = calculateMoodPatterns(logs)
        val bbtAnalysis = calculateBBTAnalysis(logs)

        return SymptomAnalysis(
            mostCommonSymptoms = symptomFrequencies,
            symptomTrends = symptomTrends,
            moodPatterns = moodPatterns,
            bbtAnalysis = bbtAnalysis
        )
    }

    private fun calculateSymptomFrequencies(logs: List<DailyLog>): List<SymptomFrequency> {
        val symptomCounts = mutableMapOf<String, Int>()
        
        logs.forEach { log ->
            log.symptoms.forEach { symptom ->
                symptomCounts[symptom.name] = (symptomCounts[symptom.name] ?: 0) + 1
            }
        }

        return symptomCounts.map { (symptom, count) ->
            SymptomFrequency(
                symptom = symptom,
                frequency = count.toDouble() / logs.size,
                cyclePhaseDistribution = mapOf() // Simplified for now
            )
        }.sortedByDescending { it.frequency }.take(10)
    }

    private fun calculateSymptomTrends(logs: List<DailyLog>): Map<String, TrendData> {
        // Simplified trend calculation
        return mapOf()
    }

    private fun calculateMoodPatterns(logs: List<DailyLog>): MoodPatterns {
        val moodCounts = logs.mapNotNull { it.mood }.groupingBy { it }.eachCount()
        val dominantMood = moodCounts.maxByOrNull { it.value }?.key?.name ?: "NEUTRAL"
        
        return MoodPatterns(
            dominantMood = dominantMood,
            moodVariability = calculateMoodVariability(logs),
            cyclePhaseCorrelations = mapOf() // Simplified for now
        )
    }

    private fun calculateMoodVariability(logs: List<DailyLog>): Double {
        val moods = logs.mapNotNull { it.mood }
        if (moods.size < 2) return 0.0
        
        val uniqueMoods = moods.distinct().size
        return uniqueMoods.toDouble() / moods.size
    }

    private fun calculateBBTAnalysis(logs: List<DailyLog>): BBTAnalysis? {
        val bbtReadings = logs.mapNotNull { it.bbt }
        if (bbtReadings.size < HealthReportValidation.MIN_BBT_READINGS_FOR_ANALYSIS) {
            return null
        }

        val min = bbtReadings.minOrNull() ?: 0.0
        val max = bbtReadings.maxOrNull() ?: 0.0
        val average = bbtReadings.average()

        return BBTAnalysis(
            averageTemperature = average,
            temperatureRange = TemperatureRange(
                min = min,
                max = max,
                follicularAverage = average - 0.2, // Simplified calculation
                lutealAverage = average + 0.2
            ),
            ovulationDetectionRate = 0.75, // Placeholder
            temperaturePatterns = listOf() // Simplified for now
        )
    }
}

