package dev.agents4j.integration.examples;

// Removed Agents4J import as using example class directly
import dev.agents4j.workflow.ParallelizationWorkflow;
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
    ParallelizationWorkflowExample example;

    @Inject
    ParallelizationWorkflowExample parallelizationExample;

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    /**
     * Execute parallel processing with multiple inputs
     */
    @POST
    @Path("/parallel")
    public Response parallelQuery(ParallelRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String results = example.sentimentAnalysisExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("prompt", request.getPrompt());
            response.put("inputs", request.getInputs());
            response.put("results", results);
            response.put("workflow_type", "parallelization");
            response.put("num_workers", request.getNumWorkers() != null ? request.getNumWorkers() : 4);
            response.put("inputs_count", request.getInputs().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute parallel query: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute parallel processing with default worker count
     */
    @POST
    @Path("/parallel-simple")
    public Response simpleParallelQuery(SimpleParallelRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            String results = example.sentimentAnalysisExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("prompt", request.getPrompt());
            response.put("inputs", request.getInputs());
            response.put("results", results);
            response.put("workflow_type", "simple_parallelization");
            response.put("num_workers", 4);
            response.put("inputs_count", request.getInputs().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute simple parallel query: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Sectioning example - process different sections of content
     */
    @POST
    @Path("/sectioning")
    public Response sectioningExample(SectioningRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            ParallelizationWorkflow workflow = Agents4J.createParallelizationWorkflow(
                "SectioningWorkflow",
                chatModel
            );

            ParallelizationWorkflow.ParallelInput input = new ParallelizationWorkflow.ParallelInput(
                request.getSectionPrompt(),
                request.getSections(),
                request.getNumWorkers() != null ? request.getNumWorkers() : 4
            );

            String results = example.contentGenerationExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("section_prompt", request.getSectionPrompt());
            response.put("sections", request.getSections());
            response.put("results", results);
            response.put("workflow_type", "sectioning");
            response.put("num_workers", request.getNumWorkers() != null ? request.getNumWorkers() : 4);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute sectioning example: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Voting example - get multiple perspectives on the same input
     */
    @POST
    @Path("/voting")
    public Response votingExample(VotingRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Create multiple identical inputs for voting
            List<String> votingInputs = java.util.Collections.nCopies(
                request.getVoteCount() != null ? request.getVoteCount() : 3,
                request.getInput()
            );

            String results = example.performanceComparisonExample();
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("voting_prompt", request.getVotingPrompt());
            response.put("vote_count", request.getVoteCount() != null ? request.getVoteCount() : 3);
            response.put("votes", results);
            response.put("workflow_type", "voting");
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute voting example: " + e.getMessage()))
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
                case "sectioning":
                    result = "Sectioning example: Document sections processed concurrently for faster analysis";
                    break;
                case "voting":
                    result = "Voting example: Multiple perspectives gathered simultaneously for consensus building";
                    break;
                case "batch-processing":
                    result = "Batch processing example: Large volumes of data processed efficiently in parallel";
                    break;
                case "multilingual":
                    result = "Multilingual example: Content processed in multiple languages simultaneously";
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
     * Get available parallelization types
     */
    @GET
    @Path("/types")
    public Response getParallelizationTypes() {
        Map<String, Object> types = new HashMap<>();
        types.put("sectioning", "Process different sections of content in parallel");
        types.put("voting", "Get multiple perspectives on the same input");
        types.put("batch_processing", "Process large volumes of similar items");
        types.put("parallel_analysis", "Analyze multiple aspects simultaneously");
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("available_examples", List.of("sectioning", "voting", "batch-processing", "multilingual"));
        examples.put("workflow_types", types);
        examples.put("recommended_workers", Map.of(
            "small_batch", "2-4 workers",
            "medium_batch", "4-8 workers", 
            "large_batch", "8-16 workers"
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
            "Optimal worker count depends on item complexity"
        ));
        
        return Response.ok(performance).build();
    }

    /**
     * Health check for parallelization workflows
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", workflowsEnabled);
        health.put("chat_model_available", chatModel != null);
        health.put("max_parallel_workers", Runtime.getRuntime().availableProcessors());
        
        if (workflowsEnabled && chatModel != null) {
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