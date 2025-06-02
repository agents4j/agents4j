package dev.agents4j.api.suspension;

import dev.agents4j.api.context.ContextMergeStrategy;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration options for workflow resumption operations.
 * Provides control over validation, context merging, timeouts, and error handling
 * during workflow resume operations.
 */
public final class ResumeOptions {

    private final boolean validateState;
    private final boolean allowVersionMismatch;
    private final ContextMergeStrategy contextMergeStrategy;
    private final Optional<Duration> timeout;
    private final boolean failOnConflicts;
    private final boolean enableMigration;
    private final Optional<String> targetVersion;

    private ResumeOptions(Builder builder) {
        this.validateState = builder.validateState;
        this.allowVersionMismatch = builder.allowVersionMismatch;
        this.contextMergeStrategy = builder.contextMergeStrategy;
        this.timeout = Optional.ofNullable(builder.timeout);
        this.failOnConflicts = builder.failOnConflicts;
        this.enableMigration = builder.enableMigration;
        this.targetVersion = Optional.ofNullable(builder.targetVersion);
    }

    /**
     * Whether to validate the state before resuming.
     *
     * @return true if state validation should be performed
     */
    public boolean shouldValidateState() {
        return validateState;
    }

    /**
     * Whether to allow resumption when workflow versions don't match.
     *
     * @return true if version mismatches are allowed
     */
    public boolean shouldAllowVersionMismatch() {
        return allowVersionMismatch;
    }

    /**
     * Gets the context merge strategy to use.
     *
     * @return The context merge strategy
     */
    public ContextMergeStrategy getContextMergeStrategy() {
        return contextMergeStrategy;
    }

    /**
     * Gets the optional timeout for resume operations.
     *
     * @return Optional timeout duration
     */
    public Optional<Duration> getTimeout() {
        return timeout;
    }

    /**
     * Whether to fail immediately on context conflicts.
     *
     * @return true if conflicts should cause immediate failure
     */
    public boolean shouldFailOnConflicts() {
        return failOnConflicts;
    }

    /**
     * Whether to enable automatic state migration.
     *
     * @return true if migration should be attempted
     */
    public boolean shouldEnableMigration() {
        return enableMigration;
    }

    /**
     * Gets the optional target version for migration.
     *
     * @return Optional target version
     */
    public Optional<String> getTargetVersion() {
        return targetVersion;
    }

    /**
     * Creates a new builder for ResumeOptions.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates ResumeOptions with default safe settings.
     *
     * @return ResumeOptions with safe defaults
     */
    public static ResumeOptions safe() {
        return builder()
            .validateState(true)
            .allowVersionMismatch(false)
            .contextMergeStrategy(ContextMergeStrategy.MERGE_SAFE)
            .failOnConflicts(true)
            .enableMigration(false)
            .timeout(Duration.ofMinutes(5))
            .build();
    }

    /**
     * Creates ResumeOptions with permissive settings.
     *
     * @return ResumeOptions with permissive settings
     */
    public static ResumeOptions permissive() {
        return builder()
            .validateState(false)
            .allowVersionMismatch(true)
            .contextMergeStrategy(ContextMergeStrategy.MERGE_LATEST)
            .failOnConflicts(false)
            .enableMigration(true)
            .build();
    }

    /**
     * Creates ResumeOptions for development/testing.
     *
     * @return ResumeOptions suitable for development
     */
    public static ResumeOptions development() {
        return builder()
            .validateState(true)
            .allowVersionMismatch(true)
            .contextMergeStrategy(ContextMergeStrategy.RESUME_WINS)
            .failOnConflicts(false)
            .enableMigration(true)
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Builder for creating ResumeOptions instances.
     */
    public static final class Builder {

        private boolean validateState = true;
        private boolean allowVersionMismatch = false;
        private ContextMergeStrategy contextMergeStrategy =
            ContextMergeStrategy.MERGE_SAFE;
        private Duration timeout;
        private boolean failOnConflicts = true;
        private boolean enableMigration = false;
        private String targetVersion;

        private Builder() {}

        /**
         * Sets whether to validate state before resuming.
         *
         * @param validateState true to enable state validation
         * @return this builder
         */
        public Builder validateState(boolean validateState) {
            this.validateState = validateState;
            return this;
        }

        /**
         * Sets whether to allow version mismatches.
         *
         * @param allowVersionMismatch true to allow version mismatches
         * @return this builder
         */
        public Builder allowVersionMismatch(boolean allowVersionMismatch) {
            this.allowVersionMismatch = allowVersionMismatch;
            return this;
        }

        /**
         * Sets the context merge strategy.
         *
         * @param contextMergeStrategy the merge strategy to use
         * @return this builder
         */
        public Builder contextMergeStrategy(
            ContextMergeStrategy contextMergeStrategy
        ) {
            this.contextMergeStrategy = Objects.requireNonNull(
                contextMergeStrategy,
                "Context merge strategy cannot be null"
            );
            return this;
        }

        /**
         * Sets the timeout for resume operations.
         *
         * @param timeout the timeout duration
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets whether to fail on context conflicts.
         *
         * @param failOnConflicts true to fail on conflicts
         * @return this builder
         */
        public Builder failOnConflicts(boolean failOnConflicts) {
            this.failOnConflicts = failOnConflicts;
            return this;
        }

        /**
         * Sets whether to enable automatic migration.
         *
         * @param enableMigration true to enable migration
         * @return this builder
         */
        public Builder enableMigration(boolean enableMigration) {
            this.enableMigration = enableMigration;
            return this;
        }

        /**
         * Sets the target version for migration.
         *
         * @param targetVersion the target version
         * @return this builder
         */
        public Builder targetVersion(String targetVersion) {
            this.targetVersion = targetVersion;
            return this;
        }

        /**
         * Builds the ResumeOptions instance.
         *
         * @return a new ResumeOptions instance
         */
        public ResumeOptions build() {
            return new ResumeOptions(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResumeOptions that = (ResumeOptions) obj;
        return (
            validateState == that.validateState &&
            allowVersionMismatch == that.allowVersionMismatch &&
            failOnConflicts == that.failOnConflicts &&
            enableMigration == that.enableMigration &&
            contextMergeStrategy == that.contextMergeStrategy &&
            Objects.equals(timeout, that.timeout) &&
            Objects.equals(targetVersion, that.targetVersion)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            validateState,
            allowVersionMismatch,
            contextMergeStrategy,
            timeout,
            failOnConflicts,
            enableMigration,
            targetVersion
        );
    }

    @Override
    public String toString() {
        return (
            "ResumeOptions{" +
            "validateState=" +
            validateState +
            ", allowVersionMismatch=" +
            allowVersionMismatch +
            ", contextMergeStrategy=" +
            contextMergeStrategy +
            ", timeout=" +
            timeout +
            ", failOnConflicts=" +
            failOnConflicts +
            ", enableMigration=" +
            enableMigration +
            ", targetVersion=" +
            targetVersion +
            '}'
        );
    }
}
