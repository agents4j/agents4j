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
 */
public class WorkflowState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final String workflowId;
    private final Map<String, Object> data;
    private final String currentNodeId;
    private final Instant lastModified;
    private final long version;
    
    public WorkflowState(String workflowId, Map<String, Object> data, String currentNodeId,
                        Instant lastModified, long version) {
        this.workflowId = Objects.requireNonNull(workflowId, "Workflow ID cannot be null");
        this.data = Collections.unmodifiableMap(data != null ? new HashMap<>(data) : Collections.emptyMap());
        this.currentNodeId = currentNodeId;
        this.lastModified = lastModified != null ? lastModified : Instant.now();
        this.version = version;
    }
    
    /**
     * Creates a new workflow state with the given workflow ID.
     *
     * @param workflowId The workflow ID
     * @return A new WorkflowState instance
     */
    public static WorkflowState create(String workflowId) {
        return new WorkflowState(workflowId, Collections.emptyMap(), null, Instant.now(), 1L);
    }
    
    /**
     * Creates a new workflow state with initial data.
     *
     * @param workflowId The workflow ID
     * @param initialData Initial state data
     * @return A new WorkflowState instance
     */
    public static WorkflowState create(String workflowId, Map<String, Object> initialData) {
        return new WorkflowState(workflowId, initialData, null, Instant.now(), 1L);
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
     * @return Unmodifiable map of state data
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * Gets a specific state value.
     *
     * @param key The state key
     * @param <T> The expected type
     * @return The state value wrapped in Optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        return Optional.ofNullable((T) data.get(key));
    }
    
    /**
     * Gets a specific state value with a default.
     *
     * @param key The state key
     * @param defaultValue The default value
     * @param <T> The expected type
     * @return The state value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
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
     * Creates a new state with updates applied.
     *
     * @param updates The updates to apply
     * @return A new WorkflowState with updates applied
     */
    public WorkflowState withUpdates(Map<String, Object> updates) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.putAll(updates);
        return new WorkflowState(workflowId, newData, currentNodeId, Instant.now(), version + 1);
    }
    
    /**
     * Creates a new state with the current node ID updated.
     *
     * @param nodeId The new current node ID
     * @return A new WorkflowState with updated current node
     */
    public WorkflowState withCurrentNode(String nodeId) {
        return new WorkflowState(workflowId, data, nodeId, Instant.now(), version + 1);
    }
    
    /**
     * Creates a new state with both updates and current node ID.
     *
     * @param updates The updates to apply
     * @param nodeId The new current node ID
     * @return A new WorkflowState with updates and current node
     */
    public WorkflowState withUpdatesAndCurrentNode(Map<String, Object> updates, String nodeId) {
        Map<String, Object> newData = new HashMap<>(this.data);
        newData.putAll(updates);
        return new WorkflowState(workflowId, newData, nodeId, Instant.now(), version + 1);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowState that = (WorkflowState) o;
        return version == that.version &&
                Objects.equals(workflowId, that.workflowId) &&
                Objects.equals(data, that.data) &&
                Objects.equals(currentNodeId, that.currentNodeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(workflowId, data, currentNodeId, version);
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowState{id='%s', currentNode='%s', version=%d, dataSize=%d}",
                workflowId, currentNodeId, version, data.size());
    }
}