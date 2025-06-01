package dev.agents4j.api.graph;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Modern graph workflow state that provides type-safe context management
 * and graph-specific navigation capabilities. Replaces the unsafe Map<String, Object>
 * approach with compile-time type safety.
 *
 * @param <S> The type of the workflow state data
 */
public record GraphWorkflowState<S>(
    WorkflowId workflowId,
    S data,
    WorkflowContext context,
    Optional<NodeId> currentNode,
    GraphPosition position,
    StateMetadata metadata
) {
    public GraphWorkflowState {
        Objects.requireNonNull(workflowId, "Workflow ID cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        Objects.requireNonNull(
            currentNode,
            "Current node optional cannot be null"
        );
        Objects.requireNonNull(position, "Graph position cannot be null");
        Objects.requireNonNull(metadata, "State metadata cannot be null");
    }

    /**
     * Creates a new graph workflow state starting at the specified node.
     *
     * @param workflowId The workflow ID
     * @param initialData The initial state data
     * @param startNode The starting node ID
     * @param <S> The type of the state data
     * @return A new GraphWorkflowState
     */
    public static <S> GraphWorkflowState<S> create(
        WorkflowId workflowId,
        S initialData,
        NodeId startNode
    ) {
        Objects.requireNonNull(startNode, "Start node cannot be null");
        return new GraphWorkflowState<>(
            workflowId,
            initialData,
            dev.agents4j.api.context.ExecutionContext.empty(),
            Optional.of(startNode),
            GraphPosition.at(startNode),
            StateMetadata.initial()
        );
    }

    /**
     * Creates a new graph workflow state with initial context.
     *
     * @param workflowId The workflow ID
     * @param initialData The initial state data
     * @param startNode The starting node ID
     * @param initialContext The initial workflow context
     * @param <S> The type of the state data
     * @return A new GraphWorkflowState
     */
    public static <S> GraphWorkflowState<S> create(
        WorkflowId workflowId,
        S initialData,
        NodeId startNode,
        WorkflowContext initialContext
    ) {
        Objects.requireNonNull(startNode, "Start node cannot be null");
        Objects.requireNonNull(
            initialContext,
            "Initial context cannot be null"
        );
        return new GraphWorkflowState<>(
            workflowId,
            initialData,
            initialContext,
            Optional.of(startNode),
            GraphPosition.at(startNode),
            StateMetadata.initial()
        );
    }

    /**
     * Creates a new state with updated data.
     *
     * @param newData The new state data
     * @return A new GraphWorkflowState with updated data
     */
    public GraphWorkflowState<S> withData(S newData) {
        return new GraphWorkflowState<>(
            workflowId,
            newData,
            context,
            currentNode,
            position,
            metadata.advance()
        );
    }

    /**
     * Creates a new state with a context value added.
     *
     * @param key The typed context key
     * @param value The context value
     * @param <T> The type of the context value
     * @return A new GraphWorkflowState with updated context
     */
    public <T> GraphWorkflowState<S> withContext(ContextKey<T> key, T value) {
        return new GraphWorkflowState<>(
            workflowId,
            data,
            context.with(key, value),
            currentNode,
            position,
            metadata.advance()
        );
    }

    /**
     * Creates a new state with updated context.
     *
     * @param newContext The new workflow context
     * @return A new GraphWorkflowState with updated context
     */
    public GraphWorkflowState<S> withContext(WorkflowContext newContext) {
        Objects.requireNonNull(newContext, "New context cannot be null");
        return new GraphWorkflowState<>(
            workflowId,
            data,
            newContext,
            currentNode,
            position,
            metadata.advance()
        );
    }

    /**
     * Creates a new state by moving to the specified node.
     *
     * @param nodeId The target node ID
     * @return A new GraphWorkflowState at the target node
     */
    public GraphWorkflowState<S> moveToNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        return new GraphWorkflowState<>(
            workflowId,
            data,
            context,
            Optional.of(nodeId),
            position.moveTo(nodeId),
            metadata.advance()
        );
    }

    /**
     * Creates a new state by traversing an edge to the target node.
     *
     * @param edgeId The edge being traversed
     * @param targetNode The target node ID
     * @return A new GraphWorkflowState after edge traversal
     */
    public GraphWorkflowState<S> traverseEdge(
        EdgeId edgeId,
        NodeId targetNode
    ) {
        Objects.requireNonNull(edgeId, "Edge ID cannot be null");
        Objects.requireNonNull(targetNode, "Target node cannot be null");
        return new GraphWorkflowState<>(
            workflowId,
            data,
            context,
            Optional.of(targetNode),
            position.traverseEdge(edgeId, targetNode),
            metadata.advance()
        );
    }

    /**
     * Creates a new state with data, context, and node updates all applied.
     *
     * @param newData The new state data
     * @param contextUpdates The context to merge with current context
     * @param nodeId The new current node ID
     * @return A new GraphWorkflowState with all updates applied
     */
    public GraphWorkflowState<S> withDataContextAndNode(
        S newData,
        WorkflowContext contextUpdates,
        NodeId nodeId
    ) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        Objects.requireNonNull(
            contextUpdates,
            "Context updates cannot be null"
        );

        return new GraphWorkflowState<>(
            workflowId,
            newData,
            context.merge(contextUpdates),
            Optional.of(nodeId),
            position.moveTo(nodeId),
            metadata.advance()
        );
    }

    /**
     * Gets a typed context value.
     *
     * @param key The typed context key
     * @param <T> The type of the context value
     * @return Optional containing the context value if present and of correct type
     */
    public <T> Optional<T> getContext(ContextKey<T> key) {
        return context.get(key);
    }

    /**
     * Gets a typed context value with a default.
     *
     * @param key The typed context key
     * @param defaultValue The default value if key not found
     * @param <T> The type of the context value
     * @return The context value or default
     */
    public <T> T getContextOrDefault(ContextKey<T> key, T defaultValue) {
        return context.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if a context key exists.
     *
     * @param key The context key to check
     * @return true if the key exists in the context
     */
    public boolean hasContext(ContextKey<?> key) {
        return context.contains(key);
    }

    /**
     * Checks if the workflow has visited the current node before (cycle detection).
     *
     * @return true if the current node creates a cycle
     */
    public boolean hasCycle() {
        return position.hasCycle();
    }

    /**
     * Checks if a specific node has been visited.
     *
     * @param nodeId The node ID to check
     * @return true if the node has been visited
     */
    public boolean hasVisited(NodeId nodeId) {
        return position.hasVisited(nodeId);
    }

    /**
     * Gets the complete path taken through the graph.
     *
     * @return An immutable list of node IDs representing the path
     */
    public List<NodeId> getPath() {
        return position.getPath();
    }

    /**
     * Gets the current depth in the graph traversal.
     *
     * @return The current traversal depth
     */
    public int getDepth() {
        return position.depth();
    }

    /**
     * Gets the workflow version.
     *
     * @return The current version
     */
    public long getVersion() {
        return metadata.version();
    }

    /**
     * Gets the last modified timestamp.
     *
     * @return The last modified time
     */
    public Instant getLastModified() {
        return metadata.lastModified();
    }

    /**
     * Gets the creation timestamp.
     *
     * @return The creation time
     */
    public Instant getCreatedAt() {
        return metadata.createdAt();
    }

    /**
     * Gets the previous node ID if available.
     *
     * @return Optional containing the previous node ID
     */
    public Optional<NodeId> getPreviousNode() {
        return position.previousNodeId();
    }

    /**
     * Gets the path as a string representation.
     *
     * @return String representation of the traversal path
     */
    public String getPathString() {
        return position.getPathString();
    }

    /**
     * Creates a new state by resetting to a specific node (e.g., for error recovery).
     *
     * @param nodeId The node to reset to
     * @return A new GraphWorkflowState reset to the specified node
     */
    public GraphWorkflowState<S> resetToNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        return new GraphWorkflowState<>(
            workflowId,
            data,
            context,
            Optional.of(nodeId),
            position.resetTo(nodeId),
            metadata.advance()
        );
    }

    /**
     * Gets the age of the workflow state.
     *
     * @return Duration since creation
     */
    public java.time.Duration getAge() {
        return metadata.getAge();
    }

    /**
     * Gets the time since last modification.
     *
     * @return Duration since last modification
     */
    public java.time.Duration getTimeSinceLastModified() {
        return metadata.getTimeSinceLastModified();
    }

    @Override
    public String toString() {
        return String.format(
            "GraphWorkflowState{id=%s, currentNode=%s, depth=%d, version=%d, contextSize=%d}",
            workflowId.value(),
            currentNode.map(NodeId::value).orElse("none"),
            getDepth(),
            metadata.version(),
            context.size()
        );
    }
}
