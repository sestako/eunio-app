package com.eunio.healthapp.services

import android.util.Log
import com.eunio.healthapp.models.UserProfile
import com.eunio.healthapp.network.RetryPolicy
import com.eunio.healthapp.network.withRetry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AndroidUserProfileService : UserProfileService {
    
    companion object {
        private const val TAG = "UserProfileService"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    init {
        Log.i(TAG, "AndroidUserProfileService initialized")
    }
    
    override suspend fun createProfile(profile: UserProfile): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.AGGRESSIVE, // Critical operation - use aggressive retry
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying createProfile (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                val profileMap = mapOf(
                    "userId" to profile.userId,
                    "email" to profile.email,
                    "displayName" to profile.displayName,
                    "createdAt" to profile.createdAt,
                    "updatedAt" to profile.updatedAt
                )
                usersCollection.document(profile.userId)
                    .set(profileMap)
                    .await()
            }
            Log.d(TAG, "Successfully created profile for user ${profile.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create profile after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getProfile(userId: String): Result<UserProfile?> {
        return try {
            val profile = withRetry(
                policy = RetryPolicy.DEFAULT, // Read operation - use default retry
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying getProfile (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                val snapshot = usersCollection.document(userId).get().await()
                if (!snapshot.exists()) {
                    return@withRetry null
                }
                val data = snapshot.data ?: return@withRetry null
                UserProfile(
                    userId = data["userId"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    displayName = data["displayName"] as? String ?: "",
                    createdAt = data["createdAt"] as? Long ?: 0L,
                    updatedAt = data["updatedAt"] as? Long ?: 0L
                )
            }
            Log.d(TAG, "Successfully retrieved profile for user $userId")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.AGGRESSIVE, // Critical write operation
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying updateProfile (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                val updatedProfile = profile.copy(updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
                val profileMap = mapOf(
                    "userId" to updatedProfile.userId,
                    "email" to updatedProfile.email,
                    "displayName" to updatedProfile.displayName,
                    "createdAt" to updatedProfile.createdAt,
                    "updatedAt" to updatedProfile.updatedAt
                )
                usersCollection.document(profile.userId)
                    .set(profileMap)
                    .await()
            }
            Log.d(TAG, "Successfully updated profile for user ${profile.userId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile after retries", e)
            Result.failure(e)
        }
    }
    
    override suspend fun deleteProfile(userId: String): Result<Unit> {
        return try {
            withRetry(
                policy = RetryPolicy.AGGRESSIVE, // Critical delete operation
                onRetry = { attempt, error, delay ->
                    Log.w(TAG, "Retrying deleteProfile (attempt ${attempt + 1}) in ${delay.inWholeSeconds}s: ${error.message}")
                }
            ) {
                usersCollection.document(userId).delete().await()
            }
            Log.d(TAG, "Successfully deleted profile for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile after retries", e)
            Result.failure(e)
        }
    }
}
