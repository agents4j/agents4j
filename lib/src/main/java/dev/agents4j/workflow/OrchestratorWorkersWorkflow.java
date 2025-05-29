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
 * Implements the Orchestrator-Workers Workflow pattern for complex task decomposition
 * and specialized processing. This pattern demonstrates how to implement more complex
 * agent-like behavior while maintaining control through a central orchestrator that
 * coordinates task decomposition and specialized workers that handle specific subtasks.
 * 
 * <p>The pattern consists of three main phases:</p>
 * 
 * <ul>
 * <li><b>Orchestration</b>: A central LLM analyzes the input task and determines what
 * subtasks need to be performed, how they should be executed, and which workers should
 * handle them.</li>
 * <li><b>Execution</b>: Specialized workers process subtasks in parallel, each optimized
 * for specific types of operations or domains.</li>
 * <li><b>Synthesis</b>: Results from all workers are collected and combined into a
 * coherent final response.</li>
 * </ul>
 *
 * <p><b>Key Benefits:</b></p>
 * <ul>
 * <li>Dynamic task decomposition based on content analysis</li>
 * <li>Specialized processing through domain-specific workers</li>
 * <li>Parallel execution of independent subtasks</li>
 * <li>Clear boundaries that maintain system reliability</li>
 * <li>Adaptive problem-solving capabilities</li>
 * </ul>
 *
 * <p><b>When to Use:</b></p>
 * <ul>
 * <li>Complex tasks where subtasks can't be predicted upfront</li>
 * <li>Tasks requiring different approaches or perspectives</li>
 * <li>Situations needing adaptive problem-solving</li>
 * <li>Multi-domain problems requiring specialized expertise</li>
 * <li>Tasks where the processing strategy depends on the input content</li>
 * </ul>
 *
 * <p><b>Implementation Considerations:</b></p>
 * <ul>
 * <li>Ensure workers are truly specialized and independent</li>
 * <li>Design clear interfaces between orchestrator and workers</li>
 * <li>Consider the overhead of orchestration for simple tasks</li>
 * <li>Implement robust error handling for failed subtasks</li>
 * <li>Monitor resource usage with parallel worker execution</li>
 * </ul>
 *
 * @see dev.langchain4j.model.chat.ChatModel
 * @see dev.agents4j.workflow.ParallelizationWorkflow
 * @see <a href="https://docs.langchain4j.dev/">LangChain4J Documentation</a>
 * @see <a href="https://www.anthropic.com/research/building-effective-agents">Building Effective Agents</a>
 */
public class OrchestratorWorkersWorkflow implements AgentWorkflow<OrchestratorWorkersWorkflow.OrchestratorInput, OrchestratorWorkersWorkflow.WorkerResponse> {

    private final String name;
    private final ChatModel chatModel;
    private final Map<String, WorkerConfig> workerConfigs;
    private final String orchestratorPrompt;
    private final String synthesizerPrompt;

    /**
     * Creates a new OrchestratorWorkersWorkflow.
     *
     * @param name The name of the workflow
     * @param chatModel The ChatModel to use for orchestration and workers
     * @param workerConfigs Configuration for available workers
     * @param orchestratorPrompt Custom prompt for the orchestrator (optional)
     * @param synthesizerPrompt Custom prompt for result synthesis (optional)
     */
    public OrchestratorWorkersWorkflow(
        String name,
        ChatModel chatModel,
        Map<String, WorkerConfig> workerConfigs,
        String orchestratorPrompt,
        String synthesizerPrompt
    ) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.chatModel = Objects.requireNonNull(chatModel, "ChatModel cannot be null");
        this.workerConfigs = new HashMap<>(Objects.requireNonNull(workerConfigs, "Worker configs cannot be null"));
        this.orchestratorPrompt = orchestratorPrompt != null ? orchestratorPrompt : getDefaultOrchestratorPrompt();
        this.synthesizerPrompt = synthesizerPrompt != null ? synthesizerPrompt : getDefaultSynthesizerPrompt();
        
        if (workerConfigs.isEmpty()) {
            throw new IllegalArgumentException("At least one worker configuration must be provided");
        }
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
    public WorkerResponse execute(OrchestratorInput input) throws WorkflowExecutionException {
        try {
            // Phase 1: Orchestrator analyzes task and determines subtasks
            OrchestratorResponse orchestratorResponse = orchestrate(input.getTaskDescription());
            
            // Phase 2: Workers process subtasks in parallel
            List<SubtaskResult> workerResponses = executeSubtasks(orchestratorResponse.getSubtasks());
            
            // Phase 3: Synthesize results into final response
            String finalResult = synthesizeResults(input.getTaskDescription(), workerResponses);
            
            return new WorkerResponse(
                finalResult,
                orchestratorResponse.getSubtasks(),
                workerResponses,
                true
            );
            
        } catch (Exception e) {
            throw new WorkflowExecutionException(name, "Orchestrator-Workers workflow execution failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkerResponse execute(OrchestratorInput input, Map<String, Object> context) throws WorkflowExecutionException {
        // Store execution context
        context.put("workflow_name", name);
        context.put("task_description", input.getTaskDescription());
        context.put("available_workers", workerConfigs.keySet());
        
        long startTime = System.currentTimeMillis();
        WorkerResponse response = execute(input);
        long endTime = System.currentTimeMillis();
        
        // Store results in context
        context.put("subtasks_count", response.getSubtasks().size());
        context.put("execution_time", endTime - startTime);
        context.put("successful_subtasks", response.getSubtaskResults().stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum());
        
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<WorkerResponse> executeAsync(OrchestratorInput input) {
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
    public CompletableFuture<WorkerResponse> executeAsync(OrchestratorInput input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Phase 1: Orchestrator analyzes the task and decomposes it into subtasks.
     */
    private OrchestratorResponse orchestrate(String taskDescription) {
        List<ChatMessage> messages = new ArrayList<>();
        
        String fullPrompt = orchestratorPrompt + "\n\nAvailable workers: " + 
            workerConfigs.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue().getDescription())
                .collect(Collectors.joining(", ")) + 
            "\n\nTask: " + taskDescription;
            
        messages.add(SystemMessage.from(fullPrompt));
        messages.add(UserMessage.from("Please analyze this task and decompose it into subtasks. " +
            "For each subtask, specify the worker type and provide clear instructions."));
        
        AiMessage response = chatModel.chat(messages).aiMessage();
        
        // Parse the orchestrator response to extract subtasks
        return parseOrchestratorResponse(response.text());
    }

    /**
     * Phase 2: Execute subtasks in parallel using appropriate workers.
     */
    private List<SubtaskResult> executeSubtasks(List<Subtask> subtasks) {
        if (subtasks.isEmpty()) {
            return new ArrayList<>();
        }

        int numWorkers = Math.min(subtasks.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        
        try {
            List<CompletableFuture<SubtaskResult>> futures = subtasks.stream()
                .map(subtask -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeSubtask(subtask);
                    } catch (Exception e) {
                        return new SubtaskResult(
                            subtask.getId(),
                            subtask.getWorkerType(),
                            "Error: " + e.getMessage(),
                            false
                        );
                    }
                }, executor))
                .collect(Collectors.toList());

            // Wait for all subtasks to complete
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
     * Execute a single subtask using the appropriate worker.
     */
    private SubtaskResult executeSubtask(Subtask subtask) {
        WorkerConfig workerConfig = workerConfigs.get(subtask.getWorkerType());
        if (workerConfig == null) {
            throw new IllegalArgumentException("Unknown worker type: " + subtask.getWorkerType());
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(workerConfig.getSystemPrompt()));
        messages.add(UserMessage.from(subtask.getInstructions()));
        
        AiMessage response = chatModel.chat(messages).aiMessage();
        
        return new SubtaskResult(
            subtask.getId(),
            subtask.getWorkerType(),
            response.text(),
            true
        );
    }

    /**
     * Phase 3: Synthesize all worker results into a final coherent response.
     */
    private String synthesizeResults(String originalTask, List<SubtaskResult> results) {
        List<ChatMessage> messages = new ArrayList<>();
        
        String resultsText = results.stream()
            .map(result -> "Worker " + result.getWorkerType() + " (Task " + result.getSubtaskId() + "): " + result.getResult())
            .collect(Collectors.joining("\n\n"));
        
        String fullPrompt = synthesizerPrompt + 
            "\n\nOriginal task: " + originalTask +
            "\n\nWorker results:\n" + resultsText;
            
        messages.add(SystemMessage.from(fullPrompt));
        messages.add(UserMessage.from("Please synthesize these results into a coherent final response."));
        
        AiMessage response = chatModel.chat(messages).aiMessage();
        return response.text();
    }

    /**
     * Parse the orchestrator response to extract subtasks.
     * This is a simplified parser - in production, you might want to use JSON or more structured parsing.
     */
    private OrchestratorResponse parseOrchestratorResponse(String responseText) {
        List<Subtask> subtasks = new ArrayList<>();
        
        // Simple parsing logic - look for patterns like "Worker: [type] - [instructions]"
        String[] lines = responseText.split("\n");
        int taskId = 1;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Worker:") || line.contains(" worker") || line.contains(" Worker")) {
                // Try to extract worker type and instructions
                String workerType = extractWorkerType(line);
                String instructions = extractInstructions(line);
                
                if (workerType != null && instructions != null && workerConfigs.containsKey(workerType)) {
                    subtasks.add(new Subtask(String.valueOf(taskId++), workerType, instructions));
                }
            }
        }
        
        // If no subtasks were parsed, create a default subtask
        if (subtasks.isEmpty()) {
            String defaultWorker = workerConfigs.keySet().iterator().next();
            subtasks.add(new Subtask("1", defaultWorker, responseText));
        }
        
        return new OrchestratorResponse(responseText, subtasks);
    }

    private String extractWorkerType(String line) {
        // Simple extraction logic - look for known worker types
        for (String workerType : workerConfigs.keySet()) {
            if (line.toLowerCase().contains(workerType.toLowerCase())) {
                return workerType;
            }
        }
        return null;
    }

    private String extractInstructions(String line) {
        // Extract everything after a dash or colon
        int dashIndex = line.indexOf(" - ");
        int colonIndex = line.indexOf(": ");
        
        if (dashIndex != -1) {
            return line.substring(dashIndex + 3).trim();
        } else if (colonIndex != -1) {
            return line.substring(colonIndex + 2).trim();
        }
        
        return line.trim();
    }

    private String getDefaultOrchestratorPrompt() {
        return "You are an intelligent task orchestrator. Your job is to analyze complex tasks and break them down into smaller, manageable subtasks that can be handled by specialized workers. " +
            "For each subtask, you should specify which worker should handle it and provide clear, specific instructions. " +
            "Consider the capabilities of each available worker and assign tasks accordingly. " +
            "Output your analysis in a clear format specifying: Worker: [worker_type] - [specific instructions for the subtask].";
    }

    private String getDefaultSynthesizerPrompt() {
        return "You are a result synthesizer. Your job is to take the outputs from multiple specialized workers and combine them into a single, coherent, and comprehensive response. " +
            "Ensure the final response directly addresses the original task while incorporating insights from all worker results. " +
            "Maintain consistency and clarity while preserving important details from each worker's contribution.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("workflowType", "orchestrator-workers");
        config.put("workerTypes", workerConfigs.keySet());
        config.put("maxParallelWorkers", Runtime.getRuntime().availableProcessors());
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

    /**
     * Input container for OrchestratorWorkersWorkflow.
     */
    public static class OrchestratorInput {
        private final String taskDescription;
        private final Map<String, Object> context;

        public OrchestratorInput(String taskDescription) {
            this(taskDescription, new HashMap<>());
        }

        public OrchestratorInput(String taskDescription, Map<String, Object> context) {
            this.taskDescription = Objects.requireNonNull(taskDescription, "Task description cannot be null");
            this.context = new HashMap<>(Objects.requireNonNull(context, "Context cannot be null"));
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public Map<String, Object> getContext() {
            return new HashMap<>(context);
        }
    }

    /**
     * Response from the orchestrator containing task decomposition.
     */
    public static class OrchestratorResponse {
        private final String analysisText;
        private final List<Subtask> subtasks;

        public OrchestratorResponse(String analysisText, List<Subtask> subtasks) {
            this.analysisText = Objects.requireNonNull(analysisText, "Analysis text cannot be null");
            this.subtasks = new ArrayList<>(Objects.requireNonNull(subtasks, "Subtasks cannot be null"));
        }

        public String getAnalysisText() {
            return analysisText;
        }

        public List<Subtask> getSubtasks() {
            return new ArrayList<>(subtasks);
        }
    }

    /**
     * Represents a subtask to be executed by a worker.
     */
    public static class Subtask {
        private final String id;
        private final String workerType;
        private final String instructions;

        public Subtask(String id, String workerType, String instructions) {
            this.id = Objects.requireNonNull(id, "Subtask ID cannot be null");
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.instructions = Objects.requireNonNull(instructions, "Instructions cannot be null");
        }

        public String getId() {
            return id;
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getInstructions() {
            return instructions;
        }
    }

    /**
     * Result from executing a subtask.
     */
    public static class SubtaskResult {
        private final String subtaskId;
        private final String workerType;
        private final String result;
        private final boolean successful;

        public SubtaskResult(String subtaskId, String workerType, String result, boolean successful) {
            this.subtaskId = Objects.requireNonNull(subtaskId, "Subtask ID cannot be null");
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.result = Objects.requireNonNull(result, "Result cannot be null");
            this.successful = successful;
        }

        public String getSubtaskId() {
            return subtaskId;
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getResult() {
            return result;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

    /**
     * Final response from the workflow containing synthesized results.
     */
    public static class WorkerResponse {
        private final String finalResult;
        private final List<Subtask> subtasks;
        private final List<SubtaskResult> subtaskResults;
        private final boolean successful;

        public WorkerResponse(
            String finalResult,
            List<Subtask> subtasks,
            List<SubtaskResult> subtaskResults,
            boolean successful
        ) {
            this.finalResult = Objects.requireNonNull(finalResult, "Final result cannot be null");
            this.subtasks = new ArrayList<>(Objects.requireNonNull(subtasks, "Subtasks cannot be null"));
            this.subtaskResults = new ArrayList<>(Objects.requireNonNull(subtaskResults, "Subtask results cannot be null"));
            this.successful = successful;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public List<Subtask> getSubtasks() {
            return new ArrayList<>(subtasks);
        }

        public List<SubtaskResult> getSubtaskResults() {
            return new ArrayList<>(subtaskResults);
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

    /**
     * Configuration for a worker type.
     */
    public static class WorkerConfig {
        private final String workerType;
        private final String description;
        private final String systemPrompt;

        public WorkerConfig(String workerType, String description, String systemPrompt) {
            this.workerType = Objects.requireNonNull(workerType, "Worker type cannot be null");
            this.description = Objects.requireNonNull(description, "Description cannot be null");
            this.systemPrompt = Objects.requireNonNull(systemPrompt, "System prompt cannot be null");
        }

        public String getWorkerType() {
            return workerType;
        }

        public String getDescription() {
            return description;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }
    }

    /**
     * Builder for creating OrchestratorWorkersWorkflow instances.
     */
    public static class Builder {
        private String name;
        private ChatModel chatModel;
        private final Map<String, WorkerConfig> workerConfigs = new HashMap<>();
        private String orchestratorPrompt;
        private String synthesizerPrompt;

        /**
         * Sets the name of the workflow.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ChatModel to use.
         */
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Adds a worker configuration.
         */
        public Builder addWorker(String workerType, String description, String systemPrompt) {
            workerConfigs.put(workerType, new WorkerConfig(workerType, description, systemPrompt));
            return this;
        }

        /**
         * Adds a worker configuration.
         */
        public Builder addWorker(WorkerConfig workerConfig) {
            workerConfigs.put(workerConfig.getWorkerType(), workerConfig);
            return this;
        }

        /**
         * Sets a custom orchestrator prompt.
         */
        public Builder orchestratorPrompt(String orchestratorPrompt) {
            this.orchestratorPrompt = orchestratorPrompt;
            return this;
        }

        /**
         * Sets a custom synthesizer prompt.
         */
        public Builder synthesizerPrompt(String synthesizerPrompt) {
            this.synthesizerPrompt = synthesizerPrompt;
            return this;
        }

        /**
         * Builds the OrchestratorWorkersWorkflow instance.
         */
        public OrchestratorWorkersWorkflow build() {
            if (name == null) {
                name = "OrchestratorWorkersWorkflow-" + System.currentTimeMillis();
            }
            if (chatModel == null) {
                throw new IllegalStateException("ChatModel must be set");
            }
            if (workerConfigs.isEmpty()) {
                throw new IllegalStateException("At least one worker configuration must be added");
            }
            
            return new OrchestratorWorkersWorkflow(name, chatModel, workerConfigs, orchestratorPrompt, synthesizerPrompt);
        }
    }

    /**
     * Creates a new Builder for constructing OrchestratorWorkersWorkflow instances.
     */
    public static Builder builder() {
        return new Builder();
    }
}