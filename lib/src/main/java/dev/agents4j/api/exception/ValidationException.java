package dev.agents4j.api.exception;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends AgentException {
    
    private final List<String> validationErrors;
    
    public ValidationException(String message, List<String> validationErrors) {
        super(ErrorCode.INVALID_INPUT, message);
        this.validationErrors = validationErrors;
        addContext("validationErrors", validationErrors);
    }
    
    public ValidationException(String message, List<String> validationErrors, Map<String, Object> context) {
        super(ErrorCode.INVALID_INPUT, message, context);
        this.validationErrors = validationErrors;
        addContext("validationErrors", validationErrors);
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}