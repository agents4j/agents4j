package dev.agents4j.api.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of WorkflowContext for execution contexts.
 * Uses immutable data structures with structural sharing for efficiency.
 */
public final class ExecutionContext implements WorkflowContext {

    private final Map<ContextKey<?>, Object> data;

    private ExecutionContext(Map<ContextKey<?>, Object> data) {
        this.data = Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Creates an empty execution context.
     *
     * @return An empty ExecutionContext
     */
    public static ExecutionContext empty() {
        return new ExecutionContext(Collections.emptyMap());
    }

    /**
     * Creates an execution context with a single key-value pair.
     *
     * @param key The typed key
     * @param value The value
     * @param <T> The type of the value
     * @return A new ExecutionContext with the single entry
     */
    public static <T> ExecutionContext of(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "Context key cannot be null");
        if (value != null && !key.isCompatible(value)) {
            throw new IllegalArgumentException(
                String.format(
                    "Value type %s is not compatible with key type %s",
                    value.getClass().getSimpleName(),
                    key.type().getSimpleName()
                )
            );
        }

        Map<ContextKey<?>, Object> data = new HashMap<>();
        data.put(key, value);
        return new ExecutionContext(data);
    }

    /**
     * Creates an execution context from a map of key-value pairs.
     *
     * @param entries The entries to include
     * @return A new ExecutionContext with the entries
     */
    public static ExecutionContext from(Map<ContextKey<?>, Object> entries) {
        Objects.requireNonNull(entries, "Entries cannot be null");

        // Validate all entries
        for (Map.Entry<ContextKey<?>, Object> entry : entries.entrySet()) {
            ContextKey<?> key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !key.isCompatible(value)) {
                throw new IllegalArgumentException(
                    String.format(
                        "Value type %s is not compatible with key type %s for key '%s'",
                        value.getClass().getSimpleName(),
                        key.type().getSimpleName(),
                        key.name()
                    )
                );
            }
        }

        return new ExecutionContext(entries);
    }

    @Override
    public <T> Optional<T> get(ContextKey<T> key) {
        Objects.requireNonNull(key, "Context key cannot be null");
        Object value = data.get(key);
        if (value == null) {
            return Optional.empty();
        }

        // Safe cast since we validate types on insertion
        T typedValue = key.cast(value);
        return Optional.ofNullable(typedValue);
    }

    @Override
    public <T> WorkflowContext with(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "Context key cannot be null");
        if (value != null && !key.isCompatible(value)) {
            throw new IllegalArgumentException(
                String.format(
                    "Value type %s is not compatible with key type %s",
                    value.getClass().getSimpleName(),
                    key.type().getSimpleName()
                )
            );
        }

        Map<ContextKey<?>, Object> newData = new HashMap<>(this.data);
        newData.put(key, value);
        return new ExecutionContext(newData);
    }

    @Override
    public WorkflowContext without(ContextKey<?> key) {
        Objects.requireNonNull(key, "Context key cannot be null");
        if (!data.containsKey(key)) {
            return this; // Return same instance if key doesn't exist
        }

        Map<ContextKey<?>, Object> newData = new HashMap<>(this.data);
        newData.remove(key);
        return new ExecutionContext(newData);
    }

    @Override
    public boolean contains(ContextKey<?> key) {
        Objects.requireNonNull(key, "Context key cannot be null");
        return data.containsKey(key);
    }

    @Override
    public Set<ContextKey<?>> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public WorkflowContext merge(WorkflowContext other) {
        Objects.requireNonNull(other, "Other context cannot be null");
        if (other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }

        Map<ContextKey<?>, Object> mergedData = new HashMap<>(this.data);

        // Add all entries from other context
        for (ContextKey<?> key : other.keys()) {
            Object value = other.get(key).orElse(null);
            mergedData.put(key, value);
        }

        return new ExecutionContext(mergedData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return String.format(
            "ExecutionContext{size=%d, keys=%s}",
            data.size(),
            data.keySet().stream().map(key -> key.name()).sorted().toList()
        );
    }
}
