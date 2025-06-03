package dev.agents4j.langchain4j.workflow.factory;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.workflow.GraphWorkflowFactory;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Factory for creating sequence-based workflows with LLM nodes.
 * This factory provides convenient methods for constructing linear workflows
 * where nodes are executed in sequence.
 */
public class WorkflowSequenceFactory {

    /**
     * Creates a builder for constructing sequence workflows.
     *
     * @param <T> The input type for the workflow
     * @param name The workflow name
     * @param inputType The input type class for type safety
     * @return A new SequenceWorkflowBuilder instance
     */
    public static <T> SequenceWorkflowBuilder<T> sequence(String name, Class<T> inputType) {
        return new SequenceWorkflowBuilder<>(name, inputType);
    }

    /**
     * Builder for creating sequence workflows with fluent API.
     *
     * @param <T> The input type for the workflow
     */
    public static class SequenceWorkflowBuilder<T> {
        private final String name;
        private final Class<T> inputType;
        private final List<LLMNodeSpec<T>> nodeSpecs = new ArrayList<>();
        private String version = "1.0.0";

        private SequenceWorkflowBuilder(String name, Class<T> inputType) {
            this.name = name;
            this.inputType = inputType;
        }

        /**
         * Sets the workflow version.
         *
         * @param version The workflow version
         * @return This builder
         */
        public SequenceWorkflowBuilder<T> version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Adds an LLM node to the sequence.
         *
         * @param nodeId The node ID
         * @param model The ChatModel to use
         * @param systemPrompt The system prompt for the LLM
         * @return This builder
         */
        public SequenceWorkflowBuilder<T> addNode(String nodeId, ChatModel model, String systemPrompt) {
            nodeSpecs.add(new LLMNodeSpec<>(nodeId, model, systemPrompt, null));
            return this;
        }

        /**
         * Adds an LLM node to the sequence with a custom message extractor.
         *
         * @param nodeId The node ID
         * @param model The ChatModel to use
         * @param systemPrompt The system prompt for the LLM
         * @param userMessageExtractor Function to extract the user message from the current state
         * @return This builder
         */
        public SequenceWorkflowBuilder<T> addNode(
            String nodeId, 
            ChatModel model, 
            String systemPrompt,
            Function<dev.agents4j.api.graph.GraphWorkflowState<T>, String> userMessageExtractor
        ) {
            nodeSpecs.add(new LLMNodeSpec<>(nodeId, model, systemPrompt, userMessageExtractor));
            return this;
        }

        /**
         * Adds a pre-configured LLM node specification to the sequence.
         *
         * @param nodeSpec The LLM node specification
         * @return This builder
         */
        public SequenceWorkflowBuilder<T> addNode(LLMNodeSpec<T> nodeSpec) {
            nodeSpecs.add(nodeSpec);
            return this;
        }

        /**
         * Adds multiple LLM node specifications to the sequence.
         *
         * @param nodeSpecs The LLM node specifications
         * @return This builder
         */
        @SafeVarargs
        public final SequenceWorkflowBuilder<T> addNodes(LLMNodeSpec<T>... nodeSpecs) {
            this.nodeSpecs.addAll(Arrays.asList(nodeSpecs));
            return this;
        }

        /**
         * Adds multiple LLM node specifications to the sequence.
         *
         * @param nodeSpecs The LLM node specifications
         * @return This builder
         */
        public SequenceWorkflowBuilder<T> addNodes(List<LLMNodeSpec<T>> nodeSpecs) {
            this.nodeSpecs.addAll(nodeSpecs);
            return this;
        }

        /**
         * Builds the sequence workflow with the configured properties.
         *
         * @return A new GraphWorkflow instance
         * @throws IllegalStateException if no nodes are specified
         */
        public GraphWorkflow<T, String> build() {
            if (nodeSpecs.isEmpty()) {
                throw new IllegalArgumentException("At least one node specification is required");
            }

            GraphWorkflowBuilder<T, String> builder = GraphWorkflowBuilder.<T, String>create(inputType)
                .name(name)
                .version(version)
                .outputExtractor(GraphWorkflowFactory.createResponseExtractor());

            // Create nodes with proper next node IDs
            @SuppressWarnings("unchecked")
            GraphWorkflowNode<T>[] nodes = new GraphWorkflowNode[nodeSpecs.size()];
            for (int i = 0; i < nodeSpecs.size(); i++) {
                LLMNodeSpec<T> spec = nodeSpecs.get(i);
                
                LLMNodeFactory.LLMNodeBuilder<T> nodeBuilder = LLMNodeFactory
                    .llmNode(spec.nodeId(), spec.model(), spec.systemPrompt());
                
                if (spec.userMessageExtractor() != null) {
                    nodeBuilder.userMessageExtractor(spec.userMessageExtractor());
                }
                
                // If this is the last node, make it completing
                if (i == nodeSpecs.size() - 1) {
                    nodes[i] = nodeBuilder.completing().build();
                } else {
                    // Set next node ID for sequence flow
                    String nextNodeId = nodeSpecs.get(i + 1).nodeId();
                    nodes[i] = nodeBuilder.nextNode(nextNodeId).build();
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
        Function<dev.agents4j.api.graph.GraphWorkflowState<T>, String> userMessageExtractor
    ) {
        /**
         * Creates a simple LLM node specification without custom message extractor.
         *
         * @param <T> The input/output type
         * @param nodeId The node ID
         * @param model The ChatModel to use
         * @param systemPrompt The system prompt for the LLM
         * @return A new LLMNodeSpec
         */
        public static <T> LLMNodeSpec<T> of(String nodeId, ChatModel model, String systemPrompt) {
            return new LLMNodeSpec<>(nodeId, model, systemPrompt, null);
        }

        /**
         * Creates an LLM node specification with custom message extractor.
         *
         * @param <T> The input/output type
         * @param nodeId The node ID
         * @param model The ChatModel to use
         * @param systemPrompt The system prompt for the LLM
         * @param userMessageExtractor Function to extract the user message from the current state
         * @return A new LLMNodeSpec
         */
        public static <T> LLMNodeSpec<T> of(
            String nodeId, 
            ChatModel model, 
            String systemPrompt,
            Function<dev.agents4j.api.graph.GraphWorkflowState<T>, String> userMessageExtractor
        ) {
            return new LLMNodeSpec<>(nodeId, model, systemPrompt, userMessageExtractor);
        }
    }
}