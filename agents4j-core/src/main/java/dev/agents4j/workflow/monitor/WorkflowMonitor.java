package dev.agents4j.workflow.monitor;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.error.WorkflowError;

/**
 * Workflow monitoring interface for tracking execution events.
 */
public interface WorkflowMonitor {
    /**
     * Called when a workflow starts execution.
     *
     * @param workflowId The workflow ID
     * @param workflowName The workflow name
     * @param initialState The initial workflow state
     */
    void onWorkflowStarted(
        WorkflowId workflowId, 
        String workflowName, 
        GraphWorkflowState<?> initialState
    );

    /**
     * Called when a workflow is resumed from a suspended state.
     *
     * @param workflowId The workflow ID
     * @param resumedState The resumed workflow state
     */
    void onWorkflowResumed(
        WorkflowId workflowId, 
        GraphWorkflowState<?> resumedState
    );

    /**
     * Called when a workflow completes successfully.
     *
     * @param workflowId The workflow ID
     * @param finalState The final workflow state
     */
    void onWorkflowCompleted(
        WorkflowId workflowId, 
        GraphWorkflowState<?> finalState
    );

    /**
     * Called when a workflow is suspended.
     *
     * @param workflowId The workflow ID
     * @param suspendedState The suspended workflow state
     */
    void onWorkflowSuspended(
        WorkflowId workflowId, 
        GraphWorkflowState<?> suspendedState
    );

    /**
     * Called when a workflow encounters an error.
     *
     * @param workflowId The workflow ID
     * @param error The workflow error
     * @param state The current workflow state
     * @param exception The exception that caused the error, if any
     */
    void onWorkflowError(
        WorkflowId workflowId, 
        WorkflowError error, 
        GraphWorkflowState<?> state, 
        Exception exception
    );

    /**
     * Called when a node starts processing.
     *
     * @param workflowId The workflow ID
     * @param nodeId The node ID
     * @param state The current workflow state
     */
    void onNodeStarted(
        WorkflowId workflowId, 
        NodeId nodeId, 
        GraphWorkflowState<?> state
    );

    /**
     * Called when a node completes processing.
     *
     * @param workflowId The workflow ID
     * @param nodeId The node ID
     * @param state The current workflow state
     * @param processingTime The time taken to process the node
     */
    void onNodeCompleted(
        WorkflowId workflowId, 
        NodeId nodeId, 
        GraphWorkflowState<?> state, 
        long processingTime
    );

    /**
     * Called when a node encounters an error.
     *
     * @param workflowId The workflow ID
     * @param nodeId The node ID
     * @param error The error that occurred
     * @param state The current workflow state
     * @param exception The exception that caused the error, if any
     */
    void onNodeError(
        WorkflowId workflowId, 
        NodeId nodeId, 
        WorkflowError error, 
        GraphWorkflowState<?> state, 
        Exception exception
    );

    /**
     * Called when a transition occurs between nodes.
     *
     * @param workflowId The workflow ID
     * @param edgeId The edge ID
     * @param fromNodeId The source node ID
     * @param toNodeId The target node ID
     * @param state The current workflow state
     */
    void onNodeTransition(
        WorkflowId workflowId, 
        EdgeId edgeId, 
        NodeId fromNodeId, 
        NodeId toNodeId, 
        GraphWorkflowState<?> state
    );

    /**
     * Called to report a warning during workflow execution.
     *
     * @param workflowId The workflow ID
     * @param message The warning message
     * @param state The current workflow state
     */
    void onWarning(
        WorkflowId workflowId, 
        String message, 
        GraphWorkflowState<?> state
    );
}