package com.eunio.healthapp.android

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Simple Firebase connectivity test
 */
object FirebaseTest {
    
    private const val TAG = "FirebaseTest"
    
    fun testFirebaseServices() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "Starting Firebase Services Test")
        Log.d(TAG, "========================================")
        
        testAuth()
        testFirestore()
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "Firebase Services Test Complete")
        Log.d(TAG, "========================================")
    }
    
    private fun testAuth() {
        try {
            val auth = FirebaseAuth.getInstance()
            Log.d(TAG, "✅ FirebaseAuth initialized: $auth")
            Log.d(TAG, "   Current user: ${auth.currentUser?.uid ?: "Not signed in"}")
            
            // Test anonymous sign in
            auth.signInAnonymously()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "✅ Anonymous sign-in successful!")
                    Log.d(TAG, "   User ID: ${result.user?.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Anonymous sign-in failed: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAuth test failed: ${e.message}", e)
        }
    }
    
    private fun testFirestore() {
        try {
            val db = FirebaseFirestore.getInstance()
            Log.d(TAG, "✅ Firestore initialized: $db")
            
            // Set up real-time listener for iOS data
            Log.d(TAG, "👂 Setting up real-time listener for iOS data...")
            db.collection("test_connections")
                .document("ios_test")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "⚠️ Error listening to iOS data: ${error.message}", error)
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Log.d(TAG, "🔔 Real-time update from iOS!")
                        Log.d(TAG, "   Platform: ${snapshot.getString("platform")}")
                        Log.d(TAG, "   Message: ${snapshot.getString("testMessage")}")
                        Log.d(TAG, "   User ID: ${snapshot.getString("userId")}")
                        Log.d(TAG, "   Update Count: ${snapshot.getLong("updateCount") ?: 0}")
                    } else {
                        Log.d(TAG, "⚠️ iOS document doesn't exist yet (waiting for iOS to write...)")
                    }
                }
            
            // First, try to read iOS data (one-time read)
            Log.d(TAG, "🔍 Attempting to read iOS test data...")
            db.collection("test_connections")
                .document("ios_test")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d(TAG, "✅ Successfully read iOS data!")
                        Log.d(TAG, "   Platform: ${document.getString("platform")}")
                        Log.d(TAG, "   Message: ${document.getString("testMessage")}")
                        Log.d(TAG, "   User ID: ${document.getString("userId")}")
                    } else {
                        Log.d(TAG, "⚠️ iOS test document not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to read iOS data: ${e.message}", e)
                }
            
            // Then write Android test data
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
            val testData = hashMapOf(
                "userId" to userId,
                "platform" to "Android",
                "testMessage" to "Firebase Android integration test",
                "timestamp" to com.google.firebase.Timestamp.now(),
                "updateCount" to 1
            )
            
            db.collection("test_connections")
                .document("android_test")
                .set(testData)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Firestore write successful!")
                    Log.d(TAG, "   Written to: test_connections/android_test")
                    
                    // Read it back to verify
                    db.collection("test_connections")
                        .document("android_test")
                        .get()
                        .addOnSuccessListener { doc ->
                            Log.d(TAG, "✅ Firestore read successful!")
                            Log.d(TAG, "   Data: ${doc.data}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Firestore read failed: ${e.message}", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Firestore write failed: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore test failed: ${e.message}", e)
        }
    }
}
