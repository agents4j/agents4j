package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.langchain4j.workflow.factory.ContentRouterFactory;
import dev.agents4j.langchain4j.workflow.factory.LLMNodeFactory;
import dev.agents4j.langchain4j.workflow.factory.WorkflowSequenceFactory;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Simplified factory for creating Graph-based agent workflows and components.
 * This factory provides convenient methods for constructing components
 * compatible with the GraphWorkflow API, delegating to specialized factories
 * for focused functionality.
 */
public class GraphAgentFactory {

    // =====================================
    // Content Router Factory Methods
    // =====================================

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
        return ContentRouterFactory.<T>contentRouter(nodeId, model)
            .routes(routes)
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
        return ContentRouterFactory.<T>contentRouter(nodeId, model)
            .routes(routeNames)
            .build();
    }

    // =====================================
    // LLM Node Factory Methods
    // =====================================

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
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
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
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
            .userMessageExtractor(userMessageExtractor)
            .build();
    }

    /**
     * Creates an LLM node for sequence workflows with proper next node routing.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param nextNodeId The ID of the next node in the sequence
     * @return A GraphWorkflowNode that processes input with the LLM
     */
    public static <T> GraphWorkflowNode<T> createSequenceLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt,
        String nextNodeId
    ) {
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
            .nextNode(nextNodeId)
            .build();
    }

    /**
     * Creates an LLM node for sequence workflows with custom message extractor and proper next node routing.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param userMessageExtractor Function to extract the user message from the current state
     * @param nextNodeId The ID of the next node in the sequence
     * @return A GraphWorkflowNode that processes input with the LLM
     */
    public static <T> GraphWorkflowNode<T> createSequenceLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt,
        Function<GraphWorkflowState<T>, String> userMessageExtractor,
        String nextNodeId
    ) {
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
            .userMessageExtractor(userMessageExtractor)
            .nextNode(nextNodeId)
            .build();
    }

    /**
     * Creates a completing LLM node that uses GraphCommandComplete instead of traversing.
     * This is ideal for the last node in a sequence workflow.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @return A GraphWorkflowNode that completes the workflow
     */
    public static <T> GraphWorkflowNode<T> createCompletingLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt
    ) {
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
            .completing()
            .build();
    }

    /**
     * Creates a completing LLM node with custom message extractor that uses GraphCommandComplete.
     * This is ideal for the last node in a sequence workflow.
     *
     * @param <T> The type of input/output
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param userMessageExtractor Function to extract the user message from the current state
     * @return A GraphWorkflowNode that completes the workflow
     */
    public static <T> GraphWorkflowNode<T> createCompletingLLMNode(
        String nodeId,
        ChatModel model,
        String systemPrompt,
        Function<GraphWorkflowState<T>, String> userMessageExtractor
    ) {
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt)
            .userMessageExtractor(userMessageExtractor)
            .completing()
            .build();
    }

    // =====================================
    // Workflow Sequence Factory Methods
    // =====================================

    /**
     * Creates a linear sequence workflow from multiple LLM node specifications.
     *
     * @param <T> The input/output type
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @param nodeSpecs The specifications for each LLM node in the sequence
     * @return A configured sequence workflow
     */
    @SafeVarargs
    public static <T> GraphWorkflow<T, String> createLLMSequence(
        String name,
        Class<T> inputType,
        LLMNodeSpec<T>... nodeSpecs
    ) {
        if (nodeSpecs.length == 0) {
            throw new IllegalArgumentException("At least one node specification is required");
        }
        
        WorkflowSequenceFactory.SequenceWorkflowBuilder<T> builder = WorkflowSequenceFactory.sequence(name, inputType);
        for (LLMNodeSpec<T> spec : nodeSpecs) {
            if (spec.userMessageExtractor() != null) {
                builder.addNode(spec.nodeId(), spec.model(), spec.systemPrompt(), spec.userMessageExtractor());
            } else {
                builder.addNode(spec.nodeId(), spec.model(), spec.systemPrompt());
            }
        }
        return builder.build();
    }

    /**
     * Creates a linear sequence workflow from multiple LLM node specifications (List convenience method).
     *
     * @param <T> The input/output type
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @param nodeSpecs The specifications for each LLM node in the sequence
     * @return A configured sequence workflow
     */
    public static <T> GraphWorkflow<T, String> createLLMSequence(
        String name,
        Class<T> inputType,
        List<LLMNodeSpec<T>> nodeSpecs
    ) {
        if (nodeSpecs == null || nodeSpecs.isEmpty()) {
            throw new IllegalArgumentException("At least one node specification is required");
        }
        
        WorkflowSequenceFactory.SequenceWorkflowBuilder<T> builder = WorkflowSequenceFactory.sequence(name, inputType);
        for (LLMNodeSpec<T> spec : nodeSpecs) {
            if (spec.userMessageExtractor() != null) {
                builder.addNode(spec.nodeId(), spec.model(), spec.systemPrompt(), spec.userMessageExtractor());
            } else {
                builder.addNode(spec.nodeId(), spec.model(), spec.systemPrompt());
            }
        }
        return builder.build();
    }

    // =====================================
    // Builder-style Helper Methods
    // =====================================

    /**
     * Builder-style helper for creating LLM node specifications.
     *
     * @param <T> The input/output type
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @return A new LLMNodeSpec
     */
    public static <T> LLMNodeSpec<T> llmNode(String nodeId, ChatModel model, String systemPrompt) {
        return new LLMNodeSpec<>(nodeId, model, systemPrompt, null);
    }

    /**
     * Builder-style helper for creating LLM node specifications with custom message extractor.
     *
     * @param <T> The input/output type
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param userMessageExtractor Function to extract the user message from the current state
     * @return A new LLMNodeSpec
     */
    public static <T> LLMNodeSpec<T> llmNode(
        String nodeId, 
        ChatModel model, 
        String systemPrompt,
        Function<GraphWorkflowState<T>, String> userMessageExtractor
    ) {
        return new LLMNodeSpec<>(nodeId, model, systemPrompt, userMessageExtractor);
    }

    // =====================================
    // Enhanced API Methods (New)
    // =====================================

    /**
     * Creates a builder for constructing content routers with fluent API.
     *
     * @param <T> The type of content to be routed
     * @param nodeId The node ID for the router
     * @param model The ChatModel to use for content classification
     * @return A new ContentRouterBuilder instance
     */
    public static <T> ContentRouterFactory.ContentRouterBuilder<T> contentRouter(String nodeId, ChatModel model) {
        return ContentRouterFactory.<T>contentRouter(nodeId, model);
    }

    /**
     * Creates a builder for constructing LLM workflow nodes with enhanced capabilities.
     *
     * @param <T> The type of input/output for the node
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @return A new LLMNodeBuilder instance
     */
    public static <T> LLMNodeFactory.LLMNodeBuilder<T> node(String nodeId, ChatModel model, String systemPrompt) {
        return LLMNodeFactory.<T>llmNode(nodeId, model, systemPrompt);
    }

    /**
     * Creates a builder for constructing sequence workflows.
     *
     * @param <T> The input type for the workflow
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @return A new SequenceWorkflowBuilder instance
     */
    public static <T> WorkflowSequenceFactory.SequenceWorkflowBuilder<T> sequence(String name, Class<T> inputType) {
        return WorkflowSequenceFactory.sequence(name, inputType);
    }

    // =====================================
    // Backward Compatibility Types
    // =====================================

    /**
     * Specification for an LLM node in a sequence.
     *
     * @param <T> The input/output type
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @param userMessageExtractor Optional function to extract the user message from the current state
     */
    public record LLMNodeSpec<T>(
        String nodeId,
        ChatModel model,
        String systemPrompt,
        Function<GraphWorkflowState<T>, String> userMessageExtractor
    ) {}
}