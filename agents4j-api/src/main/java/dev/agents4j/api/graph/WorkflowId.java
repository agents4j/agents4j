package dev.agents4j.api.graph;

import java.util.Objects;

/**
 * Value object representing a unique identifier for a workflow instance.
 * Provides type safety for workflow references and operations.
 */
public record WorkflowId(String value) {
    
    public WorkflowId {
        Objects.requireNonNull(value, "Workflow ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID cannot be empty");
        }
    }
    
    /**
     * Creates a new WorkflowId from a string value.
     *
     * @param value The workflow identifier string
     * @return A new WorkflowId instance
     */
    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }
    
    /**
     * Generates a unique WorkflowId.
     *
     * @return A new WorkflowId with a generated UUID
     */
    public static WorkflowId generate() {
        return new WorkflowId(java.util.UUID.randomUUID().toString());
    }
    
    /**
     * Creates a WorkflowId with a prefix and generated suffix.
     *
     * @param prefix The prefix for the workflow ID
     * @return A new WorkflowId with the prefix and UUID
     */
    public static WorkflowId withPrefix(String prefix) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        return new WorkflowId(prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * Creates a WorkflowId with timestamp prefix for ordering.
     *
     * @return A new WorkflowId with timestamp prefix
     */
    public static WorkflowId withTimestamp() {
        long timestamp = System.currentTimeMillis();
        return new WorkflowId("wf-" + timestamp + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
    }
    
    @Override
    public String toString() {
        return value;
    }
}