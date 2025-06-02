package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandComplete;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;
import dev.agents4j.workflow.GraphWorkflowFactory;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.history.NodeInteraction;
import dev.agents4j.workflow.history.ProcessingHistory;
import dev.agents4j.workflow.history.ProcessingHistoryUtils;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        return LLMGraphWorkflowNode.<T>builder()
            .nodeId(nodeId)
            .model(model)
            .systemPrompt(systemPrompt)
            .nextNodeId(nextNodeId)
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
        return LLMGraphWorkflowNode.<T>builder()
            .nodeId(nodeId)
            .model(model)
            .systemPrompt(systemPrompt)
            .userMessageExtractor(userMessageExtractor)
            .nextNodeId(nextNodeId)
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
        return new CompletingLLMNode<>(nodeId, model, systemPrompt, null);
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
        return new CompletingLLMNode<>(nodeId, model, systemPrompt, userMessageExtractor);
    }

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
    @SuppressWarnings("unchecked")
    public static <T> GraphWorkflow<T, String> createLLMSequence(
        String name,
        Class<T> inputType,
        LLMNodeSpec<T>... nodeSpecs
    ) {
        if (nodeSpecs.length == 0) {
            throw new IllegalArgumentException("At least one node specification is required");
        }

        GraphWorkflowBuilder<T, String> builder = GraphWorkflowBuilder.<T, String>create(inputType)
            .name(name)
            .version("1.0.0")
            .outputExtractor(GraphWorkflowFactory.createResponseExtractor());

        // Create nodes with proper next node IDs
        @SuppressWarnings("unchecked")
        GraphWorkflowNode<T>[] nodes = new GraphWorkflowNode[nodeSpecs.length];
        for (int i = 0; i < nodeSpecs.length; i++) {
            LLMNodeSpec<T> spec = nodeSpecs[i];
            String nextNodeId = (i < nodeSpecs.length - 1) ? nodeSpecs[i + 1].nodeId() : null;
            
            // Always use sequence nodes for proper routing in sequences
            if (spec.userMessageExtractor() != null) {
                if (nextNodeId != null) {
                    nodes[i] = createSequenceLLMNode(spec.nodeId(), spec.model(), spec.systemPrompt(), 
                                                   spec.userMessageExtractor(), nextNodeId);
                } else {
                    // Last node - use completing LLM node that will complete the workflow
                    nodes[i] = createCompletingLLMNode(spec.nodeId(), spec.model(), spec.systemPrompt(), 
                                                     spec.userMessageExtractor());
                }
            } else {
                if (nextNodeId != null) {
                    nodes[i] = createSequenceLLMNode(spec.nodeId(), spec.model(), spec.systemPrompt(), nextNodeId);
                } else {
                    // Last node - use completing LLM node that will complete the workflow
                    nodes[i] = createCompletingLLMNode(spec.nodeId(), spec.model(), spec.systemPrompt());
                }
            }
            
            builder.addNode(nodes[i]);
        }

        // Add edges between consecutive nodes
        for (int i = 0; i < nodes.length - 1; i++) {
            builder.addEdge(nodes[i].getNodeId(), nodes[i + 1].getNodeId());
        }

        return builder
            .defaultEntryPoint(nodes[0].getNodeId())
            .build();
    }

    /**
     * Creates a linear sequence workflow from multiple LLM node specifications (varargs convenience method).
     *
     * @param <T> The input/output type
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @param nodeSpecs The specifications for each LLM node in the sequence
     * @return A configured sequence workflow
     */
    @SuppressWarnings("unchecked")
    public static <T> GraphWorkflow<T, String> createLLMSequence(
        String name,
        Class<T> inputType,
        List<LLMNodeSpec<T>> nodeSpecs
    ) {
        LLMNodeSpec<T>[] specArray = nodeSpecs.toArray(new LLMNodeSpec[0]);
        return createLLMSequence(name, inputType, specArray);
    }

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

    /**
     * A specialized LLM node that completes the workflow instead of traversing to the next node.
     * This is ideal for the last node in a sequence workflow.
     */
    private static class CompletingLLMNode<T> implements GraphWorkflowNode<T> {
        private static final Logger LOGGER = Logger.getLogger(CompletingLLMNode.class.getName());
        
        private final NodeId id;
        private final ChatModel model;
        private final String systemPrompt;
        private final Function<GraphWorkflowState<T>, String> userMessageExtractor;
        private final String name;

        public CompletingLLMNode(
            String nodeId,
            ChatModel model,
            String systemPrompt,
            Function<GraphWorkflowState<T>, String> userMessageExtractor
        ) {
            this.id = NodeId.of(nodeId);
            this.model = model;
            this.systemPrompt = systemPrompt;
            this.userMessageExtractor = userMessageExtractor != null 
                ? userMessageExtractor 
                : state -> state.data().toString();
            this.name = "CompletingLLM-" + nodeId;
        }

        @Override
        public WorkflowResult<GraphCommand<T>, WorkflowError> process(GraphWorkflowState<T> state) {
            LOGGER.info(() -> "Processing in completing LLM node: " + id.value());
            
            try {
                // Extract the user message from the state using the provided extractor
                String userMessage = userMessageExtractor.apply(state);
                LOGGER.fine(() -> "Extracted user message: " + userMessage);

                // Create chat messages
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(SystemMessage.from(systemPrompt));
                messages.add(UserMessage.from(userMessage));

                LOGGER.info(() -> "Sending request to LLM with " + messages.size() + " messages");
                
                // Get LLM response
                long startTime = System.currentTimeMillis();
                AiMessage response = model.chat(messages).aiMessage();
                long duration = System.currentTimeMillis() - startTime;
                
                String responseText = response.text();
                LOGGER.info(() -> "Received LLM response in " + duration + "ms");
                LOGGER.fine(() -> "Response content: " + responseText);

                // Get or create the processing history
                ProcessingHistory history = ProcessingHistoryUtils.getOrCreateHistory(state);

                // Add this interaction to the history
                NodeInteraction interaction = new NodeInteraction(
                    id,
                    getName(),
                    userMessage,
                    responseText,
                    Instant.now()
                );
                history.addInteraction(interaction);
                LOGGER.fine(() -> "Added interaction to history. Total interactions: " + 
                             history.getAllInteractions().size());

                // Create updated context with the response and history
                WorkflowContext updatedContext = state
                    .context()
                    // Keep the response key for backward compatibility
                    .with(ContextKey.of("response", Object.class), responseText)
                    // Store the processing history
                    .with(ProcessingHistory.HISTORY_KEY, history);

                LOGGER.info(() -> "LLM processing complete, completing workflow");
                
                // Use GraphCommandComplete to complete the workflow
                return WorkflowResult.success(
                    GraphCommandComplete.withResultAndContext(responseText, updatedContext)
                );
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing with LLM: " + e.getMessage(), e);
                
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
            return name;
        }

        @Override
        public String getDescription() {
            return "Completing LLM Node: " + systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "...";
        }
    }
}
