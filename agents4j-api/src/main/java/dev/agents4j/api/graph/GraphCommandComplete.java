package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to complete the workflow execution.
 */
public record GraphCommandComplete<S>(
    Object result,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData,
    String reason
) implements GraphCommand<S> {
    
    /**
     * Creates a new graph command complete with validation.
     * 
     * @param result the result of the command execution
     * @param contextUpdates optional context updates to apply
     * @param stateData optional state data updates
     * @throws NullPointerException if any required parameter is null
     */
    public GraphCommandComplete {
        Objects.requireNonNull(result, "Result cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(
            stateData,
            "State data optional cannot be null"
        );
        reason = reason != null
            ? reason
            : "Workflow completed successfully";
    }

    /**
     * Creates a command to complete workflow execution with a result.
     * 
     * @param <S> the state type
     * @param result the result to return
     * @return a new GraphCommandComplete instance
     */
    public static <S> GraphCommandComplete<S> withResult(Object result) {
        return new GraphCommandComplete<>(
            result,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandComplete<S> withResultAndContext(
        Object result,
        WorkflowContext context
    ) {
        return new GraphCommandComplete<>(
            result,
            Optional.of(context),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandComplete<S> withResultAndData(
        Object result,
        S stateData
    ) {
        return new GraphCommandComplete<>(
            result,
            Optional.empty(),
            Optional.of(stateData),
            null
        );
    }

    public static <S> GraphCommandComplete<S> withAll(
        Object result,
        WorkflowContext context,
        S stateData,
        String reason
    ) {
        return new GraphCommandComplete<>(
            result,
            Optional.of(context),
            Optional.of(stateData),
            reason
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