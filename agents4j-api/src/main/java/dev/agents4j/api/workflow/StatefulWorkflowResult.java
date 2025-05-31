package dev.agents4j.api.workflow;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of a stateful workflow execution.
 * Contains the output (if completed), the current state, and execution status.
 *
 * @param <S> The type of the workflow state data
 * @param <O> The output type of the workflow
 */
public class StatefulWorkflowResult<S, O> {

    public enum Status {
        COMPLETED, // Workflow completed successfully
        SUSPENDED, // Workflow was suspended and can be resumed
        ERROR, // Workflow encountered an error
    }

    private final Status status;
    private final O output;
    private final GraphWorkflowState<S> state;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    private StatefulWorkflowResult(
        Status status,
        O output,
        GraphWorkflowState<S> state,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.output = output;
        this.state = Objects.requireNonNull(state, "State cannot be null");
        this.errorMessage = errorMessage;
        this.metadata = Collections.unmodifiableMap(
            metadata != null ? metadata : Collections.emptyMap()
        );
    }

    /**
     * Gets the execution status.
     *
     * @return The status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Gets the workflow output (if completed).
     *
     * @return The output wrapped in Optional
     */
    public Optional<O> getOutput() {
        return Optional.ofNullable(output);
    }

    /**
     * Gets the current workflow state.
     *
     * @return The workflow state
     */
    public GraphWorkflowState<S> getState() {
        return state;
    }

    /**
     * Gets the error message (if status is ERROR).
     *
     * @return The error message wrapped in Optional
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Gets the execution metadata.
     *
     * @return Unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Checks if the workflow completed successfully.
     *
     * @return true if completed
     */
    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    /**
     * Checks if the workflow was suspended.
     *
     * @return true if suspended
     */
    public boolean isSuspended() {
        return status == Status.SUSPENDED;
    }

    /**
     * Checks if the workflow encountered an error.
     *
     * @return true if error occurred
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * Creates a completed result.
     *
     * @param output The workflow output
     * @param state The final state
     * @param <S> The state type
     * @param <O> The output type
     * @return A completed result
     */
    public static <S, O> StatefulWorkflowResult<S, O> completed(
        O output,
        GraphWorkflowState<S> state
    ) {
        return new StatefulWorkflowResult<>(
            Status.COMPLETED,
            output,
            state,
            null,
            null
        );
    }

    /**
     * Creates a suspended result.
     *
     * @param state The current state
     * @param <S> The state type
     * @param <O> The output type
     * @return A suspended result
     */
    public static <S, O> StatefulWorkflowResult<S, O> suspended(
        GraphWorkflowState<S> state
    ) {
        return new StatefulWorkflowResult<>(
            Status.SUSPENDED,
            null,
            state,
            null,
            null
        );
    }

    /**
     * Creates an error result.
     *
     * @param errorMessage The error message
     * @param state The current state
     * @param <S> The state type
     * @param <O> The output type
     * @return An error result
     */
    public static <S, O> StatefulWorkflowResult<S, O> error(
        String errorMessage,
        GraphWorkflowState<S> state
    ) {
        return new StatefulWorkflowResult<>(
            Status.ERROR,
            null,
            state,
            errorMessage,
            null
        );
    }

    /**
     * Creates a result with metadata.
     *
     * @param status The status
     * @param output The output (can be null)
     * @param state The state
     * @param errorMessage The error message (can be null)
     * @param metadata The metadata
     * @param <S> The state type
     * @param <O> The output type
     * @return A result with metadata
     */
    public static <S, O> StatefulWorkflowResult<S, O> withMetadata(
        Status status,
        O output,
        GraphWorkflowState<S> state,
        String errorMessage,
        Map<String, Object> metadata
    ) {
        return new StatefulWorkflowResult<>(
            status,
            output,
            state,
            errorMessage,
            metadata
        );
    }

    @Override
    public String toString() {
        return String.format(
            "StatefulWorkflowResult{status=%s, hasOutput=%s, state=%s}",
            status,
            output != null,
            state
        );
    }
}
