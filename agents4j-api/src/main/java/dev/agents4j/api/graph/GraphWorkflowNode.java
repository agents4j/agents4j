package dev.agents4j.api.graph;

import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for nodes in a graph workflow that provides type-safe processing
 * with modern error handling and graph navigation capabilities.
 *
 * @param <S> The type of the workflow state data
 */
public interface GraphWorkflowNode<S> {
    /**
     * Process this node with the current graph state.
     * Returns commands for graph navigation and state updates.
     *
     * @param state The current graph workflow state
     * @return A WorkflowResult containing graph commands or errors
     */
    WorkflowResult<GraphCommand<S>, WorkflowError> process(
        GraphWorkflowState<S> state
    );

    /**
     * Get the unique node identifier.
     *
     * @return The node ID
     */
    NodeId getNodeId();

    /**
     * Get the human-readable name of this node.
     *
     * @return The node name
     */
    String getName();

    /**
     * Get the description of what this node does.
     *
     * @return The node description
     */
    default String getDescription() {
        return "Graph workflow node: " + getName();
    }

    /**
     * Get node metadata for graph visualization and analysis.
     *
     * @return The node metadata
     */
    default GraphNodeMetadata getMetadata() {
        return GraphNodeMetadata.of(
            getNodeId(),
            getName(),
            getDescription(),
            GraphNodeMetadata.NodeType.TASK
        );
    }

    /**
     * Get outgoing edges from this node.
     * These define the possible transitions from this node.
     *
     * @return Set of outgoing edges
     */
    default Set<GraphEdge> getOutgoingEdges() {
        return Collections.emptySet();
    }

    /**
     * Get incoming edges to this node.
     * These are typically provided by the graph structure.
     *
     * @return Set of incoming edges
     */
    default Set<GraphEdge> getIncomingEdges() {
        return Collections.emptySet();
    }

    /**
     * Validate that this node can process the given state.
     * Allows for pre-processing validation and early error detection.
     *
     * @param state The state to validate
     * @return A WorkflowResult indicating validation success or failure
     */
    default WorkflowResult<GraphWorkflowState<S>, WorkflowError> validateState(
        GraphWorkflowState<S> state
    ) {
        Objects.requireNonNull(state, "State cannot be null");
        return WorkflowResult.success(state);
    }

    /**
     * Check if this node can be an entry point to the graph.
     *
     * @return true if this node can be an entry point
     */
    default boolean isEntryPoint() {
        return getIncomingEdges().isEmpty();
    }

    /**
     * Check if this node is an exit point from the graph.
     *
     * @return true if this node is an exit point
     */
    default boolean isExitPoint() {
        return getOutgoingEdges().isEmpty();
    }

    /**
     * Check if this node can suspend the workflow.
     *
     * @return true if this node can suspend execution
     */
    default boolean canSuspend() {
        return true;
    }

    /**
     * Get the expected execution duration for this node.
     * Used for planning and timeout configuration.
     *
     * @return Optional containing the expected duration
     */
    default Optional<Duration> getExpectedDuration() {
        return Optional.empty();
    }

    /**
     * Get the set of node IDs that this node can directly transition to.
     * Empty set means no restrictions (can go anywhere based on graph structure).
     *
     * @return Set of allowed target node IDs
     */
    default Set<NodeId> getAllowedTargets() {
        return getOutgoingEdges()
            .stream()
            .map(GraphEdge::toNode)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Called before the node is executed.
     * Can be used for setup, logging, or preparation.
     *
     * @param state The current state
     */
    default void onBeforeExecution(GraphWorkflowState<S> state) {
        // Default: no action
    }

    /**
     * Called after the node is executed successfully.
     * Can be used for cleanup, logging, or post-processing.
     *
     * @param state The current state
     * @param command The resulting command
     */
    default void onAfterExecution(
        GraphWorkflowState<S> state,
        GraphCommand<S> command
    ) {
        // Default: no action
    }

    /**
     * Called when the node execution fails.
     * Can be used for error handling, logging, or cleanup.
     *
     * @param state The current state
     * @param error The error that occurred
     */
    default void onExecutionError(
        GraphWorkflowState<S> state,
        WorkflowError error
    ) {
        // Default: no action
    }

    /**
     * Processes the node with full lifecycle management.
     * This is the main entry point that handles validation, execution, and lifecycle callbacks.
     *
     * @param state The current graph workflow state
     * @return A WorkflowResult containing the command or error
     */
    default WorkflowResult<GraphCommand<S>, WorkflowError> processWithLifecycle(
        GraphWorkflowState<S> state
    ) {
        try {
            // Pre-execution validation
            var validationResult = validateState(state);
            if (validationResult.isFailure()) {
                return WorkflowResult.failure(
                    validationResult.getError().get()
                );
            }

            // Pre-execution callback
            onBeforeExecution(state);

            // Main execution
            var result = process(state);

            if (result.isSuccess()) {
                var command = result.getValue().get();

                // Post-execution callback
                onAfterExecution(state, command);
                return result;
            } else if (result.isFailure()) {
                var error = result.getError().get();
                onExecutionError(state, error);
                return result;
            } else {
                // Suspended
                return result;
            }
        } catch (Exception e) {
            onExecutionError(state, null);
            return WorkflowResult.suspended(
                "execution-error",
                state.data(),
                "Unexpected exception during node execution: " + e.getMessage()
            );
        }
    }
}

/**
 * Metadata about a graph workflow node.
 */
record GraphNodeMetadata(
    NodeId nodeId,
    String name,
    String description,
    NodeType type,
    Set<String> tags,
    Map<String, Object> properties,
    Optional<Duration> expectedDuration
) {
    public GraphNodeMetadata {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(type, "Node type cannot be null");
        Objects.requireNonNull(tags, "Tags cannot be null");
        Objects.requireNonNull(properties, "Properties cannot be null");
        Objects.requireNonNull(
            expectedDuration,
            "Expected duration optional cannot be null"
        );

        tags = Set.copyOf(tags);
        properties = Map.copyOf(properties);
    }

    /**
     * Types of nodes in a graph workflow.
     */
    public enum NodeType {
        TASK, // Regular processing node
        DECISION, // Conditional routing node
        FORK, // Parallel execution start
        JOIN, // Parallel execution merge
        START, // Workflow entry point
        END, // Workflow exit point
        SUSPEND, // Suspension point
    }

    public static GraphNodeMetadata of(
        NodeId nodeId,
        String name,
        String description,
        NodeType type
    ) {
        return new GraphNodeMetadata(
            nodeId,
            name,
            description,
            type,
            Collections.emptySet(),
            Collections.emptyMap(),
            Optional.empty()
        );
    }

    public static GraphNodeMetadata withTags(
        NodeId nodeId,
        String name,
        String description,
        NodeType type,
        Set<String> tags
    ) {
        return new GraphNodeMetadata(
            nodeId,
            name,
            description,
            type,
            tags,
            Collections.emptyMap(),
            Optional.empty()
        );
    }

    public static GraphNodeMetadata withDuration(
        NodeId nodeId,
        String name,
        String description,
        NodeType type,
        Duration expectedDuration
    ) {
        return new GraphNodeMetadata(
            nodeId,
            name,
            description,
            type,
            Collections.emptySet(),
            Collections.emptyMap(),
            Optional.of(expectedDuration)
        );
    }

    public GraphNodeMetadata withTag(String tag) {
        var newTags = new java.util.HashSet<>(tags);
        newTags.add(tag);
        return new GraphNodeMetadata(
            nodeId,
            name,
            description,
            type,
            newTags,
            properties,
            expectedDuration
        );
    }

    public GraphNodeMetadata withProperty(String key, Object value) {
        var newProperties = new java.util.HashMap<>(properties);
        newProperties.put(key, value);
        return new GraphNodeMetadata(
            nodeId,
            name,
            description,
            type,
            tags,
            newProperties,
            expectedDuration
        );
    }

    public <T> Optional<T> getProperty(String key) {
        @SuppressWarnings("unchecked")
        T value = (T) properties.get(key);
        return Optional.ofNullable(value);
    }
}
