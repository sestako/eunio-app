package com.eunio.healthapp.data.sync

import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.DailyLog
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Instant

/**
 * Service for resolving data conflicts between local and remote versions.
 * Implements various conflict resolution strategies including last-write-wins,
 * field-level merging, and user-guided resolution.
 */
class ConflictResolutionService(
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Resolves conflicts between local and remote user data
     */
    suspend fun resolveUserConflict(
        localUser: User,
        remoteUser: User,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): Result<ConflictResolution<User>> {
        return try {
            when (strategy) {
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    val resolvedUser = if (localUser.updatedAt > remoteUser.updatedAt) {
                        localUser
                    } else {
                        remoteUser
                    }
                    
                    Result.success(ConflictResolution(
                        resolvedData = resolvedUser,
                        strategy = strategy,
                        conflictType = ConflictType.TIMESTAMP_CONFLICT,
                        wasAutoResolved = true
                    ))
                }
                
                ConflictResolutionStrategy.FIELD_LEVEL_MERGE -> {
                    val mergedUser = mergeUserFields(localUser, remoteUser)
                    Result.success(ConflictResolution(
                        resolvedData = mergedUser,
                        strategy = strategy,
                        conflictType = ConflictType.FIELD_CONFLICT,
                        wasAutoResolved = true
                    ))
                }
                
                ConflictResolutionStrategy.USER_GUIDED -> {
                    // For user-guided resolution, return the conflict for UI handling
                    Result.success(ConflictResolution(
                        resolvedData = localUser, // Temporary - UI will handle
                        strategy = strategy,
                        conflictType = ConflictType.USER_INTERVENTION_REQUIRED,
                        wasAutoResolved = false,
                        localVersion = localUser,
                        remoteVersion = remoteUser
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Resolves conflicts between local and remote daily log data
     */
    suspend fun resolveDailyLogConflict(
        localLog: DailyLog,
        remoteLog: DailyLog,
        strategy: ConflictResolutionStrategy = ConflictResolutionStrategy.LAST_WRITE_WINS
    ): Result<ConflictResolution<DailyLog>> {
        return try {
            // Validate that logs are for the same date and user
            if (localLog.userId != remoteLog.userId || localLog.date != remoteLog.date) {
                return Result.error(AppError.ValidationError("Cannot resolve conflict between logs for different users or dates"))
            }
            
            when (strategy) {
                ConflictResolutionStrategy.LAST_WRITE_WINS -> {
                    val resolvedLog = if (localLog.updatedAt > remoteLog.updatedAt) {
                        localLog
                    } else {
                        remoteLog
                    }
                    
                    Result.success(ConflictResolution(
                        resolvedData = resolvedLog,
                        strategy = strategy,
                        conflictType = ConflictType.TIMESTAMP_CONFLICT,
                        wasAutoResolved = true
                    ))
                }
                
                ConflictResolutionStrategy.FIELD_LEVEL_MERGE -> {
                    val mergedLog = mergeDailyLogFields(localLog, remoteLog)
                    Result.success(ConflictResolution(
                        resolvedData = mergedLog,
                        strategy = strategy,
                        conflictType = ConflictType.FIELD_CONFLICT,
                        wasAutoResolved = true
                    ))
                }
                
                ConflictResolutionStrategy.USER_GUIDED -> {
                    Result.success(ConflictResolution(
                        resolvedData = localLog, // Temporary - UI will handle
                        strategy = strategy,
                        conflictType = ConflictType.USER_INTERVENTION_REQUIRED,
                        wasAutoResolved = false,
                        localVersion = localLog,
                        remoteVersion = remoteLog
                    ))
                }
            }
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
    
    /**
     * Merges user fields intelligently, preferring non-null and more recent values
     */
    private fun mergeUserFields(localUser: User, remoteUser: User): User {
        return User(
            id = localUser.id, // ID should be the same
            email = if (localUser.updatedAt > remoteUser.updatedAt) localUser.email else remoteUser.email,
            name = if (localUser.updatedAt > remoteUser.updatedAt) localUser.name else remoteUser.name,
            onboardingComplete = localUser.onboardingComplete || remoteUser.onboardingComplete, // Prefer true
            primaryGoal = if (localUser.updatedAt > remoteUser.updatedAt) localUser.primaryGoal else remoteUser.primaryGoal,
            createdAt = minOf(localUser.createdAt, remoteUser.createdAt), // Prefer earlier creation
            updatedAt = maxOf(localUser.updatedAt, remoteUser.updatedAt) // Use latest update time
        )
    }
    
    /**
     * Merges daily log fields intelligently, preferring non-null and more recent values
     */
    private fun mergeDailyLogFields(localLog: DailyLog, remoteLog: DailyLog): DailyLog {
        return DailyLog(
            id = localLog.id, // ID should be the same
            userId = localLog.userId, // User ID should be the same
            date = localLog.date, // Date should be the same
            periodFlow = mergeNullableField(localLog.periodFlow, remoteLog.periodFlow, localLog.updatedAt, remoteLog.updatedAt),
            symptoms = mergeSymptoms(localLog.symptoms, remoteLog.symptoms),
            mood = mergeNullableField(localLog.mood, remoteLog.mood, localLog.updatedAt, remoteLog.updatedAt),
            sexualActivity = mergeNullableField(localLog.sexualActivity, remoteLog.sexualActivity, localLog.updatedAt, remoteLog.updatedAt),
            bbt = mergeNullableField(localLog.bbt, remoteLog.bbt, localLog.updatedAt, remoteLog.updatedAt),
            cervicalMucus = mergeNullableField(localLog.cervicalMucus, remoteLog.cervicalMucus, localLog.updatedAt, remoteLog.updatedAt),
            opkResult = mergeNullableField(localLog.opkResult, remoteLog.opkResult, localLog.updatedAt, remoteLog.updatedAt),
            notes = mergeNotes(localLog.notes, remoteLog.notes, localLog.updatedAt, remoteLog.updatedAt),
            createdAt = minOf(localLog.createdAt, remoteLog.createdAt), // Prefer earlier creation
            updatedAt = maxOf(localLog.updatedAt, remoteLog.updatedAt) // Use latest update time
        )
    }
    
    /**
     * Merges nullable fields by preferring non-null values or more recent timestamps
     */
    private fun <T> mergeNullableField(
        localValue: T?,
        remoteValue: T?,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): T? {
        return when {
            localValue != null && remoteValue != null -> {
                // Both have values, prefer more recent
                if (localTimestamp > remoteTimestamp) localValue else remoteValue
            }
            localValue != null -> localValue
            remoteValue != null -> remoteValue
            else -> null
        }
    }
    
    /**
     * Merges symptom lists by combining unique symptoms
     */
    private fun mergeSymptoms(
        localSymptoms: List<com.eunio.healthapp.domain.model.Symptom>,
        remoteSymptoms: List<com.eunio.healthapp.domain.model.Symptom>
    ): List<com.eunio.healthapp.domain.model.Symptom> {
        return (localSymptoms + remoteSymptoms).distinct()
    }
    
    /**
     * Merges notes by combining them if both exist
     */
    private fun mergeNotes(
        localNotes: String?,
        remoteNotes: String?,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): String? {
        return when {
            localNotes.isNullOrBlank() && remoteNotes.isNullOrBlank() -> null
            localNotes.isNullOrBlank() -> remoteNotes
            remoteNotes.isNullOrBlank() -> localNotes
            localNotes == remoteNotes -> localNotes
            else -> {
                // Combine notes with timestamp indication
                if (localTimestamp > remoteTimestamp) {
                    "$localNotes\n\n[Previous note: $remoteNotes]"
                } else {
                    "$remoteNotes\n\n[Previous note: $localNotes]"
                }
            }
        }
    }
    
    /**
     * Detects conflicts between two data versions
     */
    fun <T> detectConflict(
        localData: T,
        remoteData: T,
        localTimestamp: Instant,
        remoteTimestamp: Instant
    ): ConflictDetectionResult {
        return when {
            localData == remoteData -> ConflictDetectionResult.NO_CONFLICT
            localTimestamp == remoteTimestamp -> ConflictDetectionResult.SIMULTANEOUS_EDIT
            kotlin.math.abs((localTimestamp - remoteTimestamp).inWholeMinutes) < 10 -> ConflictDetectionResult.NEAR_SIMULTANEOUS_EDIT
            else -> ConflictDetectionResult.TIMESTAMP_CONFLICT
        }
    }
}

/**
 * Strategies for resolving data conflicts
 */
enum class ConflictResolutionStrategy {
    LAST_WRITE_WINS,
    FIELD_LEVEL_MERGE,
    USER_GUIDED
}

/**
 * Types of conflicts that can occur
 */
enum class ConflictType {
    TIMESTAMP_CONFLICT,
    FIELD_CONFLICT,
    USER_INTERVENTION_REQUIRED,
    NO_CONFLICT
}

/**
 * Result of conflict detection
 */
enum class ConflictDetectionResult {
    NO_CONFLICT,
    TIMESTAMP_CONFLICT,
    SIMULTANEOUS_EDIT,
    NEAR_SIMULTANEOUS_EDIT
}

/**
 * Result of conflict resolution
 */
data class ConflictResolution<T>(
    val resolvedData: T,
    val strategy: ConflictResolutionStrategy,
    val conflictType: ConflictType,
    val wasAutoResolved: Boolean,
    val localVersion: T? = null,
    val remoteVersion: T? = null,
    val resolutionTimestamp: Instant = kotlinx.datetime.Clock.System.now()
)