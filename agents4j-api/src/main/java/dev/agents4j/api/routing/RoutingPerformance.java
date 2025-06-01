package dev.agents4j.api.routing;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents performance metrics and characteristics for routing operations.
 * 
 * <p>This class captures detailed performance information about routing
 * decisions including processing time, throughput estimates, resource
 * utilization, and quality metrics. It's used for monitoring, optimization,
 * and capacity planning of routing systems.</p>
 * 
 * <p>Key metrics include:</p>
 * <ul>
 * <li>Processing time (actual and estimated)</li>
 * <li>Content analysis complexity</li>
 * <li>Router resource utilization</li>
 * <li>Quality and confidence metrics</li>
 * <li>Scalability characteristics</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * RoutingPerformance performance = RoutingPerformance.builder()
 *     .routerName("ml-content-classifier")
 *     .contentType("CustomerMessage")
 *     .routeCount(5)
 *     .estimatedProcessingTimeMs(250)
 *     .actualProcessingTimeMs(230)
 *     .confidenceScore(0.92)
 *     .build();
 * }</pre>
 */
public class RoutingPerformance {
    
    private final String routerName;
    private final String contentType;
    private final int routeCount;
    private final long estimatedProcessingTimeMs;
    private final long actualProcessingTimeMs;
    private final double confidenceScore;
    private final double throughputPerSecond;
    private final int contentComplexity;
    private final Map<String, Object> resourceMetrics;
    private final Map<String, Object> qualityMetrics;
    private final Instant measurementTime;
    
    private RoutingPerformance(Builder builder) {
        this.routerName = Objects.requireNonNull(builder.routerName, "Router name cannot be null");
        this.contentType = Objects.requireNonNull(builder.contentType, "Content type cannot be null");
        this.routeCount = Math.max(0, builder.routeCount);
        this.estimatedProcessingTimeMs = Math.max(0, builder.estimatedProcessingTimeMs);
        this.actualProcessingTimeMs = Math.max(0, builder.actualProcessingTimeMs);
        this.confidenceScore = validateScore(builder.confidenceScore, "Confidence score");
        this.throughputPerSecond = Math.max(0, builder.throughputPerSecond);
        this.contentComplexity = Math.max(0, builder.contentComplexity);
        this.resourceMetrics = Map.copyOf(builder.resourceMetrics);
        this.qualityMetrics = Map.copyOf(builder.qualityMetrics);
        this.measurementTime = builder.measurementTime != null ? builder.measurementTime : Instant.now();
    }
    
    /**
     * Gets the name of the router that generated these metrics.
     * 
     * @return The router name
     */
    public String getRouterName() {
        return routerName;
    }
    
    /**
     * Gets the content type that was being routed.
     * 
     * @return The content type name
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Gets the number of routes available for routing decisions.
     * 
     * @return The number of available routes
     */
    public int getRouteCount() {
        return routeCount;
    }
    
    /**
     * Gets the estimated processing time in milliseconds.
     * 
     * @return Estimated processing time
     */
    public long getEstimatedProcessingTimeMs() {
        return estimatedProcessingTimeMs;
    }
    
    /**
     * Gets the actual processing time in milliseconds.
     * 
     * @return Actual processing time, or 0 if not measured
     */
    public long getActualProcessingTimeMs() {
        return actualProcessingTimeMs;
    }
    
    /**
     * Gets the confidence score for the routing decision.
     * 
     * @return Confidence score between 0.0 and 1.0
     */
    public double getConfidenceScore() {
        return confidenceScore;
    }
    
    /**
     * Gets the estimated throughput in operations per second.
     * 
     * @return Estimated throughput
     */
    public double getThroughputPerSecond() {
        return throughputPerSecond;
    }
    
    /**
     * Gets the content complexity score.
     * 
     * <p>Higher values indicate more complex content that requires
     * more processing time or resources to analyze.</p>
     * 
     * @return Content complexity score (typically 1-10)
     */
    public int getContentComplexity() {
        return contentComplexity;
    }
    
    /**
     * Gets resource utilization metrics.
     * 
     * <p>May include CPU usage, memory consumption, network calls,
     * external service dependencies, etc.</p>
     * 
     * @return Map of resource metrics
     */
    public Map<String, Object> getResourceMetrics() {
        return resourceMetrics;
    }
    
    /**
     * Gets quality metrics for the routing operation.
     * 
     * <p>May include accuracy estimates, precision/recall metrics,
     * historical performance data, etc.</p>
     * 
     * @return Map of quality metrics
     */
    public Map<String, Object> getQualityMetrics() {
        return qualityMetrics;
    }
    
    /**
     * Gets the time when these metrics were measured.
     * 
     * @return Measurement timestamp
     */
    public Instant getMeasurementTime() {
        return measurementTime;
    }
    
    /**
     * Calculates the accuracy of time estimation.
     * 
     * @return Estimation accuracy ratio (actual/estimated), or 0 if no actual time
     */
    public double getTimeEstimationAccuracy() {
        if (actualProcessingTimeMs == 0 || estimatedProcessingTimeMs == 0) {
            return 0.0;
        }
        return (double) actualProcessingTimeMs / estimatedProcessingTimeMs;
    }
    
    /**
     * Checks if the actual processing time exceeded the estimate.
     * 
     * @return true if actual time was longer than estimated
     */
    public boolean exceededEstimate() {
        return actualProcessingTimeMs > estimatedProcessingTimeMs;
    }
    
    /**
     * Gets the processing time variance from estimate.
     * 
     * @return Difference between actual and estimated time in milliseconds
     */
    public long getProcessingTimeVariance() {
        return actualProcessingTimeMs - estimatedProcessingTimeMs;
    }
    
    /**
     * Calculates estimated operations per minute based on processing time.
     * 
     * @return Estimated operations per minute
     */
    public double getEstimatedOperationsPerMinute() {
        if (estimatedProcessingTimeMs == 0) {
            return 0.0;
        }
        return 60000.0 / estimatedProcessingTimeMs; // 60,000 ms per minute
    }
    
    /**
     * Gets a specific resource metric value.
     * 
     * @param key The metric key
     * @param <T> The expected type
     * @return The metric value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getResourceMetric(String key) {
        return (T) resourceMetrics.get(key);
    }
    
    /**
     * Gets a specific quality metric value.
     * 
     * @param key The metric key
     * @param <T> The expected type
     * @return The metric value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getQualityMetric(String key) {
        return (T) qualityMetrics.get(key);
    }
    
    /**
     * Creates a performance summary for logging and monitoring.
     * 
     * @return A formatted performance summary string
     */
    public String getPerformanceSummary() {
        return String.format(
            "Router: %s | Content: %s | Routes: %d | Time: %dms (est: %dms) | Confidence: %.2f | Throughput: %.1f/sec",
            routerName, contentType, routeCount, actualProcessingTimeMs, 
            estimatedProcessingTimeMs, confidenceScore, throughputPerSecond
        );
    }
    
    /**
     * Validates a score value is between 0.0 and 1.0.
     */
    private static double validateScore(double score, String name) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }
    
    /**
     * Creates a new Builder for constructing RoutingPerformance instances.
     * 
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating RoutingPerformance instances.
     */
    public static class Builder {
        private String routerName;
        private String contentType;
        private int routeCount = 0;
        private long estimatedProcessingTimeMs = 0;
        private long actualProcessingTimeMs = 0;
        private double confidenceScore = 0.0;
        private double throughputPerSecond = 0.0;
        private int contentComplexity = 1;
        private Map<String, Object> resourceMetrics = new java.util.HashMap<>();
        private Map<String, Object> qualityMetrics = new java.util.HashMap<>();
        private Instant measurementTime;
        
        /**
         * Sets the router name.
         * 
         * @param routerName The router name
         * @return This builder instance
         */
        public Builder routerName(String routerName) {
            this.routerName = routerName;
            return this;
        }
        
        /**
         * Sets the content type.
         * 
         * @param contentType The content type name
         * @return This builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }
        
        /**
         * Sets the number of available routes.
         * 
         * @param routeCount The route count
         * @return This builder instance
         */
        public Builder routeCount(int routeCount) {
            this.routeCount = routeCount;
            return this;
        }
        
        /**
         * Sets the estimated processing time.
         * 
         * @param estimatedProcessingTimeMs Estimated time in milliseconds
         * @return This builder instance
         */
        public Builder estimatedProcessingTimeMs(long estimatedProcessingTimeMs) {
            this.estimatedProcessingTimeMs = estimatedProcessingTimeMs;
            return this;
        }
        
        /**
         * Sets the actual processing time.
         * 
         * @param actualProcessingTimeMs Actual time in milliseconds
         * @return This builder instance
         */
        public Builder actualProcessingTimeMs(long actualProcessingTimeMs) {
            this.actualProcessingTimeMs = actualProcessingTimeMs;
            return this;
        }
        
        /**
         * Sets the confidence score.
         * 
         * @param confidenceScore Confidence score (0.0 to 1.0)
         * @return This builder instance
         */
        public Builder confidenceScore(double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }
        
        /**
         * Sets the estimated throughput.
         * 
         * @param throughputPerSecond Operations per second
         * @return This builder instance
         */
        public Builder throughputPerSecond(double throughputPerSecond) {
            this.throughputPerSecond = throughputPerSecond;
            return this;
        }
        
        /**
         * Sets the content complexity score.
         * 
         * @param contentComplexity Complexity score (typically 1-10)
         * @return This builder instance
         */
        public Builder contentComplexity(int contentComplexity) {
            this.contentComplexity = contentComplexity;
            return this;
        }
        
        /**
         * Adds a resource metric.
         * 
         * @param key The metric key
         * @param value The metric value
         * @return This builder instance
         */
        public Builder addResourceMetric(String key, Object value) {
            this.resourceMetrics.put(key, value);
            return this;
        }
        
        /**
         * Sets all resource metrics.
         * 
         * @param resourceMetrics Map of resource metrics
         * @return This builder instance
         */
        public Builder resourceMetrics(Map<String, Object> resourceMetrics) {
            this.resourceMetrics = new java.util.HashMap<>(resourceMetrics);
            return this;
        }
        
        /**
         * Adds a quality metric.
         * 
         * @param key The metric key
         * @param value The metric value
         * @return This builder instance
         */
        public Builder addQualityMetric(String key, Object value) {
            this.qualityMetrics.put(key, value);
            return this;
        }
        
        /**
         * Sets all quality metrics.
         * 
         * @param qualityMetrics Map of quality metrics
         * @return This builder instance
         */
        public Builder qualityMetrics(Map<String, Object> qualityMetrics) {
            this.qualityMetrics = new java.util.HashMap<>(qualityMetrics);
            return this;
        }
        
        /**
         * Sets the measurement time.
         * 
         * @param measurementTime When the metrics were measured
         * @return This builder instance
         */
        public Builder measurementTime(Instant measurementTime) {
            this.measurementTime = measurementTime;
            return this;
        }
        
        /**
         * Builds the RoutingPerformance instance.
         * 
         * @return A new RoutingPerformance instance
         * @throws IllegalStateException if required fields are not set
         */
        public RoutingPerformance build() {
            if (routerName == null) {
                throw new IllegalStateException("Router name must be set");
            }
            if (contentType == null) {
                throw new IllegalStateException("Content type must be set");
            }
            return new RoutingPerformance(this);
        }
    }
    
    @Override
    public String toString() {
        return getPerformanceSummary();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingPerformance that = (RoutingPerformance) o;
        return routeCount == that.routeCount &&
               estimatedProcessingTimeMs == that.estimatedProcessingTimeMs &&
               actualProcessingTimeMs == that.actualProcessingTimeMs &&
               Double.compare(that.confidenceScore, confidenceScore) == 0 &&
               Double.compare(that.throughputPerSecond, throughputPerSecond) == 0 &&
               contentComplexity == that.contentComplexity &&
               Objects.equals(routerName, that.routerName) &&
               Objects.equals(contentType, that.contentType) &&
               Objects.equals(resourceMetrics, that.resourceMetrics) &&
               Objects.equals(qualityMetrics, that.qualityMetrics) &&
               Objects.equals(measurementTime, that.measurementTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(routerName, contentType, routeCount, estimatedProcessingTimeMs,
                          actualProcessingTimeMs, confidenceScore, throughputPerSecond,
                          contentComplexity, resourceMetrics, qualityMetrics, measurementTime);
    }
}