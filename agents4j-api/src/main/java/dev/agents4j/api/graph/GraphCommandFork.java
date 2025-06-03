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
    
    /**
     * Creates a new graph command fork with validation.
     * 
     * @param targetNodes the set of nodes to fork to
     * @param strategy the fork execution strategy
     * @param contextUpdates optional context updates to apply
     * @param stateData optional state data updates
     * @param reason optional reason for the fork
     * @throws NullPointerException if any required parameter is null
     */
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

    /**
     * Strategy for executing forked branches.
     */
    public enum ForkStrategy {
        /** Execute all branches in parallel */
        PARALLEL, // Execute all branches in parallel
        /** Execute branches one after another */
        SEQUENTIAL, // Execute branches one after another
        /** Execute based on conditions */
        CONDITIONAL, // Execute based on conditions
    }

    /**
     * Creates a parallel fork command.
     * 
     * @param <S> the state type
     * @param nodes the nodes to execute in parallel
     * @return a new GraphCommandFork instance
     */
    public static <S> GraphCommandFork<S> parallel(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.PARALLEL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a sequential fork command.
     * 
     * @param <S> the state type
     * @param nodes the nodes to execute sequentially
     * @return a new GraphCommandFork instance
     */
    public static <S> GraphCommandFork<S> sequential(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.SEQUENTIAL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a conditional fork command.
     * 
     * @param <S> the state type
     * @param nodes the nodes to execute conditionally
     * @return a new GraphCommandFork instance
     */
    public static <S> GraphCommandFork<S> conditional(Set<NodeId> nodes) {
        return new GraphCommandFork<>(
            nodes,
            ForkStrategy.CONDITIONAL,
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a parallel fork command with context updates.
     * 
     * @param <S> the state type
     * @param nodes the nodes to execute in parallel
     * @param context the context updates to apply
     * @return a new GraphCommandFork instance
     */
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