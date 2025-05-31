package dev.agents4j.api.state;

import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing workflow state operations.
 * Provides a clean abstraction for state manipulation, persistence, and querying.
 * 
 * <p>This interface separates state management concerns from workflow execution,
 * allowing for different state storage implementations (in-memory, database, etc.)
 * and enabling better testability through dependency injection.</p>
 * 
 * @param <S> The type of the workflow state data
 */
public interface StateManager<S> {
    
    /**
     * Creates a new workflow state with the given parameters.
     *
     * @param workflowId The unique workflow identifier
     * @param initialData The initial state data
     * @return A new WorkflowState instance
     */
    WorkflowState<S> createState(String workflowId, S initialData);
    
    /**
     * Creates a new workflow state with initial data and context.
     *
     * @param workflowId The unique workflow identifier
     * @param initialData The initial state data
     * @param initialContext The initial context data
     * @return A new WorkflowState instance
     */
    WorkflowState<S> createState(String workflowId, S initialData, Map<String, Object> initialContext);
    
    /**
     * Updates the state data while preserving other state properties.
     *
     * @param currentState The current workflow state
     * @param newData The new state data
     * @return An updated WorkflowState instance
     */
    WorkflowState<S> updateData(WorkflowState<S> currentState, S newData);
    
    /**
     * Updates the context while preserving other state properties.
     *
     * @param currentState The current workflow state
     * @param contextUpdates The context updates to apply
     * @return An updated WorkflowState instance
     */
    WorkflowState<S> updateContext(WorkflowState<S> currentState, Map<String, Object> contextUpdates);
    
    /**
     * Updates the current node ID.
     *
     * @param currentState The current workflow state
     * @param nodeId The new current node ID
     * @return An updated WorkflowState instance
     */
    WorkflowState<S> updateCurrentNode(WorkflowState<S> currentState, String nodeId);
    
    /**
     * Performs a comprehensive update of all state components.
     *
     * @param currentState The current workflow state
     * @param newData The new state data (can be null to keep current)
     * @param contextUpdates The context updates to apply (can be null or empty)
     * @param nodeId The new current node ID (can be null to keep current)
     * @return An updated WorkflowState instance
     */
    WorkflowState<S> updateState(WorkflowState<S> currentState, S newData, 
                                Map<String, Object> contextUpdates, String nodeId);
    
    /**
     * Saves the workflow state for persistence.
     * Default implementation is a no-op for stateless managers.
     *
     * @param state The workflow state to save
     * @return The saved state (may be the same instance or a new one with updated metadata)
     */
    default WorkflowState<S> saveState(WorkflowState<S> state) {
        return state;
    }
    
    /**
     * Loads a workflow state by its ID.
     * Default implementation returns empty for stateless managers.
     *
     * @param workflowId The workflow ID to load
     * @return The loaded workflow state, or empty if not found
     */
    default Optional<WorkflowState<S>> loadState(String workflowId) {
        return Optional.empty();
    }
    
    /**
     * Deletes a workflow state by its ID.
     * Default implementation is a no-op for stateless managers.
     *
     * @param workflowId The workflow ID to delete
     * @return true if the state was deleted, false if it didn't exist
     */
    default boolean deleteState(String workflowId) {
        return false;
    }
    
    /**
     * Checks if this state manager supports persistence.
     *
     * @return true if the manager can persist states, false otherwise
     */
    default boolean supportsPersistence() {
        return false;
    }
    
    /**
     * Gets metadata about the state manager's capabilities.
     *
     * @return A map containing capability information
     */
    default Map<String, Object> getCapabilities() {
        return Map.of(
            "persistence", supportsPersistence(),
            "managerType", getClass().getSimpleName()
        );
    }
}