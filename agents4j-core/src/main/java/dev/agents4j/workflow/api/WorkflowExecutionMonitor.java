package dev.agents4j.workflow.api;

import dev.agents4j.api.workflow.WorkflowState;
import java.time.Duration;
import java.util.Map;

/**
 * Interface for monitoring workflow execution.
 * Provides observability into workflow performance and behavior.
 */
public interface WorkflowExecutionMonitor {
    
    /**
     * Called when a workflow starts execution.
     *
     * @param workflowId The unique identifier of the workflow instance
     * @param workflowName The name of the workflow
     * @param context Additional context information
     */
    void onWorkflowStarted(String workflowId, String workflowName, Map<String, Object> context);
    
    /**
     * Called when a node starts execution.
     *
     * @param workflowId The workflow instance identifier
     * @param nodeId The identifier of the node starting execution
     * @param state The current workflow state
     */
    void onNodeStarted(String workflowId, String nodeId, WorkflowState state);
    
    /**
     * Called when a node completes execution.
     *
     * @param workflowId The workflow instance identifier
     * @param nodeId The identifier of the completed node
     * @param executionTime The time taken to execute the node
     * @param newState The updated workflow state after node execution
     */
    void onNodeCompleted(String workflowId, String nodeId, Duration executionTime, WorkflowState newState);
    
    /**
     * Called when a workflow completes successfully.
     *
     * @param workflowId The workflow instance identifier
     * @param totalTime The total execution time of the workflow
     * @param finalState The final state of the workflow
     */
    void onWorkflowCompleted(String workflowId, Duration totalTime, WorkflowState finalState);
    
    /**
     * Called when a workflow is suspended.
     *
     * @param workflowId The workflow instance identifier
     * @param state The current state when suspended
     * @param reason The reason for suspension
     */
    void onWorkflowSuspended(String workflowId, WorkflowState state, String reason);
    
    /**
     * Called when a workflow encounters an error.
     *
     * @param workflowId The workflow instance identifier
     * @param error The error message
     * @param state The workflow state when the error occurred
     * @param cause The underlying exception that caused the error
     */
    void onWorkflowError(String workflowId, String error, WorkflowState state, Exception cause);
    
    /**
     * Called when workflow state is updated.
     *
     * @param workflowId The workflow instance identifier
     * @param oldState The previous state
     * @param newState The updated state
     */
    void onStateUpdated(String workflowId, WorkflowState oldState, WorkflowState newState);
    
    /**
     * Called when a workflow resumes from a suspended state.
     *
     * @param workflowId The workflow instance identifier
     * @param state The state from which the workflow is resuming
     */
    void onWorkflowResumed(String workflowId, WorkflowState state);
    
    /**
     * Gets the name of this monitor for identification purposes.
     *
     * @return A descriptive name for this monitor
     */
    default String getMonitorName() {
        return this.getClass().getSimpleName();
    }
}