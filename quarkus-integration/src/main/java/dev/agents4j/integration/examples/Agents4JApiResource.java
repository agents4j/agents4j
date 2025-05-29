package dev.agents4j.integration.examples;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main API resource providing overview and discovery of all Agents4J REST endpoints.
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Agents4JApiResource {

    @ConfigProperty(name = "agents4j.workflows.enabled", defaultValue = "true")
    boolean workflowsEnabled;

    @ConfigProperty(name = "agents4j.orchestrator.enabled", defaultValue = "true")
    boolean orchestratorEnabled;

    /**
     * Get API overview and available endpoints
     */
    @GET
    @Path("/overview")
    public Response getApiOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        overview.put("service_name", "Agents4J REST API");
        overview.put("version", "1.0.0");
        overview.put("description", "REST API for Agents4J workflow patterns and agent orchestration");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // Chain Workflow endpoints
        Map<String, Object> chainWorkflow = new HashMap<>();
        chainWorkflow.put("base_path", "/chain-workflow");
        chainWorkflow.put("description", "Chain workflow pattern for sequential agent processing");
        chainWorkflow.put("endpoints", List.of(
            Map.of("path", "/simple", "method", "POST", "description", "Simple single-agent query"),
            Map.of("path", "/complex", "method", "POST", "description", "Multi-agent sequential processing"),
            Map.of("path", "/three-whys", "method", "POST", "description", "Deep analysis through three why questions"),
            Map.of("path", "/conversational", "method", "POST", "description", "Chain with conversation memory"),
            Map.of("path", "/examples/{type}", "method", "GET", "description", "Predefined examples"),
            Map.of("path", "/types", "method", "GET", "description", "Available chain workflow types"),
            Map.of("path", "/health", "method", "GET", "description", "Health check")
        ));
        endpoints.put("chain_workflow", chainWorkflow);

        // Parallelization Workflow endpoints
        Map<String, Object> parallelization = new HashMap<>();
        parallelization.put("base_path", "/parallelization");
        parallelization.put("description", "Parallelization workflow for concurrent processing");
        parallelization.put("endpoints", List.of(
            Map.of("path", "/parallel", "method", "POST", "description", "Parallel processing with custom workers"),
            Map.of("path", "/parallel-simple", "method", "POST", "description", "Parallel processing with default workers"),
            Map.of("path", "/sectioning", "method", "POST", "description", "Process different content sections"),
            Map.of("path", "/voting", "method", "POST", "description", "Multiple perspectives on same input"),
            Map.of("path", "/examples/{type}", "method", "GET", "description", "Predefined examples"),
            Map.of("path", "/types", "method", "GET", "description", "Available parallelization types"),
            Map.of("path", "/performance", "method", "GET", "description", "Performance metrics and recommendations"),
            Map.of("path", "/health", "method", "GET", "description", "Health check")
        ));
        endpoints.put("parallelization", parallelization);

        // Strategy Pattern endpoints
        Map<String, Object> strategyPattern = new HashMap<>();
        strategyPattern.put("base_path", "/strategy-pattern");
        strategyPattern.put("description", "Strategy pattern for flexible execution approaches");
        strategyPattern.put("endpoints", List.of(
            Map.of("path", "/sequential", "method", "POST", "description", "Sequential execution strategy"),
            Map.of("path", "/parallel", "method", "POST", "description", "Parallel execution strategy"),
            Map.of("path", "/conditional", "method", "POST", "description", "Conditional execution strategy"),
            Map.of("path", "/batch", "method", "POST", "description", "Batch processing strategy"),
            Map.of("path", "/compare", "method", "POST", "description", "Compare different strategies"),
            Map.of("path", "/examples/{type}", "method", "GET", "description", "Predefined examples"),
            Map.of("path", "/types", "method", "GET", "description", "Available strategy types"),
            Map.of("path", "/health", "method", "GET", "description", "Health check")
        ));
        endpoints.put("strategy_pattern", strategyPattern);

        // Routing Pattern endpoints
        Map<String, Object> routingPattern = new HashMap<>();
        routingPattern.put("base_path", "/routing-pattern");
        routingPattern.put("description", "Routing pattern for intelligent content routing");
        routingPattern.put("endpoints", List.of(
            Map.of("path", "/customer-support", "method", "POST", "description", "Customer support routing"),
            Map.of("path", "/content-categorization", "method", "POST", "description", "Content categorization routing"),
            Map.of("path", "/multi-language", "method", "POST", "description", "Multi-language routing"),
            Map.of("path", "/llm-routing", "method", "POST", "description", "LLM-based intelligent routing"),
            Map.of("path", "/rule-based", "method", "POST", "description", "Rule-based pattern routing"),
            Map.of("path", "/analyze", "method", "POST", "description", "Analyze routing recommendations"),
            Map.of("path", "/examples/{type}", "method", "GET", "description", "Predefined examples"),
            Map.of("path", "/types", "method", "GET", "description", "Available routing types"),
            Map.of("path", "/health", "method", "GET", "description", "Health check")
        ));
        endpoints.put("routing_pattern", routingPattern);

        // Orchestrator-Workers endpoints
        Map<String, Object> orchestratorWorkers = new HashMap<>();
        orchestratorWorkers.put("base_path", "/orchestrator-workers");
        orchestratorWorkers.put("description", "Orchestrator-workers pattern for complex task decomposition");
        orchestratorWorkers.put("endpoints", List.of(
            Map.of("path", "/query", "method", "POST", "description", "Simple orchestrated query"),
            Map.of("path", "/custom-query", "method", "POST", "description", "Custom workers query"),
            Map.of("path", "/detailed", "method", "POST", "description", "Detailed workflow execution"),
            Map.of("path", "/examples/{type}", "method", "GET", "description", "Predefined examples"),
            Map.of("path", "/worker-types", "method", "GET", "description", "Available worker types"),
            Map.of("path", "/health", "method", "GET", "description", "Health check")
        ));
        endpoints.put("orchestrator_workers", orchestratorWorkers);

        overview.put("endpoints", endpoints);
        
        Map<String, Object> status = new HashMap<>();
        status.put("workflows_enabled", workflowsEnabled);
        status.put("orchestrator_enabled", orchestratorEnabled);
        status.put("total_patterns", endpoints.size());
        
        overview.put("status", status);
        
        return Response.ok(overview).build();
    }

    /**
     * Get quick start guide
     */
    @GET
    @Path("/quickstart")
    public Response getQuickStart() {
        Map<String, Object> quickStart = new HashMap<>();
        
        quickStart.put("title", "Agents4J REST API Quick Start");
        quickStart.put("description", "Get started with Agents4J workflow patterns");
        
        List<Map<String, Object>> steps = List.of(
            Map.of(
                "step", 1,
                "title", "Try a Simple Chain Workflow",
                "method", "POST",
                "endpoint", "/chain-workflow/simple",
                "example", Map.of(
                    "query", "What is artificial intelligence?",
                    "systemPrompt", "You are a helpful AI assistant. Provide clear and informative answers."
                )
            ),
            Map.of(
                "step", 2,
                "title", "Try Parallel Processing",
                "method", "POST",
                "endpoint", "/parallelization/parallel-simple",
                "example", Map.of(
                    "prompt", "Translate the following to French:",
                    "inputs", List.of("Hello", "Good morning", "Thank you")
                )
            ),
            Map.of(
                "step", 3,
                "title", "Try Orchestrated Task Decomposition",
                "method", "POST",
                "endpoint", "/orchestrator-workers/query",
                "example", Map.of(
                    "task", "Create a comprehensive business analysis report for a new startup"
                )
            ),
            Map.of(
                "step", 4,
                "title", "Explore Predefined Examples",
                "method", "GET",
                "endpoint", "/chain-workflow/examples/simple",
                "description", "See working examples for each pattern"
            )
        );
        
        quickStart.put("steps", steps);
        
        Map<String, Object> tips = new HashMap<>();
        tips.put("documentation", "Visit /q/swagger-ui/ for interactive API documentation");
        tips.put("health_checks", "Use /api/health for overall system status");
        tips.put("examples", "Each pattern has /examples/{type} endpoints for demos");
        tips.put("configuration", "Set agents4j.workflows.enabled=false to disable workflows");
        
        quickStart.put("tips", tips);
        
        return Response.ok(quickStart).build();
    }

    /**
     * Get system health and status
     */
    @GET
    @Path("/health")
    public Response getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("service", "Agents4J REST API");
        health.put("status", "healthy");
        health.put("workflows_enabled", workflowsEnabled);
        health.put("orchestrator_enabled", orchestratorEnabled);
        
        Map<String, String> components = new HashMap<>();
        components.put("chain_workflow", workflowsEnabled ? "enabled" : "disabled");
        components.put("parallelization", workflowsEnabled ? "enabled" : "disabled");
        components.put("strategy_pattern", workflowsEnabled ? "enabled" : "disabled");
        components.put("routing_pattern", workflowsEnabled ? "enabled" : "disabled");
        components.put("orchestrator_workers", orchestratorEnabled ? "enabled" : "disabled");
        
        health.put("components", components);
        
        Map<String, Object> system = new HashMap<>();
        system.put("max_parallel_workers", Runtime.getRuntime().availableProcessors());
        system.put("available_memory_mb", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        system.put("java_version", System.getProperty("java.version"));
        
        health.put("system_info", system);
        
        return Response.ok(health).build();
    }

    /**
     * Get API statistics and usage information
     */
    @GET
    @Path("/stats")
    public Response getApiStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_workflow_patterns", 5);
        stats.put("total_endpoints", 35);
        
        Map<String, Integer> endpointsByPattern = new HashMap<>();
        endpointsByPattern.put("chain_workflow", 7);
        endpointsByPattern.put("parallelization", 8);
        endpointsByPattern.put("strategy_pattern", 7);
        endpointsByPattern.put("routing_pattern", 9);
        endpointsByPattern.put("orchestrator_workers", 6);
        
        stats.put("endpoints_by_pattern", endpointsByPattern);
        
        Map<String, String> capabilities = new HashMap<>();
        capabilities.put("sequential_processing", "Chain multiple agents in sequence");
        capabilities.put("parallel_processing", "Execute multiple operations concurrently");
        capabilities.put("strategy_selection", "Choose execution strategy dynamically");
        capabilities.put("intelligent_routing", "Route content based on analysis");
        capabilities.put("task_orchestration", "Decompose complex tasks automatically");
        
        stats.put("capabilities", capabilities);
        
        return Response.ok(stats).build();
    }

    /**
     * Search endpoints by keyword
     */
    @GET
    @Path("/search")
    public Response searchEndpoints(@QueryParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Query parameter 'q' is required"))
                    .build();
        }
        
        Map<String, Object> results = new HashMap<>();
        results.put("query", query);
        
        List<Map<String, Object>> matches = new java.util.ArrayList<>();
        
        // Search logic (simplified)
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.contains("chain") || lowerQuery.contains("sequential")) {
            matches.add(Map.of(
                "pattern", "chain_workflow",
                "endpoint", "/chain-workflow/simple",
                "description", "Simple chain workflow for sequential processing",
                "relevance", "high"
            ));
        }
        
        if (lowerQuery.contains("parallel") || lowerQuery.contains("concurrent")) {
            matches.add(Map.of(
                "pattern", "parallelization",
                "endpoint", "/parallelization/parallel",
                "description", "Parallel processing for concurrent operations",
                "relevance", "high"
            ));
        }
        
        if (lowerQuery.contains("orchestrat") || lowerQuery.contains("decompos")) {
            matches.add(Map.of(
                "pattern", "orchestrator_workers",
                "endpoint", "/orchestrator-workers/query",
                "description", "Orchestrator-workers for task decomposition",
                "relevance", "high"
            ));
        }
        
        if (lowerQuery.contains("rout") || lowerQuery.contains("classif")) {
            matches.add(Map.of(
                "pattern", "routing_pattern",
                "endpoint", "/routing-pattern/llm-routing",
                "description", "Intelligent content routing and classification",
                "relevance", "high"
            ));
        }
        
        if (lowerQuery.contains("strateg") || lowerQuery.contains("execution")) {
            matches.add(Map.of(
                "pattern", "strategy_pattern",
                "endpoint", "/strategy-pattern/compare",
                "description", "Compare different execution strategies",
                "relevance", "medium"
            ));
        }
        
        results.put("matches", matches);
        results.put("total_matches", matches.size());
        
        return Response.ok(results).build();
    }
}