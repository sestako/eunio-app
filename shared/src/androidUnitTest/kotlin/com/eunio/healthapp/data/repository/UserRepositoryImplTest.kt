package com.eunio.healthapp.data.repository

import com.eunio.healthapp.data.local.dao.UserDao
import com.eunio.healthapp.data.remote.FirestoreService
import com.eunio.healthapp.data.remote.auth.AuthService
import com.eunio.healthapp.domain.error.AppError
import com.eunio.healthapp.domain.model.HealthGoal
import com.eunio.healthapp.domain.model.User
import com.eunio.healthapp.domain.util.ErrorHandler
import com.eunio.healthapp.domain.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserRepositoryImplTest {
    
    private val authService = mockk<AuthService>()
    private val firestoreService = mockk<FirestoreService>()
    private val userDao = mockk<UserDao>()
    private val errorHandler = mockk<ErrorHandler>()
    
    private val repository = UserRepositoryImpl(
        authService = authService,
        firestoreService = firestoreService,
        userDao = userDao,
        errorHandler = errorHandler
    )
    
    private val testUser = User(
        id = "test-user-id",
        email = "test@example.com",
        name = "Test User",
        onboardingComplete = true,
        primaryGoal = HealthGoal.CYCLE_TRACKING,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )
    
    @Test
    fun `getCurrentUser returns user from local cache when available`() = runTest {
        // Given
        coEvery { authService.getCurrentUser() } returns Result.success(testUser)
        coEvery { userDao.getUserById(testUser.id) } returns testUser
        coEvery { firestoreService.getUser(testUser.id) } returns Result.success(testUser)
        coEvery { userDao.insertOrUpdate(any()) } returns Unit
        coEvery { userDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.getCurrentUser()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        coVerify { userDao.getUserById(testUser.id) }
    }
    
    @Test
    fun `getCurrentUser returns null when no user is authenticated`() = runTest {
        // Given
        coEvery { authService.getCurrentUser() } returns Result.success(null)
        
        // When
        val result = repository.getCurrentUser()
        
        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
    
    @Test
    fun `getCurrentUser fetches from remote when not in local cache`() = runTest {
        // Given
        coEvery { authService.getCurrentUser() } returns Result.success(testUser)
        coEvery { userDao.getUserById(testUser.id) } returns null
        coEvery { firestoreService.getUser(testUser.id) } returns Result.success(testUser)
        coEvery { userDao.insertOrUpdate(testUser) } returns Unit
        coEvery { userDao.markAsSynced(testUser.id) } returns Unit
        
        // When
        val result = repository.getCurrentUser()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        coVerify { firestoreService.getUser(testUser.id) }
        coVerify { userDao.insertOrUpdate(testUser) }
    }
    
    @Test
    fun `updateUser saves locally and syncs to remote`() = runTest {
        // Given
        coEvery { userDao.insertOrUpdate(any()) } returns Unit
        coEvery { firestoreService.updateUser(any()) } returns Result.success(Unit)
        coEvery { userDao.markAsSynced(testUser.id) } returns Unit
        
        // When
        val result = repository.updateUser(testUser)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userDao.insertOrUpdate(any()) }
        coVerify { firestoreService.updateUser(any()) }
        coVerify { userDao.markAsSynced(testUser.id) }
    }
    
    @Test
    fun `updateUser succeeds even if remote sync fails`() = runTest {
        // Given
        coEvery { userDao.insertOrUpdate(any()) } returns Unit
        coEvery { firestoreService.updateUser(any()) } returns Result.error(AppError.NetworkError("Network error"))
        
        // When
        val result = repository.updateUser(testUser)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userDao.insertOrUpdate(any()) }
        coVerify(exactly = 0) { userDao.markAsSynced(any()) }
    }
    
    @Test
    fun `completeOnboarding updates user with onboarding completion`() = runTest {
        // Given
        val incompleteUser = testUser.copy(onboardingComplete = false)
        coEvery { authService.getCurrentUser() } returns Result.success(incompleteUser)
        coEvery { userDao.getUserById(incompleteUser.id) } returns incompleteUser
        coEvery { firestoreService.getUser(incompleteUser.id) } returns Result.success(incompleteUser)
        coEvery { userDao.insertOrUpdate(any()) } returns Unit
        coEvery { userDao.markAsSynced(any()) } returns Unit
        coEvery { firestoreService.updateUser(any()) } returns Result.success(Unit)
        
        // When
        val result = repository.completeOnboarding(testUser.id, HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { 
            userDao.insertOrUpdate(match { user ->
                user.onboardingComplete && user.primaryGoal == HealthGoal.CONCEPTION
            })
        }
    }
    
    @Test
    fun `completeOnboarding fails when user ID mismatch`() = runTest {
        // Given
        coEvery { authService.getCurrentUser() } returns Result.success(testUser)
        coEvery { userDao.getUserById(testUser.id) } returns testUser
        coEvery { firestoreService.getUser(testUser.id) } returns Result.success(testUser)
        coEvery { userDao.insertOrUpdate(any()) } returns Unit
        coEvery { userDao.markAsSynced(any()) } returns Unit
        
        // When
        val result = repository.completeOnboarding("different-user-id", HealthGoal.CONCEPTION)
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.AuthenticationError>(result.errorOrNull())
    }
    
    @Test
    fun `createUser validates input and creates user through auth service`() = runTest {
        // Given
        coEvery { authService.signUp("test@example.com", "password123", "Test User") } returns Result.success(testUser)
        coEvery { userDao.insertOrUpdate(testUser) } returns Unit
        coEvery { firestoreService.saveUser(testUser) } returns Result.success(Unit)
        coEvery { userDao.markAsSynced(testUser.id) } returns Unit
        
        // When
        val result = repository.createUser("test@example.com", "password123", "Test User")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        coVerify { authService.signUp("test@example.com", "password123", "Test User") }
        coVerify { userDao.insertOrUpdate(testUser) }
        coVerify { firestoreService.saveUser(testUser) }
    }
    
    @Test
    fun `createUser fails with validation error for empty email`() = runTest {
        // Given
        every { errorHandler.createValidationError("Email cannot be empty", "email") } returns 
            AppError.ValidationError("Email cannot be empty", "email")
        
        // When
        val result = repository.createUser("", "password123", "Test User")
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `createUser fails with validation error for short password`() = runTest {
        // Given
        every { errorHandler.createValidationError("Password must be at least 6 characters", "password") } returns 
            AppError.ValidationError("Password must be at least 6 characters", "password")
        
        // When
        val result = repository.createUser("test@example.com", "123", "Test User")
        
        // Then
        assertTrue(result.isError)
        assertIs<AppError.ValidationError>(result.errorOrNull())
    }
    
    @Test
    fun `signInUser validates input and signs in through auth service`() = runTest {
        // Given
        coEvery { authService.signIn("test@example.com", "password123") } returns Result.success(testUser)
        coEvery { firestoreService.getUser(testUser.id) } returns Result.success(testUser)
        coEvery { userDao.insertOrUpdate(testUser) } returns Unit
        coEvery { userDao.markAsSynced(testUser.id) } returns Unit
        
        // When
        val result = repository.signInUser("test@example.com", "password123")
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
        coVerify { authService.signIn("test@example.com", "password123") }
    }
    
    @Test
    fun `signOutUser calls auth service sign out`() = runTest {
        // Given
        coEvery { authService.signOut() } returns Result.success(Unit)
        
        // When
        val result = repository.signOutUser()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { authService.signOut() }
    }
    
    @Test
    fun `deleteUser removes from remote and local then signs out`() = runTest {
        // Given
        coEvery { firestoreService.deleteUser(testUser.id) } returns Result.success(Unit)
        coEvery { userDao.deleteUser(testUser.id) } returns Unit
        coEvery { authService.signOut() } returns Result.success(Unit)
        
        // When
        val result = repository.deleteUser(testUser.id)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { firestoreService.deleteUser(testUser.id) }
        coVerify { userDao.deleteUser(testUser.id) }
        coVerify { authService.signOut() }
    }
    
    @Test
    fun `syncPendingChanges syncs all pending users to remote`() = runTest {
        // Given
        val pendingUsers = listOf(testUser)
        coEvery { userDao.getPendingSyncUsers() } returns pendingUsers
        coEvery { firestoreService.updateUser(testUser) } returns Result.success(Unit)
        coEvery { userDao.markAsSynced(testUser.id) } returns Unit
        
        // When
        val result = repository.syncPendingChanges()
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { userDao.getPendingSyncUsers() }
        coVerify { firestoreService.updateUser(testUser) }
        coVerify { userDao.markAsSynced(testUser.id) }
    }
}