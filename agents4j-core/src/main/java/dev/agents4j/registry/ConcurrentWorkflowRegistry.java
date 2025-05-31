/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import dev.agents4j.api.Workflow;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe implementation of WorkflowRegistry with enhanced concurrency support.
 * This implementation uses read-write locks for better performance in read-heavy scenarios.
 */
public class ConcurrentWorkflowRegistry implements WorkflowRegistry {

    private final Map<String, WorkflowProvider<?, ?>> providers =
        new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public <I, O> void registerWorkflowType(
        String workflowType,
        WorkflowProvider<I, O> provider
    ) throws WorkflowRegistrationException {
        if (workflowType == null || workflowType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Workflow type cannot be null or empty"
            );
        }
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        lock.writeLock().lock();
        try {
            // Validate the provider before registration
            provider.validate();

            WorkflowProvider<?, ?> existing = providers.put(
                workflowType,
                provider
            );
            if (existing != null) {
                // Log that we're replacing an existing provider
                System.out.println(
                    "Warning: Replacing existing provider for workflow type: " +
                    workflowType
                );
            }
        } catch (WorkflowProviderException e) {
            throw new WorkflowRegistrationException(
                workflowType,
                "Provider validation failed: " + e.getMessage(),
                e
            );
        } catch (Exception e) {
            throw new WorkflowRegistrationException(
                workflowType,
                "Failed to register workflow provider: " + e.getMessage(),
                e
            );
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean unregisterWorkflowType(String workflowType) {
        if (workflowType == null || workflowType.trim().isEmpty()) {
            return false;
        }

        lock.writeLock().lock();
        try {
            return providers.remove(workflowType) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> Optional<Workflow<I, O>> createWorkflow(
        String workflowType,
        WorkflowConfig config
    ) throws WorkflowCreationException {
        if (workflowType == null || workflowType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Workflow type cannot be null or empty"
            );
        }
        if (config == null) {
            throw new IllegalArgumentException(
                "Workflow config cannot be null"
            );
        }

        WorkflowProvider<I, O> provider;
        lock.readLock().lock();
        try {
            provider = (WorkflowProvider<I, O>) providers.get(workflowType);
        } finally {
            lock.readLock().unlock();
        }

        if (provider == null) {
            return Optional.empty();
        }

        try {
            // Check if provider supports the configuration
            if (!provider.supportsConfiguration(config)) {
                throw new WorkflowCreationException(
                    workflowType,
                    "Provider does not support the provided configuration"
                );
            }

            Workflow<I, O> workflow = provider.create(config);
            return Optional.of(workflow);
        } catch (WorkflowCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new WorkflowCreationException(
                workflowType,
                "Failed to create workflow: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean isRegistered(String workflowType) {
        if (workflowType == null) {
            return false;
        }

        lock.readLock().lock();
        try {
            return providers.containsKey(workflowType);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getRegisteredTypes() {
        lock.readLock().lock();
        try {
            return Set.copyOf(providers.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> Optional<WorkflowProvider<I, O>> getProvider(
        String workflowType
    ) {
        if (workflowType == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            return Optional.ofNullable(
                (WorkflowProvider<I, O>) providers.get(workflowType)
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<WorkflowTypeMetadata> getWorkflowMetadata(
        String workflowType
    ) {
        lock.readLock().lock();
        try {
            WorkflowProvider<?, ?> provider = providers.get(workflowType);
            return provider != null
                ? Optional.of(provider.getMetadata())
                : Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<WorkflowProviderInfo> listProviders() {
        Map<String, WorkflowProvider<?, ?>> providersCopy;
        lock.readLock().lock();
        try {
            providersCopy = new HashMap<>(providers);
        } finally {
            lock.readLock().unlock();
        }

        return providersCopy
            .entrySet()
            .stream()
            .map(entry -> {
                String type = entry.getKey();
                WorkflowProvider<?, ?> provider = entry.getValue();
                WorkflowTypeMetadata metadata = provider.getMetadata();
                boolean isAvailable = provider.isAvailable();
                return new WorkflowProviderInfo(type, metadata, isAvailable);
            })
            .sorted(Comparator.comparing(WorkflowProviderInfo::getWorkflowType))
            .toList();
    }

    @Override
    public ValidationResult validateProviders() {
        Map<String, WorkflowProvider<?, ?>> providersCopy;
        lock.readLock().lock();
        try {
            providersCopy = new HashMap<>(providers);
        } finally {
            lock.readLock().unlock();
        }

        Map<String, String> errors = new HashMap<>();

        for (Map.Entry<
            String,
            WorkflowProvider<?, ?>
        > entry : providersCopy.entrySet()) {
            String type = entry.getKey();
            WorkflowProvider<?, ?> provider = entry.getValue();

            try {
                // Validate the provider
                provider.validate();

                // Check if provider is available
                if (!provider.isAvailable()) {
                    errors.put(type, "Provider is not available");
                }

                // Validate metadata
                WorkflowTypeMetadata metadata = provider.getMetadata();
                if (metadata == null) {
                    errors.put(type, "Provider metadata is null");
                } else if (
                    metadata.getName() == null ||
                    metadata.getName().trim().isEmpty()
                ) {
                    errors.put(type, "Provider metadata name is null or empty");
                }
            } catch (WorkflowProviderException e) {
                errors.put(
                    type,
                    "Provider validation failed: " + e.getMessage()
                );
            } catch (Exception e) {
                errors.put(
                    type,
                    "Unexpected error during validation: " + e.getMessage()
                );
            }
        }

        return errors.isEmpty()
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            providers.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getProviderCount() {
        lock.readLock().lock();
        try {
            return providers.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all providers (for debugging/testing purposes).
     * Returns a snapshot of the current providers.
     *
     * @return Map of workflow types to providers
     */
    protected Map<String, WorkflowProvider<?, ?>> getAllProviders() {
        lock.readLock().lock();
        try {
            return Map.copyOf(providers);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Validates a specific provider without registering it.
     *
     * @param workflowType The workflow type
     * @param provider The provider to validate
     * @return ValidationResult indicating if the provider is valid
     */
    public ValidationResult validateProvider(
        String workflowType,
        WorkflowProvider<?, ?> provider
    ) {
        if (workflowType == null || workflowType.trim().isEmpty()) {
            return ValidationResult.failure(
                Map.of(workflowType, "Workflow type cannot be null or empty")
            );
        }
        if (provider == null) {
            return ValidationResult.failure(
                Map.of(workflowType, "Provider cannot be null")
            );
        }

        try {
            provider.validate();

            if (!provider.isAvailable()) {
                return ValidationResult.failure(
                    Map.of(workflowType, "Provider is not available")
                );
            }

            WorkflowTypeMetadata metadata = provider.getMetadata();
            if (metadata == null) {
                return ValidationResult.failure(
                    Map.of(workflowType, "Provider metadata is null")
                );
            }

            return ValidationResult.success();
        } catch (WorkflowProviderException e) {
            return ValidationResult.failure(
                Map.of(workflowType, "Validation failed: " + e.getMessage())
            );
        } catch (Exception e) {
            return ValidationResult.failure(
                Map.of(workflowType, "Unexpected error: " + e.getMessage())
            );
        }
    }

    /**
     * Gets providers sorted by priority (highest first).
     * Returns a snapshot of providers at the time of call.
     *
     * @return List of providers sorted by priority
     */
    public List<
        Map.Entry<String, WorkflowProvider<?, ?>>
    > getProvidersByPriority() {
        Map<String, WorkflowProvider<?, ?>> providersCopy;
        lock.readLock().lock();
        try {
            providersCopy = new HashMap<>(providers);
        } finally {
            lock.readLock().unlock();
        }

        return providersCopy
            .entrySet()
            .stream()
            .sorted((e1, e2) ->
                Integer.compare(
                    e2.getValue().getPriority(),
                    e1.getValue().getPriority()
                )
            )
            .toList();
    }

    /**
     * Finds providers that support a specific capability.
     *
     * @param capability The capability to search for
     * @return List of workflow types that support the capability
     */
    public List<String> findProvidersByCapability(String capability) {
        if (capability == null || capability.trim().isEmpty()) {
            return List.of();
        }

        Map<String, WorkflowProvider<?, ?>> providersCopy;
        lock.readLock().lock();
        try {
            providersCopy = new HashMap<>(providers);
        } finally {
            lock.readLock().unlock();
        }

        return providersCopy
            .entrySet()
            .stream()
            .filter(entry -> {
                WorkflowTypeMetadata metadata = entry.getValue().getMetadata();
                return metadata != null && metadata.hasCapability(capability);
            })
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }

    /**
     * Atomically replaces all providers with a new set.
     * This is useful for bulk updates or configuration reloads.
     *
     * @param newProviders Map of workflow types to providers
     * @throws WorkflowRegistrationException if any provider validation fails
     */
    public void replaceAllProviders(
        Map<String, WorkflowProvider<?, ?>> newProviders
    ) throws WorkflowRegistrationException {
        if (newProviders == null) {
            throw new IllegalArgumentException(
                "New providers map cannot be null"
            );
        }

        // Validate all providers first
        for (Map.Entry<
            String,
            WorkflowProvider<?, ?>
        > entry : newProviders.entrySet()) {
            String type = entry.getKey();
            WorkflowProvider<?, ?> provider = entry.getValue();

            if (type == null || type.trim().isEmpty()) {
                throw new WorkflowRegistrationException(
                    type,
                    "Workflow type cannot be null or empty"
                );
            }
            if (provider == null) {
                throw new WorkflowRegistrationException(
                    type,
                    "Provider cannot be null"
                );
            }

            try {
                provider.validate();
            } catch (WorkflowProviderException e) {
                throw new WorkflowRegistrationException(
                    type,
                    "Provider validation failed: " + e.getMessage(),
                    e
                );
            }
        }

        // Replace all providers atomically
        lock.writeLock().lock();
        try {
            providers.clear();
            providers.putAll(newProviders);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
