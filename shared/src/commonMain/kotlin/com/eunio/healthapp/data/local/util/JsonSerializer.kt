package com.eunio.healthapp.data.local.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Utility object for JSON serialization and deserialization operations.
 * Provides a centralized configuration for JSON handling across the app.
 */
object JsonSerializer {
    
    /**
     * JSON configuration with lenient parsing and pretty printing disabled for storage efficiency
     */
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }
    
    /**
     * Serializes an object to JSON string
     * 
     * @param value The object to serialize
     * @return JSON string representation
     * @throws Exception if serialization fails
     */
    inline fun <reified T> toJson(value: T): String {
        return json.encodeToString(value)
    }
    
    /**
     * Deserializes a JSON string to an object
     * 
     * @param jsonString The JSON string to deserialize
     * @return The deserialized object
     * @throws Exception if deserialization fails
     */
    inline fun <reified T> fromJson(jsonString: String): T {
        return json.decodeFromString(jsonString)
    }
    
    /**
     * Safely deserializes a JSON string to an object, returning null on failure
     * 
     * @param jsonString The JSON string to deserialize
     * @return The deserialized object or null if deserialization fails
     */
    inline fun <reified T> fromJsonOrNull(jsonString: String?): T? {
        return try {
            if (jsonString.isNullOrBlank()) null
            else json.decodeFromString<T>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Safely serializes an object to JSON string, returning null on failure
     * 
     * @param value The object to serialize
     * @return JSON string representation or null if serialization fails
     */
    inline fun <reified T> toJsonOrNull(value: T?): String? {
        return try {
            if (value == null) null
            else json.encodeToString(value)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validates if a string is valid JSON
     * 
     * @param jsonString The string to validate
     * @return true if valid JSON, false otherwise
     */
    fun isValidJson(jsonString: String?): Boolean {
        return try {
            if (jsonString.isNullOrBlank()) false
            else {
                json.parseToJsonElement(jsonString)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculates the approximate size of a JSON string in bytes
     * 
     * @param jsonString The JSON string
     * @return Size in bytes
     */
    fun calculateJsonSize(jsonString: String?): Int {
        return jsonString?.encodeToByteArray()?.size ?: 0
    }
}