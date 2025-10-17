package com.eunio.healthapp.domain.security

import com.eunio.healthapp.domain.util.Result

/**
 * Interface for data encryption and decryption operations
 */
interface DataEncryption {
    
    /**
     * Encrypts sensitive data before local storage
     */
    suspend fun encrypt(data: String): Result<String>
    
    /**
     * Decrypts data retrieved from local storage
     */
    suspend fun decrypt(encryptedData: String): Result<String>
    
    /**
     * Generates a secure key for encryption
     */
    suspend fun generateKey(): Result<String>
    
    /**
     * Validates encryption key integrity
     */
    suspend fun validateKey(key: String): Boolean
    
    /**
     * Securely deletes encryption keys
     */
    suspend fun deleteKey(): Result<Unit>
}

/**
 * Identifies sensitive data fields that require encryption
 */
object SensitiveDataClassifier {
    
    private val sensitiveFields = setOf(
        "bbt", // Basal body temperature
        "cervicalMucus",
        "sexualActivity",
        "notes", // Personal notes may contain sensitive info
        "symptoms",
        "mood"
    )
    
    /**
     * Checks if a field contains sensitive data
     */
    fun isSensitive(fieldName: String): Boolean {
        return sensitiveFields.contains(fieldName.lowercase())
    }
    
    /**
     * Gets all sensitive field names
     */
    fun getSensitiveFields(): Set<String> {
        return sensitiveFields.toSet()
    }
}

/**
 * Encryption configuration and constants
 */
object EncryptionConfig {
    const val ALGORITHM = "AES/GCM/NoPadding"
    const val KEY_SIZE = 256
    const val IV_SIZE = 12
    const val TAG_SIZE = 16
    const val KEY_ALIAS = "eunio_health_encryption_key"
    
    /**
     * Validates encryption configuration
     */
    fun validateConfig(): Boolean {
        return KEY_SIZE >= 256 && IV_SIZE >= 12 && TAG_SIZE >= 16
    }
}