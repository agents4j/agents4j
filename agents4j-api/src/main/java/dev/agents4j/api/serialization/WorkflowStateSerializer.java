package dev.agents4j.api.serialization;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for serializing and deserializing workflow states with version awareness.
 * Provides standardized serialization strategies to handle workflow state persistence
 * across different workflow versions and schema changes.
 *
 * @param <S> The type of workflow state to serialize
 */
public interface WorkflowStateSerializer<S> {
    /**
     * Serializes a workflow state to a string representation.
     *
     * @param state The workflow state to serialize
     * @param workflowVersion The current workflow version
     * @return Serialized state as string
     * @throws SerializationException if serialization fails
     */
    String serialize(S state, String workflowVersion)
        throws SerializationException;

    /**
     * Deserializes a workflow state from string representation.
     *
     * @param serializedState The serialized state string
     * @param expectedVersion The expected workflow version
     * @return The deserialized workflow state
     * @throws DeserializationException if deserialization fails
     * @throws VersionMismatchException if version is incompatible
     */
    S deserialize(String serializedState, String expectedVersion)
        throws DeserializationException, VersionMismatchException;

    /**
     * Checks if a serialized version is compatible with the current version.
     *
     * @param serializedVersion The version from serialized data
     * @param currentVersion The current workflow version
     * @return true if versions are compatible
     */
    boolean isCompatible(String serializedVersion, String currentVersion);

    /**
     * Attempts to migrate a serialized state from one version to another.
     *
     * @param serializedState The serialized state to migrate
     * @param fromVersion The source version
     * @param toVersion The target version
     * @return Optional containing migrated state, empty if migration not possible
     * @throws MigrationException if migration fails
     */
    default Optional<String> migrate(
        String serializedState,
        String fromVersion,
        String toVersion
    ) throws MigrationException {
        return Optional.empty();
    }

    /**
     * Gets the supported serialization format.
     *
     * @return The format identifier (e.g., "json", "binary", "xml")
     */
    default String getFormat() {
        return "json";
    }

    /**
     * Gets metadata about this serializer.
     *
     * @return Map containing serializer metadata
     */
    default Map<String, Object> getMetadata() {
        return Map.of(
            "format",
            getFormat(),
            "serializerClass",
            getClass().getSimpleName(),
            "supportsVersioning",
            true,
            "supportsMigration",
            true
        );
    }

    /**
     * Validates that a serialized state can be deserialized.
     *
     * @param serializedState The serialized state to validate
     * @return Validation result
     */
    default ValidationResult validate(String serializedState) {
        try {
            // Extract version from serialized data for basic validation
            String version = extractVersion(serializedState);
            if (version == null || version.isEmpty()) {
                return ValidationResult.invalid("No version information found");
            }
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid(
                "Validation failed: " + e.getMessage()
            );
        }
    }

    /**
     * Extracts version information from serialized state without full deserialization.
     *
     * @param serializedState The serialized state
     * @return The version string, or null if not found
     */
    default String extractVersion(String serializedState) {
        // Default implementation - subclasses should override for efficiency
        try {
            S state = deserialize(serializedState, "any");
            if (state instanceof VersionedState versionedState) {
                return versionedState.getVersion();
            }
        } catch (Exception e) {
            // Ignore and return null
        }
        return null;
    }

    /**
     * Exception thrown when serialization fails.
     */
    class SerializationException extends Exception {

        public SerializationException(String message) {
            super(message);
        }

        public SerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when deserialization fails.
     */
    class DeserializationException extends Exception {

        public DeserializationException(String message) {
            super(message);
        }

        public DeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when workflow versions are incompatible.
     */
    class VersionMismatchException extends Exception {

        private final String serializedVersion;
        private final String expectedVersion;

        public VersionMismatchException(
            String serializedVersion,
            String expectedVersion
        ) {
            super(
                String.format(
                    "Version mismatch: serialized=%s, expected=%s",
                    serializedVersion,
                    expectedVersion
                )
            );
            this.serializedVersion = serializedVersion;
            this.expectedVersion = expectedVersion;
        }

        public String getSerializedVersion() {
            return serializedVersion;
        }

        public String getExpectedVersion() {
            return expectedVersion;
        }
    }

    /**
     * Exception thrown when state migration fails.
     */
    class MigrationException extends Exception {

        public MigrationException(String message) {
            super(message);
        }

        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Interface for states that carry version information.
     */
    interface VersionedState {
        String getVersion();
    }

    /**
     * Validation result for serialized state.
     */
    record ValidationResult(boolean _valid, String message) {
        public static ValidationResult valid() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return _valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
