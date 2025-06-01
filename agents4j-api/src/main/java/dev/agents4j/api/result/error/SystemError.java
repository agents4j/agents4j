package dev.agents4j.api.result.error;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * System-level errors (infrastructure, configuration, etc.).
 */
public record SystemError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String component,
    ErrorSeverity severity
)
    implements WorkflowError {
    public SystemError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        details = details != null
            ? Map.copyOf(details)
            : Collections.emptyMap();
    }

    public static SystemError of(
        String code,
        String message,
        String component
    ) {
        return new SystemError(
            code,
            message,
            Collections.emptyMap(),
            Instant.now(),
            component,
            ErrorSeverity.ERROR
        );
    }

    public static SystemError critical(
        String code,
        String message,
        String component
    ) {
        return new SystemError(
            code,
            message,
            Collections.emptyMap(),
            Instant.now(),
            component,
            ErrorSeverity.CRITICAL
        );
    }

    public static SystemError configurationError(
        String component,
        String configKey,
        String reason
    ) {
        return new SystemError(
            "CONFIGURATION_ERROR",
            String.format(
                "Configuration error in %s for key '%s': %s",
                component,
                configKey,
                reason
            ),
            Map.of("configKey", configKey, "component", component),
            Instant.now(),
            component,
            ErrorSeverity.CRITICAL
        );
    }

    public static SystemError resourceUnavailable(
        String component,
        String resource
    ) {
        return new SystemError(
            "RESOURCE_UNAVAILABLE",
            String.format(
                "Resource '%s' is unavailable in component '%s'",
                resource,
                component
            ),
            Map.of("resource", resource),
            Instant.now(),
            component,
            ErrorSeverity.ERROR
        );
    }

    @Override
    public boolean isRecoverable() {
        return severity != ErrorSeverity.CRITICAL;
    }
}
