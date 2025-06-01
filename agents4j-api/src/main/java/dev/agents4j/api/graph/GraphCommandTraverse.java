package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.util.Objects;
import java.util.Optional;

/**
 * Command to traverse to a specific node in the graph.
 */
public record GraphCommandTraverse<S>(
    NodeId targetNode,
    Optional<WorkflowContext> contextUpdates,
    Optional<S> stateData,
    Optional<EdgeCondition> condition,
    String reason
) implements GraphCommand<S> {
    
    public GraphCommandTraverse {
        Objects.requireNonNull(targetNode, "Target node cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates optional cannot be null"
        );
        Objects.requireNonNull(
            stateData,
            "State data optional cannot be null"
        );
        Objects.requireNonNull(
            condition,
            "Condition optional cannot be null"
        );
        reason = reason != null
            ? reason
            : "Traverse to " + targetNode.value();
    }

    public static <S> GraphCommandTraverse<S> to(NodeId nodeId) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandTraverse<S> toWithContext(
        NodeId nodeId,
        WorkflowContext context
    ) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.of(context),
            Optional.empty(),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandTraverse<S> toWithData(NodeId nodeId, S stateData) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.empty(),
            Optional.of(stateData),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandTraverse<S> toWithCondition(
        NodeId nodeId,
        EdgeCondition condition
    ) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.empty(),
            Optional.empty(),
            Optional.of(condition),
            null
        );
    }

    public static <S> GraphCommandTraverse<S> toWithUpdates(
        NodeId nodeId,
        WorkflowContext context,
        S stateData
    ) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.of(context),
            Optional.of(stateData),
            Optional.empty(),
            null
        );
    }

    public static <S> GraphCommandTraverse<S> toWithReason(
        NodeId nodeId,
        String reason
    ) {
        return new GraphCommandTraverse<>(
            nodeId,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
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