package dev.agents4j.api.workflow;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the state of a stateful workflow. The state is serializable
 * and can be persisted for workflow suspension and resumption.
 *
 * @param <S> The type of the state data
 */
public class GraphWorkflowState<S> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String workflowId;
    private final S data;
    private final Map<String, Object> context;
    private final String currentNodeId;
    private final Instant lastModified;
    private final long version;

    public GraphWorkflowState(
        String workflowId,
        S data,
        Map<String, Object> context,
        String currentNodeId,
        Instant lastModified,
        long version
    ) {
        this.workflowId = Objects.requireNonNull(
            workflowId,
            "Workflow ID cannot be null"
        );
        this.data = data;
        this.context = Collections.unmodifiableMap(
            context != null ? new HashMap<>(context) : Collections.emptyMap()
        );
        this.currentNodeId = currentNodeId;
        this.lastModified = lastModified != null ? lastModified : Instant.now();
        this.version = version;
    }

    /**
     * Creates a new workflow state with the given workflow ID.
     *
     * @param workflowId The workflow ID
     * @param <S> The type of the state data
     * @return A new GraphWorkflowState instance
     */
    public static <S> GraphWorkflowState<S> create(String workflowId) {
        return new GraphWorkflowState<>(
            workflowId,
            null,
            Collections.emptyMap(),
            null,
            Instant.now(),
            1L
        );
    }

    /**
     * Creates a new workflow state with initial data.
     *
     * @param workflowId The workflow ID
     * @param initialData Initial state data
     * @param <S> The type of the state data
     * @return A new GraphWorkflowState instance
     */
    public static <S> GraphWorkflowState<S> create(
        String workflowId,
        S initialData
    ) {
        return new GraphWorkflowState<>(
            workflowId,
            initialData,
            Collections.emptyMap(),
            null,
            Instant.now(),
            1L
        );
    }

    /**
     * Gets the workflow ID.
     *
     * @return The workflow ID
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Gets the state data.
     *
     * @return The state data
     */
    public S getData() {
        return data;
    }

    /**
     * Gets the context data.
     *
     * @return Unmodifiable map of context data
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * Gets a specific context value.
     *
     * @param key The context key
     * @param <T> The expected type
     * @return The context value wrapped in Optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getContextValue(String key) {
        return Optional.ofNullable((T) context.get(key));
    }

    /**
     * Gets a specific context value with a default.
     *
     * @param key The context key
     * @param defaultValue The default value
     * @param <T> The expected type
     * @return The context value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }

    /**
     * Gets the current node ID.
     *
     * @return The current node ID wrapped in Optional
     */
    public Optional<String> getCurrentNodeId() {
        return Optional.ofNullable(currentNodeId);
    }

    /**
     * Gets the last modified timestamp.
     *
     * @return The last modified timestamp
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Gets the state version.
     *
     * @return The state version
     */
    public long getVersion() {
        return version;
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
            currentNodeId,
            Instant.now(),
            version + 1
        );
    }

    /**
     * Creates a new state with context updates applied.
     *
     * @param contextUpdates The context updates to apply
     * @return A new GraphWorkflowState with updated context
     */
    public GraphWorkflowState<S> withContextUpdates(
        Map<String, Object> contextUpdates
    ) {
        Map<String, Object> newContext = new HashMap<>(this.context);
        newContext.putAll(contextUpdates);
        return new GraphWorkflowState<>(
            workflowId,
            data,
            newContext,
            currentNodeId,
            Instant.now(),
            version + 1
        );
    }

    /**
     * Creates a new state with the current node ID updated.
     *
     * @param nodeId The new current node ID
     * @return A new GraphWorkflowState with updated current node
     */
    public GraphWorkflowState<S> withCurrentNode(String nodeId) {
        return new GraphWorkflowState<>(
            workflowId,
            data,
            context,
            nodeId,
            Instant.now(),
            version + 1
        );
    }

    /**
     * Creates a new state with data, context updates and current node ID.
     *
     * @param newData The new state data
     * @param contextUpdates The context updates to apply
     * @param nodeId The new current node ID
     * @return A new GraphWorkflowState with all updates applied
     */
    public GraphWorkflowState<S> withDataContextAndCurrentNode(
        S newData,
        Map<String, Object> contextUpdates,
        String nodeId
    ) {
        Map<String, Object> newContext = new HashMap<>(this.context);
        if (contextUpdates != null) {
            newContext.putAll(contextUpdates);
        }
        return new GraphWorkflowState<>(
            workflowId,
            newData,
            newContext,
            nodeId,
            Instant.now(),
            version + 1
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphWorkflowState<?> that = (GraphWorkflowState<?>) o;
        return (
            version == that.version &&
            Objects.equals(workflowId, that.workflowId) &&
            Objects.equals(data, that.data) &&
            Objects.equals(context, that.context) &&
            Objects.equals(currentNodeId, that.currentNodeId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowId, data, context, currentNodeId, version);
    }

    @Override
    public String toString() {
        return String.format(
            "WorkflowState{id='%s', currentNode='%s', version=%d, contextSize=%d}",
            workflowId,
            currentNodeId,
            version,
            context.size()
        );
    }
}
