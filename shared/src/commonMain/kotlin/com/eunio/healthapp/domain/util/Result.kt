package com.eunio.healthapp.domain.util

import com.eunio.healthapp.domain.error.AppError

/**
 * A sealed class representing the result of an operation that can either succeed or fail.
 * Used throughout the repository layer to handle operations that may fail gracefully.
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with a value
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with an error
     */
    data class Error(val error: AppError) : Result<Nothing>()
    
    /**
     * Returns true if this result represents a successful operation
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if this result represents a failed operation
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns the data if successful, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Returns the error if failed, null otherwise
     */
    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
    }
    
    /**
     * Returns the data if successful, or throws the error if failed
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error
    }
    
    /**
     * Transforms the data if successful, preserves error if failed
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }
    
    /**
     * Flat maps the result, allowing chaining of operations that return Result
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
    }
    
    /**
     * Executes the given action if the result is successful
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }
    
    /**
     * Executes the given action if the result is an error
     */
    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }
    
    companion object {
        /**
         * Creates a successful result
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Creates an error result
         */
        fun <T> error(error: AppError): Result<T> = Error(error)
        
        /**
         * Creates a result from a nullable value, using the provided error if null
         */
        fun <T> fromNullable(data: T?, error: AppError): Result<T> = 
            if (data != null) Success(data) else Error(error)
        
        /**
         * Wraps a potentially throwing operation in a Result
         */
        inline fun <T> catching(errorHandler: ErrorHandler, operation: () -> T): Result<T> {
            return try {
                Success(operation())
            } catch (e: Exception) {
                Error(errorHandler.handleError(e))
            }
        }
    }
}