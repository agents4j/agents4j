package dev.agents4j.workflow.routing;

import dev.agents4j.api.AgentWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.routing.RoutingDecision;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A workflow implementation that uses intelligent routing to select execution paths.
 * 
 * <p>This workflow analyzes input content using a ContentRouter and selects the most
 * appropriate route for processing. Each route contains specialized nodes and execution
 * strategies optimized for specific types of input content.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Intelligent content classification and routing</li>
 * <li>Specialized processing paths for different content types</li>
 * <li>Confidence-based route selection with fallbacks</li>
 * <li>Comprehensive routing analytics and monitoring</li>
 * <li>Integration with Strategy Pattern for route execution</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RoutingWorkflow<String, String> supportWorkflow = RoutingWorkflow.<String, String>builder()
 *     .name("CustomerSupportRouting")
 *     .router(LLMContentRouter.create(model, "Classify support tickets..."))
 *     .addRoute(technicalRoute)
 *     .addRoute(billingRoute)
 *     .addRoute(generalRoute)
 *     .fallbackRoute(escalationRoute)
 *     .confidenceThreshold(0.7)
 *     .build();
 * 
 * String response = supportWorkflow.execute("My server is down and showing error 500");
 * }</pre>
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class RoutingWorkflow<I, O> implements AgentWorkflow<I, O> {

    private final String name;
    private final ContentRouter<I> router;
    private final Map<String, Route<I, O>> routes;
    private final Route<I, O> fallbackRoute;
    private final double confidenceThreshold;
    private final Map<String, Object> defaultContext;
    private final boolean enableFallbackOnLowConfidence;
    private final boolean enableRouteAnalytics;

    /**
     * Creates a new RoutingWorkflow with the specified configuration.
     */
    private RoutingWorkflow(String name, ContentRouter<I> router, Map<String, Route<I, O>> routes,
                           Route<I, O> fallbackRoute, double confidenceThreshold, 
                           Map<String, Object> defaultContext, boolean enableFallbackOnLowConfidence,
                           boolean enableRouteAnalytics) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.router = Objects.requireNonNull(router, "Router cannot be null");
        this.routes = Collections.unmodifiableMap(new LinkedHashMap<>(routes));
        this.fallbackRoute = fallbackRoute;
        this.confidenceThreshold = validateConfidence(confidenceThreshold);
        this.defaultContext = new HashMap<>(defaultContext != null ? defaultContext : Collections.emptyMap());
        this.enableFallbackOnLowConfidence = enableFallbackOnLowConfidence;
        this.enableRouteAnalytics = enableRouteAnalytics;

        if (routes.isEmpty()) {
            throw new IllegalArgumentException("At least one route must be defined");
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
    public O execute(I input) throws WorkflowExecutionException {
        return execute(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public O execute(I input, Map<String, Object> context) throws WorkflowExecutionException {
        Objects.requireNonNull(input, "Input cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        // Merge default context with provided context
        Map<String, Object> mergedContext = new HashMap<>(defaultContext);
        mergedContext.putAll(context);

        // Add workflow metadata
        mergedContext.put("workflow_name", name);
        mergedContext.put("workflow_type", "routing");
        mergedContext.put("router_name", router.getRouterName());

        long workflowStartTime = System.currentTimeMillis();

        try {
            // Step 1: Route the input to determine the best path
            RoutingDecision decision = routeInput(input, mergedContext);
            
            // Step 2: Select the actual route to execute
            Route<I, O> selectedRoute = selectRoute(decision, mergedContext);
            
            // Step 3: Execute the selected route
            O result = executeRoute(selectedRoute, input, decision, mergedContext);
            
            // Step 4: Record analytics and metrics
            recordExecutionMetrics(decision, selectedRoute, workflowStartTime, mergedContext);
            
            return result;
            
        } catch (WorkflowExecutionException e) {
            mergedContext.put("execution_successful", false);
            mergedContext.put("error_message", e.getMessage());
            throw e;
        } catch (Exception e) {
            mergedContext.put("execution_successful", false);
            mergedContext.put("error_message", e.getMessage());
            throw new WorkflowExecutionException(
                name, 
                "Routing workflow execution failed: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(I input) {
        return executeAsync(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async routing workflow execution failed", e);
            }
        });
    }

    /**
     * Routes the input using the configured router.
     */
    private RoutingDecision routeInput(I input, Map<String, Object> context) throws WorkflowExecutionException {
        Set<String> availableRoutes = routes.keySet();
        
        long routingStartTime = System.currentTimeMillis();
        RoutingDecision decision = router.route(input, availableRoutes, context);
        long routingEndTime = System.currentTimeMillis();
        
        // Store routing information in context
        context.put("routing_decision", decision);
        context.put("routing_time_ms", routingEndTime - routingStartTime);
        context.put("selected_route_id", decision.getSelectedRoute());
        context.put("routing_confidence", decision.getConfidence());
        
        return decision;
    }

    /**
     * Selects the actual route to execute based on the routing decision.
     */
    private Route<I, O> selectRoute(RoutingDecision decision, Map<String, Object> context) throws WorkflowExecutionException {
        String selectedRouteId = decision.getSelectedRoute();
        Route<I, O> selectedRoute = routes.get(selectedRouteId);
        
        if (selectedRoute == null) {
            throw new WorkflowExecutionException(
                name, 
                "Selected route '" + selectedRouteId + "' not found in available routes: " + routes.keySet()
            );
        }
        
        // Check confidence threshold
        if (enableFallbackOnLowConfidence && 
            decision.getConfidence() < confidenceThreshold && 
            fallbackRoute != null) {
            
            context.put("low_confidence_fallback", true);
            context.put("original_route", selectedRouteId);
            context.put("fallback_reason", "Confidence " + decision.getConfidence() + " below threshold " + confidenceThreshold);
            
            return fallbackRoute;
        }
        
        // Check route-specific confidence threshold
        double routeThreshold = selectedRoute.getConfidenceThreshold();
        if (routeThreshold > 0.0 && decision.getConfidence() < routeThreshold) {
            if (fallbackRoute != null) {
                context.put("route_threshold_fallback", true);
                context.put("original_route", selectedRouteId);
                context.put("fallback_reason", "Confidence " + decision.getConfidence() + " below route threshold " + routeThreshold);
                return fallbackRoute;
            } else {
                throw new WorkflowExecutionException(
                    name, 
                    "Route '" + selectedRouteId + "' requires confidence " + routeThreshold + 
                    " but got " + decision.getConfidence() + " and no fallback route is configured"
                );
            }
        }
        
        return selectedRoute;
    }

    /**
     * Executes the selected route with the input.
     */
    private O executeRoute(Route<I, O> route, I input, RoutingDecision decision, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        // Create route-specific context
        Map<String, Object> routeContext = new HashMap<>(context);
        routeContext.put("route_id", route.getId());
        routeContext.put("route_description", route.getDescription());
        routeContext.put("route_strategy", route.getStrategy().getStrategyName());
        routeContext.put("route_priority", route.getPriority());
        routeContext.put("route_tags", route.getTags());
        
        // Add route metadata to context
        route.getMetadata().forEach(routeContext::put);
        
        long routeStartTime = System.currentTimeMillis();
        
        try {
            // Execute the route using its strategy
            O result = route.getStrategy().execute(route.getNodes(), input, routeContext);
            
            long routeEndTime = System.currentTimeMillis();
            routeContext.put("route_execution_time_ms", routeEndTime - routeStartTime);
            routeContext.put("route_execution_successful", true);
            
            return result;
            
        } catch (Exception e) {
            long routeEndTime = System.currentTimeMillis();
            routeContext.put("route_execution_time_ms", routeEndTime - routeStartTime);
            routeContext.put("route_execution_successful", false);
            routeContext.put("route_error", e.getMessage());
            
            // Try fallback route if available
            if (route.getFallbackRoute() != null) {
                routeContext.put("using_route_fallback", true);
                return executeRoute(route.getFallbackRoute(), input, decision, routeContext);
            } else if (fallbackRoute != null && !route.equals(fallbackRoute)) {
                routeContext.put("using_workflow_fallback", true);
                return executeRoute(fallbackRoute, input, decision, routeContext);
            }
            
            throw new WorkflowExecutionException(
                name, 
                "Route '" + route.getId() + "' execution failed: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Records execution metrics and analytics.
     */
    private void recordExecutionMetrics(RoutingDecision decision, Route<I, O> executedRoute, 
                                       long workflowStartTime, Map<String, Object> context) {
        if (!enableRouteAnalytics) {
            return;
        }
        
        long workflowEndTime = System.currentTimeMillis();
        long totalExecutionTime = workflowEndTime - workflowStartTime;
        
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("workflow_name", name);
        analytics.put("executed_route_id", executedRoute.getId());
        analytics.put("routing_confidence", decision.getConfidence());
        analytics.put("total_execution_time_ms", totalExecutionTime);
        analytics.put("routing_time_ms", context.get("routing_time_ms"));
        analytics.put("route_execution_time_ms", context.get("route_execution_time_ms"));
        analytics.put("execution_successful", context.getOrDefault("execution_successful", true));
        analytics.put("used_fallback", context.containsKey("low_confidence_fallback") || 
                                      context.containsKey("route_threshold_fallback") ||
                                      context.containsKey("using_route_fallback") ||
                                      context.containsKey("using_workflow_fallback"));
        
        context.put("routing_analytics", analytics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("workflowType", "routing");
        config.put("routerName", router.getRouterName());
        config.put("routeCount", routes.size());
        config.put("confidenceThreshold", confidenceThreshold);
        config.put("hasFallbackRoute", fallbackRoute != null);
        config.put("enableFallbackOnLowConfidence", enableFallbackOnLowConfidence);
        config.put("enableRouteAnalytics", enableRouteAnalytics);
        
        // Include route information
        Map<String, Object> routeInfo = routes.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Map.of(
                    "description", entry.getValue().getDescription(),
                    "strategy", entry.getValue().getStrategy().getStrategyName(),
                    "nodeCount", entry.getValue().getNodes().size(),
                    "priority", entry.getValue().getPriority(),
                    "tags", entry.getValue().getTags()
                )
            ));
        config.put("routes", routeInfo);
        
        // Include router configuration
        config.put("routerConfiguration", router.getRouterConfiguration());
        
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
     * Gets the content router used by this workflow.
     *
     * @return The content router
     */
    public ContentRouter<I> getRouter() {
        return router;
    }

    /**
     * Gets the available routes in this workflow.
     *
     * @return Unmodifiable map of routes by ID
     */
    public Map<String, Route<I, O>> getRoutes() {
        return routes;
    }

    /**
     * Gets the fallback route for this workflow.
     *
     * @return The fallback route, or null if not configured
     */
    public Route<I, O> getFallbackRoute() {
        return fallbackRoute;
    }

    /**
     * Gets the confidence threshold for this workflow.
     *
     * @return The confidence threshold
     */
    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    /**
     * Validates that a confidence value is between 0.0 and 1.0.
     */
    private static double validateConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0, got: " + confidence);
        }
        return confidence;
    }

    /**
     * Builder for creating RoutingWorkflow instances.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     */
    public static class Builder<I, O> {
        private String name;
        private ContentRouter<I> router;
        private final Map<String, Route<I, O>> routes = new LinkedHashMap<>();
        private Route<I, O> fallbackRoute;
        private double confidenceThreshold = 0.0;
        private final Map<String, Object> defaultContext = new HashMap<>();
        private boolean enableFallbackOnLowConfidence = true;
        private boolean enableRouteAnalytics = true;

        /**
         * Sets the workflow name.
         *
         * @param name The workflow name
         * @return This builder instance
         */
        public Builder<I, O> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the content router.
         *
         * @param router The content router to use
         * @return This builder instance
         */
        public Builder<I, O> router(ContentRouter<I> router) {
            this.router = router;
            return this;
        }

        /**
         * Adds a route to the workflow.
         *
         * @param route The route to add
         * @return This builder instance
         */
        public Builder<I, O> addRoute(Route<I, O> route) {
            this.routes.put(route.getId(), route);
            return this;
        }

        /**
         * Adds multiple routes to the workflow.
         *
         * @param routes The routes to add
         * @return This builder instance
         */
        @SafeVarargs
        public final Builder<I, O> addRoutes(Route<I, O>... routes) {
            for (Route<I, O> route : routes) {
                addRoute(route);
            }
            return this;
        }

        /**
         * Sets the fallback route.
         *
         * @param fallbackRoute The fallback route
         * @return This builder instance
         */
        public Builder<I, O> fallbackRoute(Route<I, O> fallbackRoute) {
            this.fallbackRoute = fallbackRoute;
            return this;
        }

        /**
         * Sets the confidence threshold.
         *
         * @param threshold The minimum confidence threshold
         * @return This builder instance
         */
        public Builder<I, O> confidenceThreshold(double threshold) {
            this.confidenceThreshold = threshold;
            return this;
        }

        /**
         * Adds a default context value.
         *
         * @param key The context key
         * @param value The context value
         * @return This builder instance
         */
        public Builder<I, O> defaultContext(String key, Object value) {
            this.defaultContext.put(key, value);
            return this;
        }

        /**
         * Adds multiple default context values.
         *
         * @param context The context map to add
         * @return This builder instance
         */
        public Builder<I, O> defaultContext(Map<String, Object> context) {
            this.defaultContext.putAll(context);
            return this;
        }

        /**
         * Sets whether to enable fallback on low confidence.
         *
         * @param enable Whether to enable fallback on low confidence
         * @return This builder instance
         */
        public Builder<I, O> enableFallbackOnLowConfidence(boolean enable) {
            this.enableFallbackOnLowConfidence = enable;
            return this;
        }

        /**
         * Sets whether to enable route analytics.
         *
         * @param enable Whether to enable route analytics
         * @return This builder instance
         */
        public Builder<I, O> enableRouteAnalytics(boolean enable) {
            this.enableRouteAnalytics = enable;
            return this;
        }

        /**
         * Builds the RoutingWorkflow instance.
         *
         * @return A new RoutingWorkflow instance
         * @throws IllegalStateException if required fields are not set
         */
        public RoutingWorkflow<I, O> build() {
            if (name == null) {
                name = "RoutingWorkflow-" + System.currentTimeMillis();
            }
            if (router == null) {
                throw new IllegalStateException("Router must be set");
            }
            if (routes.isEmpty()) {
                throw new IllegalStateException("At least one route must be added");
            }

            return new RoutingWorkflow<>(name, router, routes, fallbackRoute, confidenceThreshold,
                                       defaultContext, enableFallbackOnLowConfidence, enableRouteAnalytics);
        }
    }

    /**
     * Creates a new Builder for constructing RoutingWorkflow instances.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @return A new Builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }
}