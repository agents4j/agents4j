package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to suspend execution at the current node.
 */
public record GraphCommandSuspend<S>(
    String suspensionId,
    String reason,
    Optional<Duration> timeout,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData
)
    implements GraphCommand<S> {
    /**
     * Creates a new graph command suspend with validation.
     *
     * @param suspensionId the unique identifier for this suspension
     * @param contextUpdates optional context updates to apply
     * @param stateData optional state data updates
     * @param timeout optional timeout duration
     * @param reason optional reason for the suspension
     * @throws NullPointerException if any required parameter is null
     */
    public GraphCommandSuspend {
        Objects.requireNonNull(suspensionId, "Suspension ID cannot be null");
        Objects.requireNonNull(reason, "Suspension reason cannot be null");
        Objects.requireNonNull(timeout, "Timeout optional cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(stateData, "State data optional cannot be null");

        if (suspensionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Suspension ID cannot be empty");
        }
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Suspension reason cannot be empty"
            );
        }
    }

    /**
     * Creates a suspend command with an ID and reason.
     *
     * @param <S> the state type
     * @param suspensionId the unique identifier for this suspension
     * @param reason the reason for suspension
     * @return a new GraphCommandSuspend instance
     */
    public static <S> GraphCommandSuspend<S> withId(
        String suspensionId,
        String reason
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            reason,
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    /**
     * Creates a suspend command with a timeout.
     *
     * @param <S> the state type
     * @param suspensionId the unique identifier for this suspension
     * @param timeout the timeout duration
     * @return a new GraphCommandSuspend instance
     */
    public static <S> GraphCommandSuspend<S> withTimeout(
        String suspensionId,
        Duration timeout
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            "Suspended with timeout: " + timeout,
            Optional.of(timeout),
            Optional.empty(),
            Optional.empty()
        );
    }

    /**
     * Creates a suspend command with context updates.
     *
     * @param <S> the state type
     * @param suspensionId the unique identifier for this suspension
     * @param context the context updates to apply
     * @return a new GraphCommandSuspend instance
     */
    public static <S> GraphCommandSuspend<S> withContext(
        String suspensionId,
        WorkflowContext context
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            "Suspended with context updates",
            Optional.empty(),
            Optional.of(context),
            Optional.empty()
        );
    }

    /**
     * Creates a suspend command with context updates.
     *
     * @param <S> the state type
     * @param suspensionId the unique identifier for this suspension
     * @param reason the reason for the suspension
     * @param context the context updates to apply
     * @return a new GraphCommandSuspend instance
     */
    public static <S> GraphCommandSuspend<S> withContext(
        String suspensionId,
        String reason,
        WorkflowContext context
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            reason,
            Optional.empty(),
            Optional.of(context),
            Optional.empty()
        );
    }

    /**
     * Creates a suspend command with state data.
     *
     * @param <S> the state type
     * @param suspensionId the unique identifier for this suspension
     * @param stateData the state data to include
     * @return a new GraphCommandSuspend instance
     */
    public static <S> GraphCommandSuspend<S> withData(
        String suspensionId,
        S stateData
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            "Suspended with state data",
            Optional.empty(),
            Optional.empty(),
            Optional.of(stateData)
        );
    }

    @Override
    public Optional<WorkflowContext> getContextUpdates() {
        return contextUpdates;
    }

    @Override
    public Optional<S> getStateData() {
        return stateData;
    }
}
