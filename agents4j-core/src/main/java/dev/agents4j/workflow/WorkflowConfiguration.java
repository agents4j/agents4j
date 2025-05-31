package dev.agents4j.workflow;

import dev.agents4j.api.AgentWorkflow;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Configuration class for agent workflows.
 * This class provides centralized configuration for workflow settings
 * and execution parameters.
 */
public class WorkflowConfiguration {

    private final Map<String, Object> properties = new HashMap<>();
    private ChatModel defaultModel;
    private ChatMemory defaultMemory;

    /**
     * Creates a new WorkflowConfiguration with default settings.
     */
    public WorkflowConfiguration() {
        // Default constructor
    }

    /**
     * Sets a configuration property.
     *
     * @param key The property key
     * @param value The property value
     * @return This configuration instance for method chaining
     */
    public WorkflowConfiguration setProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    /**
     * Gets a configuration property.
     *
     * @param key The property key
     * @param <T> The expected type of the property value
     * @return The property value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) {
        return (T) properties.get(key);
    }

    /**
     * Gets a configuration property with a default value.
     *
     * @param key The property key
     * @param defaultValue The default value to return if the property is not found
     * @param <T> The expected type of the property value
     * @return The property value, or the default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    /**
     * Sets the default ChatModel to use for agents that don't specify one.
     *
     * @param model The default model
     * @return This configuration instance for method chaining
     */
    public WorkflowConfiguration setDefaultModel(ChatModel model) {
        this.defaultModel = model;
        return this;
    }

    /**
     * Gets the default ChatModel.
     *
     * @return The default model, or null if not set
     */
    public ChatModel getDefaultModel() {
        return defaultModel;
    }

    /**
     * Sets the default ChatMemory to use for agents that don't specify one.
     *
     * @param memory The default memory
     * @return This configuration instance for method chaining
     */
    public WorkflowConfiguration setDefaultMemory(ChatMemory memory) {
        this.defaultMemory = memory;
        return this;
    }

    /**
     * Gets the default ChatMemory.
     *
     * @return The default memory, or null if not set
     */
    public ChatMemory getDefaultMemory() {
        return defaultMemory;
    }

    /**
     * Create a new workflow with this configuration.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param workflowBuilder A consumer that configures a workflow builder
     * @return The configured workflow
     */
    public <I, O> AgentWorkflow<I, O> createWorkflow(
        Consumer<ChainWorkflow.Builder<I, O>> workflowBuilder
    ) {
        ChainWorkflow.Builder<I, O> builder = ChainWorkflow.builder();
        workflowBuilder.accept(builder);
        return builder.build();
    }

    /**
     * Configures an existing workflow with this configuration's settings.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param workflow The workflow to configure
     * @return The configured workflow
     */
    public <I, O> AgentWorkflow<I, O> configureWorkflow(
        AgentWorkflow<I, O> workflow
    ) {
        // Apply any global configuration to the workflow
        // This could involve setting up monitoring, logging, etc.
        return workflow;
    }

    /**
     * Creates an execution context map with the current configuration properties.
     *
     * @return A map containing the execution context
     */
    public Map<String, Object> createExecutionContext() {
        return new HashMap<>(properties);
    }

    /**
     * Builder for creating WorkflowConfiguration instances.
     */
    public static class Builder {

        private final WorkflowConfiguration config =
            new WorkflowConfiguration();

        /**
         * Set a configuration property.
         *
         * @param key The property key
         * @param value The property value
         * @return This builder instance for method chaining
         */
        public Builder property(String key, Object value) {
            config.setProperty(key, value);
            return this;
        }

        /**
         * Set the default model for the configuration.
         *
         * @param model The default ChatModel to use
         * @return This builder instance for method chaining
         */
        public Builder defaultModel(ChatModel model) {
            config.setDefaultModel(model);
            return this;
        }

        /**
         * Set the default memory for the configuration.
         *
         * @param memory The default ChatMemory to use
         * @return This builder instance for method chaining
         */
        public Builder defaultMemory(ChatMemory memory) {
            config.setDefaultMemory(memory);
            return this;
        }

        /**
         * Build the WorkflowConfiguration instance.
         *
         * @return A new WorkflowConfiguration instance
         */
        public WorkflowConfiguration build() {
            return config;
        }
    }

    /**
     * Create a new Builder to construct a WorkflowConfiguration.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
