package com.eunio.healthapp.domain.usecase.fertility

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.CervicalMucus
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.util.ValidationResult
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Use case for logging cervical mucus observations with categorization and fertility tracking.
 * Handles mucus type validation, fertility window calculation, and pattern analysis.
 */
class LogCervicalMucusUseCase(
    private val logRepository: LogRepository
) {
    
    /**
     * Logs cervical mucus observation for a specific date.
     * 
     * @param userId The ID of the user
     * @param date The date of the observation
     * @param mucusType The type of cervical mucus observed
     * @return Result indicating success or failure of the logging operation
     */
    suspend operator fun invoke(
        userId: String,
        date: LocalDate,
        mucusType: CervicalMucus
    ): Result<Unit> {
        // Validate the date
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (date > today) {
            return Result.error(
                AppError.ValidationError("Cannot log cervical mucus for future dates")
            )
        }
        
        // Get existing daily log or create new one
        val existingLogResult = logRepository.getDailyLog(userId, date)
        if (existingLogResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve existing log: ${existingLogResult.error.message}")
            )
        }
        
        val existingLog = existingLogResult.getOrNull()
        val updatedLog = if (existingLog != null) {
            // Update existing log with cervical mucus
            existingLog.copy(
                cervicalMucus = mucusType,
                updatedAt = Clock.System.now()
            )
        } else {
            // Create new log with cervical mucus
            DailyLog(
                id = generateLogId(userId, date),
                userId = userId,
                date = date,
                cervicalMucus = mucusType,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
        }
        
        // Save the updated log
        return when (val result = logRepository.saveDailyLog(updatedLog)) {
            is Result.Success -> Result.success(Unit)
            is Result.Error -> Result.error(
                AppError.DataSyncError("Failed to save cervical mucus log: ${result.error.message}")
            )
        }
    }
    
    /**
     * Categorizes cervical mucus types by fertility potential.
     * 
     * @param mucusType The observed cervical mucus type
     * @return FertilityIndicator representing the fertility potential
     */
    fun categorizeFertilityPotential(mucusType: CervicalMucus): FertilityIndicator {
        return when (mucusType) {
            CervicalMucus.DRY -> FertilityIndicator.LOW
            CervicalMucus.STICKY -> FertilityIndicator.LOW
            CervicalMucus.CREAMY -> FertilityIndicator.MODERATE
            CervicalMucus.WATERY -> FertilityIndicator.HIGH
            CervicalMucus.EGG_WHITE -> FertilityIndicator.PEAK
        }
    }
    
    /**
     * Provides educational information about cervical mucus types.
     * 
     * @param mucusType The cervical mucus type
     * @return Educational description of the mucus type and its significance
     */
    fun getMucusDescription(mucusType: CervicalMucus): String {
        return when (mucusType) {
            CervicalMucus.DRY -> "No noticeable cervical mucus. Typically indicates low fertility period."
            CervicalMucus.STICKY -> "Thick, tacky mucus that breaks easily. Usually indicates low fertility."
            CervicalMucus.CREAMY -> "Smooth, lotion-like mucus. May indicate approaching fertility window."
            CervicalMucus.WATERY -> "Thin, clear mucus similar to water. Indicates increasing fertility."
            CervicalMucus.EGG_WHITE -> "Clear, stretchy mucus resembling raw egg white. Peak fertility indicator - ovulation likely within 24-48 hours."
        }
    }
    
    /**
     * Analyzes cervical mucus pattern over a date range to identify fertility trends.
     * 
     * @param userId The ID of the user
     * @param startDate Start of the analysis period
     * @param endDate End of the analysis period
     * @return Result containing fertility pattern analysis
     */
    suspend fun analyzeMucusPattern(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<MucusPatternAnalysis> {
        val logsResult = logRepository.getFertilityLogsInRange(userId, startDate, endDate)
        if (logsResult is Result.Error) {
            return Result.error(
                AppError.DataSyncError("Failed to retrieve fertility logs: ${logsResult.error.message}")
            )
        }
        
        val logs = logsResult.getOrNull()?.filter { it.cervicalMucus != null } ?: emptyList()
        
        if (logs.isEmpty()) {
            return Result.success(
                MucusPatternAnalysis(
                    totalObservations = 0,
                    peakFertilityDays = emptyList(),
                    fertilityTrend = FertilityTrend.INSUFFICIENT_DATA,
                    recommendations = listOf("Continue tracking cervical mucus daily for better pattern recognition")
                )
            )
        }
        
        val peakDays = logs.filter { log ->
            log.cervicalMucus == CervicalMucus.EGG_WHITE || log.cervicalMucus == CervicalMucus.WATERY 
        }.map { log -> log.date }
        
        val fertilityTrend = determineFertilityTrend(logs)
        val recommendations = generateRecommendations(logs, fertilityTrend)
        
        return Result.success(
            MucusPatternAnalysis(
                totalObservations = logs.size,
                peakFertilityDays = peakDays,
                fertilityTrend = fertilityTrend,
                recommendations = recommendations
            )
        )
    }
    
    /**
     * Determines fertility trend based on mucus observations.
     */
    private fun determineFertilityTrend(logs: List<DailyLog>): FertilityTrend {
        val mucusTypes = logs.mapNotNull { it.cervicalMucus }
        
        val peakCount = mucusTypes.count { it == CervicalMucus.EGG_WHITE }
        val highFertilityCount = mucusTypes.count { it == CervicalMucus.WATERY || it == CervicalMucus.EGG_WHITE }
        
        return when {
            logs.size < 7 -> FertilityTrend.INSUFFICIENT_DATA
            peakCount >= 2 -> FertilityTrend.REGULAR_OVULATION
            highFertilityCount >= 3 -> FertilityTrend.IRREGULAR_OVULATION
            else -> FertilityTrend.ANOVULATORY_PATTERN
        }
    }
    
    /**
     * Generates personalized recommendations based on mucus patterns.
     */
    private fun generateRecommendations(logs: List<DailyLog>, trend: FertilityTrend): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (trend) {
            FertilityTrend.REGULAR_OVULATION -> {
                recommendations.add("Your cervical mucus shows regular ovulation patterns")
                recommendations.add("Continue daily tracking for optimal fertility awareness")
            }
            FertilityTrend.IRREGULAR_OVULATION -> {
                recommendations.add("Your ovulation patterns appear irregular")
                recommendations.add("Consider tracking BBT alongside cervical mucus for better accuracy")
                recommendations.add("Consult healthcare provider if trying to conceive")
            }
            FertilityTrend.ANOVULATORY_PATTERN -> {
                recommendations.add("Limited signs of ovulation detected in recent observations")
                recommendations.add("Consider additional fertility tracking methods")
                recommendations.add("Consult healthcare provider for fertility evaluation")
            }
            FertilityTrend.INSUFFICIENT_DATA -> {
                recommendations.add("Continue daily tracking for at least 2-3 cycles")
                recommendations.add("Track at the same time each day for consistency")
            }
        }
        
        return recommendations
    }
    
    /**
     * Generates a unique log ID for the user and date.
     */
    private fun generateLogId(userId: String, date: LocalDate): String {
        return "${userId}_${date}"
    }
}

/**
 * Represents fertility potential based on cervical mucus type.
 */
enum class FertilityIndicator {
    LOW, MODERATE, HIGH, PEAK
}

/**
 * Represents fertility trends based on cervical mucus patterns.
 */
enum class FertilityTrend {
    REGULAR_OVULATION,
    IRREGULAR_OVULATION,
    ANOVULATORY_PATTERN,
    INSUFFICIENT_DATA
}

/**
 * Analysis result for cervical mucus patterns.
 */
data class MucusPatternAnalysis(
    val totalObservations: Int,
    val peakFertilityDays: List<LocalDate>,
    val fertilityTrend: FertilityTrend,
    val recommendations: List<String>
)