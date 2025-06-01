package dev.agents4j.api.graph;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable metadata for workflow state tracking versioning and timing information.
 * Provides state evolution tracking and debugging capabilities.
 */
public record StateMetadata(
    long version,
    Instant createdAt,
    Instant lastModified
) {
    
    public StateMetadata {
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        Objects.requireNonNull(lastModified, "Last modified timestamp cannot be null");
        if (version < 1) {
            throw new IllegalArgumentException("Version must be positive");
        }
        if (lastModified.isBefore(createdAt)) {
            throw new IllegalArgumentException("Last modified cannot be before created time");
        }
    }
    
    /**
     * Creates initial metadata for a new workflow state.
     *
     * @return A new StateMetadata with version 1 and current timestamp
     */
    public static StateMetadata initial() {
        Instant now = Instant.now();
        return new StateMetadata(1L, now, now);
    }
    
    /**
     * Creates metadata with a specific creation time.
     *
     * @param createdAt The creation timestamp
     * @return A new StateMetadata with version 1 and specified creation time
     */
    public static StateMetadata createdAt(Instant createdAt) {
        Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
        return new StateMetadata(1L, createdAt, createdAt);
    }
    
    /**
     * Advances the version and updates the last modified timestamp.
     *
     * @return A new StateMetadata with incremented version and current timestamp
     */
    public StateMetadata advance() {
        return new StateMetadata(version + 1, createdAt, Instant.now());
    }
    
    /**
     * Advances to a specific version.
     *
     * @param newVersion The new version number
     * @return A new StateMetadata with the specified version
     * @throws IllegalArgumentException if newVersion is not greater than current version
     */
    public StateMetadata withVersion(long newVersion) {
        if (newVersion <= version) {
            throw new IllegalArgumentException("New version must be greater than current version");
        }
        return new StateMetadata(newVersion, createdAt, Instant.now());
    }
    
    /**
     * Gets the age of the state since creation.
     *
     * @return Duration since creation
     */
    public java.time.Duration getAge() {
        return java.time.Duration.between(createdAt, Instant.now());
    }
    
    /**
     * Gets the time since last modification.
     *
     * @return Duration since last modification
     */
    public java.time.Duration getTimeSinceLastModified() {
        return java.time.Duration.between(lastModified, Instant.now());
    }
    
    /**
     * Checks if this state is newer than another state.
     *
     * @param other The other StateMetadata to compare
     * @return true if this state has a higher version
     */
    public boolean isNewerThan(StateMetadata other) {
        Objects.requireNonNull(other, "Other metadata cannot be null");
        return this.version > other.version;
    }
    
    /**
     * Checks if this state is older than another state.
     *
     * @param other The other StateMetadata to compare
     * @return true if this state has a lower version
     */
    public boolean isOlderThan(StateMetadata other) {
        Objects.requireNonNull(other, "Other metadata cannot be null");
        return this.version < other.version;
    }
    
    /**
     * Checks if the state was created before a specific time.
     *
     * @param timestamp The timestamp to compare against
     * @return true if state was created before the timestamp
     */
    public boolean wasCreatedBefore(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        return createdAt.isBefore(timestamp);
    }
    
    /**
     * Checks if the state was modified after a specific time.
     *
     * @param timestamp The timestamp to compare against
     * @return true if state was modified after the timestamp
     */
    public boolean wasModifiedAfter(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        return lastModified.isAfter(timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("StateMetadata{version=%d, age=%s, lastModified=%s}", 
            version, getAge(), getTimeSinceLastModified());
    }
}