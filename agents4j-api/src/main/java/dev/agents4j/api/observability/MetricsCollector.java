package dev.agents4j.api.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Interface for collecting metrics and telemetry data from workflow operations.
 * Provides a clean abstraction for different metrics backends (Prometheus, Micrometer, etc.)
 * while enabling comprehensive observability of workflow execution.
 * 
 * <p>This interface supports various metric types including counters, gauges, timers,
 * and histograms, allowing for detailed monitoring of workflow performance and behavior.</p>
 */
public interface MetricsCollector {
    
    /**
     * Increments a counter metric by 1.
     *
     * @param name The metric name
     * @param tags Optional tags for metric dimensions
     */
    void incrementCounter(String name, Map<String, String> tags);
    
    /**
     * Increments a counter metric by 1 without tags.
     *
     * @param name The metric name
     */
    default void incrementCounter(String name) {
        incrementCounter(name, Map.of());
    }
    
    /**
     * Increments a counter metric by a specific amount.
     *
     * @param name The metric name
     * @param amount The amount to increment by
     * @param tags Optional tags for metric dimensions
     */
    void incrementCounter(String name, double amount, Map<String, String> tags);
    
    /**
     * Records a gauge value (point-in-time measurement).
     *
     * @param name The metric name
     * @param value The gauge value
     * @param tags Optional tags for metric dimensions
     */
    void recordGauge(String name, double value, Map<String, String> tags);
    
    /**
     * Records a gauge value without tags.
     *
     * @param name The metric name
     * @param value The gauge value
     */
    default void recordGauge(String name, double value) {
        recordGauge(name, value, Map.of());
    }
    
    /**
     * Records a timer measurement for operation duration.
     *
     * @param name The metric name
     * @param duration The operation duration
     * @param unit The time unit
     * @param tags Optional tags for metric dimensions
     */
    void recordTimer(String name, long duration, TimeUnit unit, Map<String, String> tags);
    
    /**
     * Records a timer measurement using Duration.
     *
     * @param name The metric name
     * @param duration The operation duration
     * @param tags Optional tags for metric dimensions
     */
    default void recordTimer(String name, Duration duration, Map<String, String> tags) {
        recordTimer(name, duration.toNanos(), TimeUnit.NANOSECONDS, tags);
    }
    
    /**
     * Records a timer measurement without tags.
     *
     * @param name The metric name
     * @param duration The operation duration
     */
    default void recordTimer(String name, Duration duration) {
        recordTimer(name, duration, Map.of());
    }
    
    /**
     * Times an operation and records the duration.
     *
     * @param name The metric name
     * @param operation The operation to time
     * @param tags Optional tags for metric dimensions
     * @param <T> The operation return type
     * @return The operation result
     * @throws Exception if the operation throws an exception
     */
    <T> T timeOperation(String name, TimedOperation<T> operation, Map<String, String> tags) throws Exception;
    
    /**
     * Times an operation without tags.
     *
     * @param name The metric name
     * @param operation The operation to time
     * @param <T> The operation return type
     * @return The operation result
     * @throws Exception if the operation throws an exception
     */
    default <T> T timeOperation(String name, TimedOperation<T> operation) throws Exception {
        return timeOperation(name, operation, Map.of());
    }
    
    /**
     * Records a histogram value for distribution analysis.
     *
     * @param name The metric name
     * @param value The value to record
     * @param tags Optional tags for metric dimensions
     */
    void recordHistogram(String name, double value, Map<String, String> tags);
    
    /**
     * Records a histogram value without tags.
     *
     * @param name The metric name
     * @param value The value to record
     */
    default void recordHistogram(String name, double value) {
        recordHistogram(name, value, Map.of());
    }
    
    /**
     * Creates a timer sample for manual timing operations.
     *
     * @return A timer sample that can be stopped to record duration
     */
    TimerSample startTimer();
    
    /**
     * Records workflow-specific metrics.
     *
     * @param workflowId The workflow identifier
     * @param nodeId The current node identifier (optional)
     * @param metricType The type of workflow metric
     * @param value The metric value
     * @param tags Additional tags for the metric
     */
    void recordWorkflowMetric(String workflowId, String nodeId, WorkflowMetricType metricType, 
                             double value, Map<String, String> tags);
    
    /**
     * Records an error occurrence.
     *
     * @param errorType The type or category of error
     * @param context Context information about where the error occurred
     * @param tags Additional tags for the error metric
     */
    void recordError(String errorType, String context, Map<String, String> tags);
    
    /**
     * Records an error without additional tags.
     *
     * @param errorType The type or category of error
     * @param context Context information about where the error occurred
     */
    default void recordError(String errorType, String context) {
        recordError(errorType, context, Map.of());
    }
    
    /**
     * Gets the current metric registry information.
     *
     * @return Map containing information about registered metrics
     */
    Map<String, Object> getMetricRegistry();
    
    /**
     * Gets all metric names currently registered.
     *
     * @return Set of all metric names
     */
    Set<String> getMetricNames();
    
    /**
     * Checks if a specific metric exists.
     *
     * @param name The metric name to check
     * @return true if the metric exists, false otherwise
     */
    boolean hasMetric(String name);
    
    /**
     * Gets metadata about this metrics collector.
     *
     * @return Map containing collector configuration and capabilities
     */
    default Map<String, Object> getCollectorInfo() {
        return Map.of(
            "collectorType", getClass().getSimpleName(),
            "metricCount", getMetricNames().size(),
            "supportsHistograms", true,
            "supportsTimers", true
        );
    }
    
    /**
     * Flushes any buffered metrics to the backend.
     * Default implementation is no-op for collectors that don't buffer.
     */
    default void flush() {
        // Default implementation is no-op
    }
    
    /**
     * Closes the metrics collector and releases resources.
     * Default implementation is no-op for collectors that don't require cleanup.
     */
    default void close() {
        // Default implementation is no-op
    }
}

/**
 * Functional interface for operations that need to be timed.
 */
@FunctionalInterface
interface TimedOperation<T> {
    T execute() throws Exception;
}

/**
 * Interface for timer samples that can measure elapsed time.
 */
interface TimerSample {
    
    /**
     * Stops the timer and records the elapsed time.
     *
     * @param name The metric name
     * @param tags Optional tags for the metric
     */
    void stop(String name, Map<String, String> tags);
    
    /**
     * Stops the timer and records the elapsed time without tags.
     *
     * @param name The metric name
     */
    default void stop(String name) {
        stop(name, Map.of());
    }
    
    /**
     * Gets the elapsed time since the timer was started.
     *
     * @return The elapsed duration
     */
    Duration getElapsed();
    
    /**
     * Gets the start time of this timer sample.
     *
     * @return The instant when timing began
     */
    Instant getStartTime();
}

/**
 * Enumeration of workflow-specific metric types.
 */
enum WorkflowMetricType {
    /** Time taken to execute a workflow */
    WORKFLOW_DURATION,
    
    /** Time taken to execute a node */
    NODE_DURATION,
    
    /** Number of nodes executed in a workflow */
    NODE_COUNT,
    
    /** Number of workflow executions */
    WORKFLOW_EXECUTIONS,
    
    /** Number of workflow failures */
    WORKFLOW_FAILURES,
    
    /** Number of workflow suspensions */
    WORKFLOW_SUSPENSIONS,
    
    /** Number of workflow resumptions */
    WORKFLOW_RESUMPTIONS,
    
    /** Size of workflow state data */
    STATE_SIZE,
    
    /** Number of state updates */
    STATE_UPDATES,
    
    /** Memory usage during execution */
    MEMORY_USAGE,
    
    /** CPU usage during execution */
    CPU_USAGE,
    
    /** Number of retries performed */
    RETRY_COUNT,
    
    /** Queue depth for workflow executions */
    QUEUE_DEPTH,
    
    /** Throughput metrics */
    THROUGHPUT
}