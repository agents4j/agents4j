/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import dev.agents4j.api.AgentWorkflow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry for workflow providers following the Open/Closed Principle.
 * This interface allows new workflow types to be registered without modifying existing code.
 */
public interface WorkflowRegistry {

    /**
     * Registers a workflow provider for a specific workflow type.
     *
     * @param workflowType The type identifier for the workflow
     * @param provider The provider that can create workflows of this type
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @throws IllegalArgumentException if workflowType is null or empty, or provider is null
     * @throws WorkflowRegistrationException if registration fails
     */
    <I, O> void registerWorkflowType(String workflowType, WorkflowProvider<I, O> provider) 
        throws WorkflowRegistrationException;

    /**
     * Unregisters a workflow provider for a specific workflow type.
     *
     * @param workflowType The type identifier for the workflow
     * @return true if a provider was removed, false if no provider was registered for this type
     */
    boolean unregisterWorkflowType(String workflowType);

    /**
     * Creates a workflow of the specified type using the registered provider.
     *
     * @param workflowType The type of workflow to create
     * @param config Configuration for the workflow
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @return The created workflow wrapped in an Optional, or empty if no provider is registered
     * @throws WorkflowCreationException if workflow creation fails
     */
    <I, O> Optional<AgentWorkflow<I, O>> createWorkflow(String workflowType, WorkflowConfig config) 
        throws WorkflowCreationException;

    /**
     * Checks if a workflow type is registered.
     *
     * @param workflowType The workflow type to check
     * @return true if the type is registered, false otherwise
     */
    boolean isRegistered(String workflowType);

    /**
     * Gets all registered workflow types.
     *
     * @return Set of registered workflow type identifiers
     */
    Set<String> getRegisteredTypes();

    /**
     * Gets the provider for a specific workflow type.
     *
     * @param workflowType The workflow type
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @return The provider wrapped in an Optional, or empty if not registered
     */
    <I, O> Optional<WorkflowProvider<I, O>> getProvider(String workflowType);

    /**
     * Gets metadata about a registered workflow type.
     *
     * @param workflowType The workflow type
     * @return Metadata about the workflow type, or empty if not registered
     */
    Optional<WorkflowTypeMetadata> getWorkflowMetadata(String workflowType);

    /**
     * Lists all available workflow providers with their metadata.
     *
     * @return List of workflow provider information
     */
    List<WorkflowProviderInfo> listProviders();

    /**
     * Validates that all registered providers are properly configured.
     *
     * @return ValidationResult containing any validation errors
     */
    ValidationResult validateProviders();

    /**
     * Clears all registered providers.
     */
    void clear();

    /**
     * Gets the number of registered providers.
     *
     * @return The provider count
     */
    int getProviderCount();

    /**
     * Creates a new default implementation of WorkflowRegistry.
     *
     * @return A new DefaultWorkflowRegistry instance
     */
    static WorkflowRegistry create() {
        return new DefaultWorkflowRegistry();
    }

    /**
     * Creates a new thread-safe implementation of WorkflowRegistry.
     *
     * @return A new ConcurrentWorkflowRegistry instance
     */
    static WorkflowRegistry createConcurrent() {
        return new ConcurrentWorkflowRegistry();
    }

    /**
     * Information about a registered workflow provider.
     */
    class WorkflowProviderInfo {
        private final String workflowType;
        private final WorkflowTypeMetadata metadata;
        private final boolean isAvailable;

        public WorkflowProviderInfo(String workflowType, WorkflowTypeMetadata metadata, boolean isAvailable) {
            this.workflowType = workflowType;
            this.metadata = metadata;
            this.isAvailable = isAvailable;
        }

        public String getWorkflowType() {
            return workflowType;
        }

        public WorkflowTypeMetadata getMetadata() {
            return metadata;
        }

        public boolean isAvailable() {
            return isAvailable;
        }
    }

    /**
     * Validation result for provider validation.
     */
    class ValidationResult {
        private final boolean valid;
        private final Map<String, String> errors;

        public ValidationResult(boolean valid, Map<String, String> errors) {
            this.valid = valid;
            this.errors = Map.copyOf(errors != null ? errors : Map.of());
        }

        public boolean isValid() {
            return valid;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, Map.of());
        }

        public static ValidationResult failure(Map<String, String> errors) {
            return new ValidationResult(false, errors);
        }
    }
}