package dev.agents4j.api.result;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Sealed interface representing different types of workflow errors.
 * Provides structured error handling with type safety and exhaustive pattern matching.
 */
public sealed interface WorkflowError 
    permits ValidationError, ExecutionError, SystemError, SecurityError {
    
    /**
     * Gets the error code.
     *
     * @return The error code
     */
    String code();
    
    /**
     * Gets the error message.
     *
     * @return The error message
     */
    String message();
    
    /**
     * Gets additional error details.
     *
     * @return Map of error details
     */
    Map<String, Object> details();
    
    /**
     * Gets the timestamp when this error occurred.
     *
     * @return The error timestamp
     */
    Instant timestamp();
    
    /**
     * Checks if this error is recoverable.
     *
     * @return true if the error is recoverable
     */
    default boolean isRecoverable() {
        return false;
    }
    
    /**
     * Gets the severity level of this error.
     *
     * @return The error severity
     */
    default ErrorSeverity severity() {
        return ErrorSeverity.ERROR;
    }
    
    /**
     * Error severity levels.
     */
    enum ErrorSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
}

/**
 * Validation-related errors.
 */
record ValidationError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String fieldName,
    Object invalidValue
) implements WorkflowError {
    
    public ValidationError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }
    
    public static ValidationError of(String code, String message, String fieldName, Object invalidValue) {
        return new ValidationError(code, message, Collections.emptyMap(), 
            Instant.now(), fieldName, invalidValue);
    }
    
    public static ValidationError required(String fieldName) {
        return new ValidationError("FIELD_REQUIRED", 
            String.format("Field '%s' is required", fieldName),
            Collections.emptyMap(), Instant.now(), fieldName, null);
    }
    
    public static ValidationError invalidFormat(String fieldName, Object value, String expectedFormat) {
        return new ValidationError("INVALID_FORMAT",
            String.format("Field '%s' has invalid format. Expected: %s, Got: %s", 
                fieldName, expectedFormat, value),
            Map.of("expectedFormat", expectedFormat),
            Instant.now(), fieldName, value);
    }
    
    @Override
    public boolean isRecoverable() {
        return true; // Validation errors are typically recoverable
    }
    
    @Override
    public ErrorSeverity severity() {
        return ErrorSeverity.WARNING;
    }
}

/**
 * Execution-related errors during workflow processing.
 */
record ExecutionError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String nodeId,
    Throwable cause
) implements WorkflowError {
    
    public ExecutionError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }
    
    public static ExecutionError of(String code, String message, String nodeId) {
        return new ExecutionError(code, message, Collections.emptyMap(), 
            Instant.now(), nodeId, null);
    }
    
    public static ExecutionError withCause(String code, String message, String nodeId, Throwable cause) {
        return new ExecutionError(code, message, 
            Map.of("causeMessage", cause.getMessage(), "causeType", cause.getClass().getName()),
            Instant.now(), nodeId, cause);
    }
    
    public static ExecutionError timeout(String nodeId, long timeoutMs) {
        return new ExecutionError("EXECUTION_TIMEOUT",
            String.format("Node '%s' execution timed out after %d ms", nodeId, timeoutMs),
            Map.of("timeoutMs", timeoutMs),
            Instant.now(), nodeId, null);
    }
    
    @Override
    public boolean isRecoverable() {
        return cause == null || !(cause instanceof OutOfMemoryError);
    }
}

/**
 * System-level errors (infrastructure, configuration, etc.).
 */
record SystemError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String component,
    ErrorSeverity severity
) implements WorkflowError {
    
    public SystemError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }
    
    public static SystemError of(String code, String message, String component) {
        return new SystemError(code, message, Collections.emptyMap(), 
            Instant.now(), component, ErrorSeverity.ERROR);
    }
    
    public static SystemError critical(String code, String message, String component) {
        return new SystemError(code, message, Collections.emptyMap(), 
            Instant.now(), component, ErrorSeverity.CRITICAL);
    }
    
    public static SystemError configurationError(String component, String configKey, String reason) {
        return new SystemError("CONFIGURATION_ERROR",
            String.format("Configuration error in %s for key '%s': %s", component, configKey, reason),
            Map.of("configKey", configKey, "component", component),
            Instant.now(), component, ErrorSeverity.CRITICAL);
    }
    
    public static SystemError resourceUnavailable(String component, String resource) {
        return new SystemError("RESOURCE_UNAVAILABLE",
            String.format("Resource '%s' is unavailable in component '%s'", resource, component),
            Map.of("resource", resource),
            Instant.now(), component, ErrorSeverity.ERROR);
    }
    
    @Override
    public boolean isRecoverable() {
        return severity != ErrorSeverity.CRITICAL;
    }
}

/**
 * Security-related errors (authentication, authorization, etc.).
 */
record SecurityError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String principal,
    String requiredPermission
) implements WorkflowError {
    
    public SecurityError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }
    
    public static SecurityError of(String code, String message, String principal) {
        return new SecurityError(code, message, Collections.emptyMap(), 
            Instant.now(), principal, null);
    }
    
    public static SecurityError unauthorized(String principal, String requiredPermission) {
        return new SecurityError("UNAUTHORIZED",
            String.format("Principal '%s' lacks required permission: %s", principal, requiredPermission),
            Map.of("requiredPermission", requiredPermission),
            Instant.now(), principal, requiredPermission);
    }
    
    public static SecurityError unauthenticated() {
        return new SecurityError("UNAUTHENTICATED",
            "Authentication required but no principal provided",
            Collections.emptyMap(), Instant.now(), null, null);
    }
    
    public static SecurityError forbidden(String principal, String action) {
        return new SecurityError("FORBIDDEN",
            String.format("Principal '%s' is forbidden from performing action: %s", principal, action),
            Map.of("action", action),
            Instant.now(), principal, null);
    }
    
    @Override
    public boolean isRecoverable() {
        return false; // Security errors are typically not recoverable
    }
    
    @Override
    public ErrorSeverity severity() {
        return ErrorSeverity.ERROR;
    }
}