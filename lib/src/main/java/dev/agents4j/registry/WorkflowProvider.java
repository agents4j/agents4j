/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import dev.agents4j.api.AgentWorkflow;

import java.util.Map;

/**
 * Interface for providing workflow instances.
 * Implementations should be able to create workflows of a specific type
 * based on the provided configuration.
 *
 * @param <I> The input type for workflows created by this provider
 * @param <O> The output type for workflows created by this provider
 */
public interface WorkflowProvider<I, O> {

    /**
     * Creates a new workflow instance based on the provided configuration.
     *
     * @param config The configuration for creating the workflow
     * @return A new workflow instance
     * @throws WorkflowCreationException if workflow creation fails
     */
    AgentWorkflow<I, O> create(WorkflowConfig config) throws WorkflowCreationException;

    /**
     * Gets the workflow type that this provider creates.
     *
     * @return The workflow type identifier
     */
    String getWorkflowType();

    /**
     * Gets metadata about the workflows this provider creates.
     *
     * @return Workflow type metadata
     */
    WorkflowTypeMetadata getMetadata();

    /**
     * Validates that the provider can create workflows with the given configuration.
     *
     * @param config The configuration to validate
     * @return true if the configuration is valid for this provider
     */
    boolean supportsConfiguration(WorkflowConfig config);

    /**
     * Gets the required configuration keys for this provider.
     *
     * @return Array of required configuration keys
     */
    String[] getRequiredConfigurationKeys();

    /**
     * Gets the optional configuration keys for this provider.
     *
     * @return Array of optional configuration keys
     */
    default String[] getOptionalConfigurationKeys() {
        return new String[0];
    }

    /**
     * Gets default configuration values for this provider.
     *
     * @return Map of default configuration values
     */
    default Map<String, Object> getDefaultConfiguration() {
        return Map.of();
    }

    /**
     * Checks if this provider is available and ready to create workflows.
     *
     * @return true if the provider is available
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Gets the priority of this provider when multiple providers
     * are registered for the same workflow type.
     *
     * @return Priority value (higher values have higher priority)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Gets version information for this provider.
     *
     * @return Version string
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Gets a description of what this provider does.
     *
     * @return Provider description
     */
    String getDescription();

    /**
     * Validates the provider's current state and configuration.
     *
     * @throws WorkflowProviderException if validation fails
     */
    default void validate() throws WorkflowProviderException {
        // Default implementation does nothing
    }
}