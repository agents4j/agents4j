package dev.agents4j.integration.examples;

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
            String result = orchestratorExample.simpleOrchestrationExample();
            
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
     * Business analysis workflow endpoint
     */
    @POST
    @Path("/business-analysis")
    public Response businessAnalysis(TaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = orchestratorExample.businessAnalysisExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "business_analysis");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute business analysis: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Advanced workflow execution endpoint
     */
    @POST
    @Path("/advanced")
    public Response advancedWorkflow(TaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = orchestratorExample.advancedWorkflowExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "advanced");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute advanced workflow: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Multi-perspective analysis endpoint
     */
    @POST
    @Path("/multi-perspective")
    public Response multiPerspectiveAnalysis(TaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = orchestratorExample.multiPerspectiveExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "multi_perspective");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute multi-perspective analysis: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Content creation workflow endpoint
     */
    @POST
    @Path("/content-creation")
    public Response contentCreation(TaskRequest request) {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            String result = orchestratorExample.contentCreationExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("task", request.getTask());
            response.put("result", result);
            response.put("workflow_type", "content_creation");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute content creation: " + e.getMessage()))
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
                    result = orchestratorExample.simpleOrchestrationExample();
                    break;
                case "business":
                    result = orchestratorExample.businessAnalysisExample();
                    break;
                case "advanced":
                    result = orchestratorExample.advancedWorkflowExample();
                    break;
                case "multi-perspective":
                    result = orchestratorExample.multiPerspectiveExample();
                    break;
                case "content-creation":
                    result = orchestratorExample.contentCreationExample();
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Unknown example type: " + exampleType + 
                                         ". Available types: simple, business, advanced, multi-perspective, content-creation"))
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
     * Run all examples
     */
    @POST
    @Path("/run-all-examples")
    public Response runAllExamples() {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        try {
            orchestratorExample.runAllExamples();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "All examples executed successfully");
            response.put("examples_run", java.util.List.of(
                "simpleOrchestrationExample",
                "businessAnalysisExample", 
                "advancedWorkflowExample",
                "multiPerspectiveExample",
                "contentCreationExample"
            ));
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to run all examples: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get available workflow types
     */
    @GET
    @Path("/workflow-types")
    public Response getAvailableWorkflowTypes() {
        if (!orchestratorEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Orchestrator-Workers workflow is disabled"))
                    .build();
        }

        Map<String, Object> workflowTypes = new HashMap<>();
        workflowTypes.put("available_workflows", Map.of(
            "simple", "Basic orchestrator-workers workflow with standard configuration",
            "business", "Business analysis workflow for market and financial analysis",
            "advanced", "Advanced workflow with complex task decomposition",
            "multi-perspective", "Multi-perspective analysis from different expert viewpoints",
            "content-creation", "Content creation workflow with research, writing, editing, and SEO"
        ));
        
        workflowTypes.put("endpoints", Map.of(
            "/query", "Simple orchestrated query",
            "/business-analysis", "Business analysis workflow",
            "/advanced", "Advanced workflow execution",
            "/multi-perspective", "Multi-perspective analysis",
            "/content-creation", "Content creation workflow"
        ));
        
        return Response.ok(workflowTypes).build();
    }

    // Request DTOs

    public static class TaskRequest {
        private String task;

        public String getTask() { return task; }
        public void setTask(String task) { this.task = task; }
    }
}