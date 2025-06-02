package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Arrays;
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


}
