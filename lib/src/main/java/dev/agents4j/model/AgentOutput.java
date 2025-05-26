package dev.agents4j.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents output data from an agent in the workflow.
 * This class provides a flexible way to return output content along with
 * metadata and additional results from agent nodes.
 */
public class AgentOutput {

    private final String content;
    private final Map<String, Object> metadata;
    private final Map<String, Object> results;
    private final boolean successful;

    /**
     * Creates a new successful AgentOutput with the given content.
     *
     * @param content The primary content from the agent
     */
    public AgentOutput(String content) {
        this(content, true);
    }

    /**
     * Creates a new AgentOutput with the given content and success status.
     *
     * @param content The primary content from the agent
     * @param successful Whether the agent processed the input successfully
     */
    public AgentOutput(String content, boolean successful) {
        this(content, new HashMap<>(), new HashMap<>(), successful);
    }

    /**
     * Creates a new AgentOutput with the given content, metadata, and success status.
     *
     * @param content The primary content from the agent
     * @param metadata Additional metadata about the output
     * @param successful Whether the agent processed the input successfully
     */
    public AgentOutput(
        String content,
        Map<String, Object> metadata,
        boolean successful
    ) {
        this(content, metadata, new HashMap<>(), successful);
    }

    /**
     * Creates a new AgentOutput with the given content, metadata, results, and success status.
     *
     * @param content The primary content from the agent
     * @param metadata Additional metadata about the output
     * @param results Additional results produced by the agent
     * @param successful Whether the agent processed the input successfully
     */
    public AgentOutput(
        String content,
        Map<String, Object> metadata,
        Map<String, Object> results,
        boolean successful
    ) {
        this.content = content;
        this.metadata = new HashMap<>(metadata);
        this.results = new HashMap<>(results);
        this.successful = successful;
    }

    /**
     * Gets the primary content of this output.
     *
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets an unmodifiable view of the metadata map.
     *
     * @return The metadata map
     */
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    /**
     * Gets an unmodifiable view of the results map.
     *
     * @return The results map
     */
    public Map<String, Object> getResults() {
        return Collections.unmodifiableMap(results);
    }

    /**
     * Checks if the agent processed the input successfully.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Gets a metadata value.
     *
     * @param key The metadata key
     * @param <T> The expected type of the value
     * @return The metadata value wrapped in an Optional, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadataValue(String key) {
        return Optional.ofNullable((T) metadata.get(key));
    }

    /**
     * Gets a result value.
     *
     * @param key The result key
     * @param <T> The expected type of the value
     * @return The result value wrapped in an Optional, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getResultValue(String key) {
        return Optional.ofNullable((T) results.get(key));
    }

    /**
     * Creates a new builder for AgentOutput.
     *
     * @param content The primary content
     * @return A new Builder instance
     */
    public static Builder builder(String content) {
        return new Builder(content);
    }

    /**
     * Builder for creating AgentOutput instances.
     */
    public static class Builder {

        private final String content;
        private final Map<String, Object> metadata = new HashMap<>();
        private final Map<String, Object> results = new HashMap<>();
        private boolean successful = true;

        private Builder(String content) {
            this.content = content;
        }

        /**
         * Sets the success status of the output.
         *
         * @param successful Whether the processing was successful
         * @return This builder instance for method chaining
         */
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder instance for method chaining
         */
        public Builder withMetadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        /**
         * Adds a result entry.
         *
         * @param key The result key
         * @param value The result value
         * @return This builder instance for method chaining
         */
        public Builder withResult(String key, Object value) {
            results.put(key, value);
            return this;
        }

        /**
         * Adds all entries from the given metadata map.
         *
         * @param metadata The metadata map to add
         * @return This builder instance for method chaining
         */
        public Builder withAllMetadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Adds all entries from the given results map.
         *
         * @param results The results map to add
         * @return This builder instance for method chaining
         */
        public Builder withAllResults(Map<String, Object> results) {
            this.results.putAll(results);
            return this;
        }

        /**
         * Builds the AgentOutput instance.
         *
         * @return A new AgentOutput instance
         */
        public AgentOutput build() {
            return new AgentOutput(content, metadata, results, successful);
        }
    }
}
