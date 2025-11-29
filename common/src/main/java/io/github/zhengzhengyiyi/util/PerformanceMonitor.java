package io.github.zhengzhengyiyi.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A performance monitoring utility for tracking operation execution times, memory usage, and error rates.
 * Provides detailed statistics for performance analysis and optimization.
 */
public class PerformanceMonitor {
    private static final ConcurrentHashMap<String, OperationStats> statsMap = new ConcurrentHashMap<>();
    private static final AtomicLong totalMemoryUsed = new AtomicLong(0);
    private static final AtomicInteger activeOperations = new AtomicInteger(0);
    private static volatile boolean enabled = true;

    /**
     * Statistics container for tracking performance metrics of individual operations.
     */
    public static class OperationStats {
        /** Total execution time in nanoseconds */
        public final AtomicLong totalTime = new AtomicLong(0);
        /** Number of times the operation was executed */
        public final AtomicLong count = new AtomicLong(0);
        /** Maximum execution time in nanoseconds */
        public final AtomicLong maxTime = new AtomicLong(0);
        /** Minimum execution time in nanoseconds */
        public final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        /** Number of errors encountered during execution */
        public final AtomicLong errors = new AtomicLong(0);
    }

    /**
     * AutoCloseable timer that automatically records performance metrics when closed.
     * Usage with try-with-resources pattern is recommended.
     */
    public static class Timer implements AutoCloseable {
        private final String operationName;
        private final long startTime;
        private final long startMemory;
        private boolean closed = false;

        /**
         * Creates a new timer for the specified operation.
         * @param operationName the name of the operation to monitor
         */
        public Timer(String operationName) {
            if (!enabled) {
                this.operationName = null;
                this.startTime = 0;
                this.startMemory = 0;
                return;
            }
            this.operationName = operationName;
            this.startTime = System.nanoTime();
            this.startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            activeOperations.incrementAndGet();
        }

        /**
         * Stops the timer and records the performance metrics.
         */
        @Override
        public void close() {
            if (!enabled || closed || operationName == null) return;
            
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = endMemory - startMemory;

            OperationStats stats = statsMap.computeIfAbsent(operationName, k -> new OperationStats());
            stats.totalTime.addAndGet(duration);
            stats.count.incrementAndGet();
            stats.maxTime.updateAndGet(current -> Math.max(current, duration));
            stats.minTime.updateAndGet(current -> Math.min(current, duration));

            totalMemoryUsed.addAndGet(memoryUsed);
            activeOperations.decrementAndGet();
            closed = true;
        }

        /**
         * Records an error for the current operation.
         * Should be called when the operation fails.
         */
        public void recordError() {
            if (!enabled || closed || operationName == null) return;
            
            OperationStats stats = statsMap.get(operationName);
            if (stats != null) {
                stats.errors.incrementAndGet();
            }
        }
    }

    /**
     * Starts timing an operation.
     * @param operationName the name of the operation to monitor
     * @return a Timer instance that should be closed when the operation completes
     */
    public static Timer start(String operationName) {
        return new Timer(operationName);
    }

    /**
     * Gets statistics for a specific operation.
     * @param operationName the name of the operation
     * @return the OperationStats for the specified operation, or null if not found
     */
    public static OperationStats getStats(String operationName) {
        return statsMap.get(operationName);
    }

    /**
     * Gets all collected performance statistics.
     * @return a copy of all operation statistics
     */
    public static ConcurrentHashMap<String, OperationStats> getAllStats() {
        return new ConcurrentHashMap<>(statsMap);
    }

    /**
     * Resets all collected statistics and counters.
     */
    public static void reset() {
        statsMap.clear();
        totalMemoryUsed.set(0);
        activeOperations.set(0);
    }

    /**
     * Enables or disables performance monitoring.
     * @param isEnabled true to enable monitoring, false to disable
     */
    public static void setEnabled(boolean isEnabled) {
        enabled = isEnabled;
    }

    /**
     * Checks if performance monitoring is currently enabled.
     * @return true if monitoring is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the total memory used by all monitored operations.
     * @return total memory usage in bytes
     */
    public static long getTotalMemoryUsed() {
        return totalMemoryUsed.get();
    }

    /**
     * Gets the number of currently active operations being monitored.
     * @return count of active operations
     */
    public static int getActiveOperations() {
        return activeOperations.get();
    }

    /**
     * Generates a formatted summary of all performance statistics.
     * @return a string containing formatted performance summary
     */
    public static String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Performance Monitor Summary:\n");
        summary.append("Active Operations: ").append(activeOperations.get()).append("\n");
        summary.append("Total Memory Used: ").append(totalMemoryUsed.get() / 1024 / 1024).append("MB\n");
        
        statsMap.forEach((name, stats) -> {
            if (stats.count.get() > 0) {
                double avgTime = stats.totalTime.get() / (double) stats.count.get() / 1_000_000.0;
                summary.append(String.format("%s: count=%d, avg=%.2fms, min=%.2fms, max=%.2fms, errors=%d\n",
                    name, stats.count.get(), avgTime,
                    stats.minTime.get() / 1_000_000.0,
                    stats.maxTime.get() / 1_000_000.0,
                    stats.errors.get()));
            }
        });
        
        return summary.toString();
    }
}
