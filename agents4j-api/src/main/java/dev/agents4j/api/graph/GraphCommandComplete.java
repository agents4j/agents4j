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