package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Command to fork execution to multiple parallel nodes.
 */
public record GraphCommandFork<S>(
    Set<NodeId> targetNodes,
    ForkStrategy strategy,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData,
    String reason
) implements GraphCommand<S> {
    
    public GraphCommandFork {
        Objects.requireNonNull(targetNodes, "Target nodes cannot be null");
        Objects.requireNonNull(strategy, "Fork strategy cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(
            stateData,
            "State data optional cannot be null"
        );

        if (targetNodes.isEmpty()) {
            throw new IllegalArgumentException(
                "Target nodes cannot be empty"
            );
        }

        reason = reason != null
            ? reason
            : "Fork to " + targetNodes.size() + " nodes";

        // Make target nodes immutable
        targetNodes = Set.copyOf(targetNodes);
    }

    public enum ForkStrategy {
        PARALLEL, // Execute all branches in parallel
        SEQUENTIAL, // Execute branches one after another
        CONDITIONAL, // Execute based on conditions
    }

    public static <S> GraphCommandFork<S> parallel(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.PARALLEL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandFork<S> sequential(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.SEQUENTIAL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandFork<S> conditional(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.CONDITIONAL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandFork<S> parallelWithContext(
        Set<NodeId> nodes,
        WorkflowContext context
    ) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.PARALLEL,
            Optional.of(context),
            Optional.empty(),
            null
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