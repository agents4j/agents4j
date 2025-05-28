package dev.agents4j.api.exception;

/**
 * Enumeration of error codes for structured error handling.
 */
public enum ErrorCode {
    
    // Validation errors (1000-1999)
    INVALID_INPUT("AGT-1001", "Invalid input provided"),
    INVALID_CONFIGURATION("AGT-1002", "Invalid configuration"),
    MISSING_REQUIRED_PARAMETER("AGT-1003", "Missing required parameter"),
    
    // Workflow errors (2000-2999)
    WORKFLOW_EXECUTION_FAILED("AGT-2001", "Workflow execution failed"),
    WORKFLOW_TIMEOUT("AGT-2002", "Workflow execution timed out"),
    WORKFLOW_INTERRUPTED("AGT-2003", "Workflow execution was interrupted"),
    
    // Agent errors (3000-3999)
    AGENT_PROCESSING_FAILED("AGT-3001", "Agent processing failed"),
    AGENT_INITIALIZATION_FAILED("AGT-3002", "Agent initialization failed"),
    AGENT_NOT_FOUND("AGT-3003", "Agent not found"),
    
    // Provider errors (4000-4999)
    PROVIDER_ERROR("AGT-4001", "Provider error"),
    PROVIDER_RATE_LIMIT("AGT-4002", "Provider rate limit exceeded"),
    PROVIDER_AUTHENTICATION_FAILED("AGT-4003", "Provider authentication failed"),
    
    // System errors (5000-5999)
    INTERNAL_ERROR("AGT-5001", "Internal system error"),
    RESOURCE_EXHAUSTED("AGT-5002", "System resources exhausted"),
    CONFIGURATION_ERROR("AGT-5003", "System configuration error");
    
    private final String code;
    private final String description;
    
    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }
}