package dev.agents4j.api.graph;

import java.util.Objects;

/**
 * Value object representing a unique identifier for a graph workflow node.
 * Provides type safety for node references in graph operations.
 */
public record NodeId(String value) {
    
    public NodeId {
        Objects.requireNonNull(value, "Node ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Node ID cannot be empty");
        }
    }
    
    /**
     * Creates a new NodeId from a string value.
     *
     * @param value The node identifier string
     * @return A new NodeId instance
     */
    public static NodeId of(String value) {
        return new NodeId(value);
    }
    
    /**
     * Generates a unique NodeId.
     *
     * @return A new NodeId with a generated UUID
     */
    public static NodeId generate() {
        return new NodeId(java.util.UUID.randomUUID().toString());
    }
    
    /**
     * Creates a NodeId with a prefix and generated suffix.
     *
     * @param prefix The prefix for the node ID
     * @return A new NodeId with the prefix and UUID
     */
    public static NodeId withPrefix(String prefix) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        return new NodeId(prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
    }
    
    @Override
    public String toString() {
        return value;
    }
}