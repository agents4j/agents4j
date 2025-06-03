package dev.agents4j.langchain4j.workflow.factory;

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
import dev.agents4j.langchain4j.workflow.LLMGraphWorkflowNode;
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
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating LLM-based workflow nodes with enhanced capabilities.
 * This factory provides convenient methods for constructing LLM nodes
 * compatible with the GraphWorkflow API.
 */
public class LLMNodeFactory {

    /**
     * Creates a builder for constructing LLM workflow nodes.
     *
     * @param <T> The type of input/output for the node
     * @param nodeId The node ID
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the LLM
     * @return A new LLMNodeBuilder instance
     */
    public static <T> LLMNodeBuilder<T> llmNode(String nodeId, ChatModel model, String systemPrompt) {
        return new LLMNodeBuilder<T>(nodeId, model, systemPrompt);
    }

    /**
     * Builder for creating LLM workflow nodes with fluent API and enhanced capabilities.
     *
     * @param <T> The type of input/output for the node
     */
    public static class LLMNodeBuilder<T> {
        private final String nodeId;
        private final ChatModel model;
        private final String systemPrompt;
        private Function<GraphWorkflowState<T>, String> userMessageExtractor;
        private String nextNodeId;
        private boolean completing = false;
        private String name;
        private String description;

        private LLMNodeBuilder(String nodeId, ChatModel model, String systemPrompt) {
            this.nodeId = nodeId;
            this.model = model;
            this.systemPrompt = systemPrompt;
        }

        /**
         * Sets a custom user message extractor function.
         *
         * @param extractor Function to extract the user message from the current state
         * @return This builder
         */
        public LLMNodeBuilder<T> userMessageExtractor(Function<GraphWorkflowState<T>, String> extractor) {
            this.userMessageExtractor = extractor;
            return this;
        }

        /**
         * Sets the next node ID for sequence workflows.
         *
         * @param nextNodeId The ID of the next node in the sequence
         * @return This builder
         */
        public LLMNodeBuilder<T> nextNode(String nextNodeId) {
            this.nextNodeId = nextNodeId;
            return this;
        }

        /**
         * Configures this node to complete the workflow instead of traversing to the next node.
         * This is ideal for the last node in a sequence workflow.
         *
         * @return This builder
         */
        public LLMNodeBuilder<T> completing() {
            this.completing = true;
            return this;
        }

        /**
         * Sets a custom name for the node.
         *
         * @param name The node name
         * @return This builder
         */
        public LLMNodeBuilder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a custom description for the node.
         *
         * @param description The node description
         * @return This builder
         */
        public LLMNodeBuilder<T> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the GraphWorkflowNode with the configured properties.
         *
         * @return A new GraphWorkflowNode instance
         */
        public GraphWorkflowNode<T> build() {
            if (completing) {
                return new CompletingLLMNode<>(this);
            } else {
                LLMGraphWorkflowNode.Builder<T> builder = LLMGraphWorkflowNode.<T>builder()
                    .nodeId(nodeId)
                    .model(model)
                    .systemPrompt(systemPrompt);

                if (userMessageExtractor != null) {
                    builder.userMessageExtractor(userMessageExtractor);
                }
                if (nextNodeId != null) {
                    builder.nextNodeId(nextNodeId);
                }
                if (name != null) {
                    builder.name(name);
                }
                if (description != null) {
                    builder.description(description);
                }

                return builder.build();
            }
        }
    }

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
        private final String description;

        private CompletingLLMNode(LLMNodeBuilder<T> builder) {
            this.id = NodeId.of(builder.nodeId);
            this.model = builder.model;
            this.systemPrompt = builder.systemPrompt;
            this.userMessageExtractor = builder.userMessageExtractor != null 
                ? builder.userMessageExtractor 
                : state -> state.data().toString();
            this.name = builder.name != null ? builder.name : "CompletingLLM-" + builder.nodeId;
            this.description = builder.description != null ? builder.description :
                "Completing LLM Node: " + systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "...";
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
            return description;
        }
    }
}