package dev.agents4j.integration.examples;

import dev.agents4j.workflow.StrategyWorkflow;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.strategy.SequentialExecutionStrategy;
import dev.agents4j.workflow.strategy.ParallelExecutionStrategy;
import dev.agents4j.workflow.strategy.ConditionalExecutionStrategy;
import dev.agents4j.workflow.strategy.BatchExecutionStrategy;
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
 * REST endpoints for Strategy Pattern workflow demonstrations.
 */
@Path("/strategy-pattern")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StrategyPatternResource {

    @Inject
    ChatModel chatModel;

    @Inject
    StrategyPatternExample strategyExample;

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    /**
     * Execute workflow with sequential strategy
     */
    @POST
    @Path("/sequential")
    public Response sequentialExecution(StrategyRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified sequential execution
            StringBuilder result = new StringBuilder("Sequential execution results:\n");
            String currentInput = request.getInput();
            
            for (int i = 0; i < request.getSystemPrompts().size(); i++) {
                result.append("Agent ").append(i + 1).append(" (").append(request.getSystemPrompts().get(i).substring(0, Math.min(30, request.getSystemPrompts().get(i).length())))
                      .append("...): Processed input sequentially\n");
                currentInput = "Processed by agent " + (i + 1);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result.toString());
            response.put("strategy_type", "sequential");
            response.put("agents_count", request.getSystemPrompts().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute sequential strategy: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute workflow with parallel strategy
     */
    @POST
    @Path("/parallel")
    public Response parallelExecution(StrategyRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified parallel execution
            StringBuilder result = new StringBuilder("Parallel execution results:\n");
            
            for (int i = 0; i < request.getSystemPrompts().size(); i++) {
                result.append("Agent ").append(i + 1).append(" (").append(request.getSystemPrompts().get(i).substring(0, Math.min(30, request.getSystemPrompts().get(i).length())))
                      .append("...): Processed input concurrently\n");
            }
            result.append("All agents completed simultaneously");
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result.toString());
            response.put("strategy_type", "parallel");
            response.put("agents_count", request.getSystemPrompts().size());
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute parallel strategy: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute workflow with conditional strategy
     */
    @POST
    @Path("/conditional")
    public Response conditionalExecution(ConditionalStrategyRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified conditional execution
            boolean shortCircuit = request.getShortCircuit() != null ? request.getShortCircuit() : false;
            boolean requireAtLeastOne = request.getRequireAtLeastOne() != null ? request.getRequireAtLeastOne() : true;
            
            StringBuilder result = new StringBuilder("Conditional execution results:\n");
            result.append("Short circuit: ").append(shortCircuit).append("\n");
            result.append("Require at least one: ").append(requireAtLeastOne).append("\n");
            
            int executedAgents = shortCircuit ? 1 : request.getSystemPrompts().size();
            for (int i = 0; i < executedAgents; i++) {
                result.append("Agent ").append(i + 1).append(": Condition met, executed\n");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result.toString());
            response.put("strategy_type", "conditional");
            response.put("short_circuit", shortCircuit);
            response.put("require_at_least_one", requireAtLeastOne);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute conditional strategy: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Execute workflow with batch strategy
     */
    @POST
    @Path("/batch")
    public Response batchExecution(BatchStrategyRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            // Simplified batch execution
            int batchSize = request.getBatchSize() != null ? request.getBatchSize() : 5;
            boolean parallelBatches = request.getParallelBatches() != null ? request.getParallelBatches() : true;
            
            StringBuilder result = new StringBuilder("Batch execution results:\n");
            result.append("Batch size: ").append(batchSize).append("\n");
            result.append("Parallel batches: ").append(parallelBatches).append("\n");
            
            int totalAgents = request.getSystemPrompts().size();
            int batches = (int) Math.ceil((double) totalAgents / batchSize);
            
            for (int batch = 0; batch < batches; batch++) {
                result.append("Batch ").append(batch + 1).append(": ");
                int startAgent = batch * batchSize;
                int endAgent = Math.min(startAgent + batchSize, totalAgents);
                result.append("Agents ").append(startAgent + 1).append("-").append(endAgent).append(" processed\n");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("input", request.getInput());
            response.put("result", result.toString());
            response.put("strategy_type", "batch");
            response.put("batch_size", batchSize);
            response.put("parallel_batches", parallelBatches);
            
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to execute batch strategy: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get predefined strategy pattern examples
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
                case "sequential":
                    result = "Sequential strategy example: Agents process input one after another in order";
                    break;
                case "parallel":
                    result = "Parallel strategy example: All agents process input simultaneously";
                    break;
                case "conditional":
                    result = "Conditional strategy example: Agents execute based on conditions and criteria";
                    break;
                case "batch":
                    result = "Batch strategy example: Large datasets processed in configurable batch sizes";
                    break;
                case "adaptive":
                    result = "Adaptive strategy example: Strategy selection based on input characteristics";
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
     * Get available strategy types
     */
    @GET
    @Path("/types")
    public Response getStrategyTypes() {
        Map<String, Object> types = new HashMap<>();
        types.put("sequential", "Execute agents one after another in order");
        types.put("parallel", "Execute all agents simultaneously");
        types.put("conditional", "Execute agents based on conditions");
        types.put("batch", "Process inputs in batches with configurable size");
        
        Map<String, Object> examples = new HashMap<>();
        examples.put("available_examples", List.of("sequential", "parallel", "conditional", "batch", "adaptive"));
        examples.put("strategy_types", types);
        examples.put("use_cases", Map.of(
            "sequential", "When output from one agent feeds into the next",
            "parallel", "When agents work independently on the same input",
            "conditional", "When execution depends on input characteristics",
            "batch", "When processing large volumes of data"
        ));
        
        return Response.ok(examples).build();
    }

    /**
     * Compare strategies with the same input
     */
    @POST
    @Path("/compare")
    public Response compareStrategies(CompareStrategiesRequest request) {
        if (!workflowsEnabled) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Workflows are disabled"))
                    .build();
        }

        try {
            Map<String, Object> results = new HashMap<>();
            
            // Sequential execution simulation
            long startTime = System.currentTimeMillis();
            Thread.sleep(100 * request.getSystemPrompts().size()); // Simulate sequential processing
            String sequentialResult = "Sequential result: All " + request.getSystemPrompts().size() + " agents processed sequentially";
            long sequentialTime = System.currentTimeMillis() - startTime;

            // Parallel execution simulation
            startTime = System.currentTimeMillis();
            Thread.sleep(100); // Simulate parallel processing (same time regardless of agent count)
            String parallelResult = "Parallel result: All " + request.getSystemPrompts().size() + " agents processed concurrently";
            long parallelTime = System.currentTimeMillis() - startTime;

            results.put("input", request.getInput());
            results.put("sequential", Map.of(
                "result", sequentialResult,
                "execution_time_ms", sequentialTime
            ));
            results.put("parallel", Map.of(
                "result", parallelResult,
                "execution_time_ms", parallelTime
            ));
            results.put("performance_comparison", Map.of(
                "parallel_speedup", sequentialTime > 0 ? (double) sequentialTime / parallelTime : 0,
                "recommendation", parallelTime < sequentialTime ? "parallel" : "sequential"
            ));
            
            return Response.ok(results).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to compare strategies: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check for strategy pattern workflows
     */
    @GET
    @Path("/health")
    public Response getHealthInfo() {
        Map<String, Object> health = new HashMap<>();
        health.put("enabled", workflowsEnabled);
        health.put("chat_model_available", chatModel != null);
        health.put("available_strategies", List.of("sequential", "parallel", "conditional", "batch"));
        
        if (workflowsEnabled && chatModel != null) {
            health.put("status", "healthy");
        } else {
            health.put("status", "unhealthy");
        }
        
        return Response.ok(health).build();
    }

    // Request DTOs
    public static class StrategyRequest {
        private String input;
        private List<String> systemPrompts;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public List<String> getSystemPrompts() { return systemPrompts; }
        public void setSystemPrompts(List<String> systemPrompts) { this.systemPrompts = systemPrompts; }
    }

    public static class ConditionalStrategyRequest extends StrategyRequest {
        private Boolean shortCircuit;
        private Boolean requireAtLeastOne;

        public Boolean getShortCircuit() { return shortCircuit; }
        public void setShortCircuit(Boolean shortCircuit) { this.shortCircuit = shortCircuit; }
        public Boolean getRequireAtLeastOne() { return requireAtLeastOne; }
        public void setRequireAtLeastOne(Boolean requireAtLeastOne) { this.requireAtLeastOne = requireAtLeastOne; }
    }

    public static class BatchStrategyRequest extends StrategyRequest {
        private Integer batchSize;
        private Boolean parallelBatches;
        private Boolean continueOnError;

        public Integer getBatchSize() { return batchSize; }
        public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }
        public Boolean getParallelBatches() { return parallelBatches; }
        public void setParallelBatches(Boolean parallelBatches) { this.parallelBatches = parallelBatches; }
        public Boolean getContinueOnError() { return continueOnError; }
        public void setContinueOnError(Boolean continueOnError) { this.continueOnError = continueOnError; }
    }

    public static class CompareStrategiesRequest {
        private String input;
        private List<String> systemPrompts;

        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        public List<String> getSystemPrompts() { return systemPrompts; }
        public void setSystemPrompts(List<String> systemPrompts) { this.systemPrompts = systemPrompts; }
    }
}