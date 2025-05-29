package dev.agents4j.integration.examples;

// Removed Agents4J import as using example class directly
import dev.agents4j.workflow.OrchestratorWorkersWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoints for demonstrating the Orchestrator-Workers workflow pattern.
 * 
 * This resource provides HTTP endpoints to interact with orchestrator-workers
 * workflows, making it easy to test and integrate the pattern in web applications.
 */
@Path("/orchestrator-workers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestratorWorkersResource {

    @Inject
    ChatModel chatModel;

    @Inject
    OrchestratorWorkersExample orchestratorExample;

    @ConfigProperty(name = "agents4j.orchestrator.enabled", defaultValue = "true")
    boolean orchestratorEnabled;

    /**
     * Simple orchestrated query endpoint
     */
    @POST
    @Path("/query")
    public Response orchestratedQuery(TaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = Agents4J.orchestratedQuery(chatModel, request.getTask());
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "standard");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute orchestrated query: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Custom workers query endpoint
     */
    @POST
    @Path("/custom-query")
    public Response customOrchestratedQuery(CustomTaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = Agents4J.customOrchestratedQuery(
                chatModel,
                request.getTask(),
                request.getWorkers().stream()
                    .map(w -> Agents4J.worker(w.getType(), w.getDescription(), w.getSystemPrompt()))
                    .toArray(dev.agents4j.workflow.AgentWorkflowFactory.WorkerDefinition[]::new)
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "custom");
            response.put("workers_used", request.getWorkers().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute custom orchestrated query: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Detailed workflow execution with full response
     */
    @POST
    @Path("/detailed")
    public Response detailedWorkflowExecution(DetailedTaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            OrchestratorWorkersWorkflow.Builder builder = OrchestratorWorkersWorkflow.builder()
                .name(request.getWorkflowName() != null ? request.getWorkflowName() : "REST-Workflow")
                .chatModel(chatModel);

            // Add workers
            for (WorkerDefinition worker : request.getWorkers()) {
                builder.addWorker(worker.getType(), worker.getDescription(), worker.getSystemPrompt());
            }

            // Add custom prompts if provided
            if (request.getOrchestratorPrompt() != null) {
                builder.orchestratorPrompt(request.getOrchestratorPrompt());
            }
            if (request.getSynthesizerPrompt() != null) {
                builder.synthesizerPrompt(request.getSynthesizerPrompt());
            }

            OrchestratorWorkersWorkflow workflow = builder.build();
            
            OrchestratorWorkersWorkflow.OrchestratorInput input = 
                new OrchestratorWorkersWorkflow.OrchestratorInput(request.getTask());

            Map<String, Object> context = new HashMap<>();
            OrchestratorWorkersWorkflow.WorkerResponse workflowResponse = workflow.execute(input, context);

            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("final_result", workflowResponse.getFinalResult());
            response.put("successful", workflowResponse.isSuccessful());
            response.put("subtasks", workflowResponse.getSubtasks());
            response.put("subtask_results", workflowResponse.getSubtaskResults());
            response.put("execution_context", context);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute detailed workflow: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get predefined workflow examples
     */
    @GET
    @Path("/examples/{exampleType}")
    public Response getExample(@PathParam("exampleType") String exampleType) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result;
            switch (exampleType.toLowerCase()) {
                case "simple":
                    result = orchestratorExample.simpleOrchestratedExample();
                    break;
                case "custom":
                    result = orchestratorExample.customWorkersExample();
                    break;
                case "multi-perspective":
                    result = orchestratorExample.multiPerspectiveExample();
                    break;
                case "content-creation":
                    result = orchestratorExample.contentCreationExample();
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Unknown example type: " + exampleType))
                            .build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("example_type", exampleType);
            response.put("result", result);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute example: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get workflow health and configuration info
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", orchestratorEnabled);
        health.put("healthy", orchestratorExample.isWorkflowHealthy());
        health.put("chat_model_available", chatModel != null);
        
        if (orchestratorEnabled && orchestratorExample.isWorkflowHealthy()) {
            health.put("configuration", orchestratorExample.getWorkflowInfo());
        }
        
        return Response.ok(health).build();
    }

    /**
     * Get available worker types for standard workflow
     */
    @GET
    @Path("/worker-types")
    public Response getAvailableWorkerTypes() {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        Map<String, Object> workerTypes = new HashMap<>();
        workerTypes.put("standard_workers", Map.of(
            "analyst", "Analyzes data and provides insights",
            "writer", "Creates written content and documentation",
            "researcher", "Conducts research and fact-checking",
            "summarizer", "Summarizes and condenses information"
        ));
        
        workerTypes.put("example_custom_workers", Map.of(
            "technical_architect", "Designs technical architecture and specifications",
            "ui_designer", "Designs user interfaces and user experience",
            "marketing_strategist", "Develops marketing and business strategies",
            "economist", "Economic perspective analysis",
            "sociologist", "Social impact analysis",
            "technologist", "Technical feasibility and innovation analysis"
        ));
        
        return Response.ok(workerTypes).build();
    }

    // Request/Response DTOs

    public static class TaskRequest {
        private String task;

        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
    }

    public static class CustomTaskRequest {
        private String task;
        private java.util.List<WorkerDefinition> workers;

        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
        public java.util.List<WorkerDefinition> getWorkers() { return workers; }
        public void setWorkers(java.util.List<WorkerDefinition> workers) { this.workers = workers; }
    }

    public static class DetailedTaskRequest {
        private String task;
        private String workflowName;
        private java.util.List<WorkerDefinition> workers;
        private String orchestratorPrompt;
        private String synthesizerPrompt;

        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public java.util.List<WorkerDefinition> getWorkers() { return workers; }
        public void setWorkers(java.util.List<WorkerDefinition> workers) { this.workers = workers; }
        public String getOrchestratorPrompt() { return orchestratorPrompt; }
        public void setOrchestratorPrompt(String orchestratorPrompt) { this.orchestratorPrompt = orchestratorPrompt; }
        public String getSynthesizerPrompt() { return synthesizerPrompt; }
        public void setSynthesizerPrompt(String synthesizerPrompt) { this.synthesizerPrompt = synthesizerPrompt; }
    }

    public static class WorkerDefinition {
        private String type;
        private String description;
        private String systemPrompt;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }
}