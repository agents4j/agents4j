package dev.agents4j.workflow.impl;

import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.WorkflowExecutionMonitor;

import java.time.Duration;
import java.util.Map;

/**
 * No-operation implementation of WorkflowExecutionMonitor.
 * This implementation does nothing and serves as a default when monitoring is disabled.
 */
public class NoOpWorkflowExecutionMonitor implements WorkflowExecutionMonitor {
    
    public static final NoOpWorkflowExecutionMonitor INSTANCE = new NoOpWorkflowExecutionMonitor();
    
    private NoOpWorkflowExecutionMonitor() {
        // Private constructor to enforce singleton pattern
    }
    
    @Override
    public void onWorkflowStarted(String workflowId, String workflowName, Map<String, Object> context) {
        // No-op
    }
    
    @Override
    public void onNodeStarted(String workflowId, String nodeId, WorkflowState state) {
        // No-op
    }
    
    @Override
    public void onNodeCompleted(String workflowId, String nodeId, Duration executionTime, WorkflowState newState) {
        // No-op
    }
    
    @Override
    public void onWorkflowCompleted(String workflowId, Duration totalTime, WorkflowState finalState) {
        // No-op
    }
    
    @Override
    public void onWorkflowSuspended(String workflowId, WorkflowState state, String reason) {
        // No-op
    }
    
    @Override
    public void onWorkflowError(String workflowId, String error, WorkflowState state, Exception cause) {
        // No-op
    }
    
    @Override
    public void onStateUpdated(String workflowId, WorkflowState oldState, WorkflowState newState) {
        // No-op
    }
    
    @Override
    public void onWorkflowResumed(String workflowId, WorkflowState state) {
        // No-op
    }
    
    @Override
    public String getMonitorName() {
        return "NoOpWorkflowExecutionMonitor";
    }
}