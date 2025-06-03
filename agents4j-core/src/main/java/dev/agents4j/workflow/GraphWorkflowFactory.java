package dev.agents4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.workflow.builder.*;
import dev.agents4j.workflow.history.NodeInteraction;
import dev.agents4j.workflow.history.ProcessingHistory;
import dev.agents4j.workflow.output.OutputExtractor;
import java.util.Arrays;
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
     * Creates a linear sequence workflow with an arbitrary number of nodes.
     *
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @param nodes The nodes to sequence in order
     * @param <T> The input/output type
     * @return A configured sequence workflow
     */
    @SafeVarargs
    public static <T> GraphWorkflow<T, String> createSequence(
        String name,
        Class<T> inputType,
        GraphWorkflowNode<T>... nodes
    ) {
        if (nodes.length == 0) {
            throw new IllegalArgumentException(
                "At least one node is required for a sequence"
            );
        }

        GraphWorkflowBuilder<T, String> builder = GraphWorkflowBuilder.<
                T,
                String
            >create(inputType)
            .name(name)
            .version("1.0.0")
            .outputExtractor(createResponseExtractor());

        // Add all nodes
        for (GraphWorkflowNode<T> node : nodes) {
            builder.addNode(node);
        }

        // Add edges between consecutive nodes
        for (int i = 0; i < nodes.length - 1; i++) {
            builder.addEdge(nodes[i].getNodeId(), nodes[i + 1].getNodeId());
        }

        return builder.defaultEntryPoint(nodes[0].getNodeId()).build();
    }

    /**
     * Creates a linear sequence workflow with an arbitrary number of nodes (List version).
     *
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @param nodes The nodes to sequence in order
     * @param <T> The input/output type
     * @return A configured sequence workflow
     */
    @SuppressWarnings("unchecked")
    public static <T> GraphWorkflow<T, String> createSequence(
        String name,
        Class<T> inputType,
        List<GraphWorkflowNode<T>> nodes
    ) {
        GraphWorkflowNode<T>[] nodeArray = nodes.toArray(
            new GraphWorkflowNode[0]
        );
        return createSequence(name, inputType, nodeArray);
    }
}
