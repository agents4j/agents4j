package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandTraverse;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.WorkflowError;
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
 * A GraphWorkflowNode implementation that processes input with a Large Language Model.
 * This node uses a ChatModel to process user messages with a specified system prompt,
 * and tracks interaction history.
 *
 * @param <T> The type of input/output for the node
 */
public class LLMGraphWorkflowNode<T> implements GraphWorkflowNode<T> {
    private static final Logger LOGGER = Logger.getLogger(LLMGraphWorkflowNode.class.getName());
    
    private final NodeId id;
    private final ChatModel model;
    private final String systemPrompt;
    private final Function<GraphWorkflowState<T>, String> userMessageExtractor;
    private final String name;
    private final String description;
    private final NodeId nextNodeId;

    private LLMGraphWorkflowNode(Builder<T> builder) {
        this.id = NodeId.of(builder.nodeId);
        this.model = builder.model;
        this.systemPrompt = builder.systemPrompt;
        this.userMessageExtractor = builder.userMessageExtractor;
        this.name = builder.name != null ? builder.name : "LLM Node";
        this.description = builder.description != null ? 
                builder.description : 
                "Processes input with LLM using system prompt: " + systemPrompt;
        this.nextNodeId = builder.nextNodeId != null ? 
                NodeId.of(builder.nextNodeId) : 
                NodeId.of("next");
    }

    @Override
    public WorkflowResult<GraphCommand<T>, WorkflowError> process(GraphWorkflowState<T> state) {
        LOGGER.info(() -> "Processing in LLM node: " + id.value());
        
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

            LOGGER.info(() -> "LLM processing complete, traversing to node: " + nextNodeId.value());
            
            return WorkflowResult.success(
                GraphCommandTraverse.toWithContext(nextNodeId, updatedContext)
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

    /**
     * Creates a builder for constructing LLMGraphWorkflowNode instances.
     *
     * @param <T> The type of input/output for the node
     * @return A new Builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for creating LLMGraphWorkflowNode instances.
     *
     * @param <T> The type of input/output for the node
     */
    public static class Builder<T> {
        private String nodeId;
        private ChatModel model;
        private String systemPrompt;
        private Function<GraphWorkflowState<T>, String> userMessageExtractor;
        private String name;
        private String description;
        private String nextNodeId;

        /**
         * Sets the node ID for the LLM node.
         *
         * @param nodeId The node ID
         * @return This builder
         */
        public Builder<T> nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * Sets the ChatModel to use for LLM processing.
         *
         * @param model The ChatModel instance
         * @return This builder
         */
        public Builder<T> model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the system prompt to use for LLM processing.
         *
         * @param systemPrompt The system prompt
         * @return This builder
         */
        public Builder<T> systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Sets the function to extract user messages from the workflow state.
         *
         * @param userMessageExtractor Function to extract user messages
         * @return This builder
         */
        public Builder<T> userMessageExtractor(Function<GraphWorkflowState<T>, String> userMessageExtractor) {
            this.userMessageExtractor = userMessageExtractor;
            return this;
        }

        /**
         * Sets the name of the LLM node.
         *
         * @param name The node name
         * @return This builder
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description of the LLM node.
         *
         * @param description The node description
         * @return This builder
         */
        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the ID of the next node to traverse to after processing.
         *
         * @param nextNodeId The next node ID
         * @return This builder
         */
        public Builder<T> nextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId;
            return this;
        }

        /**
         * Builds a new LLMGraphWorkflowNode with the configured properties.
         *
         * @return A new LLMGraphWorkflowNode instance
         * @throws IllegalStateException if required properties are not set
         */
        public LLMGraphWorkflowNode<T> build() {
            if (nodeId == null) {
                throw new IllegalStateException("Node ID must be specified");
            }
            if (model == null) {
                throw new IllegalStateException("ChatModel must be specified");
            }
            if (systemPrompt == null) {
                throw new IllegalStateException("System prompt must be specified");
            }
            if (userMessageExtractor == null) {
                // Default extractor that converts the state data to string
                userMessageExtractor = state -> state.data().toString();
            }
            
            return new LLMGraphWorkflowNode<>(this);
        }
    }
}