/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.workflow;

import dev.agents4j.api.AgentWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
public class ParallelizationWorkflow implements AgentWorkflow<ParallelizationWorkflow.ParallelInput, List<String>> {

    private final String name;
    private final ChatModel chatModel;

    /**
     * Creates a new ParallelizationWorkflow with the specified name and chat model.
     *
     * @param name The name of the workflow
     * @param chatModel The ChatModel to use for processing
     */
    public ParallelizationWorkflow(String name, ChatModel chatModel) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel cannot be null");
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
    public List<String> execute(ParallelInput input) throws WorkflowExecutionException {
        try {
            return parallel(input.getPrompt(), input.getInputs(), input.getNumWorkers());
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
    public List<String> execute(ParallelInput input, Map<String, Object> context) throws WorkflowExecutionException {
        // Store execution context for potential debugging or tracking
        context.put("workflow_name", name);
        context.put("num_inputs", input.getInputs().size());
        context.put("num_workers", input.getNumWorkers());
        
        long startTime = System.currentTimeMillis();
        List<String> results = execute(input);
        long endTime = System.currentTimeMillis();
        
        // Store results in context
        context.put("results", results);
        context.put("execution_time", endTime - startTime);
        
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<List<String>> executeAsync(ParallelInput input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<List<String>> executeAsync(ParallelInput input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("workflowType", "parallelization");
        config.put("maxConcurrency", Runtime.getRuntime().availableProcessors());
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationProperty(String key, T defaultValue) {
        return (T) getConfiguration().getOrDefault(key, defaultValue);
    }
}