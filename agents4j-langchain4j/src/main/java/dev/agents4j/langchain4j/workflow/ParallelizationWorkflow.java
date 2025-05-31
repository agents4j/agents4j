/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.langchain4j.workflow;

import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
public class ParallelizationWorkflow implements StatefulWorkflow<ParallelizationWorkflow.ParallelInput, List<String>> {

    private static final String PARALLEL_PROCESSOR_NODE = "parallel-processor";
    private static final String AGGREGATOR_NODE = "aggregator";
    
    private final String name;
    private final ChatModel chatModel;
    private final List<StatefulAgentNode<ParallelInput>> nodes;
    private final List<WorkflowRoute<ParallelInput>> routes;

    /**
     * Creates a new ParallelizationWorkflow with the specified name and chat model.
     *
     * @param name The name of the workflow
     * @param chatModel The ChatModel to use for processing
     */
    public ParallelizationWorkflow(String name, ChatModel chatModel) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.nodes = createNodes();
        this.routes = createRoutes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<List<String>> start(ParallelInput input) throws WorkflowExecutionException {
        return start(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<List<String>> start(ParallelInput input, Map<String, Object> context) 
            throws WorkflowExecutionException {
        WorkflowState initialState = WorkflowState.create(name + "-" + System.currentTimeMillis());
        return start(input, initialState, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<List<String>> start(ParallelInput input, WorkflowState initialState, 
            Map<String, Object> context) throws WorkflowExecutionException {
        try {
            // Store execution context
            context.put("workflow_name", name);
            context.put("num_inputs", input.getInputs().size());
            context.put("num_workers", input.getNumWorkers());
            
            long startTime = System.currentTimeMillis();
            
            // Initialize state with input data
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("prompt", input.getPrompt());
            stateData.put("inputs", input.getInputs());
            stateData.put("numWorkers", input.getNumWorkers());
            stateData.put("startTime", startTime);
            
            WorkflowState state = initialState.withUpdatesAndCurrentNode(stateData, PARALLEL_PROCESSOR_NODE);
            
            // Execute parallel processing
            StatefulAgentNode<ParallelInput> processorNode = getNode(PARALLEL_PROCESSOR_NODE)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Parallel processor node not found"));
            
            WorkflowCommand<ParallelInput> command = processorNode.process(input, state, context);
            
            // Handle the command
            return handleCommand(command, input, state, context);
            
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("inputCount", input.getInputs().size());
            errorContext.put("numWorkers", input.getNumWorkers());
            throw new WorkflowExecutionException(name, "Parallelization workflow execution failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<List<String>> resume(ParallelInput input, WorkflowState state) 
            throws WorkflowExecutionException {
        return resume(input, state, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<List<String>> resume(ParallelInput input, WorkflowState state, 
            Map<String, Object> context) throws WorkflowExecutionException {
        try {
            String currentNodeId = state.getCurrentNodeId()
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Cannot resume: no current node in state"));
            
            StatefulAgentNode<ParallelInput> currentNode = getNode(currentNodeId)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Current node not found: " + currentNodeId));
            
            WorkflowCommand<ParallelInput> command = currentNode.process(input, state, context);
            
            return handleCommand(command, input, state, context);
            
        } catch (Exception e) {
            throw new WorkflowExecutionException(name, "Failed to resume workflow", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<List<String>>> startAsync(ParallelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<List<String>>> startAsync(ParallelInput input, 
            Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<List<String>>> resumeAsync(ParallelInput input, 
            WorkflowState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<List<String>>> resumeAsync(ParallelInput input, 
            WorkflowState state, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<ParallelInput>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<ParallelInput>> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<StatefulAgentNode<ParallelInput>> getNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<ParallelInput>> getRoutesFrom(String fromNodeId) {
        return routes.stream()
                .filter(route -> route.getFromNodeId().equals(fromNodeId))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<ParallelInput>> getEntryPoints() {
        return nodes.stream()
                .filter(StatefulAgentNode::canBeEntryPoint)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws IllegalStateException {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one node");
        }
        
        if (getEntryPoints().isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one entry point");
        }
        
        // Validate all routes reference existing nodes
        for (WorkflowRoute<ParallelInput> route : routes) {
            if (getNode(route.getFromNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent from node: " + route.getFromNodeId());
            }
            if (getNode(route.getToNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent to node: " + route.getToNodeId());
            }
        }
    }

    private List<StatefulAgentNode<ParallelInput>> createNodes() {
        List<StatefulAgentNode<ParallelInput>> nodeList = new ArrayList<>();
        
        // Parallel processor node
        nodeList.add(new ParallelProcessorNode(PARALLEL_PROCESSOR_NODE, chatModel));
        
        // Aggregator node
        nodeList.add(new AggregatorNode(AGGREGATOR_NODE));
        
        return nodeList;
    }

    private List<WorkflowRoute<ParallelInput>> createRoutes() {
        List<WorkflowRoute<ParallelInput>> routeList = new ArrayList<>();
        
        // Route from processor to aggregator
        routeList.add(WorkflowRoute.<ParallelInput>builder()
                .id("processor-to-aggregator")
                .from(PARALLEL_PROCESSOR_NODE)
                .to(AGGREGATOR_NODE)
                .description("Route from parallel processor to aggregator")
                .build());
        
        return routeList;
    }

    private StatefulWorkflowResult<List<String>> handleCommand(WorkflowCommand<ParallelInput> command, 
            ParallelInput input, WorkflowState state, Map<String, Object> context) {
        
        // Apply state updates
        WorkflowState newState = state.withUpdates(command.getStateUpdates());
        
        switch (command.getType()) {
            case COMPLETE:
                @SuppressWarnings("unchecked")
                List<String> results = (List<String>) newState.get("results").orElse(Collections.emptyList());
                
                // Add execution metadata
                long endTime = System.currentTimeMillis();
                long startTime = newState.get("startTime", 0L);
                context.put("execution_time", endTime - startTime);
                context.put("results", results);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("execution_time", endTime - startTime);
                metadata.put("num_results", results.size());
                
                return StatefulWorkflowResult.withMetadata(
                        StatefulWorkflowResult.Status.COMPLETED, 
                        results, 
                        newState, 
                        null, 
                        metadata);
                
            case CONTINUE:
                // Find next node via routes
                String currentNodeId = newState.getCurrentNodeId().orElse("");
                List<WorkflowRoute<ParallelInput>> availableRoutes = getRoutesFrom(currentNodeId);
                
                if (availableRoutes.isEmpty()) {
                    return StatefulWorkflowResult.error("No routes available from node: " + currentNodeId, newState);
                }
                
                // Take the first matching route
                WorkflowRoute<ParallelInput> route = availableRoutes.get(0);
                String nextNodeId = route.getToNodeId();
                
                StatefulAgentNode<ParallelInput> nextNode = getNode(nextNodeId)
                        .orElseThrow(() -> new RuntimeException("Next node not found: " + nextNodeId));
                
                WorkflowState nextState = newState.withCurrentNode(nextNodeId);
                ParallelInput nextInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<ParallelInput> nextCommand = nextNode.process(nextInput, nextState, context);
                return handleCommand(nextCommand, nextInput, nextState, context);
                
            case GOTO:
                String targetNodeId = command.getTargetNodeId()
                        .orElseThrow(() -> new RuntimeException("GOTO command missing target node"));
                
                StatefulAgentNode<ParallelInput> targetNode = getNode(targetNodeId)
                        .orElseThrow(() -> new RuntimeException("Target node not found: " + targetNodeId));
                
                WorkflowState gotoState = newState.withCurrentNode(targetNodeId);
                ParallelInput gotoInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<ParallelInput> gotoCommand = targetNode.process(gotoInput, gotoState, context);
                return handleCommand(gotoCommand, gotoInput, gotoState, context);
                
            case SUSPEND:
                return StatefulWorkflowResult.suspended(newState);
                
            case ERROR:
                String errorMessage = command.getErrorMessage().orElse("Unknown error occurred");
                return StatefulWorkflowResult.error(errorMessage, newState);
                
            default:
                return StatefulWorkflowResult.error("Unknown command type: " + command.getType(), newState);
        }
    }



    /**
     * Input container for ParallelizationWorkflow that encapsulates the prompt,
     * list of inputs to process, and the number of worker threads.
     */
    public static class ParallelInput {
        private final String prompt;
        private final List<String> inputs;
        private final int numWorkers;

        /**
         * Creates a new ParallelInput instance.
         *
         * @param prompt The prompt template to use
         * @param inputs The list of inputs to process
         * @param numWorkers The number of worker threads
         */
        public ParallelInput(String prompt, List<String> inputs, int numWorkers) {
            this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
            this.inputs = Objects.requireNonNull(inputs, "Inputs cannot be null");
            this.numWorkers = numWorkers;

            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("Inputs list cannot be empty");
            }
            if (numWorkers <= 0) {
                throw new IllegalArgumentException("Number of workers must be greater than 0");
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
         * Gets the list of inputs to process.
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
     * Processes multiple inputs concurrently using a fixed thread pool and the same prompt template.
     * This method maintains the order of results corresponding to the input order.
     *
     * @param prompt   The prompt template to use for each input. The input will be appended to this prompt.
     *                 Must not be null. Example: "Translate the following text to French:"
     * @param inputs   List of input strings to process. Each input will be processed independently
     *                 in parallel. Must not be null or empty. Example: ["Hello", "World", "Good morning"]
     * @param nWorkers The number of concurrent worker threads to use. This controls the maximum
     *                 number of simultaneous LLM API calls. Must be greater than 0. Consider API
     *                 rate limits when setting this value.
     * @return List of processed results in the same order as the inputs. Each result contains
     *         the LLM's response for the corresponding input.
     * @throws IllegalArgumentException if prompt is null, inputs is null/empty, or nWorkers &lt;= 0
     * @throws RuntimeException if processing fails for any input, with the cause containing
     *         the specific error details
     */
    public List<String> parallel(String prompt, List<String> inputs, int nWorkers) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt cannot be null");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Inputs list cannot be empty");
        }
        if (nWorkers <= 0) {
            throw new IllegalArgumentException("Number of workers must be greater than 0");
        }

        ExecutorService executor = Executors.newFixedThreadPool(nWorkers);
        try {
            List<CompletableFuture<String>> futures = inputs.stream()
                    .map(input -> CompletableFuture.supplyAsync(() -> {
                        try {
                            List<ChatMessage> messages = new ArrayList<>();
                            messages.add(SystemMessage.from(prompt));
                            messages.add(UserMessage.from("Input: " + input));
                            
                            AiMessage response = chatModel.chat(messages).aiMessage();
                            return response.text();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to process input: " + input, e);
                        }
                    }, executor))
                    .collect(Collectors.toList());

            // Wait for all tasks to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));
            allFutures.join();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Node that performs parallel processing of inputs.
     */
    private static class ParallelProcessorNode implements StatefulAgentNode<ParallelInput> {
        private final String nodeId;
        private final ChatModel chatModel;

        public ParallelProcessorNode(String nodeId, ChatModel chatModel) {
            this.nodeId = nodeId;
            this.chatModel = chatModel;
        }

        @Override
        public WorkflowCommand<ParallelInput> process(ParallelInput input, WorkflowState state, Map<String, Object> context) {
            try {
                String prompt = state.get("prompt", input.getPrompt());
                @SuppressWarnings("unchecked")
                List<String> inputs = (List<String>) state.get("inputs").orElse(input.getInputs());
                int numWorkers = state.get("numWorkers", input.getNumWorkers());

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

                    return WorkflowCommand.<ParallelInput>continueWith()
                            .updateState("results", results)
                            .updateState("processedCount", results.size())
                            .build();

                } finally {
                    executor.shutdown();
                }
            } catch (Exception e) {
                return WorkflowCommand.<ParallelInput>error("Failed to process inputs in parallel: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Parallel Processor";
        }

        @Override
        public boolean canBeEntryPoint() {
            return true;
        }
    }

    /**
     * Node that aggregates parallel processing results.
     */
    private static class AggregatorNode implements StatefulAgentNode<ParallelInput> {
        private final String nodeId;

        public AggregatorNode(String nodeId) {
            this.nodeId = nodeId;
        }

        @Override
        public WorkflowCommand<ParallelInput> process(ParallelInput input, WorkflowState state, Map<String, Object> context) {
            @SuppressWarnings("unchecked")
            List<String> results = (List<String>) state.get("results").orElse(Collections.emptyList());
            
            // Results are already processed, just complete the workflow
            return WorkflowCommand.<ParallelInput>complete()
                    .updateState("finalResults", results)
                    .updateState("completed", true)
                    .build();
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Result Aggregator";
        }
    }

    /**
     * Builder for creating ParallelizationWorkflow instances.
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

    /**
     * Creates a new Builder for constructing ParallelizationWorkflow instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}