package dev.agents4j.api.routing;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Represents a routing path with its associated handler, conditions, and metadata.
 * 
 * <p>A Route defines a complete processing path including the nodes to execute,
 * the execution strategy to use, conditions for route selection, and metadata
 * for routing decisions. Routes can be composed and nested to create complex
 * routing hierarchies.</p>
 * 
 * <p><b>Key Components:</b></p>
 * <ul>
 * <li><b>Handler</b>: The processing logic (nodes + strategy)</li>
 * <li><b>Conditions</b>: Criteria for route selection</li>
 * <li><b>Metadata</b>: Priority, tags, and configuration</li>
 * <li><b>Fallback</b>: Alternative behavior on failure</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * Route<String, String> technicalRoute = Route.<String, String>builder()
 *     .id("technical-support")
 *     .description("Handles technical support requests")
 *     .addNode(analyzerNode)
 *     .addNode(technicalResponseNode)
 *     .strategy(StrategyFactory.sequential())
 *     .condition((input, ctx) -> input.contains("error") || input.contains("bug"))
 *     .priority(10)
 *     .addTag("support")
 *     .build();
 * }</pre>
 *
 * @param <I> The input type for this route
 * @param <O> The output type for this route
 */
public class Route<I, O> {

    private final String id;
    private final String description;
    private final List<AgentNode<?, ?>> nodes;
    private final WorkflowExecutionStrategy<I, O> strategy;
    private final BiPredicate<I, Map<String, Object>> condition;
    private final int priority;
    private final Map<String, Object> metadata;
    private final List<String> tags;
    private final double confidenceThreshold;
    private final Route<I, O> fallbackRoute;

    /**
     * Creates a new Route with the specified configuration.
     */
    private Route(String id, String description, List<AgentNode<?, ?>> nodes,
                 WorkflowExecutionStrategy<I, O> strategy, BiPredicate<I, Map<String, Object>> condition,
                 int priority, Map<String, Object> metadata, List<String> tags,
                 double confidenceThreshold, Route<I, O> fallbackRoute) {
        this.id = Objects.requireNonNull(id, "Route ID cannot be null");
        this.description = description != null ? description : "";
        this.nodes = Collections.unmodifiableList(Objects.requireNonNull(nodes, "Nodes cannot be null"));
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        this.condition = condition;
        this.priority = priority;
        this.metadata = Collections.unmodifiableMap(metadata != null ? metadata : Collections.emptyMap());
        this.tags = Collections.unmodifiableList(tags != null ? tags : Collections.emptyList());
        this.confidenceThreshold = validateConfidence(confidenceThreshold);
        this.fallbackRoute = fallbackRoute;

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Route must contain at least one node");
        }
    }

    /**
     * Gets the unique identifier for this route.
     *
     * @return The route ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable description of this route.
     *
     * @return The route description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the list of nodes that will be executed by this route.
     *
     * @return Unmodifiable list of agent nodes
     */
    public List<AgentNode<?, ?>> getNodes() {
        return nodes;
    }

    /**
     * Gets the execution strategy used by this route.
     *
     * @return The workflow execution strategy
     */
    public WorkflowExecutionStrategy<I, O> getStrategy() {
        return strategy;
    }

    /**
     * Gets the condition for selecting this route.
     *
     * @return The route selection condition, or null if no condition is set
     */
    public BiPredicate<I, Map<String, Object>> getCondition() {
        return condition;
    }

    /**
     * Gets the priority of this route for conflict resolution.
     *
     * @return The route priority (higher values = higher priority)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the metadata associated with this route.
     *
     * @return Unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets a specific metadata value.
     *
     * @param key The metadata key
     * @param <T> The expected type of the value
     * @return The metadata value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }

    /**
     * Gets a specific metadata value with a default.
     *
     * @param key The metadata key
     * @param defaultValue The default value if key is not found
     * @param <T> The expected type of the value
     * @return The metadata value, or default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    /**
     * Gets the tags associated with this route.
     *
     * @return Unmodifiable list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Gets the minimum confidence threshold for this route.
     *
     * @return The confidence threshold (0.0 to 1.0)
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    /**
     * Gets the fallback route for this route.
     *
     * @return The fallback route, or null if no fallback is set
     */
    public Route<I, O> getFallbackRoute() {
        return fallbackRoute;
    }

    /**
     * Tests if this route's condition matches the given input and context.
     *
     * @param input The input to test
     * @param context The routing context
     * @return true if the condition matches or no condition is set, false otherwise
     */
    public boolean matches(I input, Map<String, Object> context) {
        if (condition == null) {
            return true; // No condition means always matches
        }
        try {
            return condition.test(input, context);
        } catch (Exception e) {
            // If condition evaluation fails, route doesn't match
            return false;
        }
    }

    /**
     * Checks if this route has the specified tag.
     *
     * @param tag The tag to check for
     * @return true if the route has the tag, false otherwise
     */
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    /**
     * Checks if this route has any of the specified tags.
     *
     * @param tags The tags to check for
     * @return true if the route has any of the tags, false otherwise
     */
    public boolean hasAnyTag(String... tags) {
        for (String tag : tags) {
            if (hasTag(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that a confidence value is between 0.0 and 1.0.
     */
    private static double validateConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0, got: " + confidence);
        }
        return confidence;
    }

    /**
     * Creates a new Builder for constructing Route instances.
     *
     * @param <I> The input type for the route
     * @param <O> The output type for the route
     * @return A new Builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }

    /**
     * Builder for creating Route instances.
     *
     * @param <I> The input type for the route
     * @param <O> The output type for the route
     */
    public static class Builder<I, O> {
        private String id;
        private String description;
        private final List<AgentNode<?, ?>> nodes = new java.util.ArrayList<>();
        private WorkflowExecutionStrategy<I, O> strategy;
        private BiPredicate<I, Map<String, Object>> condition;
        private int priority = 0;
        private final Map<String, Object> metadata = new HashMap<>();
        private final List<String> tags = new java.util.ArrayList<>();
        private double confidenceThreshold = 0.0;
        private Route<I, O> fallbackRoute;

        /**
         * Sets the route identifier.
         *
         * @param id The unique route identifier
         * @return This builder instance
         */
        public Builder<I, O> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the route description.
         *
         * @param description Human-readable description of the route
         * @return This builder instance
         */
        public Builder<I, O> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds a node to this route.
         *
         * @param node The agent node to add
         * @return This builder instance
         */
        public Builder<I, O> addNode(AgentNode<?, ?> node) {
            this.nodes.add(node);
            return this;
        }

        /**
         * Adds multiple nodes to this route.
         *
         * @param nodes The agent nodes to add
         * @return This builder instance
         */
        public Builder<I, O> addNodes(List<AgentNode<?, ?>> nodes) {
            this.nodes.addAll(nodes);
            return this;
        }

        /**
         * Sets the execution strategy for this route.
         *
         * @param strategy The workflow execution strategy
         * @return This builder instance
         */
        public Builder<I, O> strategy(WorkflowExecutionStrategy<I, O> strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Sets the condition for selecting this route.
         *
         * @param condition The route selection condition
         * @return This builder instance
         */
        public Builder<I, O> condition(BiPredicate<I, Map<String, Object>> condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Sets the priority for this route.
         *
         * @param priority The route priority (higher values = higher priority)
         * @return This builder instance
         */
        public Builder<I, O> priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder instance
         */
        public Builder<I, O> addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Sets all metadata.
         *
         * @param metadata Map of metadata entries
         * @return This builder instance
         */
        public Builder<I, O> metadata(Map<String, Object> metadata) {
            this.metadata.clear();
            this.metadata.putAll(metadata);
            return this;
        }

        /**
         * Adds a tag to this route.
         *
         * @param tag The tag to add
         * @return This builder instance
         */
        public Builder<I, O> addTag(String tag) {
            this.tags.add(tag);
            return this;
        }

        /**
         * Adds multiple tags to this route.
         *
         * @param tags The tags to add
         * @return This builder instance
         */
        public Builder<I, O> addTags(String... tags) {
            Collections.addAll(this.tags, tags);
            return this;
        }

        /**
         * Sets the confidence threshold for this route.
         *
         * @param threshold The minimum confidence threshold (0.0 to 1.0)
         * @return This builder instance
         */
        public Builder<I, O> confidenceThreshold(double threshold) {
            this.confidenceThreshold = threshold;
            return this;
        }

        /**
         * Sets the fallback route for this route.
         *
         * @param fallbackRoute The fallback route
         * @return This builder instance
         */
        public Builder<I, O> fallbackRoute(Route<I, O> fallbackRoute) {
            this.fallbackRoute = fallbackRoute;
            return this;
        }

        /**
         * Builds the Route instance.
         *
         * @return A new Route instance
         * @throws IllegalStateException if required fields are not set
         */
        public Route<I, O> build() {
            if (id == null) {
                throw new IllegalStateException("Route ID must be set");
            }
            if (strategy == null) {
                throw new IllegalStateException("Strategy must be set");
            }
            if (nodes.isEmpty()) {
                throw new IllegalStateException("At least one node must be added");
            }

            return new Route<>(id, description, nodes, strategy, condition, priority,
                             metadata, tags, confidenceThreshold, fallbackRoute);
        }
    }

    @Override
    public String toString() {
        return String.format("Route{id='%s', description='%s', nodes=%d, strategy='%s', priority=%d, tags=%s}",
            id, description, nodes.size(), strategy.getStrategyName(), priority, tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route<?, ?> route = (Route<?, ?>) o;
        return Objects.equals(id, route.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}