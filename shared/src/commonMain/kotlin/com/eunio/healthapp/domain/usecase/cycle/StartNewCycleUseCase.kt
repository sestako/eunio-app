package com.eunio.healthapp.domain.usecase.cycle

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.Cycle
import com.eunio.healthapp.domain.repository.CycleRepository
import com.eunio.healthapp.domain.repository.LogRepository
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Use case for starting a new menstrual cycle
 */
class StartNewCycleUseCase(
    private val cycleRepository: CycleRepository,
    private val logRepository: LogRepository
) {
    suspend operator fun invoke(
        userId: String,
        startDate: LocalDate
    ): Result<Cycle> {
        // Validate inputs
        if (userId.isBlank()) {
            return Result.Error(AppError.ValidationError("User ID cannot be empty"))
        }
        
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        // Don't allow future dates
        if (startDate > today) {
            return Result.Error(AppError.ValidationError("Start date cannot be in the future"))
        }
        
        // Don't allow dates more than 2 years in the past
        val twoYearsAgo = LocalDate(today.year - 2, today.month, today.dayOfMonth)
        if (startDate < twoYearsAgo) {
            return Result.Error(AppError.ValidationError("Start date cannot be more than 2 years in the past"))
        }
        
        // Start a new cycle using the repository
        return cycleRepository.startNewCycle(userId, startDate)
    }
}