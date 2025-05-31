package dev.agents4j.api.execution;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing execution context across workflow operations.
 * Provides a unified way to access runtime information, configuration,
 * and execution state without tight coupling to specific implementations.
 * 
 * <p>This interface enables clean separation of concerns and better
 * testability by abstracting context access patterns.</p>
 */
public interface ExecutionContext {
    
    /**
     * Gets the execution identifier for correlation and tracing.
     *
     * @return The unique execution identifier
     */
    String getExecutionId();
    
    /**
     * Gets the workflow identifier this execution belongs to.
     *
     * @return The workflow identifier
     */
    String getWorkflowId();
    
    /**
     * Gets the current node identifier being executed.
     *
     * @return The current node identifier, or empty if not in node execution
     */
    Optional<String> getCurrentNodeId();
    
    /**
     * Gets a context value by key.
     *
     * @param key The context key
     * @return The context value, or empty if not found
     */
    Optional<Object> getValue(String key);
    
    /**
     * Gets a typed context value by key.
     *
     * @param key The context key
     * @param type The expected value type
     * @param <T> The value type
     * @return The typed context value, or empty if not found or wrong type
     */
    <T> Optional<T> getValue(String key, Class<T> type);
    
    /**
     * Gets a context value with a default.
     *
     * @param key The context key
     * @param defaultValue The default value if not found
     * @param <T> The value type
     * @return The context value or default value
     */
    <T> T getValue(String key, T defaultValue);
    
    /**
     * Sets a context value.
     *
     * @param key The context key
     * @param value The context value
     * @return A new ExecutionContext with the updated value
     */
    ExecutionContext setValue(String key, Object value);
    
    /**
     * Sets multiple context values.
     *
     * @param values Map of key-value pairs to set
     * @return A new ExecutionContext with the updated values
     */
    ExecutionContext setValues(Map<String, Object> values);
    
    /**
     * Removes a context value.
     *
     * @param key The context key to remove
     * @return A new ExecutionContext without the specified key
     */
    ExecutionContext removeValue(String key);
    
    /**
     * Gets all context keys.
     *
     * @return Set of all context keys
     */
    Set<String> getKeys();
    
    /**
     * Gets all context values.
     *
     * @return Map of all context key-value pairs
     */
    Map<String, Object> getAllValues();
    
    /**
     * Checks if a context key exists.
     *
     * @param key The context key
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(String key);
    
    /**
     * Gets the execution start time.
     *
     * @return The instant when execution began
     */
    Instant getStartTime();
    
    /**
     * Gets the current execution time.
     *
     * @return The current instant
     */
    default Instant getCurrentTime() {
        return Instant.now();
    }
    
    /**
     * Gets the execution duration so far.
     *
     * @return Duration since execution started
     */
    default java.time.Duration getExecutionDuration() {
        return java.time.Duration.between(getStartTime(), getCurrentTime());
    }
    
    /**
     * Gets the user identifier associated with this execution.
     *
     * @return The user identifier, or empty if not available
     */
    Optional<String> getUserId();
    
    /**
     * Gets the session identifier associated with this execution.
     *
     * @return The session identifier, or empty if not available
     */
    Optional<String> getSessionId();
    
    /**
     * Gets the correlation identifier for distributed tracing.
     *
     * @return The correlation identifier, or empty if not available
     */
    Optional<String> getCorrelationId();
    
    /**
     * Gets execution metadata such as performance metrics or debugging info.
     *
     * @return Map containing execution metadata
     */
    Map<String, Object> getMetadata();
    
    /**
     * Sets execution metadata.
     *
     * @param key The metadata key
     * @param value The metadata value
     * @return A new ExecutionContext with updated metadata
     */
    ExecutionContext setMetadata(String key, Object value);
    
    /**
     * Gets the execution priority level.
     *
     * @return The execution priority
     */
    default ExecutionPriority getPriority() {
        return ExecutionPriority.NORMAL;
    }
    
    /**
     * Creates a child execution context for nested operations.
     *
     * @param childExecutionId The child execution identifier
     * @return A new ExecutionContext for the child execution
     */
    ExecutionContext createChild(String childExecutionId);
    
    /**
     * Creates a child context for a specific node execution.
     *
     * @param nodeId The node identifier
     * @return A new ExecutionContext scoped to the node
     */
    ExecutionContext forNode(String nodeId);
    
    /**
     * Gets the parent execution context if this is a child context.
     *
     * @return The parent context, or empty if this is a root context
     */
    Optional<ExecutionContext> getParent();
    
    /**
     * Checks if this execution has been cancelled.
     *
     * @return true if execution is cancelled, false otherwise
     */
    boolean isCancelled();
    
    /**
     * Gets a CompletableFuture that completes when this execution is cancelled.
     *
     * @return Future that completes on cancellation
     */
    CompletableFuture<Void> getCancellationFuture();
    
    /**
     * Gets security context information if available.
     *
     * @return The security context, or empty if not available
     */
    Optional<SecurityContext> getSecurityContext();
    
    /**
     * Gets execution configuration that may override workflow defaults.
     *
     * @return Map containing execution-specific configuration
     */
    Map<String, Object> getExecutionConfiguration();
    
    /**
     * Creates a summary of this execution context for logging or debugging.
     *
     * @return Map containing context summary information
     */
    default Map<String, Object> getSummary() {
        return Map.of(
            "executionId", getExecutionId(),
            "workflowId", getWorkflowId(),
            "currentNodeId", getCurrentNodeId().orElse("none"),
            "startTime", getStartTime(),
            "duration", getExecutionDuration(),
            "valueCount", getKeys().size(),
            "cancelled", isCancelled()
        );
    }
}

/**
 * Enumeration of execution priority levels.
 */
enum ExecutionPriority {
    LOW(1),
    NORMAL(5),
    HIGH(8),
    CRITICAL(10);
    
    private final int value;
    
    ExecutionPriority(int value) {
        this.value = value;
    }
    
    public int getValue() { return value; }
}

/**
 * Interface for security context information.
 */
interface SecurityContext {
    
    /**
     * Gets the authenticated principal.
     */
    Optional<String> getPrincipal();
    
    /**
     * Gets the user roles.
     */
    Set<String> getRoles();
    
    /**
     * Checks if the current user has a specific permission.
     */
    boolean hasPermission(String permission);
    
    /**
     * Gets additional security attributes.
     */
    Map<String, Object> getAttributes();
}