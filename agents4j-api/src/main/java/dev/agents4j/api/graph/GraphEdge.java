package dev.agents4j.api.graph;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an edge in a graph workflow, connecting two nodes with optional conditions
 * and metadata. Edges define the possible transitions between workflow nodes.
 */
public record GraphEdge(
    EdgeId edgeId,
    NodeId fromNode,
    NodeId toNode,
    EdgeCondition condition,
    EdgeMetadata metadata
) {
    public GraphEdge {
        Objects.requireNonNull(edgeId, "Edge ID cannot be null");
        Objects.requireNonNull(fromNode, "From node cannot be null");
        Objects.requireNonNull(toNode, "To node cannot be null");
        Objects.requireNonNull(condition, "Edge condition cannot be null");
        Objects.requireNonNull(metadata, "Edge metadata cannot be null");
    }

    /**
     * Creates a simple edge between two nodes with always-true condition.
     *
     * @param fromNode The source node
     * @param toNode The target node
     * @return A new GraphEdge with always condition
     */
    public static GraphEdge between(NodeId fromNode, NodeId toNode) {
        return new GraphEdge(
            EdgeId.between(fromNode, toNode),
            fromNode,
            toNode,
            EdgeCondition.always(),
            EdgeMetadata.simple()
        );
    }

    /**
     * Creates a conditional edge between two nodes.
     *
     * @param fromNode The source node
     * @param toNode The target node
     * @param condition The condition for traversal
     * @return A new GraphEdge with the specified condition
     */
    public static GraphEdge conditional(
        NodeId fromNode,
        NodeId toNode,
        EdgeCondition condition
    ) {
        return new GraphEdge(
            EdgeId.between(fromNode, toNode),
            fromNode,
            toNode,
            condition,
            EdgeMetadata.simple()
        );
    }

    /**
     * Creates a named edge between two nodes.
     *
     * @param fromNode The source node
     * @param toNode The target node
     * @param name The edge name
     * @param condition The condition for traversal
     * @return A new GraphEdge with name and condition
     */
    public static GraphEdge named(
        NodeId fromNode,
        NodeId toNode,
        String name,
        EdgeCondition condition
    ) {
        return new GraphEdge(
            EdgeId.named(fromNode, toNode, name),
            fromNode,
            toNode,
            condition,
            EdgeMetadata.withName(name)
        );
    }

    /**
     * Checks if this edge can be traversed given the current workflow state.
     *
     * @param state The current workflow state
     * @return true if the edge condition is satisfied
     */
    public boolean canTraverse(GraphWorkflowState<?> state) {
        Objects.requireNonNull(state, "State cannot be null");
        return condition.evaluate(state);
    }

    /**
     * Gets the edge name from metadata.
     *
     * @return Optional containing the edge name
     */
    public Optional<String> getName() {
        return metadata.name();
    }

    /**
     * Gets the edge description from metadata.
     *
     * @return Optional containing the edge description
     */
    public Optional<String> getDescription() {
        return metadata.description();
    }

    /**
     * Gets the edge weight from metadata.
     *
     * @return The edge weight (default 1.0)
     */
    public double getWeight() {
        return metadata.weight();
    }

    /**
     * Checks if this edge represents a loop (connects node to itself).
     *
     * @return true if this is a self-loop
     */
    public boolean isLoop() {
        return fromNode.equals(toNode);
    }

    /**
     * Creates a new edge with updated metadata.
     *
     * @param newMetadata The new metadata
     * @return A new GraphEdge with updated metadata
     */
    public GraphEdge withMetadata(EdgeMetadata newMetadata) {
        return new GraphEdge(edgeId, fromNode, toNode, condition, newMetadata);
    }

    /**
     * Creates a new edge with updated condition.
     *
     * @param newCondition The new condition
     * @return A new GraphEdge with updated condition
     */
    public GraphEdge withCondition(EdgeCondition newCondition) {
        return new GraphEdge(edgeId, fromNode, toNode, newCondition, metadata);
    }

    @Override
    public String toString() {
        return String.format(
            "GraphEdge{%s -> %s, condition=%s}",
            fromNode.value(),
            toNode.value(),
            condition.getDescription()
        );
    }

    /**
     * Metadata associated with a graph edge.
     */
    public record EdgeMetadata(
        Optional<String> name,
        Optional<String> description,
        double weight,
        Map<String, Object> properties,
        Optional<Instant> createdAt
    ) {
        public EdgeMetadata {
            Objects.requireNonNull(name, "Name optional cannot be null");
            Objects.requireNonNull(
                description,
                "Description optional cannot be null"
            );
            Objects.requireNonNull(properties, "Properties cannot be null");
            Objects.requireNonNull(
                createdAt,
                "Created at optional cannot be null"
            );

            if (weight < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }

            // Make properties immutable
            properties = Map.copyOf(properties);
        }

        /**
         * Creates simple edge metadata with default values.
         *
         * @return EdgeMetadata with defaults
         */
        public static EdgeMetadata simple() {
            return new EdgeMetadata(
                Optional.empty(),
                Optional.empty(),
                1.0,
                Map.of(),
                Optional.of(Instant.now())
            );
        }

        /**
         * Creates edge metadata with a name.
         *
         * @param name The edge name
         * @return EdgeMetadata with the specified name
         */
        public static EdgeMetadata withName(String name) {
            Objects.requireNonNull(name, "Name cannot be null");
            return new EdgeMetadata(
                Optional.of(name),
                Optional.empty(),
                1.0,
                Map.of(),
                Optional.of(Instant.now())
            );
        }

        /**
         * Creates edge metadata with name and description.
         *
         * @param name The edge name
         * @param description The edge description
         * @return EdgeMetadata with name and description
         */
        public static EdgeMetadata withNameAndDescription(
            String name,
            String description
        ) {
            Objects.requireNonNull(name, "Name cannot be null");
            Objects.requireNonNull(description, "Description cannot be null");
            return new EdgeMetadata(
                Optional.of(name),
                Optional.of(description),
                1.0,
                Map.of(),
                Optional.of(Instant.now())
            );
        }

        /**
         * Creates edge metadata with all properties.
         *
         * @param name The edge name
         * @param description The edge description
         * @param weight The edge weight
         * @param properties Additional properties
         * @return EdgeMetadata with all specified properties
         */
        public static EdgeMetadata of(
            String name,
            String description,
            double weight,
            Map<String, Object> properties
        ) {
            return new EdgeMetadata(
                Optional.ofNullable(name),
                Optional.ofNullable(description),
                weight,
                properties != null ? properties : Map.of(),
                Optional.of(Instant.now())
            );
        }

        /**
         * Creates a new metadata with updated weight.
         *
         * @param newWeight The new weight
         * @return New EdgeMetadata with updated weight
         */
        public EdgeMetadata withWeight(double newWeight) {
            if (newWeight < 0) {
                throw new IllegalArgumentException("Weight cannot be negative");
            }
            return new EdgeMetadata(
                name,
                description,
                newWeight,
                properties,
                createdAt
            );
        }

        /**
         * Creates a new metadata with additional property.
         *
         * @param key The property key
         * @param value The property value
         * @return New EdgeMetadata with added property
         */
        public EdgeMetadata withProperty(String key, Object value) {
            Objects.requireNonNull(key, "Property key cannot be null");
            var newProperties = new java.util.HashMap<>(properties);
            newProperties.put(key, value);
            return new EdgeMetadata(
                name,
                description,
                weight,
                newProperties,
                createdAt
            );
        }

        /**
         * Gets a property value.
         *
         * @param key The property key
         * @param <T> The expected type
         * @return Optional containing the property value
         */
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getProperty(String key) {
            return Optional.ofNullable((T) properties.get(key));
        }
    }
}
