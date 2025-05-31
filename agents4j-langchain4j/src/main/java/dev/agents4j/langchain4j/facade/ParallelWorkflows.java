/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.langchain4j.facade;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.exception.AgentExecutionException;
import dev.agents4j.langchain4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;
import java.util.Map;

/**
 * Focused facade for parallel workflow operations.
 * This class follows the Single Responsibility Principle by handling only parallel execution workflows.
 */
public final class ParallelWorkflows {

    private static final int DEFAULT_WORKER_COUNT = 4;

    private ParallelWorkflows() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a parallelization workflow for concurrent processing of multiple inputs.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new ParallelizationWorkflow instance
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static ParallelizationWorkflow create(String name, ChatModel model) {
        validateCreateParameters(name, model);
        
        return ParallelizationWorkflow.builder()
            .name(name)
            .chatModel(model)
            .build();
    }

    /**
     * Execute multiple inputs in parallel using the same prompt.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template to apply to each input
     * @param inputs The list of inputs to process in parallel
     * @param numWorkers The number of worker threads to use
     * @return List of responses in the same order as inputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static List<String> execute(
        ChatModel model,
        String prompt,
        List<String> inputs,
        int numWorkers
    ) {
        validateExecuteParameters(model, prompt, inputs, numWorkers);

        ParallelizationWorkflow workflow = create("ParallelQueryWorkflow", model);
        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, numWorkers);

        try {
            return workflow.start(parallelInput).getOutput().orElseThrow(() -> 
                new AgentExecutionException(
                    "ParallelQueryWorkflow",
                    "Workflow completed but produced no output",
                    null,
                    Map.of(
                        "prompt", prompt,
                        "inputCount", inputs.size(),
                        "numWorkers", numWorkers
                    )
                ));
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "ParallelQueryWorkflow",
                "Failed to execute parallel query",
                e,
                Map.of(
                    "prompt", prompt,
                    "inputCount", inputs.size(),
                    "numWorkers", numWorkers
                )
            );
        }
    }

    /**
     * Execute multiple inputs in parallel using the same prompt with default worker count.
     * Uses a default of 4 worker threads.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template to apply to each input
     * @param inputs The list of inputs to process in parallel
     * @return List of responses in the same order as inputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static List<String> execute(
        ChatModel model,
        String prompt,
        List<String> inputs
    ) {
        return execute(model, prompt, inputs, DEFAULT_WORKER_COUNT);
    }

    /**
     * Execute a batch of similar tasks in parallel with automatic batching.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template
     * @param inputs The inputs to process
     * @param batchSize The size of each batch
     * @return List of responses in the same order as inputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static List<String> executeBatch(
        ChatModel model,
        String prompt,
        List<String> inputs,
        int batchSize
    ) {
        validateExecuteParameters(model, prompt, inputs);
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        // Calculate optimal worker count based on batch size
        int numWorkers = Math.min(
            Math.max(1, inputs.size() / batchSize),
            Runtime.getRuntime().availableProcessors()
        );

        return execute(model, prompt, inputs, numWorkers);
    }

    /**
     * Execute inputs in parallel with automatic worker count optimization.
     * Automatically determines the optimal number of workers based on system capabilities and input size.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template
     * @param inputs The inputs to process
     * @return List of responses in the same order as inputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static List<String> executeOptimized(
        ChatModel model,
        String prompt,
        List<String> inputs
    ) {
        validateExecuteParameters(model, prompt, inputs);

        // Determine optimal worker count
        int optimalWorkers = calculateOptimalWorkerCount(inputs.size());
        
        return execute(model, prompt, inputs, optimalWorkers);
    }

    /**
     * Execute a voting mechanism where multiple workers process the same input.
     *
     * @param model The ChatModel to use
     * @param input The input to process
     * @param votingPrompt The prompt for voting
     * @param voteCount The number of votes to collect
     * @return The result with voting metadata
     * @throws AgentExecutionException if workflow execution fails
     */
    public static VotingResult executeVoting(
        ChatModel model,
        String input,
        String votingPrompt,
        int voteCount
    ) {
        validateVotingParameters(model, input, votingPrompt, voteCount);

        // Create multiple copies of the same input for voting
        List<String> votingInputs = java.util.Collections.nCopies(voteCount, input);
        
        List<String> votes = execute(model, votingPrompt, votingInputs, voteCount);
        
        return new VotingResult(input, votes, selectMajorityVote(votes));
    }

    private static void validateCreateParameters(String name, ChatModel model) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name cannot be null or empty");
        }
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
    }

    private static void validateExecuteParameters(ChatModel model, String prompt, List<String> inputs) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Inputs list cannot be null or empty");
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i) == null) {
                throw new IllegalArgumentException("Input at index " + i + " cannot be null");
            }
        }
    }

    private static void validateExecuteParameters(ChatModel model, String prompt, List<String> inputs, int numWorkers) {
        validateExecuteParameters(model, prompt, inputs);
        if (numWorkers <= 0) {
            throw new IllegalArgumentException("Number of workers must be positive");
        }
    }

    private static void validateVotingParameters(ChatModel model, String input, String votingPrompt, int voteCount) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }
        if (votingPrompt == null || votingPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Voting prompt cannot be null or empty");
        }
        if (voteCount <= 0) {
            throw new IllegalArgumentException("Vote count must be positive");
        }
    }

    private static int calculateOptimalWorkerCount(int inputSize) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        if (inputSize <= 2) {
            return 1;
        } else if (inputSize <= 10) {
            return Math.min(inputSize, availableProcessors);
        } else {
            // For larger inputs, use up to 75% of available processors
            return Math.min(inputSize, (int) (availableProcessors * 0.75));
        }
    }

    private static String selectMajorityVote(List<String> votes) {
        Map<String, Long> voteCount = votes.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                java.util.function.Function.identity(),
                java.util.stream.Collectors.counting()
            ));

        return voteCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(votes.get(0)); // Fallback to first vote if no majority
    }

    /**
     * Result of a voting operation containing the original input, all votes, and the selected result.
     */
    public static class VotingResult {
        private final String originalInput;
        private final List<String> votes;
        private final String selectedResult;

        public VotingResult(String originalInput, List<String> votes, String selectedResult) {
            this.originalInput = originalInput;
            this.votes = List.copyOf(votes);
            this.selectedResult = selectedResult;
        }

        public String getOriginalInput() {
            return originalInput;
        }

        public List<String> getVotes() {
            return votes;
        }

        public String getSelectedResult() {
            return selectedResult;
        }

        public int getVoteCount() {
            return votes.size();
        }

        public Map<String, Long> getVoteDistribution() {
            return votes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    java.util.function.Function.identity(),
                    java.util.stream.Collectors.counting()
                ));
        }
    }
}