package dev.agents4j.workflow.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for workflow execution.
 */
public class WorkflowConfiguration {

    private final int maxExecutionSteps;
    private final Duration maxExecutionTime;
    private final boolean detectCycles;
    private final boolean allowCycles;

    private WorkflowConfiguration(Builder builder) {
        this.maxExecutionSteps = builder.maxExecutionSteps;
        this.maxExecutionTime = builder.maxExecutionTime;
        this.detectCycles = builder.detectCycles;
        this.allowCycles = builder.allowCycles;
    }

    /**
     * Gets the maximum number of execution steps.
     *
     * @return The maximum number of steps
     */
    public int getMaxExecutionSteps() {
        return maxExecutionSteps;
    }

    /**
     * Gets the maximum execution time.
     *
     * @return The maximum execution time
     */
    public Duration getMaxExecutionTime() {
        return maxExecutionTime;
    }

    /**
     * Checks if cycle detection is enabled.
     *
     * @return true if cycle detection is enabled
     */
    public boolean isDetectCycles() {
        return detectCycles;
    }

    /**
     * Checks if cycles are allowed in the workflow.
     *
     * @return true if cycles are allowed
     */
    public boolean isAllowCycles() {
        return allowCycles;
    }

    /**
     * Creates a default configuration.
     *
     * @return The default configuration
     */
    public static WorkflowConfiguration defaultConfiguration() {
        return new Builder()
                .maxExecutionSteps(1000)
                .maxExecutionTime(Duration.ofMinutes(5))
                .detectCycles(true)
                .allowCycles(false)
                .build();
    }

    /**
     * Creates a new builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating WorkflowConfiguration instances.
     */
    public static class Builder {
        private int maxExecutionSteps = 1000;
        private Duration maxExecutionTime = Duration.ofMinutes(5);
        private boolean detectCycles = true;
        private boolean allowCycles = false;

        /**
         * Sets the maximum execution steps.
         *
         * @param maxExecutionSteps The maximum number of steps
         * @return This builder instance
         */
        public Builder maxExecutionSteps(int maxExecutionSteps) {
            this.maxExecutionSteps = maxExecutionSteps;
            return this;
        }

        /**
         * Sets the maximum execution time.
         *
         * @param maxExecutionTime The maximum execution time
         * @return This builder instance
         */
        public Builder maxExecutionTime(Duration maxExecutionTime) {
            this.maxExecutionTime = Objects.requireNonNull(maxExecutionTime, "Maximum execution time cannot be null");
            return this;
        }

        /**
         * Sets whether to detect cycles.
         *
         * @param detectCycles true to enable cycle detection
         * @return This builder instance
         */
        public Builder detectCycles(boolean detectCycles) {
            this.detectCycles = detectCycles;
            return this;
        }

        /**
         * Sets whether to allow cycles.
         *
         * @param allowCycles true to allow cycles
         * @return This builder instance
         */
        public Builder allowCycles(boolean allowCycles) {
            this.allowCycles = allowCycles;
            return this;
        }

        /**
         * Builds a new WorkflowConfiguration.
         *
         * @return The built configuration
         */
        public WorkflowConfiguration build() {
            return new WorkflowConfiguration(this);
        }
    }
}