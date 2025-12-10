package com.example.policemobiledirectory.utils

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

/**
 * Performance logging utility
 * Tracks operation performance, memory usage, and provides analytics
 */
object PerformanceLogger {
    
    private const val TAG = "PerformanceLogger"
    const val SLOW_OPERATION_THRESHOLD_MS = 1000L
    const val VERY_SLOW_OPERATION_THRESHOLD_MS = 3000L
    
    /**
     * Performance metrics data class
     */
    data class PerformanceMetrics(
        val operationName: String,
        val durationMs: Long,
        val memoryBefore: Long = 0,
        val memoryAfter: Long = 0,
        val isSlow: Boolean = false,
        val isVerySlow: Boolean = false,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        val memoryDelta: Long get() = memoryAfter - memoryBefore
        val memoryDeltaMB: Double get() = memoryDelta / (1024.0 * 1024.0)
    }
    
    /**
     * Measure execution time of a block
     */
    inline fun <T> measureOperation(
        operationName: String,
        metadata: Map<String, Any> = emptyMap(),
        block: () -> T
    ): T {
        val memoryBefore = getMemoryUsage()
        val result: T
        val duration = measureTimeMillis {
            result = block()
        }
        val memoryAfter = getMemoryUsage()
        
        val metrics = PerformanceMetrics(
            operationName = operationName,
            durationMs = duration,
            memoryBefore = memoryBefore,
            memoryAfter = memoryAfter,
            isSlow = duration > SLOW_OPERATION_THRESHOLD_MS,
            isVerySlow = duration > VERY_SLOW_OPERATION_THRESHOLD_MS,
            metadata = metadata
        )
        
        logMetrics(metrics)
        return result
    }
    
    /**
     * Measure execution time of a suspend block
     */
    suspend inline fun <T> measureSuspendOperation(
        operationName: String,
        metadata: Map<String, Any> = emptyMap(),
        block: suspend () -> T
    ): T {
        val memoryBefore = getMemoryUsage()
        val result: T
        val duration = measureTimeMillis {
            result = block()
        }
        val memoryAfter = getMemoryUsage()
        
        val metrics = PerformanceMetrics(
            operationName = operationName,
            durationMs = duration,
            memoryBefore = memoryBefore,
            memoryAfter = memoryAfter,
            isSlow = duration > SLOW_OPERATION_THRESHOLD_MS,
            isVerySlow = duration > VERY_SLOW_OPERATION_THRESHOLD_MS,
            metadata = metadata
        )
        
        logMetrics(metrics)
        return result
    }
    
    /**
     * Measure execution time of a Flow operation
     */
    fun <T> Flow<T>.measureFlowOperation(
        operationName: String,
        metadata: Map<String, Any> = emptyMap()
    ): Flow<T> = flow {
        val memoryBefore = getMemoryUsage()
        val startTime = System.currentTimeMillis()
        
        collect { value ->
            emit(value)
        }
        
        val duration = System.currentTimeMillis() - startTime
        val memoryAfter = getMemoryUsage()
        
        val metrics = PerformanceMetrics(
            operationName = operationName,
            durationMs = duration,
            memoryBefore = memoryBefore,
            memoryAfter = memoryAfter,
            isSlow = duration > SLOW_OPERATION_THRESHOLD_MS,
            isVerySlow = duration > VERY_SLOW_OPERATION_THRESHOLD_MS,
            metadata = metadata
        )
        
        logMetrics(metrics)
    }
    
    /**
     * Log performance metrics
     */
    fun logMetrics(metrics: PerformanceMetrics) {
        val logLevel = when {
            metrics.isVerySlow -> Log.ERROR
            metrics.isSlow -> Log.WARN
            else -> Log.DEBUG
        }
        
        val message = buildString {
            append("⚡ ${metrics.operationName}: ${metrics.durationMs}ms")
            if (metrics.memoryDelta != 0L) {
                append(" | Memory: ${String.format("%.2f", metrics.memoryDeltaMB)}MB")
            }
            if (metrics.isVerySlow) {
                append(" | ⚠️ VERY SLOW")
            } else if (metrics.isSlow) {
                append(" | ⚠️ SLOW")
            }
            if (metrics.metadata.isNotEmpty()) {
                append(" | Metadata: ${metrics.metadata}")
            }
        }
        
        when (logLevel) {
            Log.DEBUG -> Log.d(TAG, message)
            Log.INFO -> Log.i(TAG, message)
            Log.WARN -> Log.w(TAG, message)
            Log.ERROR -> Log.e(TAG, message)
            else -> Log.d(TAG, message)
        }
    }
    
    /**
     * Get current memory usage in bytes
     */
    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * Track database operation performance
     */
    inline fun <T> measureDatabaseOperation(
        tableName: String,
        operation: String,
        block: () -> T
    ): T {
        return measureOperation(
            operationName = "DB_${tableName}_$operation",
            metadata = mapOf("table" to tableName, "operation" to operation),
            block = block
        )
    }
    
    /**
     * Track network operation performance
     */
    inline fun <T> measureNetworkOperation(
        endpoint: String,
        method: String = "GET",
        block: () -> T
    ): T {
        return measureOperation(
            operationName = "NETWORK_${method}_$endpoint",
            metadata = mapOf("endpoint" to endpoint, "method" to method),
            block = block
        )
    }
    
    /**
     * Track UI operation performance
     */
    inline fun <T> measureUIOperation(
        screenName: String,
        action: String,
        block: () -> T
    ): T {
        return measureOperation(
            operationName = "UI_${screenName}_$action",
            metadata = mapOf("screen" to screenName, "action" to action),
            block = block
        )
    }
    
    /**
     * Track search operation performance
     */
    inline fun <T> measureSearchOperation(
        queryLength: Int,
        resultCount: Int,
        block: () -> T
    ): T {
        return measureOperation(
            operationName = "SEARCH",
            metadata = mapOf(
                "queryLength" to queryLength,
                "resultCount" to resultCount
            ),
            block = block
        )
    }
}



