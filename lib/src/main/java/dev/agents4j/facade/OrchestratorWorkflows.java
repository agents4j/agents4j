/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.facade;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.exception.AgentExecutionException;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.OrchestratorWorkersWorkflow;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Map;

/**
 * Focused facade for orchestrator-workers workflow operations.
 * This class follows the Single Responsibility Principle by handling only orchestrator pattern workflows.
 */
public final class OrchestratorWorkflows {

    private OrchestratorWorkflows() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates an orchestrator-workers workflow with standard worker types.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new OrchestratorWorkersWorkflow instance
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static OrchestratorWorkersWorkflow create(String name, ChatModel model) {
        validateCreateParameters(name, model);
        
        return AgentWorkflowFactory.createStandardOrchestratorWorkersWorkflow(name, model);
    }

    /**
     * Creates an orchestrator-workers workflow with custom workers.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param workers Worker definitions
     * @return A new OrchestratorWorkersWorkflow instance
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static OrchestratorWorkersWorkflow createCustom(
        String name,
        ChatModel model,
        AgentWorkflowFactory.WorkerDefinition... workers
    ) {
        validateCreateParameters(name, model);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException("At least one worker definition is required");
        }
        
        for (int i = 0; i < workers.length; i++) {
            if (workers[i] == null) {
                throw new IllegalArgumentException("Worker definition at index " + i + " cannot be null");
            }
        }
        
        return AgentWorkflowFactory.createCustomOrchestratorWorkersWorkflow(name, model, workers);
    }

    /**
     * Execute a complex task using orchestrator-workers pattern.
     * The orchestrator will analyze the task and delegate to appropriate workers.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @return The synthesized response from all workers
     * @throws AgentExecutionException if workflow execution fails
     */
    public static String execute(ChatModel model, String taskDescription) {
        validateExecuteParameters(model, taskDescription);
        
        OrchestratorWorkersWorkflow workflow = create("OrchestratedQuery", model);

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(taskDescription);
            OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);
            return response.getFinalResult();
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "OrchestratedQuery",
                "Failed to execute orchestrated query",
                e,
                Map.of("taskDescription", taskDescription)
            );
        }
    }

    /**
     * Execute a complex task using orchestrator-workers pattern with custom workers.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @param workers Custom worker definitions
     * @return The synthesized response from all workers
     * @throws AgentExecutionException if workflow execution fails
     */
    public static String executeCustom(
        ChatModel model,
        String taskDescription,
        AgentWorkflowFactory.WorkerDefinition... workers
    ) {
        validateExecuteParameters(model, taskDescription);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException("At least one worker definition is required");
        }

        OrchestratorWorkersWorkflow workflow = createCustom(
            "CustomOrchestratedQuery",
            model,
            workers
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(taskDescription);
            OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);
            return response.getFinalResult();
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "CustomOrchestratedQuery",
                "Failed to execute custom orchestrated query",
                e,
                Map.of(
                    "taskDescription", taskDescription,
                    "workerCount", workers.length
                )
            );
        }
    }

    /**
     * Execute a task with detailed worker response information.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @return Detailed response including individual worker outputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static OrchestratorResult executeDetailed(ChatModel model, String taskDescription) {
        validateExecuteParameters(model, taskDescription);
        
        OrchestratorWorkersWorkflow workflow = create("DetailedOrchestratedQuery", model);

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(taskDescription);
            OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);
            
            return new OrchestratorResult(
                taskDescription,
                response.getFinalResult(),
                response.getWorkerResults(),
                response.getExecutionMetadata()
            );
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "DetailedOrchestratedQuery",
                "Failed to execute detailed orchestrated query",
                e,
                Map.of("taskDescription", taskDescription)
            );
        }
    }

    /**
     * Execute a task with custom workers and detailed response information.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @param workers Custom worker definitions
     * @return Detailed response including individual worker outputs
     * @throws AgentExecutionException if workflow execution fails
     */
    public static OrchestratorResult executeCustomDetailed(
        ChatModel model,
        String taskDescription,
        AgentWorkflowFactory.WorkerDefinition... workers
    ) {
        validateExecuteParameters(model, taskDescription);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException("At least one worker definition is required");
        }

        OrchestratorWorkersWorkflow workflow = createCustom(
            "CustomDetailedOrchestratedQuery",
            model,
            workers
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(taskDescription);
            OrchestratorWorkersWorkflow.WorkerResponse response = workflow.execute(input);
            
            return new OrchestratorResult(
                taskDescription,
                response.getFinalResult(),
                response.getWorkerResults(),
                response.getExecutionMetadata()
            );
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "CustomDetailedOrchestratedQuery",
                "Failed to execute custom detailed orchestrated query",
                e,
                Map.of(
                    "taskDescription", taskDescription,
                    "workerCount", workers.length
                )
            );
        }
    }

    /**
     * Helper method to create a worker definition.
     *
     * @param type The worker type identifier
     * @param description A description of what this worker does
     * @param systemPrompt The system prompt for this worker
     * @return A new WorkerDefinition instance
     */
    public static AgentWorkflowFactory.WorkerDefinition worker(String type, String description, String systemPrompt) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Worker type cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Worker description cannot be null or empty");
        }
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Worker system prompt cannot be null or empty");
        }
        
        return AgentWorkflowFactory.worker(type, description, systemPrompt);
    }

    private static void validateCreateParameters(String name, ChatModel model) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name cannot be null or empty");
        }
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
    }

    private static void validateExecuteParameters(ChatModel model, String taskDescription) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be null or empty");
        }
    }

    /**
     * Detailed result from an orchestrator workflow execution.
     */
    public static class OrchestratorResult {
        private final String originalTask;
        private final String finalResult;
        private final Map<String, String> workerResults;
        private final Map<String, Object> executionMetadata;

        public OrchestratorResult(
            String originalTask,
            String finalResult,
            Map<String, String> workerResults,
            Map<String, Object> executionMetadata
        ) {
            this.originalTask = originalTask;
            this.finalResult = finalResult;
            this.workerResults = workerResults != null ? Map.copyOf(workerResults) : Map.of();
            this.executionMetadata = executionMetadata != null ? Map.copyOf(executionMetadata) : Map.of();
        }

        public String getOriginalTask() {
            return originalTask;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public Map<String, String> getWorkerResults() {
            return workerResults;
        }

        public Map<String, Object> getExecutionMetadata() {
            return executionMetadata;
        }

        public int getWorkerCount() {
            return workerResults.size();
        }

        public boolean hasWorkerResult(String workerType) {
            return workerResults.containsKey(workerType);
        }

        public String getWorkerResult(String workerType) {
            return workerResults.get(workerType);
        }
    }
}