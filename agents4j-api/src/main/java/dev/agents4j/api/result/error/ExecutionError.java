package dev.agents4j.api.result.error;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Execution-related errors during workflow processing.
 */
public record ExecutionError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String nodeId,
    Throwable cause
)
    implements WorkflowError {
    public ExecutionError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null
            ? Map.copyOf(details)
            : Collections.emptyMap();
    }

    public static ExecutionError of(
        String code,
        String message,
        String nodeId
    ) {
        return new ExecutionError(
            code,
            message,
            Collections.emptyMap(),
            Instant.now(),
            nodeId,
            null
        );
    }

    public static ExecutionError withCause(
        String code,
        String message,
        String nodeId,
        Throwable cause
    ) {
        return new ExecutionError(
            code,
            message,
            Map.of(
                "causeMessage",
                cause.getMessage(),
                "causeType",
                cause.getClass().getName()
            ),
            Instant.now(),
            nodeId,
            cause
        );
    }

    public static ExecutionError timeout(String nodeId, long timeoutMs) {
        return new ExecutionError(
            "EXECUTION_TIMEOUT",
            String.format(
                "Node '%s' execution timed out after %d ms",
                nodeId,
                timeoutMs
            ),
            Map.of("timeoutMs", timeoutMs),
            Instant.now(),
            nodeId,
            null
        );
    }

    @Override
    public boolean isRecoverable() {
        return cause == null || !(cause instanceof OutOfMemoryError);
    }
}
