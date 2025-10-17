package com.eunio.healthapp.services

import com.eunio.healthapp.models.UserProfile

interface UserProfileService {
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun getProfile(userId: String): Result<UserProfile?>
    suspend fun updateProfile(profile: UserProfile): Result<Unit>
    suspend fun deleteProfile(userId: String): Result<Unit>
}
