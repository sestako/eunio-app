package com.eunio.healthapp.data.remote

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Network security configuration for HTTPS/TLS enforcement
 */
object NetworkSecurityConfig {
    
    /**
     * Creates a secure HTTP client with TLS enforcement
     */
    fun createSecureHttpClient(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            // Enforce HTTPS for all requests
            install(DefaultRequest) {
                url {
                    protocol = io.ktor.http.URLProtocol.HTTPS
                }
            }
            
            // Content negotiation with JSON
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            
            // Security monitoring (logging disabled for production)
            
            // Timeout configuration for security
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
            
            // User agent for identification
            install(UserAgent) {
                agent = "EunioHealthApp/1.0"
            }
        }
    }
    
    /**
     * Validates that a URL uses HTTPS protocol
     */
    fun validateSecureUrl(url: String): Boolean {
        return url.startsWith("https://", ignoreCase = true)
    }
    
    /**
     * Checks if the app is running in debug mode
     */
    private fun isDebugMode(): Boolean {
        // This will be implemented platform-specifically
        return false
    }
}