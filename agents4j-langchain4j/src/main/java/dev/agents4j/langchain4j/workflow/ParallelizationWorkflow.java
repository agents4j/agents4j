/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphCommandComplete;
import dev.agents4j.api.graph.GraphCommandTraverse;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.workflow.GraphWorkflowImpl;

import dev.agents4j.workflow.output.OutputExtractor;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Implements the Parallelization Workflow pattern for efficient concurrent processing
 * of multiple LLM operations. This pattern enables parallel execution of LLM calls
 * with automated output aggregation, significantly improving throughput for
 * batch processing scenarios.
 * 
 * <p>The pattern manifests in two key variations:</p>
 * 
 * <ul>
 * <li><b>Sectioning</b>: Decomposes a complex task into independent subtasks that
 * can be processed concurrently. For example, analyzing different sections of a
 * document simultaneously.</li>
 * <li><b>Voting</b>: Executes identical prompts multiple times in parallel to
 * gather diverse perspectives or implement majority voting mechanisms. This is
 * particularly useful for validation or consensus-building tasks.</li>
 * </ul>
 *
 * <p><b>Key Benefits:</b></p>
 * <ul>
 * <li>Improved throughput through concurrent processing</li>
 * <li>Better resource utilization of LLM API capacity</li>
 * <li>Reduced overall processing time for batch operations</li>
 * <li>Enhanced result quality through multiple perspectives (in voting scenarios)</li>
 * </ul>
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 * <li>Processing large volumes of similar but independent items</li>
 * <li>Tasks requiring multiple independent perspectives or validations</li>
 * <li>Scenarios where processing time is critical and tasks are parallelizable</li>
 * <li>Complex operations that can be decomposed into independent subtasks</li>
 * </ul>
 *
 * <p><b>Implementation Considerations:</b></p>
 * <ul>
 * <li>Ensure tasks are truly independent to avoid consistency issues</li>
 * <li>Consider API rate limits when determining parallel execution capacity</li>
 * <li>Monitor resource usage (memory, CPU) when scaling parallel operations</li>
 * <li>Implement appropriate error handling for parallel task failures</li>
 * </ul>
 *
 * @see dev.langchain4j.model.chat.ChatModel
 * @see <a href="https://docs.langchain4j.dev/">LangChain4J Documentation</a>
 * @see <a href="https://www.anthropic.com/research/building-effective-agents">Building Effective Agents</a>
 */
public class ParallelizationWorkflow implements GraphWorkflow<ParallelizationWorkflow.ParallelInput, List<String>> {

    private static final NodeId PARALLEL_PROCESSOR_NODE = NodeId.of("parallel-processor");
    private static final NodeId AGGREGATOR_NODE = NodeId.of("aggregator");
    
    // Context keys
    private static final ContextKey<Object> RESULTS_KEY = ContextKey.of("results", Object.class);
    private static final ContextKey<Integer> PROCESSED_COUNT_KEY = ContextKey.of("processedCount", Integer.class);
    
    private final GraphWorkflowImpl<ParallelInput, List<String>> workflow;

    /**
     * Creates a new ParallelizationWorkflow with the specified name and chat model.
     *
     * @param name The name of the workflow
     * @param chatModel The ChatModel to use for processing
     */
    public ParallelizationWorkflow(String name, ChatModel chatModel) {
        Objects.requireNonNull(name, "Workflow name cannot be null");
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        
        this.workflow = createWorkflow(name, chatModel);
    }
    
    private GraphWorkflowImpl<ParallelInput, List<String>> createWorkflow(String name, ChatModel chatModel) {
        // Create processor node
        GraphWorkflowNode<ParallelInput> processorNode = new ParallelProcessorNode(PARALLEL_PROCESSOR_NODE, chatModel);
        
        // Create aggregator node
        GraphWorkflowNode<ParallelInput> aggregatorNode = new AggregatorNode(AGGREGATOR_NODE);
        
        // Create output extractor
        OutputExtractor<ParallelInput, List<String>> outputExtractor = new ParallelOutputExtractor();
        
        // Build workflow
        return GraphWorkflowImpl.<ParallelInput, List<String>>builder()
            .name(name)
            .addNode(processorNode)
            .addNode(aggregatorNode)
            .addEdge(PARALLEL_PROCESSOR_NODE, AGGREGATOR_NODE)
            .defaultEntryPoint(PARALLEL_PROCESSOR_NODE)
            .outputExtractor(outputExtractor)
            .build();
    }
    
    /**
     * Creates a new ParallelizationWorkflow with batched inputs.
     *
     * @param prompt The prompt template to use for each input
     * @param inputs The list of inputs to process in parallel
     * @param numWorkers The number of parallel workers to use
     * @param chatModel The ChatModel to use
     * @return A WorkflowResult containing the processed outputs
     */
    public static WorkflowResult<List<String>, WorkflowError> parallel(
            String prompt,
            List<String> inputs,
            int numWorkers,
            ChatModel chatModel) {
        
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        Objects.requireNonNull(inputs, "Inputs cannot be null");
        Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Inputs list cannot be empty");
        }
        
        if (numWorkers <= 0) {
            numWorkers = Math.min(inputs.size(), Runtime.getRuntime().availableProcessors());
        }
        
        ParallelInput input = new ParallelInput(prompt, inputs, numWorkers);
        ParallelizationWorkflow workflow = new ParallelizationWorkflow(
                "ParallelizationWorkflow-" + System.currentTimeMillis(),
                chatModel);
                
        return workflow.start(input);
    }

    /**
     * A GraphWorkflowNode implementation that processes inputs in parallel.
     */
    private static class ParallelProcessorNode implements GraphWorkflowNode<ParallelInput> {
        private final NodeId nodeId;
        private final ChatModel chatModel;

        public ParallelProcessorNode(NodeId nodeId, ChatModel chatModel) {
            this.nodeId = nodeId;
            this.chatModel = chatModel;
        }

        @Override
        public WorkflowResult<GraphCommand<ParallelInput>, WorkflowError> process(GraphWorkflowState<ParallelInput> state) {
            try {
                ParallelInput input = state.data();
                String prompt = input.getPrompt();
                List<String> inputs = input.getInputs();
                int numWorkers = input.getNumWorkers();

                // Process in parallel
                ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
                try {
                    List<CompletableFuture<String>> futures = IntStream.range(0, inputs.size())
                            .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                                try {
                                    String inputText = inputs.get(i);
                                    List<ChatMessage> messages = new ArrayList<>();
                                    messages.add(SystemMessage.from(prompt));
                                    messages.add(UserMessage.from("Input: " + inputText));
                                    
                                    AiMessage response = chatModel.chat(messages).aiMessage();
                                    return response.text();
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to process input at index " + i, e);
                                }
                            }, executor))
                            .collect(Collectors.toList());

                    CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                            futures.toArray(new CompletableFuture[0]));
                    allFutures.join();

                    List<String> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                            
                    // Create a new context with the results
                    WorkflowContext updatedContext = WorkflowContext.empty()
                            .with(RESULTS_KEY, results)
                            .with(PROCESSED_COUNT_KEY, results.size());
                            
                    // Navigate to the aggregator node
                    return WorkflowResult.success(GraphCommandTraverse.toWithContext(
                            AGGREGATOR_NODE, 
                            updatedContext));

                } finally {
                    executor.shutdown();
                }
            } catch (Exception e) {
                return WorkflowResult.failure(
                        ExecutionError.withCause(
                            "parallel-processing-error",
                            "Failed to process inputs in parallel: " + e.getMessage(),
                            nodeId.value(),
                            e
                        )
                );
            }
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Parallel Processor";
        }
        
        @Override
        public String getDescription() {
            return "Processes multiple inputs in parallel using LLM";
        }
        
        @Override
        public boolean isEntryPoint() {
            return true;
        }
    }

    /**
     * A GraphWorkflowNode implementation that aggregates parallel processing results.
     */
    private static class AggregatorNode implements GraphWorkflowNode<ParallelInput> {
        private final NodeId nodeId;

        public AggregatorNode(NodeId nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public WorkflowResult<GraphCommand<ParallelInput>, WorkflowError> process(GraphWorkflowState<ParallelInput> state) {
            try {
                WorkflowContext context = state.context();
                // Get the results from the context
                @SuppressWarnings("unchecked")
                List<String> results = (List<String>) context.get(RESULTS_KEY)
                        .orElse(Collections.emptyList());
                        
                // Complete the workflow with the results
                return WorkflowResult.success(GraphCommandComplete.withResult(results));
                
            } catch (Exception e) {
                return WorkflowResult.failure(
                        ExecutionError.withCause(
                            "aggregation-error", 
                            "Failed to aggregate results: " + e.getMessage(),
                            nodeId.value(),
                            e
                        )
                );
            }
        }

        @Override
        public NodeId getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Results Aggregator";
        }
        
        @Override
        public String getDescription() {
            return "Aggregates the results from parallel processing";
        }
    }
    
    /**
     * Output extractor for the parallelization workflow.
     */
    private static class ParallelOutputExtractor implements OutputExtractor<ParallelInput, List<String>> {
        @Override
        public List<String> extract(GraphWorkflowState<ParallelInput> state) {
            // Get the results from the context
            @SuppressWarnings("unchecked")
            List<String> results = (List<String>) state.context().get(RESULTS_KEY)
                    .orElse(Collections.emptyList());
            return results;
        }
    }

    /**
     * Input for parallel processing.
     */
    public static class ParallelInput {
        private final String prompt;
        private final List<String> inputs;
        private final int numWorkers;

        /**
         * Creates a new ParallelInput.
         *
         * @param prompt The prompt template to use for each input
         * @param inputs The list of inputs to process in parallel
         * @param numWorkers The number of parallel workers to use
         */
        public ParallelInput(String prompt, List<String> inputs, int numWorkers) {
            this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
            this.inputs = Objects.requireNonNull(inputs, "Inputs cannot be null");
            
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("Inputs list cannot be empty");
            }
            
            if (numWorkers <= 0) {
                this.numWorkers = Math.min(inputs.size(), Runtime.getRuntime().availableProcessors());
            } else {
                this.numWorkers = numWorkers;
            }
        }

        /**
         * Gets the prompt template.
         *
         * @return The prompt template
         */
        public String getPrompt() {
            return prompt;
        }

        /**
         * Gets the list of inputs.
         *
         * @return The list of inputs
         */
        public List<String> getInputs() {
            return inputs;
        }

        /**
         * Gets the number of worker threads.
         *
         * @return The number of worker threads
         */
        public int getNumWorkers() {
            return numWorkers;
        }
    }

    /**
     * Creates a new ParallelizationWorkflow builder.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ParallelizationWorkflow.
     */
    public static class Builder {
        private String name;
        private ChatModel chatModel;

        /**
         * Sets the name of the workflow.
         *
         * @param name The workflow name
         * @return This builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ChatModel to use.
         *
         * @param chatModel The ChatModel instance
         * @return This builder instance
         */
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Builds the ParallelizationWorkflow instance.
         *
         * @return A new ParallelizationWorkflow instance
         * @throws IllegalStateException if required fields are not set
         */
        public ParallelizationWorkflow build() {
            if (name == null) {
                name = "ParallelizationWorkflow-" + System.currentTimeMillis();
            }
            if (chatModel == null) {
                throw new IllegalStateException("ChatModel must be set");
            }
            return new ParallelizationWorkflow(name, chatModel);
        }
    }

    // GraphWorkflow interface implementations
    
    @Override
    public WorkflowResult<List<String>, WorkflowError> start(ParallelInput input) {
        return workflow.start(input);
    }

    @Override
    public WorkflowResult<List<String>, WorkflowError> start(ParallelInput input, WorkflowContext context) {
        return workflow.start(input, context);
    }
    
    @Override
    public WorkflowResult<List<String>, WorkflowError> start(
        WorkflowId workflowId,
        ParallelInput input,
        WorkflowContext context
    ) {
        return workflow.start(workflowId, input, context);
    }

    @Override
    public WorkflowResult<List<String>, WorkflowError> resume(GraphWorkflowState<ParallelInput> state) {
        return workflow.resume(state);
    }

    @Override
    public WorkflowResult<List<String>, WorkflowError> resume(GraphWorkflowState<ParallelInput> state, WorkflowContext contextUpdates) {
        return workflow.resume(state, contextUpdates);
    }

    @Override
    public CompletableFuture<WorkflowResult<List<String>, WorkflowError>> startAsync(ParallelInput input) {
        return workflow.startAsync(input);
    }

    @Override
    public CompletableFuture<WorkflowResult<List<String>, WorkflowError>> startAsync(ParallelInput input, WorkflowContext context) {
        return workflow.startAsync(input, context);
    }

    @Override
    public CompletableFuture<WorkflowResult<List<String>, WorkflowError>> resumeAsync(GraphWorkflowState<ParallelInput> state) {
        return workflow.resumeAsync(state);
    }

    @Override
    public CompletableFuture<WorkflowResult<List<String>, WorkflowError>> resumeAsync(GraphWorkflowState<ParallelInput> state, WorkflowContext contextUpdates) {
        return workflow.resumeAsync(state, contextUpdates);
    }

    @Override
    public String getName() {
        return workflow.getName();
    }

    @Override
    public Map<NodeId, GraphWorkflowNode<ParallelInput>> getNodes() {
        return workflow.getNodes();
    }

    @Override
    public Map<EdgeId, GraphEdge> getEdges() {
        return workflow.getEdges();
    }

    @Override
    public GraphWorkflowNode<ParallelInput> getNode(NodeId nodeId) {
        return workflow.getNode(nodeId);
    }
    
    @Override
    public Set<GraphEdge> getEdgesFrom(NodeId nodeId) {
        return workflow.getEdgesFrom(nodeId);
    }
    
    @Override
    public Set<NodeId> getEntryPoints() {
        return workflow.getEntryPoints();
    }

    @Override
    public ValidationResult validate() {
        return workflow.validate();
    }
}