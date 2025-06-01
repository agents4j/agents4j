package dev.agents4j.api.result.error;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Security-related errors (authentication, authorization, etc.).
 */
public record SecurityError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String principal,
    String requiredPermission
)
    implements WorkflowError {
    public SecurityError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null
            ? Map.copyOf(details)
            : Collections.emptyMap();
    }

    public static SecurityError of(
        String code,
        String message,
        String principal
    ) {
        return new SecurityError(
            code,
            message,
            Collections.emptyMap(),
            Instant.now(),
            principal,
            null
        );
    }

    public static SecurityError unauthorized(
        String principal,
        String requiredPermission
    ) {
        return new SecurityError(
            "UNAUTHORIZED",
            String.format(
                "Principal '%s' lacks required permission: %s",
                principal,
                requiredPermission
            ),
            Map.of("requiredPermission", requiredPermission),
            Instant.now(),
            principal,
            requiredPermission
        );
    }

    public static SecurityError unauthenticated() {
        return new SecurityError(
            "UNAUTHENTICATED",
            "Authentication required but no principal provided",
            Collections.emptyMap(),
            Instant.now(),
            null,
            null
        );
    }

    public static SecurityError forbidden(String principal, String action) {
        return new SecurityError(
            "FORBIDDEN",
            String.format(
                "Principal '%s' is forbidden from performing action: %s",
                principal,
                action
            ),
            Map.of("action", action),
            Instant.now(),
            principal,
            null
        );
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
