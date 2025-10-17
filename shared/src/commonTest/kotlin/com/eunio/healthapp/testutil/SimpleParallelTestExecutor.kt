package com.eunio.healthapp.testutil

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Simplified parallel test execution utilities for safe concurrent test execution.
 */
object SimpleParallelTestExecutor {
    
    private const val DEFAULT_MAX_CONCURRENCY = 4
    private val DEFAULT_TIMEOUT = 30.seconds
    
    /**
     * Execute multiple test operations in parallel with controlled concurrency
     */
    suspend fun executeInParallel(
        operations: List<suspend () -> Unit>,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
        timeout: Duration = DEFAULT_TIMEOUT
    ) = coroutineScope {
        withTimeout(timeout) {
            val semaphore = Semaphore(maxConcurrency)
            
            operations.map { operation ->
                async {
                    semaphore.withPermit {
                        operation()
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * Execute named test operations in parallel with performance monitoring
     */
    suspend fun executeWithMonitoring(
        operations: List<Pair<String, suspend () -> Unit>>,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY
    ): SimpleParallelResult = coroutineScope {
        val startTime = kotlinx.datetime.Clock.System.now()
        val semaphore = Semaphore(maxConcurrency)
        val results = mutableMapOf<String, Boolean>()
        val resultsMutex = Mutex()
        
        val jobs = operations.map { (name, operation) ->
            async {
                semaphore.withPermit {
                    try {
                        operation()
                        resultsMutex.withLock {
                            results[name] = true
                        }
                    } catch (e: Exception) {
                        resultsMutex.withLock {
                            results[name] = false
                        }
                        println("Test $name failed: ${e.message}")
                    }
                }
            }
        }
        
        jobs.awaitAll()
        
        val endTime = kotlinx.datetime.Clock.System.now()
        val totalDuration = endTime - startTime
        
        SimpleParallelResult(
            totalDuration = totalDuration,
            maxConcurrency = maxConcurrency,
            successCount = results.values.count { it },
            failureCount = results.values.count { !it },
            results = results.toMap()
        )
    }
    
    /**
     * Execute test batches in parallel
     */
    suspend fun executeBatches(
        items: List<String>,
        batchSize: Int = 10,
        maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
        operation: suspend (String) -> Unit
    ) = coroutineScope {
        val batches = items.chunked(batchSize)
        val semaphore = Semaphore(maxConcurrency)
        
        batches.map { batch ->
            async {
                semaphore.withPermit {
                    batch.forEach { item ->
                        operation(item)
                    }
                }
            }
        }.awaitAll()
    }
}

/**
 * Simple result container for parallel execution
 */
data class SimpleParallelResult(
    val totalDuration: kotlin.time.Duration,
    val maxConcurrency: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: Map<String, Boolean>
) {
    val totalCount: Int get() = successCount + failureCount
    
    fun printSummary() {
        println("Parallel Execution Summary:")
        println("  Total tests: $totalCount")
        println("  Successful: $successCount")
        println("  Failed: $failureCount")
        println("  Max concurrency: $maxConcurrency")
        println("  Total duration: $totalDuration")
        
        if (failureCount > 0) {
            println("  Failed tests:")
            results.filter { !it.value }.forEach { (name, _) ->
                println("    - $name")
            }
        }
    }
}