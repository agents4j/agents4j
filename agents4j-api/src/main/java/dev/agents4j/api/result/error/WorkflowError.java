package dev.agents4j.api.result.error;

import java.time.Instant;
import java.util.Map;

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
        INFO,
        WARNING,
        ERROR,
        CRITICAL,
    }
}
