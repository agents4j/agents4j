package dev.agents4j.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents input data for an agent in the workflow.
 * This class provides a flexible way to pass input content along with
 * metadata and parameters to agent nodes.
 */
public class AgentInput {

    private final String content;
    private final Map<String, Object> metadata;
    private final Map<String, Object> parameters;

    /**
     * Creates a new AgentInput with the given content.
     *
     * @param content The primary content for the agent
     */
    public AgentInput(String content) {
        this(content, new HashMap<>(), new HashMap<>());
    }

    /**
     * Creates a new AgentInput with the given content and metadata.
     *
     * @param content The primary content for the agent
     * @param metadata Additional metadata about the input
     */
    public AgentInput(String content, Map<String, Object> metadata) {
        this(content, metadata, new HashMap<>());
    }

    /**
     * Creates a new AgentInput with the given content, metadata, and parameters.
     *
     * @param content The primary content for the agent
     * @param metadata Additional metadata about the input
     * @param parameters Parameters that control the agent's behavior
     */
    public AgentInput(
        String content,
        Map<String, Object> metadata,
        Map<String, Object> parameters
    ) {
        this.content = content;
        this.metadata = new HashMap<>(metadata);
        this.parameters = new HashMap<>(parameters);
    }

    /**
     * Gets the primary content of this input.
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
     * Gets an unmodifiable view of the parameters map.
     *
     * @return The parameters map
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
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
     * Gets a parameter value.
     *
     * @param key The parameter key
     * @param <T> The expected type of the value
     * @return The parameter value wrapped in an Optional, or empty if not found
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getParameterValue(String key) {
        return Optional.ofNullable((T) parameters.get(key));
    }

    /**
     * Creates a new builder for AgentInput.
     *
     * @param content The primary content
     * @return A new Builder instance
     */
    public static Builder builder(String content) {
        return new Builder(content);
    }

    /**
     * Builder for creating AgentInput instances.
     */
    public static class Builder {

        private final String content;
        private final Map<String, Object> metadata = new HashMap<>();
        private final Map<String, Object> parameters = new HashMap<>();

        private Builder(String content) {
            this.content = content;
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
         * Adds a parameter entry.
         *
         * @param key The parameter key
         * @param value The parameter value
         * @return This builder instance for method chaining
         */
        public Builder withParameter(String key, Object value) {
            parameters.put(key, value);
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
         * Adds all entries from the given parameters map.
         *
         * @param parameters The parameters map to add
         * @return This builder instance for method chaining
         */
        public Builder withAllParameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);
            return this;
        }

        /**
         * Builds the AgentInput instance.
         *
         * @return A new AgentInput instance
         */
        public AgentInput build() {
            return new AgentInput(content, metadata, parameters);
        }
    }
}
