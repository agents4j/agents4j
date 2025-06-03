package dev.agents4j.api.graph;

import java.util.Objects;

/**
 * Value object representing a unique identifier for a graph workflow edge.
 * Provides type safety for edge references in graph operations.
 */
public record EdgeId(String value) {
    
    /**
     * Creates a new edge ID with validation.
     * 
     * @param value the unique identifier for this edge
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is empty or blank
     */
    public EdgeId {
        Objects.requireNonNull(value, "Edge ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Edge ID cannot be empty");
        }
    }
    
    /**
     * Creates a new EdgeId from a string value.
     *
     * @param value The edge identifier string
     * @return A new EdgeId instance
     */
    public static EdgeId of(String value) {
        return new EdgeId(value);
    }
    
    /**
     * Generates a unique EdgeId.
     *
     * @return A new EdgeId with a generated UUID
     */
    public static EdgeId generate() {
        return new EdgeId(java.util.UUID.randomUUID().toString());
    }
    
    /**
     * Creates an EdgeId from source and target nodes.
     *
     * @param fromNode The source node ID
     * @param toNode The target node ID
     * @return A new EdgeId representing the connection
     */
    public static EdgeId between(NodeId fromNode, NodeId toNode) {
        Objects.requireNonNull(fromNode, "From node cannot be null");
        Objects.requireNonNull(toNode, "To node cannot be null");
        return new EdgeId(fromNode.value() + "->" + toNode.value());
    }
    
    /**
     * Creates an EdgeId with a descriptive name between nodes.
     *
     * @param fromNode The source node ID
     * @param toNode The target node ID
     * @param name The descriptive name for the edge
     * @return A new EdgeId with descriptive naming
     */
    public static EdgeId named(NodeId fromNode, NodeId toNode, String name) {
        Objects.requireNonNull(fromNode, "From node cannot be null");
        Objects.requireNonNull(toNode, "To node cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        return new EdgeId(fromNode.value() + "-" + name + "->" + toNode.value());
    }
    
    @Override
    public String toString() {
        return value;
    }
}