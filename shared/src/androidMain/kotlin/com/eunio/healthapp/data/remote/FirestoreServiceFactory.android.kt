package com.eunio.healthapp.data.remote

import com.eunio.healthapp.domain.util.ErrorHandler
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Android implementation of FirestoreServiceFactory.
 * Creates FirestoreService using Firebase Android SDK.
 */
actual class FirestoreServiceFactory {
    
    /**
     * Creates a FirestoreService instance using Firebase Android SDK.
     */
    actual fun create(errorHandler: ErrorHandler): FirestoreService {
        val firestore = FirebaseFirestore.getInstance()
        return FirestoreServiceImpl(firestore, errorHandler)
    }
}