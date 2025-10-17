package com.eunio.healthapp.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.eunio.healthapp.domain.security.DataEncryption
import com.eunio.healthapp.domain.security.EncryptionConfig
import com.eunio.healthapp.domain.util.Result
import com.eunio.healthapp.domain.error.AppError
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Android implementation of data encryption using Android Keystore
 */
@OptIn(ExperimentalEncodingApi::class)
class AndroidDataEncryption(private val context: Context) : DataEncryption {
    
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    
    override suspend fun encrypt(data: String): Result<String> {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(EncryptionConfig.ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encryptedData
            val encoded = Base64.encode(combined)
            
            Result.Success(encoded)
        } catch (e: Exception) {
            Result.Error(AppError.SecurityError("Encryption failed: ${e.message}", e))
        }
    }
    
    override suspend fun decrypt(encryptedData: String): Result<String> {
        return try {
            val secretKey = getOrCreateSecretKey()
            val combined = Base64.decode(encryptedData)
            
            // Extract IV and encrypted data
            val iv = combined.sliceArray(0..EncryptionConfig.IV_SIZE - 1)
            val encrypted = combined.sliceArray(EncryptionConfig.IV_SIZE until combined.size)
            
            val cipher = Cipher.getInstance(EncryptionConfig.ALGORITHM)
            val spec = GCMParameterSpec(EncryptionConfig.TAG_SIZE * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(encrypted)
            val result = String(decryptedData, Charsets.UTF_8)
            
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(AppError.SecurityError("Decryption failed: ${e.message}", e))
        }
    }
    
    override suspend fun generateKey(): Result<String> {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeystore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                EncryptionConfig.KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(EncryptionConfig.KEY_SIZE)
                .setUserAuthenticationRequired(false) // For background operations
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            val secretKey = keyGenerator.generateKey()
            
            Result.Success(EncryptionConfig.KEY_ALIAS)
        } catch (e: Exception) {
            Result.Error(AppError.SecurityError("Key generation failed: ${e.message}", e))
        }
    }
    
    override suspend fun validateKey(key: String): Boolean {
        return try {
            keyStore.containsAlias(key) && keyStore.getKey(key, null) != null
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deleteKey(): Result<Unit> {
        return try {
            if (keyStore.containsAlias(EncryptionConfig.KEY_ALIAS)) {
                keyStore.deleteEntry(EncryptionConfig.KEY_ALIAS)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.SecurityError("Key deletion failed: ${e.message}", e))
        }
    }
    
    private suspend fun getOrCreateSecretKey(): SecretKey {
        return if (keyStore.containsAlias(EncryptionConfig.KEY_ALIAS)) {
            keyStore.getKey(EncryptionConfig.KEY_ALIAS, null) as SecretKey
        } else {
            generateKey()
            keyStore.getKey(EncryptionConfig.KEY_ALIAS, null) as SecretKey
        }
    }
}