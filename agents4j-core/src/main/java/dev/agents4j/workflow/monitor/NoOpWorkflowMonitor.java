package dev.agents4j.workflow.monitor;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.error.WorkflowError;

/**
 * Default implementation of WorkflowMonitor that does nothing.
 * Use this when monitoring is not required.
 */
public class NoOpWorkflowMonitor implements WorkflowMonitor {

    public static final NoOpWorkflowMonitor INSTANCE = new NoOpWorkflowMonitor();

    // Private constructor to enforce singleton pattern
    private NoOpWorkflowMonitor() {}

    @Override
    public void onWorkflowStarted(
        WorkflowId workflowId,
        String workflowName,
        GraphWorkflowState<?> initialState
    ) {
        // No-op implementation
    }

    @Override
    public void onWorkflowResumed(
        WorkflowId workflowId,
        GraphWorkflowState<?> resumedState
    ) {
        // No-op implementation
    }

    @Override
    public void onWorkflowCompleted(
        WorkflowId workflowId,
        GraphWorkflowState<?> finalState
    ) {
        // No-op implementation
    }

    @Override
    public void onWorkflowSuspended(
        WorkflowId workflowId,
        GraphWorkflowState<?> suspendedState
    ) {
        // No-op implementation
    }

    @Override
    public void onWorkflowError(
        WorkflowId workflowId,
        WorkflowError error,
        GraphWorkflowState<?> state,
        Exception exception
    ) {
        // No-op implementation
    }

    @Override
    public void onNodeStarted(
        WorkflowId workflowId,
        NodeId nodeId,
        GraphWorkflowState<?> state
    ) {
        // No-op implementation
    }

    @Override
    public void onNodeCompleted(
        WorkflowId workflowId,
        NodeId nodeId,
        GraphWorkflowState<?> state,
        long processingTime
    ) {
        // No-op implementation
    }

    @Override
    public void onNodeError(
        WorkflowId workflowId,
        NodeId nodeId,
        WorkflowError error,
        GraphWorkflowState<?> state,
        Exception exception
    ) {
        // No-op implementation
    }

    @Override
    public void onNodeTransition(
        WorkflowId workflowId,
        EdgeId edgeId,
        NodeId fromNodeId,
        NodeId toNodeId,
        GraphWorkflowState<?> state
    ) {
        // No-op implementation
    }

    @Override
    public void onWarning(
        WorkflowId workflowId,
        String message,
        GraphWorkflowState<?> state
    ) {
        // No-op implementation
    }
}