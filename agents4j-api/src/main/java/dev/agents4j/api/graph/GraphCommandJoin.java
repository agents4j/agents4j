package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to join multiple parallel executions.
 */
public record GraphCommandJoin<S>(
    NodeId joinNode,
    JoinStrategy strategy,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData,
    Optional<Duration> timeout,
    String reason
)
    implements GraphCommand<S> {
    /**
     * Creates a new graph command join with validation.
     *
     * @param joinNode the node to join to
     * @param strategy the join execution strategy
     * @param contextUpdates optional context updates to apply
     * @param stateData optional state data updates
     * @param timeout optional timeout in milliseconds
     * @param reason optional reason for the join
     * @throws NullPointerException if any required parameter is null
     */
    public GraphCommandJoin {
        Objects.requireNonNull(joinNode, "Join node cannot be null");
        Objects.requireNonNull(strategy, "Join strategy cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(stateData, "State data optional cannot be null");
        Objects.requireNonNull(timeout, "Timeout optional cannot be null");
        reason = reason != null ? reason : "Join at " + joinNode.value();
    }

    /**
     * Strategy for joining forked branches.
     */
    public enum JoinStrategy {
        /** Wait for all branches to complete */
        WAIT_ALL, // Wait for all branches to complete
        /** Proceed when any branch completes */
        WAIT_ANY, // Proceed when any branch completes
        /** Proceed when majority complete */
        WAIT_MAJORITY, // Proceed when majority complete
    }

    /**
     * Creates a join command that waits for all branches.
     *
     * @param <S> the state type
     * @param joinNode the node to join to
     * @return a new GraphCommandJoin instance
     */
    public static <S> GraphCommandJoin<S> waitAll(NodeId joinNode) {
        return new GraphCommandJoin<>(
            joinNode,
            JoinStrategy.WAIT_ALL,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a join command that waits for any branch.
     *
     * @param <S> the state type
     * @param joinNode the node to join to
     * @return a new GraphCommandJoin instance
     */
    public static <S> GraphCommandJoin<S> waitAny(NodeId joinNode) {
        return new GraphCommandJoin<>(
            joinNode,
            JoinStrategy.WAIT_ANY,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a join command that waits for majority of branches.
     *
     * @param <S> the state type
     * @param joinNode the node to join to
     * @return a new GraphCommandJoin instance
     */
    public static <S> GraphCommandJoin<S> waitMajority(NodeId joinNode) {
        return new GraphCommandJoin<>(
            joinNode,
            JoinStrategy.WAIT_MAJORITY,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    /**
     * Creates a join command that waits for all branches with timeout.
     *
     * @param <S> the state type
     * @param joinNode the node to join to
     * @param timeout timeout in milliseconds
     * @return a new GraphCommandJoin instance
     */
    public static <S> GraphCommandJoin<S> waitAllWithTimeout(
        NodeId joinNode,
        Duration timeout
    ) {
        return new GraphCommandJoin<>(
            joinNode,
            JoinStrategy.WAIT_ALL,
            Optional.empty(),
            Optional.empty(),
            Optional.of(timeout),
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
