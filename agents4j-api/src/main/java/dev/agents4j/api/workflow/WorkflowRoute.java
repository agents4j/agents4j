package dev.agents4j.api.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents a route (edge) in a stateful workflow graph that connects two nodes.
 * Routes can be conditional based on the current workflow state.
 *
 * @param <S> The type of the workflow state data
 */
public class WorkflowRoute<S> {
    
    private final String id;
    private final String fromNodeId;
    private final String toNodeId;
    private final Predicate<WorkflowState<S>> condition;
    private final int priority;
    private final String description;
    private final Map<String, Object> metadata;
    private final boolean isDefault;
    
    private WorkflowRoute(String id, String fromNodeId, String toNodeId,
                         Predicate<WorkflowState<S>> condition, int priority,
                         String description, Map<String, Object> metadata, boolean isDefault) {
        this.id = Objects.requireNonNull(id, "Route ID cannot be null");
        this.fromNodeId = Objects.requireNonNull(fromNodeId, "From node ID cannot be null");
        this.toNodeId = Objects.requireNonNull(toNodeId, "To node ID cannot be null");
        this.condition = condition;
        this.priority = priority;
        this.description = description != null ? description : "";
        this.metadata = Collections.unmodifiableMap(metadata != null ? metadata : Collections.emptyMap());
        this.isDefault = isDefault;
        
        if (fromNodeId.equals(toNodeId)) {
            throw new IllegalArgumentException("Route cannot connect a node to itself");
        }
    }
    
    /**
     * Gets the route ID.
     *
     * @return The route ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the source node ID.
     *
     * @return The from node ID
     */
    public String getFromNodeId() {
        return fromNodeId;
    }
    
    /**
     * Gets the destination node ID.
     *
     * @return The to node ID
     */
    public String getToNodeId() {
        return toNodeId;
    }
    
    /**
     * Gets the route condition.
     *
     * @return The condition predicate, or null if unconditional
     */
    public Predicate<WorkflowState<S>> getCondition() {
        return condition;
    }
    
    /**
     * Gets the route priority.
     *
     * @return The priority (higher values = higher priority)
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the route description.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the route metadata.
     *
     * @return Unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Checks if this is a default route.
     *
     * @return true if this is a default route
     */
    public boolean isDefault() {
        return isDefault;
    }
    
    /**
     * Tests if this route's condition is satisfied.
     *
     * @param state The current workflow state
     * @return true if the route condition is satisfied or no condition exists
     */
    public boolean matches(WorkflowState<S> state) {
        if (condition == null) {
            return true; // Unconditional route
        }
        try {
            return condition.test(state);
        } catch (Exception e) {
            // If condition evaluation fails, route doesn't match
            return false;
        }
    }
    
    /**
     * Creates a new route builder.
     *
     * @param <S> The state type
     * @return A new Builder instance
     */
    public static <S> Builder<S> builder() {
        return new Builder<>();
    }
    
    /**
     * Creates a simple unconditional route.
     *
     * @param id The route ID
     * @param fromNodeId The source node ID
     * @param toNodeId The destination node ID
     * @param <S> The state type
     * @return A new WorkflowRoute
     */
    public static <S> WorkflowRoute<S> simple(String id, String fromNodeId, String toNodeId) {
        return WorkflowRoute.<S>builder()
                .id(id)
                .from(fromNodeId)
                .to(toNodeId)
                .build();
    }
    
    /**
     * Creates a conditional route.
     *
     * @param id The route ID
     * @param fromNodeId The source node ID
     * @param toNodeId The destination node ID
     * @param condition The route condition
     * @param <S> The state type
     * @return A new WorkflowRoute
     */
    public static <S> WorkflowRoute<S> conditional(String id, String fromNodeId, String toNodeId,
                                                   Predicate<WorkflowState<S>> condition) {
        return WorkflowRoute.<S>builder()
                .id(id)
                .from(fromNodeId)
                .to(toNodeId)
                .condition(condition)
                .build();
    }
    
    /**
     * Builder for creating WorkflowRoute instances.
     *
     * @param <S> The state type
     */
    public static class Builder<S> {
        private String id;
        private String fromNodeId;
        private String toNodeId;
        private Predicate<WorkflowState<S>> condition;
        private int priority = 0;
        private String description;
        private final Map<String, Object> metadata = new HashMap<>();
        private boolean isDefault = false;
        
        /**
         * Sets the route ID.
         *
         * @param id The route ID
         * @return This builder
         */
        public Builder<S> id(String id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the source node ID.
         *
         * @param nodeId The source node ID
         * @return This builder
         */
        public Builder<S> from(String nodeId) {
            this.fromNodeId = nodeId;
            return this;
        }
        
        /**
         * Sets the destination node ID.
         *
         * @param nodeId The destination node ID
         * @return This builder
         */
        public Builder<S> to(String nodeId) {
            this.toNodeId = nodeId;
            return this;
        }
        
        /**
         * Sets the route condition.
         *
         * @param condition The condition predicate
         * @return This builder
         */
        public Builder<S> condition(Predicate<WorkflowState<S>> condition) {
            this.condition = condition;
            return this;
        }
        
        /**
         * Sets the route priority.
         *
         * @param priority The priority
         * @return This builder
         */
        public Builder<S> priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the route description.
         *
         * @param description The description
         * @return This builder
         */
        public Builder<S> description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Adds metadata.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder
         */
        public Builder<S> addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Marks this route as a default route.
         *
         * @return This builder
         */
        public Builder<S> asDefault() {
            this.isDefault = true;
            return this;
        }
        
        /**
         * Builds the WorkflowRoute.
         *
         * @return A new WorkflowRoute instance
         */
        public WorkflowRoute<S> build() {
            if (id == null) {
                throw new IllegalStateException("Route ID must be set");
            }
            if (fromNodeId == null) {
                throw new IllegalStateException("From node ID must be set");
            }
            if (toNodeId == null) {
                throw new IllegalStateException("To node ID must be set");
            }
            
            return new WorkflowRoute<>(id, fromNodeId, toNodeId, condition, priority, description, metadata, isDefault);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkflowRoute<?> that = (WorkflowRoute<?>) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowRoute{id='%s', from='%s', to='%s', priority=%d, default=%s}",
                id, fromNodeId, toNodeId, priority, isDefault);
    }
}