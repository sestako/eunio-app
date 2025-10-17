package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.util.ErrorHandler

/**
 * Factory for creating platform-specific FirestoreService implementations.
 * Provides the appropriate implementation based on the current platform.
 */
expect class FirestoreServiceFactory {
    
    /**
     * Creates a FirestoreService instance for the current platform.
     * 
     * @param errorHandler Error handler for converting exceptions to AppError instances
     * @return Platform-specific FirestoreService implementation
     */
    fun create(errorHandler: ErrorHandler): FirestoreService
}