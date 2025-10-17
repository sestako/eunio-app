package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.model.*
import kotlinx.datetime.LocalDate

/**
 * Validation utilities for domain models
 */
object Validation {
    
    fun validateUser(user: User): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (user.id.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        if (user.email.isBlank()) {
            errors.add("Email cannot be blank")
        } else if (!isValidEmail(user.email)) {
            errors.add("Email format is invalid")
        }
        
        if (user.name.isBlank()) {
            errors.add("Name cannot be blank")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    fun validateCycle(cycle: Cycle): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (cycle.id.isBlank()) {
            errors.add("Cycle ID cannot be blank")
        }
        
        if (cycle.userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        cycle.endDate?.let { endDate ->
            if (endDate < cycle.startDate) {
                errors.add("End date cannot be before start date")
            }
        }
        
        cycle.cycleLength?.let { length ->
            if (length < 21 || length > 35) {
                errors.add("Cycle length should be between 21 and 35 days")
            }
        }
        
        cycle.lutealPhaseLength?.let { length ->
            if (length < 10 || length > 16) {
                errors.add("Luteal phase length should be between 10 and 16 days")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    fun validateDailyLog(dailyLog: DailyLog): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (dailyLog.id.isBlank()) {
            errors.add("Daily log ID cannot be blank")
        }
        
        if (dailyLog.userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        // BBT validation
        dailyLog.bbt?.let { temp ->
            if (temp < 35.0 || temp > 40.0) {
                errors.add("BBT should be between 35.0°C and 40.0°C")
            }
        }
        
        // Notes validation
        dailyLog.notes?.let { notes ->
            if (notes.length > 1000) {
                errors.add("Notes cannot exceed 1000 characters")
            }
        }
        
        // Symptoms validation
        if (dailyLog.symptoms.size > 10) {
            errors.add("Cannot log more than 10 symptoms per day")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    fun validateInsight(insight: Insight): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (insight.id.isBlank()) {
            errors.add("Insight ID cannot be blank")
        }
        
        if (insight.userId.isBlank()) {
            errors.add("User ID cannot be blank")
        }
        
        if (insight.insightText.isBlank()) {
            errors.add("Insight text cannot be blank")
        }
        
        if (insight.confidence < 0.0 || insight.confidence > 1.0) {
            errors.add("Confidence should be between 0.0 and 1.0")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    fun isValidEmail(email: String): Boolean {
        // Check for malicious patterns
        val maliciousPatterns = listOf(
            "';", "')", "';--", "DROP", "DELETE", "INSERT", "UPDATE", "SELECT",
            "<script", "javascript:", "onload=", "onerror=", "onclick="
        )
        
        val upperEmail = email.uppercase()
        if (maliciousPatterns.any { upperEmail.contains(it.uppercase()) }) {
            return false
        }
        
        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            return false
        }
        
        // More thorough email validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }
    
    fun validatePassword(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()
        
        if (password.length < 6) {
            errors.add("Password must be at least 6 characters long")
        }
        
        if (password.length > 128) {
            errors.add("Password must be less than 128 characters")
        }
        
        if (password.isBlank()) {
            errors.add("Password cannot be empty")
        }
        
        return if (errors.isEmpty()) {
            PasswordValidationResult(true, "Password is valid")
        } else {
            PasswordValidationResult(false, errors.first())
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
}

data class PasswordValidationResult(
    val isValid: Boolean,
    val message: String
)