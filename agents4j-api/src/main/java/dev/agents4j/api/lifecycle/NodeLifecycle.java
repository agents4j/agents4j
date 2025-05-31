package dev.agents4j.api.lifecycle;

import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing the lifecycle of workflow nodes.
 * Provides hooks for initialization, cleanup, and resource management
 * of WorkflowNodes throughout their operational lifetime.
 * 
 * <p>This interface enables proper resource management, dependency injection,
 * and lifecycle-aware behavior for nodes that require setup/teardown operations.</p>
 * 
 * @param <S> The type of the workflow state data
 */
public interface NodeLifecycle<S> {
    
    /**
     * Initializes the node with the given configuration.
     * Called once when the node is first created or loaded.
     *
     * @param configuration Node-specific configuration parameters
     * @throws LifecycleException if initialization fails
     */
    void initialize(Map<String, Object> configuration) throws LifecycleException;
    
    /**
     * Prepares the node for execution in a specific workflow context.
     * Called before the node processes any workflow state.
     *
     * @param workflowContext Context information about the workflow
     * @throws LifecycleException if preparation fails
     */
    default void prepare(WorkflowContext workflowContext) throws LifecycleException {
        // Default implementation is no-op
    }
    
    /**
     * Called before the node processes a workflow state.
     * Allows for pre-processing setup or validation.
     *
     * @param state The workflow state about to be processed
     * @return Optional processing hints or metadata
     * @throws LifecycleException if pre-processing fails
     */
    default Optional<Map<String, Object>> beforeProcess(WorkflowState<S> state) throws LifecycleException {
        return Optional.empty();
    }
    
    /**
     * Called after the node successfully processes a workflow state.
     * Allows for cleanup or post-processing operations.
     *
     * @param state The workflow state that was processed
     * @param processingMetadata Optional metadata from beforeProcess
     * @throws LifecycleException if post-processing fails
     */
    default void afterProcess(WorkflowState<S> state, Optional<Map<String, Object>> processingMetadata) 
            throws LifecycleException {
        // Default implementation is no-op
    }
    
    /**
     * Called when an error occurs during node processing.
     * Allows for error-specific cleanup or recovery operations.
     *
     * @param state The workflow state being processed when error occurred
     * @param error The exception that occurred
     * @param processingMetadata Optional metadata from beforeProcess
     * @throws LifecycleException if error handling fails
     */
    default void onError(WorkflowState<S> state, Throwable error, 
                        Optional<Map<String, Object>> processingMetadata) throws LifecycleException {
        // Default implementation is no-op
    }
    
    /**
     * Pauses the node, typically when workflow is suspended.
     * Should release non-persistent resources while maintaining state.
     *
     * @throws LifecycleException if pause operation fails
     */
    default void pause() throws LifecycleException {
        // Default implementation is no-op
    }
    
    /**
     * Resumes the node after being paused.
     * Should re-acquire resources needed for operation.
     *
     * @throws LifecycleException if resume operation fails
     */
    default void resume() throws LifecycleException {
        // Default implementation is no-op
    }
    
    /**
     * Shuts down the node and releases all resources.
     * Called when the node is no longer needed.
     *
     * @throws LifecycleException if shutdown fails
     */
    void shutdown() throws LifecycleException;
    
    /**
     * Performs a health check on the node.
     *
     * @return HealthStatus indicating the current health of the node
     */
    default HealthStatus checkHealth() {
        return HealthStatus.healthy();
    }
    
    /**
     * Gets the current lifecycle state of the node.
     *
     * @return The current lifecycle state
     */
    LifecycleState getLifecycleState();
    
    /**
     * Gets resource usage information for this node.
     *
     * @return Map containing resource usage metrics
     */
    default Map<String, Object> getResourceUsage() {
        return Map.of(
            "lifecycleState", getLifecycleState(),
            "nodeType", getClass().getSimpleName()
        );
    }
    
    /**
     * Checks if the node is currently operational.
     *
     * @return true if the node can process workflow states, false otherwise
     */
    default boolean isOperational() {
        return getLifecycleState() == LifecycleState.ACTIVE;
    }
    
    /**
     * Gets configuration information about this node's lifecycle management.
     *
     * @return Map containing lifecycle configuration
     */
    default Map<String, Object> getLifecycleInfo() {
        return Map.of(
            "state", getLifecycleState(),
            "operational", isOperational(),
            "supportsHealthCheck", true,
            "supportsPauseResume", true
        );
    }
}

/**
 * Enumeration of possible lifecycle states for a node.
 */
enum LifecycleState {
    /** Node has been created but not yet initialized */
    CREATED,
    
    /** Node is initialized and ready for operation */
    INITIALIZED,
    
    /** Node is actively processing workflow states */
    ACTIVE,
    
    /** Node is temporarily paused */
    PAUSED,
    
    /** Node encountered an error and requires attention */
    ERROR,
    
    /** Node is in the process of shutting down */
    SHUTTING_DOWN,
    
    /** Node has been shut down and cannot be used */
    SHUTDOWN
}

/**
 * Represents the health status of a node.
 */
class HealthStatus {
    private final boolean healthy;
    private final String message;
    private final Map<String, Object> details;
    
    private HealthStatus(boolean healthy, String message, Map<String, Object> details) {
        this.healthy = healthy;
        this.message = message != null ? message : "";
        this.details = details != null ? Map.copyOf(details) : Map.of();
    }
    
    /**
     * Creates a healthy status.
     */
    public static HealthStatus healthy() {
        return new HealthStatus(true, "Node is healthy", null);
    }
    
    /**
     * Creates a healthy status with details.
     */
    public static HealthStatus healthy(String message, Map<String, Object> details) {
        return new HealthStatus(true, message, details);
    }
    
    /**
     * Creates an unhealthy status.
     */
    public static HealthStatus unhealthy(String message) {
        return new HealthStatus(false, message, null);
    }
    
    /**
     * Creates an unhealthy status with details.
     */
    public static HealthStatus unhealthy(String message, Map<String, Object> details) {
        return new HealthStatus(false, message, details);
    }
    
    public boolean isHealthy() { return healthy; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
    
    @Override
    public String toString() {
        return String.format("HealthStatus{healthy=%s, message='%s'}", healthy, message);
    }
}

/**
 * Context information about the workflow environment.
 */
interface WorkflowContext {
    
    /**
     * Gets the workflow identifier.
     */
    String getWorkflowId();
    
    /**
     * Gets the workflow name.
     */
    String getWorkflowName();
    
    /**
     * Gets workflow-level configuration.
     */
    Map<String, Object> getWorkflowConfiguration();
    
    /**
     * Gets runtime context information.
     */
    Map<String, Object> getRuntimeContext();
    
    /**
     * Gets a specific context value.
     */
    <T> Optional<T> getContextValue(String key, Class<T> type);
}

/**
 * Exception thrown when lifecycle operations fail.
 */
class LifecycleException extends Exception {
    
    private final LifecycleState currentState;
    private final String operation;
    
    public LifecycleException(String message, LifecycleState currentState, String operation) {
        super(message);
        this.currentState = currentState;
        this.operation = operation;
    }
    
    public LifecycleException(String message, Throwable cause, LifecycleState currentState, String operation) {
        super(message, cause);
        this.currentState = currentState;
        this.operation = operation;
    }
    
    public LifecycleState getCurrentState() { return currentState; }
    public String getOperation() { return operation; }
    
    @Override
    public String toString() {
        return String.format("LifecycleException{operation='%s', currentState=%s, message='%s'}", 
                operation, currentState, getMessage());
    }
}