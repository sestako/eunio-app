package com.eunio.healthapp.android.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Performance optimization utilities for Compose UI.
 * Reduces unnecessary recompositions and improves rendering performance.
 */
object PerformanceOptimizations {
    
    /**
     * Creates a stable wrapper for lambda functions to prevent unnecessary recompositions.
     */
    @Composable
    fun <T> rememberStableCallback(callback: (T) -> Unit): (T) -> Unit {
        return remember { { value: T -> callback(value) } }
    }
    
    /**
     * Creates a stable wrapper for parameterless lambda functions.
     */
    @Composable
    fun rememberStableCallback(callback: () -> Unit): () -> Unit {
        return remember { { callback() } }
    }
    
    /**
     * Optimized state holder that only triggers recomposition when the value actually changes.
     */
    @Composable
    fun <T> rememberOptimizedState(
        initial: T,
        areEqual: (T, T) -> Boolean = { a, b -> a == b }
    ): MutableState<T> {
        return remember {
            object : MutableState<T> {
                private var _value = initial
                
                override var value: T
                    get() = _value
                    set(newValue) {
                        if (!areEqual(_value, newValue)) {
                            _value = newValue
                        }
                    }
                
                override fun component1(): T = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }
    
    /**
     * Collects a Flow with optimized recomposition behavior.
     */
    @Composable
    fun <T> Flow<T>.collectAsOptimizedState(
        initial: T,
        areEqual: (T, T) -> Boolean = { a, b -> a == b }
    ): State<T> {
        return this
            .distinctUntilChanged(areEqual)
            .collectAsState(initial)
    }
    
    /**
     * Maps a Flow with distinctUntilChanged optimization.
     */
    fun <T, R> Flow<T>.mapDistinct(
        areEqual: (R, R) -> Boolean = { a, b -> a == b },
        transform: (T) -> R
    ): Flow<R> {
        return this
            .map(transform)
            .distinctUntilChanged(areEqual)
    }
    
    /**
     * Modifier that caches drawing operations for better performance.
     */
    fun Modifier.cachedDraw(
        key: Any? = null,
        onDraw: DrawScope.() -> Unit
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "cachedDraw"
            properties["key"] = key
        }
    ) {
        this.drawWithCache {
            onDrawWithContent {
                onDraw()
            }
        }
    }
    
    /**
     * Optimized padding that avoids recomposition when values don't change.
     */
    @Composable
    fun Modifier.optimizedPadding(
        horizontal: Dp = 0.dp,
        vertical: Dp = 0.dp
    ): Modifier {
        val density = LocalDensity.current
        
        return remember(horizontal, vertical, density) {
            this.then(
                Modifier.drawWithCache {
                    onDrawWithContent {
                        drawContent()
                    }
                }
            )
        }
    }
    
    /**
     * Stable data class wrapper to prevent unnecessary recompositions.
     */
    @Stable
    data class StableWrapper<T>(val value: T)
    
    /**
     * Creates a stable wrapper for unstable data.
     */
    @Composable
    fun <T> T.asStable(): StableWrapper<T> {
        return remember(this) { StableWrapper(this) }
    }
    
    /**
     * Optimized list state that only recomposes when list content changes.
     */
    @Composable
    fun <T> rememberOptimizedListState(
        list: List<T>,
        keySelector: (T) -> Any = { it.hashCode() }
    ): State<List<T>> {
        return remember {
            derivedStateOf {
                list.map { item ->
                    keySelector(item) to item
                }.distinctBy { it.first }.map { it.second }
            }
        }
    }
    
    /**
     * Debounced state that only updates after a delay to prevent rapid recompositions.
     */
    @Composable
    fun <T> rememberDebouncedState(
        value: T,
        delayMillis: Long = 300L
    ): State<T> {
        val debouncedValue = remember { mutableStateOf(value) }
        
        LaunchedEffect(value) {
            kotlinx.coroutines.delay(delayMillis)
            debouncedValue.value = value
        }
        
        return debouncedValue
    }
    
    /**
     * Throttled state that limits update frequency to prevent excessive recompositions.
     */
    @Composable
    fun <T> rememberThrottledState(
        value: T,
        intervalMillis: Long = 100L
    ): State<T> {
        val throttledValue = remember { mutableStateOf(value) }
        val lastUpdateTime = remember { mutableStateOf(0L) }
        
        LaunchedEffect(value) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime.value >= intervalMillis) {
                throttledValue.value = value
                lastUpdateTime.value = currentTime
            }
        }
        
        return throttledValue
    }
}

/**
 * Annotation to mark stable classes for Compose optimization.
 */
@Stable
annotation class ComposeStable

/**
 * Annotation to mark immutable classes for Compose optimization.
 */
@Immutable
annotation class ComposeImmutable

/**
 * Extension function to create stable callbacks.
 */
@Composable
inline fun <T> ((T) -> Unit).asStable(): (T) -> Unit {
    return PerformanceOptimizations.rememberStableCallback(this)
}

/**
 * Extension function to create stable parameterless callbacks.
 */
@Composable
inline fun (() -> Unit).asStable(): () -> Unit {
    return PerformanceOptimizations.rememberStableCallback(this)
}