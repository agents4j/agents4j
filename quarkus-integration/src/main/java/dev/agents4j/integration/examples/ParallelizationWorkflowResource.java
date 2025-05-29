package dev.agents4j.integration.examples;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for Parallelization Workflow pattern demonstrations.
 */
@Path("/parallelization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParallelizationWorkflowResource {

    @Inject
    ChatModel chatModel;

    @Inject
    ParallelizationWorkflowExample parallelizationExample;

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    /**
     * Execute sentiment analysis example
     */
    @POST
    @Path("/sentiment-analysis")
    public Response sentimentAnalysis(ParallelRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result = parallelizationExample.sentimentAnalysisExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("prompt", request.getPrompt());
            response.put("inputs", request.getInputs());
            response.put("result", result);
            response.put("workflow_type", "sentiment_analysis");
            response.put("num_workers", request.getNumWorkers() != null ? request.getNumWorkers() : 4);
            response.put("inputs_count", request.getInputs() != null ? request.getInputs().size() : 0);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute sentiment analysis: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute document translation example
     */
    @POST
    @Path("/translation")
    public Response documentTranslation(SimpleParallelRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result = parallelizationExample.documentTranslationExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("prompt", request.getPrompt());
            response.put("inputs", request.getInputs());
            response.put("result", result);
            response.put("workflow_type", "document_translation");
            response.put("num_workers", 4);
            response.put("inputs_count", request.getInputs() != null ? request.getInputs().size() : 0);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute document translation: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute content generation example
     */
    @POST
    @Path("/content-generation")
    public Response contentGeneration(SectioningRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result = parallelizationExample.contentGenerationExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("section_prompt", request.getSectionPrompt());
            response.put("sections", request.getSections());
            response.put("result", result);
            response.put("workflow_type", "content_generation");
            response.put("num_workers", request.getNumWorkers() != null ? request.getNumWorkers() : 4);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute content generation: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute performance comparison example
     */
    @POST
    @Path("/performance-comparison")
    public Response performanceComparison(VotingRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result = parallelizationExample.performanceComparisonExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("voting_prompt", request.getVotingPrompt());
            response.put("vote_count", request.getVoteCount() != null ? request.getVoteCount() : 3);
            response.put("result", result);
            response.put("workflow_type", "performance_comparison");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute performance comparison: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute async processing example
     */
    @POST
    @Path("/async")
    public Response asyncProcessing(ParallelRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            parallelizationExample.asyncProcessingExample().thenAccept(result -> {
                // Async processing completed
                System.out.println("Async processing completed: " + result);
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("prompt", request.getPrompt());
            response.put("inputs", request.getInputs());
            response.put("message", "Async processing started");
            response.put("workflow_type", "async_processing");
            response.put("status", "started");
            
            return Response.accepted(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to start async processing: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get predefined parallelization examples
     */
    @GET
    @Path("/examples/{exampleType}")
    public Response getExample(@PathParam("exampleType") String exampleType) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String result;
            switch (exampleType.toLowerCase()) {
                case "sentiment-analysis":
                    result = parallelizationExample.sentimentAnalysisExample();
                    break;
                case "translation":
                    result = parallelizationExample.documentTranslationExample();
                    break;
                case "content-generation":
                    result = parallelizationExample.contentGenerationExample();
                    break;
                case "performance-comparison":
                    result = parallelizationExample.performanceComparisonExample();
                    break;
                default:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Unknown example type: " + exampleType + 
                                         ". Available types: sentiment-analysis, translation, content-generation, performance-comparison"))
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
     * Get available parallelization types
     */
    @GET
    @Path("/types")
    public Response getParallelizationTypes() {
        Map<String, Object> types = new HashMap<>();
        types.put("sentiment_analysis", "Analyze sentiment of multiple texts in parallel");
        types.put("translation", "Translate documents to multiple languages simultaneously");
        types.put("content_generation", "Generate content for multiple topics in parallel");
        types.put("performance_comparison", "Compare sequential vs parallel processing performance");
        types.put("async_processing", "Demonstrate asynchronous parallel processing");
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("available_examples", List.of("sentiment-analysis", "translation", "content-generation", "performance-comparison"));
        examples.put("workflow_types", types);
        examples.put("endpoints", Map.of(
            "/sentiment-analysis", "Execute sentiment analysis in parallel",
            "/translation", "Execute document translation in parallel",
            "/content-generation", "Execute content generation in parallel",
            "/performance-comparison", "Compare performance metrics",
            "/async", "Execute async parallel processing"
        ));
        
        return Response.ok(examples).build();
    }

    /**
     * Get performance metrics and recommendations
     */
    @GET
    @Path("/performance")
    public Response getPerformanceInfo() {
        Map<String, Object> performance = new HashMap<>();
        performance.put("max_workers", Runtime.getRuntime().availableProcessors());
        performance.put("recommended_batch_sizes", Map.of(
            "small_items", "10-50 items",
            "medium_items", "50-200 items",
            "large_items", "200+ items"
        ));
        performance.put("considerations", List.of(
            "Consider API rate limits when setting worker count",
            "Monitor memory usage with large batches",
            "Optimal worker count depends on item complexity",
            "Use async processing for long-running tasks"
        ));
        
        return Response.ok(performance).build();
    }

    /**
     * Run all parallelization examples
     */
    @POST
    @Path("/run-all-examples")
    public Response runAllExamples() {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            parallelizationExample.runAllExamples();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "All examples executed successfully");
            response.put("examples_run", List.of(
                "sentimentAnalysisExample",
                "documentTranslationExample",
                "contentGenerationExample",
                "performanceComparisonExample"
            ));
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to run all examples: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check for parallelization workflows
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", workflowsEnabled);
        health.put("healthy", parallelizationExample.isWorkflowHealthy());
        health.put("chat_model_available", chatModel != null);
        health.put("max_parallel_workers", Runtime.getRuntime().availableProcessors());
        
        if (workflowsEnabled && parallelizationExample.isWorkflowHealthy()) {
            health.put("status", "healthy");
        } else {
            health.put("status", "unhealthy");
        }
        
        return Response.ok(health).build();
    }

    // Request DTOs
    public static class ParallelRequest {
        private String prompt;
        private List<String> inputs;
        private Integer numWorkers;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public List<String> getInputs() { return inputs; }
        public void setInputs(List<String> inputs) { this.inputs = inputs; }
        public Integer getNumWorkers() { return numWorkers; }
        public void setNumWorkers(Integer numWorkers) { this.numWorkers = numWorkers; }
    }

    public static class SimpleParallelRequest {
        private String prompt;
        private List<String> inputs;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public List<String> getInputs() { return inputs; }
        public void setInputs(List<String> inputs) { this.inputs = inputs; }
    }

    public static class SectioningRequest {
        private String sectionPrompt;
        private List<String> sections;
        private Integer numWorkers;

        public String getSectionPrompt() { return sectionPrompt; }
        public void setSectionPrompt(String sectionPrompt) { this.sectionPrompt = sectionPrompt; }
        public List<String> getSections() { return sections; }
        public void setSections(List<String> sections) { this.sections = sections; }
        public Integer getNumWorkers() { return numWorkers; }
        public void setNumWorkers(Integer numWorkers) { this.numWorkers = numWorkers; }
    }

    public static class VotingRequest {
        private String input;
        private String votingPrompt;
        private Integer voteCount;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public String getVotingPrompt() { return votingPrompt; }
        public void setVotingPrompt(String votingPrompt) { this.votingPrompt = votingPrompt; }
        public Integer getVoteCount() { return voteCount; }
        public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    }
}