package com.eunio.healthapp.data.remote.firebase

import com.eunio.healthapp.domain.error.AppError
import platform.Foundation.NSError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for FirebaseErrorMapper.
 * 
 * Verifies that Firebase iOS SDK errors are correctly mapped to AppError types.
 */
class FirebaseErrorMapperTest {
    
    @Test
    fun `mapError should map UNAVAILABLE to NetworkError`() {
        // Given
        val error = createNSError(code = 14, message = "Network unavailable")
        
        // When
        val result = FirebaseErrorMapper.mapError(error, "test operation")
        
        // Then
        assertIs<AppError.NetworkError>(result)
        assertTrue(result.message.contains("No internet connection"))
        assertTrue(result.message.contains("test operation"))
    }
    
    @Test
    fun `mapError should map UNAUTHENTICATED to AuthenticationError`() {
        // Given
        val error = createNSError(code = 16, message = "User not authenticated")
        
        // When
        val result = FirebaseErrorMapper.mapError(error, "save operation")
        
        // Then
        assertIs<AppError.AuthenticationError>(result)
        assertTrue(result.message.contains("Authentication required"))
        assertTrue(result.message.contains("save operation"))
    }
    
    @Test
    fun `mapError should map PERMISSION_DENIED to PermissionError`() {
        // Given
        val error = createNSError(code = 7, message = "Permission denied")
        
        // When
        val result = FirebaseErrorMapper.mapError(error, "write operation")
        
        // Then
        assertIs<AppError.PermissionError>(result)
        assertTrue(result.message.contains("Access denied"))
        assertTrue(result.message.contains("write operation"))
        assertEquals("firestore.write", result.requiredPermission)
    }
    
    @Test
    fun `mapError should map NOT_FOUND to DataSyncError`() {
        // Given
        val error = createNSError(code = 5, message = "Document not found")
        
        // When
        val result = FirebaseErrorMapper.mapError(error, "read operation")
        
        // Then
        assertIs<AppError.DataSyncError>(result)
        assertTrue(result.message.contains("Document not found"))
        assertTrue(result.message.contains("read operation"))
        assertEquals("read operation", result.operation)
    }
    
    @Test
    fun `mapError should map ALREADY_EXISTS to DataSyncError`() {
        // Given
        val error = createNSError(code = 6, message = "Document already exists")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.DataSyncError>(result)
        assertTrue(result.message.contains("already exists"))
    }
    
    @Test
    fun `mapError should map DEADLINE_EXCEEDED to NetworkError`() {
        // Given
        val error = createNSError(code = 4, message = "Deadline exceeded")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.NetworkError>(result)
        assertTrue(result.message.contains("timed out"))
    }
    
    @Test
    fun `mapError should map RESOURCE_EXHAUSTED to NetworkError`() {
        // Given
        val error = createNSError(code = 8, message = "Resource exhausted")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.NetworkError>(result)
        assertTrue(result.message.contains("temporarily unavailable"))
    }
    
    @Test
    fun `mapError should map INVALID_ARGUMENT to ValidationError`() {
        // Given
        val error = createNSError(code = 3, message = "Invalid argument")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.ValidationError>(result)
        assertTrue(result.message.contains("Invalid data"))
    }
    
    @Test
    fun `mapError should map FAILED_PRECONDITION to ValidationError`() {
        // Given
        val error = createNSError(code = 9, message = "Failed precondition")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.ValidationError>(result)
        assertTrue(result.message.contains("precondition"))
    }
    
    @Test
    fun `mapError should map ABORTED to DataSyncError`() {
        // Given
        val error = createNSError(code = 10, message = "Operation aborted")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.DataSyncError>(result)
        assertTrue(result.message.contains("aborted"))
    }
    
    @Test
    fun `mapError should map INTERNAL to DatabaseError`() {
        // Given
        val error = createNSError(code = 13, message = "Internal error")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.DatabaseError>(result)
        assertTrue(result.message.contains("Internal server error"))
    }
    
    @Test
    fun `mapError should map DATA_LOSS to DatabaseError`() {
        // Given
        val error = createNSError(code = 15, message = "Data loss")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.DatabaseError>(result)
        assertTrue(result.message.contains("Internal server error"))
    }
    
    @Test
    fun `mapError should map CANCELLED to DataSyncError`() {
        // Given
        val error = createNSError(code = 1, message = "Operation cancelled")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.DataSyncError>(result)
        assertTrue(result.message.contains("cancelled"))
    }
    
    @Test
    fun `mapError should map UNIMPLEMENTED to UnknownError`() {
        // Given
        val error = createNSError(code = 12, message = "Not implemented")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.UnknownError>(result)
        assertTrue(result.message.contains("not supported"))
    }
    
    @Test
    fun `mapError should map unknown error codes to UnknownError`() {
        // Given
        val error = createNSError(code = 999, message = "Unknown error")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.UnknownError>(result)
        assertTrue(result.message.contains("Firebase error"))
        assertTrue(result.message.contains("999"))
    }
    
    @Test
    fun `mapThrowable should map IllegalArgumentException to ValidationError`() {
        // Given
        val throwable = IllegalArgumentException("Invalid input")
        
        // When
        val result = FirebaseErrorMapper.mapThrowable(throwable, "validation")
        
        // Then
        assertIs<AppError.ValidationError>(result)
        assertTrue(result.message.contains("Invalid argument"))
        assertTrue(result.message.contains("validation"))
    }
    
    @Test
    fun `mapThrowable should map IllegalStateException to DataSyncError`() {
        // Given
        val throwable = IllegalStateException("Invalid state")
        
        // When
        val result = FirebaseErrorMapper.mapThrowable(throwable, "sync")
        
        // Then
        assertIs<AppError.DataSyncError>(result)
        assertTrue(result.message.contains("Invalid state"))
        assertTrue(result.message.contains("sync"))
        assertEquals("sync", result.operation)
    }
    
    @Test
    fun `mapThrowable should return AppError unchanged`() {
        // Given
        val appError = AppError.NetworkError("Network error")
        
        // When
        val result = FirebaseErrorMapper.mapThrowable(appError)
        
        // Then
        assertEquals(appError, result)
    }
    
    @Test
    fun `mapThrowable should map generic exceptions to UnknownError`() {
        // Given
        val throwable = RuntimeException("Something went wrong")
        
        // When
        val result = FirebaseErrorMapper.mapThrowable(throwable, "operation")
        
        // Then
        assertIs<AppError.UnknownError>(result)
        assertTrue(result.message.contains("Unexpected error"))
        assertTrue(result.message.contains("Something went wrong"))
    }
    
    @Test
    fun `mapError should work without operation context`() {
        // Given
        val error = createNSError(code = 14, message = "Network unavailable")
        
        // When
        val result = FirebaseErrorMapper.mapError(error)
        
        // Then
        assertIs<AppError.NetworkError>(result)
        assertTrue(result.message.contains("No internet connection"))
    }
    
    @Test
    fun `logError should not throw exceptions`() {
        // Given
        val error = AppError.NetworkError("Test error")
        
        // When/Then - should not throw
        FirebaseErrorMapper.logError(
            error = error,
            operation = "test",
            additionalContext = mapOf(
                "userId" to "user123",
                "logId" to "log456"
            )
        )
    }
    
    /**
     * Helper function to create an NSError for testing.
     */
    private fun createNSError(code: Long, message: String): NSError {
        return NSError.errorWithDomain(
            domain = "FIRFirestoreErrorDomain",
            code = code,
            userInfo = mapOf("NSLocalizedDescription" to message)
        )
    }
}
