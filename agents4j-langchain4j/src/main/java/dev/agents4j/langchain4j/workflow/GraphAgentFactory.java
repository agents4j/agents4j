package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.agents4j.langchain4j.workflow.history.NodeInteraction;
import dev.agents4j.langchain4j.workflow.history.ProcessingHistory;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;
import dev.agents4j.workflow.GraphWorkflowImpl;
import dev.agents4j.workflow.output.OutputExtractor;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating Graph-based agent workflows and components.
 * This factory provides convenient methods for constructing
 * components compatible with the GraphWorkflow API.
 */
public class GraphAgentFactory {

    /**
     * Creates a GraphLLMContentRouter for use in graph-based workflows.
     *
     * @param <T> The type of content to be routed
     * @param nodeId The node ID for the router
     * @param model The ChatModel to use for content classification
     * @param routes The available routes as NodeId objects
     * @return A new GraphLLMContentRouter instance
     */
    public static <T> GraphLLMContentRouter<T> createContentRouter(
        String nodeId,
        ChatModel model,
        Set<NodeId> routes
    ) {
        RoutingStrategy strategy = RoutingStrategy.basic(
            "LLM content classification",
            Object.class,
            routes
        );

        return GraphLLMContentRouter.<T>builder()
            .nodeId(nodeId)
            .model(model)
            .strategy(strategy)
            .build();
    }

    /**
     * Creates a GraphLLMContentRouter for use in graph-based workflows.
     *
     * @param <T> The type of content to be routed
     * @param nodeId The node ID for the router
     * @param model The ChatModel to use for content classification
     * @param routeNames The available routes as strings (will be converted to NodeId objects)
     * @return A new GraphLLMContentRouter instance
     */
    public static <T> GraphLLMContentRouter<T> createContentRouter(
        String nodeId,
        ChatModel model,
        String... routeNames
    ) {
        Set<NodeId> routes = Arrays.stream(routeNames)
            .map(NodeId::of)
            .collect(Collectors.toSet());

        return createContentRouter(nodeId, model, routes);
    }

    /**
     * Creates a simple LLM node for a graph workflow.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @return A GraphWorkflowNode that processes input with the LLM
     */
    public static <T> GraphWorkflowNode<T> createLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt
    ) {
        return LLMGraphWorkflowNode.<T>builder()
            .nodeId(nodeId)
            .model(model)
            .systemPrompt(systemPrompt)
            .build();
    }

    /**
     * Creates a simple LLM node for a graph workflow with a custom message extractor.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param userMessageExtractor Function to extract the user message from the current state
     * @return A GraphWorkflowNode that processes input with the LLM
     */
    public static <T> GraphWorkflowNode<T> createLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt,
        Function<GraphWorkflowState<T>, String> userMessageExtractor
    ) {
        return LLMGraphWorkflowNode.<T>builder()
            .nodeId(nodeId)
            .model(model)
            .systemPrompt(systemPrompt)
            .userMessageExtractor(userMessageExtractor)
            .build();
    }

    /**
     * Creates a simple output extractor that extracts the LLM response from the context.
     *
     * @param <T> The input type
     * @return An OutputExtractor that extracts the LLM response
     */
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
     * Creates a simple sequence workflow with two nodes.
     *
     * @param <T> The input type
     * @param name The workflow name
     * @param firstNode The first node
     * @param secondNode The second node
     * @return A GraphWorkflow that executes the nodes in sequence
     */
    public static <T> GraphWorkflow<T, String> createSequence(
        String name,
        GraphWorkflowNode<T> firstNode,
        GraphWorkflowNode<T> secondNode
    ) {
        return GraphWorkflowImpl.<T, String>builder()
            .name(name)
            .addNode(firstNode)
            .addNode(secondNode)
            .addEdge(firstNode.getNodeId(), secondNode.getNodeId())
            .defaultEntryPoint(firstNode.getNodeId())
            .outputExtractor(createResponseExtractor())
            .build();
    }
}
