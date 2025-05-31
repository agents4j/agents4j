/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.langchain4j.facade;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.exception.AgentExecutionException;
import dev.agents4j.langchain4j.workflow.OrchestratorWorkersWorkflow;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public static OrchestratorWorkersWorkflow create(
        String name,
        ChatModel model
    ) {
        validateCreateParameters(name, model);
    
        return OrchestratorWorkersWorkflow.builder()
            .name(name)
            .chatModel(model)
            .addWorker("general", "General purpose worker", "You are a helpful assistant.")
            .addWorker("analyst", "Data analysis specialist", "You are a data analysis expert.")
            .addWorker("researcher", "Research specialist", "You are a research expert.")
            .build();
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
        WorkerDefinition... workers
    ) {
        validateCreateParameters(name, model);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException(
                "At least one worker definition is required"
            );
        }

        for (int i = 0; i < workers.length; i++) {
            if (workers[i] == null) {
                throw new IllegalArgumentException(
                    "Worker definition at index " + i + " cannot be null"
                );
            }
        }

        OrchestratorWorkersWorkflow.Builder builder = OrchestratorWorkersWorkflow.builder()
            .name(name)
            .chatModel(model);
        
        for (WorkerDefinition worker : workers) {
            builder.addWorker(worker.getType(), worker.getDescription(), worker.getSystemPrompt());
        }
        
        return builder.build();
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

        OrchestratorWorkersWorkflow workflow = create(
            "OrchestratedQuery",
            model
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input =
                new OrchestratorWorkersWorkflow.OrchestratorInput(
                    taskDescription
                );
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result =
                workflow.start(input);
            OrchestratorWorkersWorkflow.WorkerResponse response = result.getOutput()
                .orElseThrow(() -> new AgentExecutionException(
                    "OrchestratedQuery",
                    "Workflow completed but produced no output",
                    null,
                    Map.of("taskDescription", taskDescription)
                ));
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
        WorkerDefinition... workers
    ) {
        validateExecuteParameters(model, taskDescription);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException(
                "At least one worker definition is required"
            );
        }

        OrchestratorWorkersWorkflow workflow = createCustom(
            "CustomOrchestratedQuery",
            model,
            workers
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input =
                new OrchestratorWorkersWorkflow.OrchestratorInput(
                    taskDescription
                );
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result =
                workflow.start(input);
            OrchestratorWorkersWorkflow.WorkerResponse response = result.getOutput()
                .orElseThrow(() -> new AgentExecutionException(
                    "CustomOrchestratedQuery",
                    "Workflow completed but produced no output",
                    null,
                    Map.of("taskDescription", taskDescription)
                ));
            return response.getFinalResult();
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "CustomOrchestratedQuery",
                "Failed to execute custom orchestrated query",
                e,
                Map.of(
                    "taskDescription",
                    taskDescription,
                    "workerCount",
                    workers.length
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
    public static OrchestratorResult executeAdaptive(
        ChatModel model,
        String taskDescription,
        WorkerDefinition... workers
    ) {
        validateExecuteParameters(model, taskDescription);
        validateWorkers(workers);

        OrchestratorWorkersWorkflow workflow = createCustom(
            "AdaptiveOrchestratedQuery",
            model,
            workers
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input =
                new OrchestratorWorkersWorkflow.OrchestratorInput(
                    taskDescription
                );
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result =
                workflow.start(input);
            OrchestratorWorkersWorkflow.WorkerResponse response = result.getOutput()
                .orElseThrow(() -> new AgentExecutionException(
                    "AdaptiveOrchestratedQuery",
                    "Workflow completed but produced no output",
                    null,
                    Map.of("taskDescription", taskDescription)
                ));
            return new OrchestratorResult(
                taskDescription,
                response.getFinalResult(),
                response.getSubtaskResults(),
                response.isSuccessful()
            );
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "AdaptiveOrchestratedQuery", 
                "Failed to execute adaptive orchestrated query",
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
        WorkerDefinition... workers
    ) {
        validateExecuteParameters(model, taskDescription);
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException(
                "At least one worker definition is required"
            );
        }

        OrchestratorWorkersWorkflow workflow = createCustom(
            "CustomDetailedOrchestratedQuery",
            model,
            workers
        );

        try {
            OrchestratorWorkersWorkflow.OrchestratorInput input =
                new OrchestratorWorkersWorkflow.OrchestratorInput(
                    taskDescription
                );
            StatefulWorkflowResult<OrchestratorWorkersWorkflow.WorkerResponse> result =
                workflow.start(input);
            OrchestratorWorkersWorkflow.WorkerResponse response = result.getOutput()
                .orElseThrow(() -> new AgentExecutionException(
                    "AdaptiveOrchestratedQuery",
                    "Workflow completed but produced no output",
                    null,
                    Map.of("taskDescription", taskDescription)
                ));
            return new OrchestratorResult(
                taskDescription,
                response.getFinalResult(),
                response.getSubtaskResults(),
                response.isSuccessful()
            );
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "CustomDetailedOrchestratedQuery",
                "Failed to execute custom detailed orchestrated query",
                e,
                Map.of(
                    "taskDescription",
                    taskDescription,
                    "workerCount",
                    workers.length
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
    public static WorkerDefinition worker(
        String type,
        String description,
        String systemPrompt
    ) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Worker type cannot be null or empty"
            );
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Worker description cannot be null or empty"
            );
        }
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Worker system prompt cannot be null or empty"
            );
        }

        return new WorkerDefinition(type, description, systemPrompt);
    }

    private static void validateWorkers(WorkerDefinition... workers) {
        if (workers == null || workers.length == 0) {
            throw new IllegalArgumentException("At least one worker must be provided");
        }
        for (WorkerDefinition worker : workers) {
            if (worker.getType() == null || worker.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Worker type cannot be null or empty");
            }
            if (worker.getDescription() == null || worker.getDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Worker description cannot be null or empty");
            }
            if (worker.getSystemPrompt() == null || worker.getSystemPrompt().trim().isEmpty()) {
                throw new IllegalArgumentException("Worker system prompt cannot be null or empty");
            }
        }
    }

    private static void validateCreateParameters(String name, ChatModel model) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Workflow name cannot be null or empty"
            );
        }
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
    }

    private static void validateExecuteParameters(
        ChatModel model,
        String taskDescription
    ) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Task description cannot be null or empty"
            );
        }
    }

    /**
     * Detailed result from an orchestrator workflow execution.
     */
    public static class OrchestratorResult {
        private final String originalTask;
        private final String finalResult;
        private final List<OrchestratorWorkersWorkflow.SubtaskResult> subtaskResults;
        private final boolean successful;

        public OrchestratorResult(
            String originalTask,
            String finalResult,
            List<OrchestratorWorkersWorkflow.SubtaskResult> subtaskResults,
            boolean successful
        ) {
            this.originalTask = originalTask;
            this.finalResult = finalResult;
            this.subtaskResults = subtaskResults != null ? List.copyOf(subtaskResults) : List.of();
            this.successful = successful;
        }

        public String getOriginalTask() {
            return originalTask;
        }

        public String getFinalResult() {
            return finalResult;
        }

        public List<OrchestratorWorkersWorkflow.SubtaskResult> getSubtaskResults() {
            return subtaskResults;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public int getSubtaskCount() {
            return subtaskResults.size();
        }

        public boolean hasSubtaskForWorker(String workerType) {
            return subtaskResults.stream()
                .anyMatch(result -> workerType.equals(result.getWorkerType()));
        }

        public Optional<String> getResultForWorker(String workerType) {
            return subtaskResults.stream()
                .filter(result -> workerType.equals(result.getWorkerType()))
                .findFirst()
                .map(OrchestratorWorkersWorkflow.SubtaskResult::getResult);
        }
    }

    /**
     * Worker definition for creating orchestrator workflows.
     */
    public static class WorkerDefinition {
        private final String type;
        private final String description;
        private final String systemPrompt;

        public WorkerDefinition(String type, String description, String systemPrompt) {
            this.type = type;
            this.description = description;
            this.systemPrompt = systemPrompt;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }
    }
}
