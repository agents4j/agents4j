package dev.agents4j.api.exception;

import java.util.Map;

/**
 * Exception thrown when workflow execution fails.
 */
public class WorkflowExecutionException extends AgentException {
    
    private final String workflowName;
    
    public WorkflowExecutionException(String workflowName, String message) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message);
        this.workflowName = workflowName;
        addContext("workflowName", workflowName);
    }
    
    public WorkflowExecutionException(String workflowName, String message, Throwable cause) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, cause);
        this.workflowName = workflowName;
        addContext("workflowName", workflowName);
    }
    
    public WorkflowExecutionException(String workflowName, String message, Map<String, Object> context) {
        super(ErrorCode.WORKFLOW_EXECUTION_FAILED, message, context);
        this.workflowName = workflowName;
        addContext("workflowName", workflowName);
    }
    
    public String getWorkflowName() {
        return workflowName;
    }
}