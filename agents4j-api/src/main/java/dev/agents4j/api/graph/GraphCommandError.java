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
)
    implements GraphCommand<S> {
    /**
     * Creates a new graph command error with validation.
     *
     * @param error the workflow error that occurred
     * @param fallbackNode optional fallback node to navigate to
     * @param contextUpdates optional context updates to apply
     * @param stateData optional state data updates
     * @param isRecoverable whether this error is recoverable
     * @throws NullPointerException if any required parameter is null
     */
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
        Objects.requireNonNull(stateData, "State data optional cannot be null");
    }

    /**
     * Creates a basic error command with just the error.
     *
     * @param <S> the state type
     * @param error the workflow error that occurred
     * @return a new GraphCommandError instance
     */
    public static <S> GraphCommandError<S> of(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            error.isRecoverable()
        );
    }

    /**
     * Creates an error command with a fallback node.
     *
     * @param <S> the state type
     * @param error the workflow error that occurred
     * @param fallbackNode the node to navigate to on error
     * @return a new GraphCommandError instance
     */
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

    /**
     * Creates a recoverable error command.
     *
     * @param <S> the state type
     * @param error the workflow error that occurred
     * @return a new recoverable GraphCommandError instance
     */
    public static <S> GraphCommandError<S> recoverable(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            true
        );
    }

    /**
     * Creates a fatal error command.
     *
     * @param <S> the state type
     * @param error the workflow error that occurred
     * @return a new fatal GraphCommandError instance
     */
    public static <S> GraphCommandError<S> fatal(WorkflowError error) {
        return new GraphCommandError<>(
            error,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false
        );
    }

    /**
     * Creates an error command with context updates.
     *
     * @param <S> the state type
     * @param error the workflow error that occurred
     * @param context the context updates to apply
     * @return a new GraphCommandError instance
     */
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
