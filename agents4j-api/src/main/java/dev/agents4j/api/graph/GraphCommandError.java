package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to indicate an error occurred during graph execution.
 */
public record GraphCommandError<S>(
    WorkflowError error,
    Optional<NodeId> fallbackNode,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData,
    boolean isRecoverable
) implements GraphCommand<S> {
    
    public GraphCommandError {
        Objects.requireNonNull(error, "Error cannot be null");
        Objects.requireNonNull(
            fallbackNode,
            "Fallback node optional cannot be null"
        );
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(
            stateData,
            "State data optional cannot be null"
        );
    }

    public static <S> GraphCommandError<S> of(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            error.isRecoverable()
        );
    }

    public static <S> GraphCommandError<S> withFallback(
        WorkflowError error,
        NodeId fallbackNode
    ) {
        return new GraphCommandError<>(
            error,
            Optional.of(fallbackNode),
            Optional.empty(),
            Optional.empty(),
            true
        );
    }

    public static <S> GraphCommandError<S> recoverable(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true
        );
    }

    public static <S> GraphCommandError<S> fatal(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false
        );
    }

    public static <S> GraphCommandError<S> withContext(
        WorkflowError error,
        WorkflowContext context
    ) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.of(context),
            Optional.empty(),
            error.isRecoverable()
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