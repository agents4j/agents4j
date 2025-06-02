package dev.agents4j.workflow.history;

import dev.agents4j.api.graph.NodeId;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single interaction with a node in the workflow.
 * Records the input, output, node information, and timestamp.
 */
public record NodeInteraction(
    NodeId nodeId,
    String nodeName,
    String input,
    String output,
    Instant timestamp
) {
    public NodeInteraction {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(nodeName, "nodeName must not be null");
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(output, "output must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}