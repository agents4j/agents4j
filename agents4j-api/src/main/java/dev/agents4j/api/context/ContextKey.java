package dev.agents4j.api.context;

import java.util.Objects;

/**
 * A type-safe key for accessing context values in workflows.
 * This eliminates the need for unsafe casting and provides compile-time type safety.
 *
 * @param <T> The type of value this key represents
 */
public record ContextKey<T>(String name, Class<T> type) {
    
    public ContextKey {
        Objects.requireNonNull(name, "Context key name cannot be null");
        Objects.requireNonNull(type, "Context key type cannot be null");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Context key name cannot be empty");
        }
    }
    
    /**
     * Creates a new context key with the specified name and type.
     *
     * @param name The name of the context key
     * @param type The type class for type safety
     * @param <T> The type parameter
     * @return A new ContextKey instance
     */
    public static <T> ContextKey<T> of(String name, Class<T> type) {
        return new ContextKey<>(name, type);
    }
    
    /**
     * Creates a string context key.
     *
     * @param name The name of the context key
     * @return A new ContextKey for String values
     */
    public static ContextKey<String> stringKey(String name) {
        return new ContextKey<>(name, String.class);
    }
    
    /**
     * Creates an integer context key.
     *
     * @param name The name of the context key
     * @return A new ContextKey for Integer values
     */
    public static ContextKey<Integer> intKey(String name) {
        return new ContextKey<>(name, Integer.class);
    }
    
    /**
     * Creates a long context key.
     *
     * @param name The name of the context key
     * @return A new ContextKey for Long values
     */
    public static ContextKey<Long> longKey(String name) {
        return new ContextKey<>(name, Long.class);
    }
    
    /**
     * Creates a boolean context key.
     *
     * @param name The name of the context key
     * @return A new ContextKey for Boolean values
     */
    public static ContextKey<Boolean> booleanKey(String name) {
        return new ContextKey<>(name, Boolean.class);
    }
    
    /**
     * Safely casts a value to the type of this key.
     *
     * @param value The value to cast
     * @return The value cast to the correct type, or null if value is null or wrong type
     */
    public T cast(Object value) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    /**
     * Checks if a value is compatible with this key's type.
     *
     * @param value The value to check
     * @return true if the value is compatible with this key's type
     */
    public boolean isCompatible(Object value) {
        return value == null || type.isInstance(value);
    }
    
    @Override
    public String toString() {
        return String.format("ContextKey{name='%s', type=%s}", name, type.getSimpleName());
    }
}