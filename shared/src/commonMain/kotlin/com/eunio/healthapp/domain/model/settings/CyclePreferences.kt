package com.eunio.healthapp.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class CyclePreferences(
    val averageCycleLength: Int = 28,
    val averageLutealPhaseLength: Int = 14,
    val periodDuration: Int = 5,
    val isCustomized: Boolean = false
) {
    /**
     * Validates that cycle preferences are within acceptable medical ranges
     * 
     * @return true if preferences are valid, false otherwise
     */
    fun isValid(): Boolean {
        return when {
            averageCycleLength < MIN_CYCLE_LENGTH || averageCycleLength > MAX_CYCLE_LENGTH -> false
            averageLutealPhaseLength < MIN_LUTEAL_PHASE || averageLutealPhaseLength > MAX_LUTEAL_PHASE -> false
            periodDuration < MIN_PERIOD_DURATION || periodDuration > MAX_PERIOD_DURATION -> false
            averageLutealPhaseLength >= averageCycleLength -> false // Luteal phase must be shorter than cycle
            else -> true
        }
    }
    
    /**
     * Returns the follicular phase length based on cycle and luteal phase lengths
     */
    val follicularPhaseLength: Int
        get() = averageCycleLength - averageLutealPhaseLength
    
    /**
     * Returns validation errors if any exist
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (averageCycleLength < MIN_CYCLE_LENGTH || averageCycleLength > MAX_CYCLE_LENGTH) {
            errors.add("Cycle length must be between $MIN_CYCLE_LENGTH and $MAX_CYCLE_LENGTH days")
        }
        
        if (averageLutealPhaseLength < MIN_LUTEAL_PHASE || averageLutealPhaseLength > MAX_LUTEAL_PHASE) {
            errors.add("Luteal phase length must be between $MIN_LUTEAL_PHASE and $MAX_LUTEAL_PHASE days")
        }
        
        if (periodDuration < MIN_PERIOD_DURATION || periodDuration > MAX_PERIOD_DURATION) {
            errors.add("Period duration must be between $MIN_PERIOD_DURATION and $MAX_PERIOD_DURATION days")
        }
        
        if (averageLutealPhaseLength >= averageCycleLength) {
            errors.add("Luteal phase length must be shorter than cycle length")
        }
        
        return errors
    }
    
    companion object {
        // Medical ranges for cycle parameters
        const val MIN_CYCLE_LENGTH = 21
        const val MAX_CYCLE_LENGTH = 45
        const val MIN_LUTEAL_PHASE = 10
        const val MAX_LUTEAL_PHASE = 16
        const val MIN_PERIOD_DURATION = 2
        const val MAX_PERIOD_DURATION = 8
        
        fun default(): CyclePreferences {
            return CyclePreferences()
        }
        
        /**
         * Creates cycle preferences with validation
         * 
         * @param cycleLength The average cycle length
         * @param lutealPhase The average luteal phase length
         * @param periodDuration The average period duration
         * @return Valid CyclePreferences or default if invalid
         */
        fun create(
            cycleLength: Int,
            lutealPhase: Int,
            periodDuration: Int
        ): CyclePreferences {
            val preferences = CyclePreferences(
                averageCycleLength = cycleLength,
                averageLutealPhaseLength = lutealPhase,
                periodDuration = periodDuration,
                isCustomized = true
            )
            
            return if (preferences.isValid()) preferences else default()
        }
    }
}