package dev.agents4j.workflow.routing;

import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.routing.RoutingDecision;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A stateful workflow implementation that uses intelligent routing to select execution paths.
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
 * <li>Stateful execution with suspend/resume capabilities</li>
 * <li>Comprehensive routing analytics and monitoring</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
 *     .name("ContentRouting")
 *     .router(contentRouter)
 *     .addRoute(technicalRoute)
 *     .addRoute(generalRoute)
 *     .fallbackRoute(escalationRoute)
 *     .confidenceThreshold(0.7)
 *     .build();
 * 
 * StatefulWorkflowResult<String> result = workflow.start("Process this content");
 * }</pre>
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class RoutingWorkflow<I, O> implements StatefulWorkflow<I, O> {

    private static final String ROUTER_NODE = "content-router";
    private static final String ROUTE_EXECUTOR_NODE = "route-executor";
    
    private final String name;
    private final ContentRouter<I> router;
    private final Map<String, Route<I, O>> routes;
    private final Route<I, O> fallbackRoute;
    private final double confidenceThreshold;
    private final Map<String, Object> defaultContext;
    private final boolean enableFallbackOnLowConfidence;
    private final boolean enableRouteAnalytics;
    private final List<StatefulAgentNode<I>> nodes;
    private final List<WorkflowRoute<I>> workflowRoutes;

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

        this.nodes = createNodes();
        this.workflowRoutes = createWorkflowRoutes();
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
    public StatefulWorkflowResult<O> start(I input) throws WorkflowExecutionException {
        return start(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<O> start(I input, Map<String, Object> context) throws WorkflowExecutionException {
        WorkflowState initialState = WorkflowState.create(name + "-" + System.currentTimeMillis());
        return start(input, initialState, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<O> start(I input, WorkflowState initialState, Map<String, Object> context) 
            throws WorkflowExecutionException {
        try {
            // Merge default context with provided context
            Map<String, Object> mergedContext = new HashMap<>(defaultContext);
            mergedContext.putAll(context);

            // Add workflow metadata
            mergedContext.put("workflow_name", name);
            mergedContext.put("workflow_type", "routing");
            mergedContext.put("router_name", router.getRouterName());
            mergedContext.put("routes", routes);

            long workflowStartTime = System.currentTimeMillis();
            
            // Initialize state with input data
            Map<String, Object> stateData = new HashMap<>();
            stateData.put("input", input);
            stateData.put("startTime", workflowStartTime);
            stateData.put("routerName", router.getRouterName());
            stateData.put("confidenceThreshold", confidenceThreshold);
            
            WorkflowState state = initialState.withUpdatesAndCurrentNode(stateData, ROUTER_NODE);
            
            // Execute routing logic
            StatefulAgentNode<I> routerNode = getNode(ROUTER_NODE)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Router node not found"));
            
            WorkflowCommand<I> command = routerNode.process(input, state, mergedContext);
            
            // Handle the command
            StatefulWorkflowResult<O> result = handleCommand(command, input, state, mergedContext);
            
            // Copy routing results back to original context
            if (mergedContext.containsKey("routing_decision")) {
                context.put("routing_decision", mergedContext.get("routing_decision"));
            }
            if (mergedContext.containsKey("selected_route_id")) {
                context.put("selected_route_id", mergedContext.get("selected_route_id"));
            }
            if (mergedContext.containsKey("routing_confidence")) {
                context.put("routing_confidence", mergedContext.get("routing_confidence"));
            }
            // Copy workflow metadata back to original context
            if (mergedContext.containsKey("workflow_name")) {
                context.put("workflow_name", mergedContext.get("workflow_name"));
            }
            if (mergedContext.containsKey("workflow_type")) {
                context.put("workflow_type", mergedContext.get("workflow_type"));
            }
            if (mergedContext.containsKey("router_name")) {
                context.put("router_name", mergedContext.get("router_name"));
            }
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("error_message", e.getMessage());
            throw new WorkflowExecutionException(name, "Routing workflow execution failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<O> resume(I input, WorkflowState state) throws WorkflowExecutionException {
        return resume(input, state, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatefulWorkflowResult<O> resume(I input, WorkflowState state, Map<String, Object> context) 
            throws WorkflowExecutionException {
        try {
            String currentNodeId = state.getCurrentNodeId()
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Cannot resume: no current node in state"));
            
            StatefulAgentNode<I> currentNode = getNode(currentNodeId)
                    .orElseThrow(() -> new WorkflowExecutionException(name, "Current node not found: " + currentNodeId));
            
            WorkflowCommand<I> command = currentNode.process(input, state, context);
            
            return handleCommand(command, input, state, context);
            
        } catch (Exception e) {
            throw new WorkflowExecutionException(name, "Failed to resume workflow", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state, 
            Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<I>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<I>> getRoutes() {
        return Collections.unmodifiableList(workflowRoutes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<StatefulAgentNode<I>> getNode(String nodeId) {
        return nodes.stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowRoute<I>> getRoutesFrom(String fromNodeId) {
        return workflowRoutes.stream()
                .filter(route -> route.getFromNodeId().equals(fromNodeId))
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StatefulAgentNode<I>> getEntryPoints() {
        return nodes.stream()
                .filter(StatefulAgentNode::canBeEntryPoint)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws IllegalStateException {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one node");
        }
        
        if (getEntryPoints().isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one entry point");
        }
        
        // Validate all routes reference existing nodes
        for (WorkflowRoute<I> route : workflowRoutes) {
            if (getNode(route.getFromNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent from node: " + route.getFromNodeId());
            }
            if (getNode(route.getToNodeId()).isEmpty()) {
                throw new IllegalStateException("Route references non-existent to node: " + route.getToNodeId());
            }
        }
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
    public Map<String, Route<I, O>> getAvailableRoutes() {
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

    private List<StatefulAgentNode<I>> createNodes() {
        List<StatefulAgentNode<I>> nodeList = new ArrayList<>();
        
        // Content router node
        nodeList.add(new ContentRouterNode<>(ROUTER_NODE, router));
        
        // Route executor node
        nodeList.add(new RouteExecutorNode<>(ROUTE_EXECUTOR_NODE, routes, fallbackRoute, 
                confidenceThreshold, enableFallbackOnLowConfidence, enableRouteAnalytics));
        
        return nodeList;
    }

    private List<WorkflowRoute<I>> createWorkflowRoutes() {
        List<WorkflowRoute<I>> routeList = new ArrayList<>();
        
        // Route from router to executor
        routeList.add(WorkflowRoute.<I>builder()
                .id("router-to-executor")
                .from(ROUTER_NODE)
                .to(ROUTE_EXECUTOR_NODE)
                .description("Route from content router to route executor")
                .build());
        
        return routeList;
    }

    private StatefulWorkflowResult<O> handleCommand(WorkflowCommand<I> command, I input, 
            WorkflowState state, Map<String, Object> context) {
        
        // Apply state updates
        WorkflowState newState = state.withUpdates(command.getStateUpdates());
        
        switch (command.getType()) {
            case COMPLETE:
                @SuppressWarnings("unchecked")
                O result = (O) newState.get("result").orElse(null);
                
                // Add execution metadata
                long endTime = System.currentTimeMillis();
                long startTime = newState.get("startTime", 0L);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("execution_time", endTime - startTime);
                metadata.put("routing_analytics", newState.get("routing_analytics").orElse(null));
                
                return StatefulWorkflowResult.withMetadata(
                        StatefulWorkflowResult.Status.COMPLETED, 
                        result, 
                        newState, 
                        null, 
                        metadata);
                
            case CONTINUE:
                // Find next node via routes
                String currentNodeId = newState.getCurrentNodeId().orElse("");
                List<WorkflowRoute<I>> availableRoutes = getRoutesFrom(currentNodeId);
                
                if (availableRoutes.isEmpty()) {
                    return StatefulWorkflowResult.error("No routes available from node: " + currentNodeId, newState);
                }
                
                // Take the first matching route
                WorkflowRoute<I> route = availableRoutes.get(0);
                String nextNodeId = route.getToNodeId();
                
                StatefulAgentNode<I> nextNode = getNode(nextNodeId)
                        .orElseThrow(() -> new RuntimeException("Next node not found: " + nextNodeId));
                
                WorkflowState nextState = newState.withCurrentNode(nextNodeId);
                I nextInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<I> nextCommand = nextNode.process(nextInput, nextState, context);
                return handleCommand(nextCommand, nextInput, nextState, context);
                
            case GOTO:
                String targetNodeId = command.getTargetNodeId()
                        .orElseThrow(() -> new RuntimeException("GOTO command missing target node"));
                
                StatefulAgentNode<I> targetNode = getNode(targetNodeId)
                        .orElseThrow(() -> new RuntimeException("Target node not found: " + targetNodeId));
                
                WorkflowState gotoState = newState.withCurrentNode(targetNodeId);
                I gotoInput = command.getNextInput().orElse(input);
                
                WorkflowCommand<I> gotoCommand = targetNode.process(gotoInput, gotoState, context);
                return handleCommand(gotoCommand, gotoInput, gotoState, context);
                
            case SUSPEND:
                return StatefulWorkflowResult.suspended(newState);
                
            case ERROR:
                String errorMessage = command.getErrorMessage().orElse("Unknown error occurred");
                return StatefulWorkflowResult.error(errorMessage, newState);
                
            default:
                return StatefulWorkflowResult.error("Unknown command type: " + command.getType(), newState);
        }
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
     * Node that handles content routing decisions.
     */
    private static class ContentRouterNode<I> implements StatefulAgentNode<I> {
        private final String nodeId;
        private final ContentRouter<I> router;

        public ContentRouterNode(String nodeId, ContentRouter<I> router) {
            this.nodeId = nodeId;
            this.router = router;
        }

        @Override
        public WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Route<I, ?>> routes = (Map<String, Route<I, ?>>) context.get("routes");
                Set<String> availableRoutes = routes != null ? routes.keySet() : Set.of();
                
                long routingStartTime = System.currentTimeMillis();
                RoutingDecision decision = router.route(input, availableRoutes, context);
                long routingEndTime = System.currentTimeMillis();
                
                // Store routing decision in context for external access
                context.put("routing_decision", decision);
                context.put("selected_route_id", decision.getSelectedRoute());
                context.put("routing_confidence", decision.getConfidence());
                
                return WorkflowCommand.<I>continueWith()
                        .updateState("routing_decision", decision)
                        .updateState("routing_time_ms", routingEndTime - routingStartTime)
                        .updateState("selected_route_id", decision.getSelectedRoute())
                        .updateState("routing_confidence", decision.getConfidence())
                        .build();
                        
            } catch (Exception e) {
                return WorkflowCommand.<I>error("Failed to route input: " + e.getMessage())
                        .updateState("execution_successful", false)
                        .build();
            }
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Content Router";
        }

        @Override
        public boolean canBeEntryPoint() {
            return true;
        }
    }

    /**
     * Node that executes the selected route.
     */
    private static class RouteExecutorNode<I, O> implements StatefulAgentNode<I> {
        private final String nodeId;
        private final Map<String, Route<I, O>> routes;
        private final Route<I, O> fallbackRoute;
        private final double confidenceThreshold;
        private final boolean enableFallbackOnLowConfidence;
        private final boolean enableRouteAnalytics;

        public RouteExecutorNode(String nodeId, Map<String, Route<I, O>> routes, Route<I, O> fallbackRoute,
                               double confidenceThreshold, boolean enableFallbackOnLowConfidence, 
                               boolean enableRouteAnalytics) {
            this.nodeId = nodeId;
            this.routes = routes;
            this.fallbackRoute = fallbackRoute;
            this.confidenceThreshold = confidenceThreshold;
            this.enableFallbackOnLowConfidence = enableFallbackOnLowConfidence;
            this.enableRouteAnalytics = enableRouteAnalytics;
        }

        @Override
        public WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context) {
            try {
                RoutingDecision decision = state.get("routing_decision")
                        .map(obj -> (RoutingDecision) obj)
                        .orElseThrow(() -> new RuntimeException("No routing decision found in state"));
                
                // Select route to execute
                Route<I, O> selectedRoute = selectRoute(decision, context);
                
                // Execute the route
                long routeStartTime = System.currentTimeMillis();
                O result = executeRoute(selectedRoute, input, context);
                long routeEndTime = System.currentTimeMillis();
                
                // Record analytics if enabled
                Map<String, Object> analytics = null;
                if (enableRouteAnalytics) {
                    analytics = recordExecutionMetrics(decision, selectedRoute, 
                            state.get("startTime", 0L), routeStartTime, routeEndTime);
                }
                
                return WorkflowCommand.<I>complete()
                        .updateState("result", result)
                        .updateState("executed_route_id", selectedRoute.getId())
                        .updateState("route_execution_time_ms", routeEndTime - routeStartTime)
                        .updateState("routing_analytics", analytics)
                        .updateState("execution_successful", true)
                        .build();
                        
            } catch (Exception e) {
                return WorkflowCommand.<I>error("Failed to execute route: " + e.getMessage())
                        .updateState("execution_successful", false)
                        .build();
            }
        }

        private Route<I, O> selectRoute(RoutingDecision decision, Map<String, Object> context) {
            String selectedRouteId = decision.getSelectedRoute();
            Route<I, O> selectedRoute = routes.get(selectedRouteId);
            
            if (selectedRoute == null) {
                throw new RuntimeException("Selected route '" + selectedRouteId + "' not found in available routes: " + routes.keySet());
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
                    throw new RuntimeException("Route '" + selectedRouteId + "' requires confidence " + routeThreshold + 
                            " but got " + decision.getConfidence() + " and no fallback route is configured");
                }
            }
            
            return selectedRoute;
        }

        private O executeRoute(Route<I, O> route, I input, Map<String, Object> context) {
            // Create route-specific context
            Map<String, Object> routeContext = new HashMap<>(context);
            routeContext.put("route_id", route.getId());
            routeContext.put("route_description", route.getDescription());
            routeContext.put("route_strategy", route.getStrategy().getStrategyName());
            routeContext.put("route_priority", route.getPriority());
            routeContext.put("route_tags", route.getTags());
            
            // Add route metadata to context
            route.getMetadata().forEach(routeContext::put);
            
            try {
                // Execute the route using its strategy
                return route.getStrategy().execute(route.getNodes(), input, routeContext);
                
            } catch (Exception e) {
                // Try fallback route if available
                if (route.getFallbackRoute() != null) {
                    routeContext.put("using_route_fallback", true);
                    return executeRoute(route.getFallbackRoute(), input, routeContext);
                } else if (fallbackRoute != null && !route.equals(fallbackRoute)) {
                    routeContext.put("using_workflow_fallback", true);
                    return executeRoute(fallbackRoute, input, routeContext);
                }
                
                throw new RuntimeException("Route '" + route.getId() + "' execution failed: " + e.getMessage(), e);
            }
        }

        private Map<String, Object> recordExecutionMetrics(RoutingDecision decision, Route<I, O> executedRoute,
                                                           long workflowStartTime, long routeStartTime, long routeEndTime) {
            long totalExecutionTime = routeEndTime - workflowStartTime;
            
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("executed_route_id", executedRoute.getId());
            analytics.put("routing_confidence", decision.getConfidence());
            analytics.put("total_execution_time_ms", totalExecutionTime);
            analytics.put("route_execution_time_ms", routeEndTime - routeStartTime);
            
            return analytics;
        }

        @Override
        public String getNodeId() {
            return nodeId;
        }

        @Override
        public String getName() {
            return "Route Executor";
        }
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