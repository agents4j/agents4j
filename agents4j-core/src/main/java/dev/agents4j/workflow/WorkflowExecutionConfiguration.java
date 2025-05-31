package dev.agents4j.workflow;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Enhanced configuration for workflow execution behavior.
 * Immutable configuration object following builder pattern.
 */
public class WorkflowExecutionConfiguration {
    
    private final int maxExecutionSteps;
    private final Duration maxExecutionTime;
    private final boolean enableMonitoring;
    private final boolean enableMetrics;
    private final RetryPolicy retryPolicy;
    private final int threadPoolSize;
    private final Executor asyncExecutor;
    private final boolean failFast;
    private final Duration nodeTimeout;
    private final boolean cleanErrorMessages;
    private final boolean preserveOriginalInput;
    
    private WorkflowExecutionConfiguration(Builder builder) {
        this.maxExecutionSteps = builder.maxExecutionSteps;
        this.maxExecutionTime = builder.maxExecutionTime;
        this.enableMonitoring = builder.enableMonitoring;
        this.enableMetrics = builder.enableMetrics;
        this.retryPolicy = builder.retryPolicy;
        this.threadPoolSize = builder.threadPoolSize;
        this.asyncExecutor = builder.asyncExecutor;
        this.failFast = builder.failFast;
        this.nodeTimeout = builder.nodeTimeout;
        this.cleanErrorMessages = builder.cleanErrorMessages;
        this.preserveOriginalInput = builder.preserveOriginalInput;
    }
    
    // Getters
    public int getMaxExecutionSteps() { 
        return maxExecutionSteps; 
    }
    
    public Duration getMaxExecutionTime() { 
        return maxExecutionTime; 
    }
    
    public boolean isMonitoringEnabled() { 
        return enableMonitoring; 
    }
    
    public boolean isMetricsEnabled() { 
        return enableMetrics; 
    }
    
    public RetryPolicy getRetryPolicy() { 
        return retryPolicy; 
    }
    
    public int getThreadPoolSize() { 
        return threadPoolSize; 
    }
    
    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }
    
    public boolean isFailFast() {
        return failFast;
    }
    
    public Duration getNodeTimeout() {
        return nodeTimeout;
    }
    
    public boolean isCleanErrorMessages() {
        return cleanErrorMessages;
    }
    
    public boolean isPreserveOriginalInput() {
        return preserveOriginalInput;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static WorkflowExecutionConfiguration defaultConfiguration() {
        return builder().build();
    }
    
    public static class Builder {
        private int maxExecutionSteps = 1000;
        private Duration maxExecutionTime = Duration.ofMinutes(30);
        private boolean enableMonitoring = true;
        private boolean enableMetrics = true;
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private int threadPoolSize = Runtime.getRuntime().availableProcessors();
        private Executor asyncExecutor = ForkJoinPool.commonPool();
        private boolean failFast = false;
        private Duration nodeTimeout = Duration.ofMinutes(5);
        private boolean cleanErrorMessages = false;
        private boolean preserveOriginalInput = true;
        
        public Builder maxExecutionSteps(int steps) {
            if (steps <= 0) {
                throw new IllegalArgumentException("Max execution steps must be positive");
            }
            this.maxExecutionSteps = steps;
            return this;
        }
        
        public Builder maxExecutionTime(Duration time) {
            this.maxExecutionTime = Objects.requireNonNull(time, "Max execution time cannot be null");
            return this;
        }
        
        public Builder enableMonitoring(boolean enable) {
            this.enableMonitoring = enable;
            return this;
        }
        
        public Builder enableMetrics(boolean enable) {
            this.enableMetrics = enable;
            return this;
        }
        
        public Builder retryPolicy(RetryPolicy policy) {
            this.retryPolicy = Objects.requireNonNull(policy, "Retry policy cannot be null");
            return this;
        }
        
        public Builder threadPoolSize(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Thread pool size must be positive");
            }
            this.threadPoolSize = size;
            return this;
        }
        
        public Builder asyncExecutor(Executor executor) {
            this.asyncExecutor = Objects.requireNonNull(executor, "Async executor cannot be null");
            return this;
        }
        
        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }
        
        public Builder nodeTimeout(Duration timeout) {
            this.nodeTimeout = Objects.requireNonNull(timeout, "Node timeout cannot be null");
            return this;
        }
        
        public Builder cleanErrorMessages(boolean cleanErrorMessages) {
            this.cleanErrorMessages = cleanErrorMessages;
            return this;
        }
        
        public Builder preserveOriginalInput(boolean preserveOriginalInput) {
            this.preserveOriginalInput = preserveOriginalInput;
            return this;
        }
        
        public WorkflowExecutionConfiguration build() {
            return new WorkflowExecutionConfiguration(this);
        }
    }
    
    /**
     * Retry policy for workflow execution.
     */
    public static class RetryPolicy {
        
        private final int maxRetries;
        private final Duration delay;
        private final double backoffMultiplier;
        private final Duration maxDelay;
        
        private RetryPolicy(int maxRetries, Duration delay, double backoffMultiplier, Duration maxDelay) {
            this.maxRetries = maxRetries;
            this.delay = delay;
            this.backoffMultiplier = backoffMultiplier;
            this.maxDelay = maxDelay;
        }
        
        public static RetryPolicy none() {
            return new RetryPolicy(0, Duration.ZERO, 1.0, Duration.ZERO);
        }
        
        public static RetryPolicy fixed(int maxRetries, Duration delay) {
            return new RetryPolicy(maxRetries, delay, 1.0, delay);
        }
        
        public static RetryPolicy exponential(int maxRetries, Duration initialDelay, double multiplier, Duration maxDelay) {
            return new RetryPolicy(maxRetries, initialDelay, multiplier, maxDelay);
        }
        
        // Getters
        public int getMaxRetries() { return maxRetries; }
        public Duration getDelay() { return delay; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public Duration getMaxDelay() { return maxDelay; }
        public boolean isEnabled() { return maxRetries > 0; }
        
        public Duration calculateDelay(int attempt) {
            if (!isEnabled() || attempt <= 0) {
                return Duration.ZERO;
            }
            
            Duration calculatedDelay = delay;
            for (int i = 1; i < attempt; i++) {
                calculatedDelay = Duration.ofMillis((long) (calculatedDelay.toMillis() * backoffMultiplier));
            }
            
            return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "WorkflowExecutionConfiguration{maxSteps=%d, maxTime=%s, monitoring=%s, metrics=%s, retryEnabled=%s, cleanErrors=%s, preserveInput=%s}",
            maxExecutionSteps, maxExecutionTime, enableMonitoring, enableMetrics, retryPolicy.isEnabled(), cleanErrorMessages, preserveOriginalInput
        );
    }
}