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
) implements GraphCommand<S> {
    
    public GraphCommandSuspend {
        Objects.requireNonNull(
            suspensionId,
            "Suspension ID cannot be null"
        );
        Objects.requireNonNull(reason, "Suspension reason cannot be null");
        Objects.requireNonNull(timeout, "Timeout optional cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(
            stateData,
            "State data optional cannot be null"
        );

        if (suspensionId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Suspension ID cannot be empty"
            );
        }
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Suspension reason cannot be empty"
            );
        }
    }

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

    public static <S> GraphCommandSuspend<S> withTimeout(
        String suspensionId,
        String reason,
        Duration timeout
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            reason,
            Optional.of(timeout),
            Optional.empty(),
            Optional.empty()
        );
    }

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

    public static <S> GraphCommandSuspend<S> withData(
        String suspensionId,
        String reason,
        S stateData
    ) {
        return new GraphCommandSuspend<>(
            suspensionId,
            reason,
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