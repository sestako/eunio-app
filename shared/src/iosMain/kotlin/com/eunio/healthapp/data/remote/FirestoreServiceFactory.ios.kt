package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.util.ErrorHandler

/**
 * iOS implementation of FirestoreServiceFactory.
 * Creates FirestoreService using Firebase iOS SDK (placeholder implementation).
 */
actual class FirestoreServiceFactory {
    
    /**
     * Creates a FirestoreService instance for iOS.
     * Currently returns a placeholder implementation.
     */
    actual fun create(errorHandler: ErrorHandler): FirestoreService {
        return FirestoreServiceImpl(errorHandler)
    }
}