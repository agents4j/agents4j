package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandTraverse;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.RoutingDecision;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.agents4j.workflow.GraphWorkflowImpl;
import dev.agents4j.workflow.output.OutputExtractor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
        return new GraphWorkflowNode<T>() {
            private final NodeId id = NodeId.of(nodeId);
            
            @Override
            public WorkflowResult<GraphCommand<T>, WorkflowError> process(
                GraphWorkflowState<T> state
            ) {
                try {
                    T input = state.data();
                    
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(SystemMessage.from(systemPrompt));
                    messages.add(UserMessage.from(input.toString()));
                    
                    AiMessage response = model.chat(messages).aiMessage();
                    String responseText = response.text();
                    
                    WorkflowContext updatedContext = WorkflowContext.empty()
                        .with(ContextKey.of("response", Object.class), responseText);
                    
                    return WorkflowResult.success(
                        GraphCommandTraverse.toWithContext(
                            NodeId.of("next"), 
                            updatedContext
                        )
                    );
                } catch (Exception e) {
                    return WorkflowResult.failure(
                        ExecutionError.withCause(
                            "llm-processing-error",
                            "Error processing with LLM: " + e.getMessage(),
                            id.value(),
                            e
                        )
                    );
                }
            }
            
            @Override
            public NodeId getNodeId() {
                return id;
            }
            
            @Override
            public String getName() {
                return "LLM Node";
            }
            
            @Override
            public String getDescription() {
                return "Processes input with LLM using system prompt: " + systemPrompt;
            }
        };
    }
    
    /**
     * Creates a simple output extractor that extracts the LLM response from the context.
     *
     * @param <T> The input type
     * @return An OutputExtractor that extracts the LLM response
     */
    public static <T> OutputExtractor<T, String> createResponseExtractor() {
        return state -> {
            Object response = state.context().get(ContextKey.of("response", Object.class)).orElse("");
            return response.toString();
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