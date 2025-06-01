package dev.agents4j.api.result.error;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Validation-related errors.
 */
public record ValidationError(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp,
    String fieldName,
    Object invalidValue
)
    implements WorkflowError {
    public ValidationError {
        Objects.requireNonNull(code, "Error code cannot be null");
        Objects.requireNonNull(message, "Error message cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        details = details != null
            ? Map.copyOf(details)
            : Collections.emptyMap();
    }

    public static ValidationError of(
        String code,
        String message,
        String fieldName,
        Object invalidValue
    ) {
        return new ValidationError(
            code,
            message,
            Collections.emptyMap(),
            Instant.now(),
            fieldName,
            invalidValue
        );
    }

    public static ValidationError required(String fieldName) {
        return new ValidationError(
            "FIELD_REQUIRED",
            String.format("Field '%s' is required", fieldName),
            Collections.emptyMap(),
            Instant.now(),
            fieldName,
            null
        );
    }

    public static ValidationError invalidFormat(
        String fieldName,
        Object value,
        String expectedFormat
    ) {
        return new ValidationError(
            "INVALID_FORMAT",
            String.format(
                "Field '%s' has invalid format. Expected: %s, Got: %s",
                fieldName,
                expectedFormat,
                value
            ),
            Map.of("expectedFormat", expectedFormat),
            Instant.now(),
            fieldName,
            value
        );
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
