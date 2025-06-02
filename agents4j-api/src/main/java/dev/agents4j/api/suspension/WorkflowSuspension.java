package dev.agents4j.api.suspension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Type-safe suspension information for workflows.
 * This interface provides compile-time type safety for suspended workflow states,
 * eliminating the need for unsafe casting.
 *
 * @param <S> The type of the suspended workflow state
 */
public interface WorkflowSuspension<S> {
    /**
     * Gets the unique suspension identifier.
     *
     * @return The suspension ID
     */
    String getSuspensionId();

    /**
     * Gets the suspended workflow state with full type safety.
     *
     * @return The suspended state
     */
    S getSuspendedState();

    /**
     * Gets the reason for suspension.
     *
     * @return The suspension reason
     */
    String getReason();

    /**
     * Gets the optional timeout for this suspension.
     *
     * @return Optional timeout duration
     */
    Optional<Duration> getTimeout();

    /**
     * Gets the timestamp when this suspension was created.
     *
     * @return The suspension timestamp
     */
    Instant getSuspendedAt();

    /**
     * Gets the workflow version at the time of suspension.
     *
     * @return The workflow version
     */
    String getWorkflowVersion();

    /**
     * Gets optional metadata associated with this suspension.
     *
     * @return Optional metadata
     */
    default Optional<String> getMetadata() {
        return Optional.empty();
    }

    /**
     * Checks if this suspension has expired based on timeout.
     *
     * @return true if the suspension has expired
     */
    default boolean isExpired() {
        return getTimeout()
            .map(timeout ->
                getSuspendedAt().plus(timeout).isBefore(Instant.now())
            )
            .orElse(false);
    }

    /**
     * Gets the remaining time before this suspension expires.
     *
     * @return Optional remaining duration, empty if no timeout
     */
    default Optional<Duration> getRemainingTime() {
        return getTimeout()
            .map(timeout -> {
                Duration elapsed = Duration.between(
                    getSuspendedAt(),
                    Instant.now()
                );
                Duration remaining = timeout.minus(elapsed);
                return remaining.isNegative() ? Duration.ZERO : remaining;
            });
    }

    /**
     * Creates a basic implementation of WorkflowSuspension.
     *
     * @param suspensionId The suspension ID
     * @param suspendedState The suspended state
     * @param reason The suspension reason
     * @param workflowVersion The workflow version
     * @param <S> The state type
     * @return A WorkflowSuspension implementation
     */
    static <S> WorkflowSuspension<S> of(
        String suspensionId,
        S suspendedState,
        String reason,
        String workflowVersion
    ) {
        return new BasicWorkflowSuspension<>(
            suspensionId,
            suspendedState,
            reason,
            Optional.empty(),
            Instant.now(),
            workflowVersion,
            Optional.empty()
        );
    }

    /**
     * Creates a WorkflowSuspension with timeout.
     *
     * @param suspensionId The suspension ID
     * @param suspendedState The suspended state
     * @param reason The suspension reason
     * @param timeout The suspension timeout
     * @param workflowVersion The workflow version
     * @param <S> The state type
     * @return A WorkflowSuspension implementation
     */
    static <S> WorkflowSuspension<S> withTimeout(
        String suspensionId,
        S suspendedState,
        String reason,
        Duration timeout,
        String workflowVersion
    ) {
        return new BasicWorkflowSuspension<>(
            suspensionId,
            suspendedState,
            reason,
            Optional.of(timeout),
            Instant.now(),
            workflowVersion,
            Optional.empty()
        );
    }

    /**
     * Creates a WorkflowSuspension with metadata.
     *
     * @param suspensionId The suspension ID
     * @param suspendedState The suspended state
     * @param reason The suspension reason
     * @param workflowVersion The workflow version
     * @param metadata Additional metadata
     * @param <S> The state type
     * @return A WorkflowSuspension implementation
     */
    static <S> WorkflowSuspension<S> withMetadata(
        String suspensionId,
        S suspendedState,
        String reason,
        String workflowVersion,
        String metadata
    ) {
        return new BasicWorkflowSuspension<>(
            suspensionId,
            suspendedState,
            reason,
            Optional.empty(),
            Instant.now(),
            workflowVersion,
            Optional.of(metadata)
        );
    }

    /**
     * Basic immutable implementation of WorkflowSuspension.
     */
    record BasicWorkflowSuspension<S>(
        String suspensionId,
        S suspendedState,
        String reason,
        Optional<Duration> timeout,
        Instant suspendedAt,
        String workflowVersion,
        Optional<String> metadata
    )
        implements WorkflowSuspension<S> {
        public BasicWorkflowSuspension {
            java.util.Objects.requireNonNull(
                suspensionId,
                "Suspension ID cannot be null"
            );
            java.util.Objects.requireNonNull(
                suspendedState,
                "Suspended state cannot be null"
            );
            java.util.Objects.requireNonNull(reason, "Reason cannot be null");
            java.util.Objects.requireNonNull(
                timeout,
                "Timeout optional cannot be null"
            );
            java.util.Objects.requireNonNull(
                suspendedAt,
                "Suspended at cannot be null"
            );
            java.util.Objects.requireNonNull(
                workflowVersion,
                "Workflow version cannot be null"
            );
            java.util.Objects.requireNonNull(
                metadata,
                "Metadata optional cannot be null"
            );
        }

        @Override
        public String getSuspensionId() {
            return suspensionId;
        }

        @Override
        public S getSuspendedState() {
            return suspendedState;
        }

        @Override
        public String getReason() {
            return reason;
        }

        @Override
        public Optional<Duration> getTimeout() {
            return timeout;
        }

        @Override
        public Instant getSuspendedAt() {
            return suspendedAt;
        }

        @Override
        public String getWorkflowVersion() {
            return workflowVersion;
        }

        @Override
        public Optional<String> getMetadata() {
            return metadata;
        }
    }
}
