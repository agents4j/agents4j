package dev.agents4j.api.context;

import java.util.Optional;
import java.util.Set;

/**
 * Type-safe context interface for workflow execution.
 * Replaces the unsafe Map&lt;String, Object&gt; pattern with compile-time type safety.
 */
public sealed interface WorkflowContext 
    permits ExecutionContext, ValidationContext, SecurityContext {
    
    /**
     * Gets a value from the context with type safety.
     *
     * @param key The typed key for the value
     * @param <T> The type of the value
     * @return An Optional containing the value if present and of correct type
     */
    <T> Optional<T> get(ContextKey<T> key);
    
    /**
     * Gets a value from the context with a default if not present.
     *
     * @param key The typed key for the value
     * @param defaultValue The default value to return if key not found
     * @param <T> The type of the value
     * @return The value or the default
     */
    default <T> T getOrDefault(ContextKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }
    
    /**
     * Creates a new context with an additional key-value pair.
     *
     * @param key The typed key
     * @param value The value
     * @param <T> The type of the value
     * @return A new WorkflowContext with the added value
     */
    <T> WorkflowContext with(ContextKey<T> key, T value);
    
    /**
     * Creates a new context without the specified key.
     *
     * @param key The key to remove
     * @return A new WorkflowContext without the key
     */
    WorkflowContext without(ContextKey<?> key);
    
    /**
     * Checks if a key exists in the context.
     *
     * @param key The key to check
     * @return true if the key exists
     */
    boolean contains(ContextKey<?> key);
    
    /**
     * Gets all keys present in this context.
     *
     * @return A set of all context keys
     */
    Set<ContextKey<?>> keys();
    
    /**
     * Gets the number of entries in this context.
     *
     * @return The size of the context
     */
    int size();
    
    /**
     * Checks if this context is empty.
     *
     * @return true if the context contains no entries
     */
    default boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Merges this context with another context.
     * Values from the other context take precedence in case of conflicts.
     *
     * @param other The other context to merge with
     * @return A new WorkflowContext containing values from both contexts
     */
    WorkflowContext merge(WorkflowContext other);
    
    /**
     * Creates an empty workflow context.
     *
     * @return An empty WorkflowContext
     */
    static WorkflowContext empty() {
        return ExecutionContext.empty();
    }
    
    /**
     * Creates a workflow context with a single key-value pair.
     *
     * @param key The typed key
     * @param value The value
     * @param <T> The type of the value
     * @return A new WorkflowContext with the single entry
     */
    static <T> WorkflowContext of(ContextKey<T> key, T value) {
        return ExecutionContext.of(key, value);
    }
}