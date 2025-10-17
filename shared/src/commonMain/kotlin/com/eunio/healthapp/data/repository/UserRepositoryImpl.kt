package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.repository.UserRepository
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import kotlinx.datetime.Clock

/**
 * Implementation of UserRepository that coordinates between local and remote data sources.
 * Provides offline-first architecture with automatic sync capabilities.
 */
class UserRepositoryImpl(
    private val authService: AuthService,
    private val firestoreService: FirestoreService,
    private val userDao: UserDao,
    private val errorHandler: ErrorHandler
) : UserRepository {

    override suspend fun getCurrentUser(): Result<User?> {
        return try {
            // First try to get from auth service
            val authResult = authService.getCurrentUser()
            if (authResult.isError) {
                return authResult
            }
            
            val authUser = authResult.getOrNull()
            if (authUser == null) {
                return Result.success(null)
            }
            
            // Try to get full user data from local cache first
            val localUser = userDao.getUserById(authUser.id)
            if (localUser != null) {
                // Attempt background sync with remote
                syncUserFromRemote(authUser.id)
                return Result.success(localUser)
            }
            
            // If not in local cache, fetch from remote
            val remoteResult = firestoreService.getUser(authUser.id)
            if (remoteResult.isSuccess) {
                val remoteUser = remoteResult.getOrNull()
                if (remoteUser != null) {
                    // Cache locally
                    userDao.insertOrUpdate(remoteUser)
                    userDao.markAsSynced(remoteUser.id)
                    return Result.success(remoteUser)
                }
            }
            
            // Return auth user as fallback
            Result.success(authUser)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val updatedUser = user.copy(updatedAt = Clock.System.now())
            
            // Save locally first (offline-first)
            userDao.insertOrUpdate(updatedUser)
            
            // Attempt to sync to remote
            val remoteResult = firestoreService.updateUser(updatedUser)
            if (remoteResult.isSuccess) {
                userDao.markAsSynced(updatedUser.id)
            }
            // Don't fail if remote sync fails - will be retried later
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun completeOnboarding(userId: String, primaryGoal: HealthGoal): Result<Unit> {
        return try {
            // Get current user
            val currentUserResult = getCurrentUser()
            if (currentUserResult.isError) {
                return Result.error(currentUserResult.errorOrNull()!!)
            }
            
            val currentUser = currentUserResult.getOrNull()
                ?: return Result.error(AppError.AuthenticationError("No authenticated user found"))
            
            if (currentUser.id != userId) {
                return Result.error(AppError.AuthenticationError("User ID mismatch"))
            }
            
            // Update user with onboarding completion
            val updatedUser = currentUser.copy(
                onboardingComplete = true,
                primaryGoal = primaryGoal,
                updatedAt = Clock.System.now()
            )
            
            updateUser(updatedUser)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun createUser(email: String, password: String, name: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank()) {
                return Result.error(errorHandler.createValidationError("Email cannot be empty", "email"))
            }
            if (password.length < 6) {
                return Result.error(errorHandler.createValidationError("Password must be at least 6 characters", "password"))
            }
            if (name.isBlank()) {
                return Result.error(errorHandler.createValidationError("Name cannot be empty", "name"))
            }
            
            // Create user through auth service
            val authResult = authService.signUp(email, password, name)
            if (authResult.isError) {
                return authResult
            }
            
            val user = authResult.getOrThrow()
            
            // Save to local cache
            userDao.insertOrUpdate(user)
            
            // Save to remote
            val remoteResult = firestoreService.saveUser(user)
            if (remoteResult.isSuccess) {
                userDao.markAsSynced(user.id)
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun signInUser(email: String, password: String): Result<User> {
        return try {
            // Validate input
            if (email.isBlank()) {
                return Result.error(errorHandler.createValidationError("Email cannot be empty", "email"))
            }
            if (password.isBlank()) {
                return Result.error(errorHandler.createValidationError("Password cannot be empty", "password"))
            }
            
            // Sign in through auth service
            val authResult = authService.signIn(email, password)
            if (authResult.isError) {
                return authResult
            }
            
            val user = authResult.getOrThrow()
            
            // Sync user data from remote
            syncUserFromRemote(user.id)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun signOutUser(): Result<Unit> {
        return try {
            authService.signOut()
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // Delete from remote first
            val remoteResult = firestoreService.deleteUser(userId)
            if (remoteResult.isError) {
                return remoteResult
            }
            
            // Delete from local cache
            userDao.deleteUser(userId)
            
            // Sign out the user
            authService.signOut()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }

    /**
     * Syncs user data from remote to local cache
     */
    private suspend fun syncUserFromRemote(userId: String) {
        try {
            val remoteResult = firestoreService.getUser(userId)
            if (remoteResult.isSuccess) {
                val remoteUser = remoteResult.getOrNull()
                if (remoteUser != null) {
                    userDao.insertOrUpdate(remoteUser)
                    userDao.markAsSynced(remoteUser.id)
                }
            }
        } catch (e: Exception) {
            // Ignore sync errors - will be retried later
        }
    }

    /**
     * Syncs pending local changes to remote
     */
    suspend fun syncPendingChanges(): Result<Unit> {
        return try {
            val pendingUsers = userDao.getPendingSyncUsers()
            
            for (user in pendingUsers) {
                val remoteResult = firestoreService.updateUser(user)
                if (remoteResult.isSuccess) {
                    userDao.markAsSynced(user.id)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(errorHandler.handleError(e))
        }
    }
}