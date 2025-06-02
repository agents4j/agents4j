package dev.agents4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.workflow.builder.*;
import dev.agents4j.workflow.history.NodeInteraction;
import dev.agents4j.workflow.history.ProcessingHistory;
import dev.agents4j.workflow.output.OutputExtractor;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating core graph-based workflows and components.
 * This factory provides convenient methods for constructing
 * basic workflow patterns and extractors.
 */
public class GraphWorkflowFactory {

    /**
     * Creates a simple output extractor that extracts the LLM response from the context.
     * This implementation prioritizes the last interaction in the processing history,
     * falling back to the legacy "response" key if history is not available.
     *
     * @param <T> The input type
     * @return An OutputExtractor that extracts the LLM response
     */
    public static <T> OutputExtractor<T, String> createResponseExtractor() {
        return state -> {
            // Try to get the last interaction from history
            Optional<String> lastResponse = state
                .context()
                .get(ProcessingHistory.HISTORY_KEY)
                .map(history -> {
                    List<NodeInteraction> interactions =
                        history.getAllInteractions();
                    if (!interactions.isEmpty()) {
                        return interactions
                            .get(interactions.size() - 1)
                            .output();
                    }
                    return null;
                });

            // Fall back to the legacy response key if history is not available
            if (lastResponse.isPresent() && lastResponse.get() != null) {
                return lastResponse.get();
            } else {
                Object response = state
                    .context()
                    .get(ContextKey.of("response", Object.class))
                    .orElse("");
                return response.toString();
            }
        };
    }

    /**
     * Creates a simple linear workflow with two nodes using enhanced implementation.
     *
     * @param name The workflow name
     * @param firstNode The first node
     * @param secondNode The second node
     * @param inputType The input type class for type safety
     * @param <T> The input/output type
     * @return A configured enhanced workflow
     */
    public static <T> GraphWorkflow<T, String> createSequence(
        String name,
        GraphWorkflowNode<T> firstNode,
        GraphWorkflowNode<T> secondNode,
        Class<T> inputType
    ) {
        return GraphWorkflowBuilder.<T, String>create(inputType)
            .name(name)
            .version("1.0.0")
            .addNode(firstNode)
            .addNode(secondNode)
            .addEdge(firstNode.getNodeId(), secondNode.getNodeId())
            .defaultEntryPoint(firstNode.getNodeId())
            .outputExtractor(createResponseExtractor())
            .build();
    }

    /**
     * Creates a simple linear workflow with two nodes (legacy method for backward compatibility).
     *
     * @param name The workflow name
     * @param firstNode The first node
     * @param secondNode The second node
     * @param <T> The input/output type
     * @return A configured workflow
     * @deprecated Use {@link #createSequence(String, GraphWorkflowNode, GraphWorkflowNode, Class)} for type safety
     */
    public static <T> GraphWorkflow<T, String> createSequence(
        String name,
        GraphWorkflowNode<T> firstNode,
        GraphWorkflowNode<T> secondNode
    ) {
        return GraphWorkflowBuilder.<T, String>builder()
            .name(name)
            .addNode(firstNode)
            .addNode(secondNode)
            .addEdge(firstNode.getNodeId(), secondNode.getNodeId())
            .defaultEntryPoint(firstNode.getNodeId())
            .outputExtractor(createResponseExtractor())
            .build();
    }
}
