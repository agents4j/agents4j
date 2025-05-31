package dev.agents4j.api.exception;

import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Exception thrown when workflow execution fails.
 * Enhanced to provide context about the workflow state when the error occurred.
 */
public class WorkflowExecutionException extends AgentException {
    
    private final String workflowName;
    private final String workflowId;
    private final String nodeId;
    private final WorkflowState state;
    
    public WorkflowExecutionException(String message) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message);
        this.workflowName = null;
        this.workflowId = null;
        this.nodeId = null;
        this.state = null;
    }
    
    public WorkflowExecutionException(String message, Throwable cause) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, cause);
        this.workflowName = null;
        this.workflowId = null;
        this.nodeId = null;
        this.state = null;
    }
    
    public WorkflowExecutionException(String workflowName, String message) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message);
        this.workflowName = workflowName;
        this.workflowId = null;
        this.nodeId = null;
        this.state = null;
        addContext("workflowName", workflowName);
    }
    
    public WorkflowExecutionException(String workflowName, String message, Throwable cause) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, cause);
        this.workflowName = workflowName;
        this.workflowId = null;
        this.nodeId = null;
        this.state = null;
        addContext("workflowName", workflowName);
    }
    
    public WorkflowExecutionException(String message, String workflowId, String nodeId, WorkflowState state) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message);
        this.workflowName = null;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.state = state;
        addContext("workflowId", workflowId);
        if (nodeId != null) addContext("nodeId", nodeId);
        if (state != null) addContext("stateVersion", state.getVersion());
    }
    
    public WorkflowExecutionException(String message, String workflowId, String nodeId, WorkflowState state, Throwable cause) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, cause);
        this.workflowName = null;
        this.workflowId = workflowId;
        this.nodeId = nodeId;
        this.state = state;
        addContext("workflowId", workflowId);
        if (nodeId != null) addContext("nodeId", nodeId);
        if (state != null) addContext("stateVersion", state.getVersion());
    }
    
    public WorkflowExecutionException(String workflowName, String message, Map<String, Object> context) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, context);
        this.workflowName = workflowName;
        this.workflowId = null;
        this.nodeId = null;
        this.state = null;
        addContext("workflowName", workflowName);
    }
    
    public String getWorkflowName() {
        return workflowName;
    }
    
    public String getWorkflowId() { 
        return workflowId; 
    }
    
    public String getNodeId() { 
        return nodeId; 
    }
    
    public WorkflowState getState() { 
        return state; 
    }
}