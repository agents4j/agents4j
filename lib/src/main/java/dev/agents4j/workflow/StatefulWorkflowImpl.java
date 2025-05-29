package dev.agents4j.workflow;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of StatefulWorkflow that supports graph-based routing,
 * state management, and workflow suspension/resumption.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class StatefulWorkflowImpl<I, O> implements StatefulWorkflow<I, O> {
    
    private final String name;
    private final Map<String, StatefulAgentNode<I>> nodes;
    private final List<WorkflowRoute<I>> routes;
    private final Map<String, List<WorkflowRoute<I>>> routesByFromNode;
    private final List<StatefulAgentNode<I>> entryPoints;
    private final StatefulAgentNode<I> defaultEntryPoint;
    private final OutputExtractor<I, O> outputExtractor;
    private final int maxExecutionSteps;

    private StatefulWorkflowImpl(String name, 
                                Map<String, StatefulAgentNode<I>> nodes,
                                List<WorkflowRoute<I>> routes,
                                StatefulAgentNode<I> defaultEntryPoint,
                                OutputExtractor<I, O> outputExtractor,
                                int maxExecutionSteps) {
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
        this.routes = Collections.unmodifiableList(new ArrayList<>(routes));
        this.defaultEntryPoint = defaultEntryPoint;
        this.outputExtractor = Objects.requireNonNull(outputExtractor, "Output extractor cannot be null");
        this.maxExecutionSteps = maxExecutionSteps;
        
        // Build route index for fast lookup
        this.routesByFromNode = routes.stream()
                .collect(Collectors.groupingBy(WorkflowRoute::getFromNodeId));
        
        // Find entry points
        this.entryPoints = nodes.values().stream()
                .filter(StatefulAgentNode::canBeEntryPoint)
                .collect(Collectors.toList());
        
        validate();
    }

    @Override
    public StatefulWorkflowResult<O> start(I input) throws WorkflowExecutionException {
        return start(input, new HashMap<>());
    }

    @Override
    public StatefulWorkflowResult<O> start(I input, Map<String, Object> context) throws WorkflowExecutionException {
        WorkflowState initialState = WorkflowState.create(generateWorkflowId());
        return start(input, initialState, context);
    }

    @Override
    public StatefulWorkflowResult<O> start(I input, WorkflowState initialState, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        // Determine entry point
        StatefulAgentNode<I> entryNode = determineEntryPoint(input, initialState, context);
        
        // Update state with entry point
        WorkflowState stateWithEntryPoint = initialState.withCurrentNode(entryNode.getNodeId());
        
        return executeWorkflow(input, stateWithEntryPoint, context);
    }

    @Override
    public StatefulWorkflowResult<O> resume(I input, WorkflowState state) throws WorkflowExecutionException {
        return resume(input, state, new HashMap<>());
    }

    @Override
    public StatefulWorkflowResult<O> resume(I input, WorkflowState state, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        if (!state.getCurrentNodeId().isPresent()) {
            throw new WorkflowExecutionException(name, "Cannot resume workflow: no current node in state");
        }
        
        return executeWorkflow(input, state, context);
    }

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

    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<StatefulAgentNode<I>> getNodes() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public List<WorkflowRoute<I>> getRoutes() {
        return routes;
    }

    @Override
    public Optional<StatefulAgentNode<I>> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public List<WorkflowRoute<I>> getRoutesFrom(String fromNodeId) {
        return routesByFromNode.getOrDefault(fromNodeId, Collections.emptyList());
    }

    @Override
    public List<StatefulAgentNode<I>> getEntryPoints() {
        return entryPoints;
    }

    @Override
    public void validate() throws IllegalStateException {
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Workflow must contain at least one node");
        }
        
        if (defaultEntryPoint == null && entryPoints.isEmpty()) {
            throw new IllegalStateException("Workflow must have at least one entry point");
        }
        
        // Validate all routes reference valid nodes
        for (WorkflowRoute<I> route : routes) {
            if (!nodes.containsKey(route.getFromNodeId())) {
                throw new IllegalStateException("Route references non-existent from node: " + route.getFromNodeId());
            }
            if (!nodes.containsKey(route.getToNodeId())) {
                throw new IllegalStateException("Route references non-existent to node: " + route.getToNodeId());
            }
        }
        
        // Check for unreachable nodes (optional warning)
        Set<String> reachableNodes = findReachableNodes();
        Set<String> allNodes = nodes.keySet();
        Set<String> unreachableNodes = new HashSet<>(allNodes);
        unreachableNodes.removeAll(reachableNodes);
        
        if (!unreachableNodes.isEmpty()) {
            // This is a warning, not an error - some workflows might have disconnected subgraphs
            System.out.println("Warning: Unreachable nodes detected: " + unreachableNodes);
        }
    }

    private StatefulWorkflowResult<O> executeWorkflow(I input, WorkflowState state, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        WorkflowState currentState = state;
        I currentInput = input;
        int stepCount = 0;
        
        while (stepCount < maxExecutionSteps) {
            stepCount++;
            
            // Get current node
            String currentNodeId = currentState.getCurrentNodeId()
                    .orElseThrow(() -> new WorkflowExecutionException(name, "No current node in workflow state"));
            
            StatefulAgentNode<I> currentNode = nodes.get(currentNodeId);
            if (currentNode == null) {
                throw new WorkflowExecutionException(name, "Current node not found: " + currentNodeId);
            }
            
            // Execute current node
            WorkflowCommand<I> command;
            try {
                command = currentNode.process(currentInput, currentState, context);
            } catch (Exception e) {
                return StatefulWorkflowResult.error("Node execution failed: " + e.getMessage(), currentState);
            }
            
            // Apply state updates
            if (!command.getStateUpdates().isEmpty()) {
                currentState = currentState.withUpdates(command.getStateUpdates());
            }
            
            // Process command
            switch (command.getType()) {
                case COMPLETE:
                    O completeOutput = outputExtractor.extractOutput(currentInput, currentState, context);
                    return StatefulWorkflowResult.completed(completeOutput, currentState);
                    
                case SUSPEND:
                    return StatefulWorkflowResult.suspended(currentState);
                    
                case ERROR:
                    String errorMessage = command.getErrorMessage().orElse("Unknown error");
                    return StatefulWorkflowResult.error(errorMessage, currentState);
                    
                case GOTO:
                    String targetNodeId = command.getTargetNodeId()
                            .orElseThrow(() -> new WorkflowExecutionException(name, "GOTO command missing target node"));
                    
                    if (!nodes.containsKey(targetNodeId)) {
                        return StatefulWorkflowResult.error("GOTO target node not found: " + targetNodeId, currentState);
                    }
                    
                    currentState = currentState.withCurrentNode(targetNodeId);
                    currentInput = command.getNextInput().orElse(currentInput);
                    break;
                    
                case CONTINUE:
                    StatefulAgentNode<I> nextNode = findNextNode(currentNode, currentInput, currentState);
                    if (nextNode == null) {
                        // No more nodes to execute - complete the workflow
                        O continueOutput = outputExtractor.extractOutput(currentInput, currentState, context);
                        return StatefulWorkflowResult.completed(continueOutput, currentState);
                    }
                    
                    currentState = currentState.withCurrentNode(nextNode.getNodeId());
                    currentInput = command.getNextInput().orElse(currentInput);
                    break;
                    
                default:
                    return StatefulWorkflowResult.error("Unknown command type: " + command.getType(), currentState);
            }
        }
        
        // Max steps exceeded - suspend workflow
        return StatefulWorkflowResult.suspended(currentState);
    }

    private StatefulAgentNode<I> determineEntryPoint(I input, WorkflowState state, Map<String, Object> context) {
        if (defaultEntryPoint != null) {
            return defaultEntryPoint;
        }
        
        if (entryPoints.size() == 1) {
            return entryPoints.get(0);
        }
        
        // If multiple entry points, use the first one (could be enhanced with routing logic)
        return entryPoints.get(0);
    }

    private StatefulAgentNode<I> findNextNode(StatefulAgentNode<I> currentNode, I input, WorkflowState state) {
        List<WorkflowRoute<I>> candidateRoutes = getRoutesFrom(currentNode.getNodeId());
        
        if (candidateRoutes.isEmpty()) {
            return null; // No outgoing routes
        }
        
        // Find matching routes
        List<WorkflowRoute<I>> matchingRoutes = candidateRoutes.stream()
                .filter(route -> route.matches(input, state))
                .sorted(Comparator.<WorkflowRoute<I>>comparingInt(WorkflowRoute::getPriority).reversed())
                .collect(Collectors.toList());
        
        if (matchingRoutes.isEmpty()) {
            // Try default routes
            Optional<WorkflowRoute<I>> defaultRoute = candidateRoutes.stream()
                    .filter(WorkflowRoute::isDefault)
                    .findFirst();
            
            if (defaultRoute.isPresent()) {
                return nodes.get(defaultRoute.get().getToNodeId());
            }
            
            return null; // No matching routes
        }
        
        // Use highest priority matching route
        WorkflowRoute<I> selectedRoute = matchingRoutes.get(0);
        return nodes.get(selectedRoute.getToNodeId());
    }

    private Set<String> findReachableNodes() {
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        // Start from entry points
        if (defaultEntryPoint != null) {
            queue.add(defaultEntryPoint.getNodeId());
        }
        entryPoints.forEach(node -> queue.add(node.getNodeId()));
        
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            if (reachable.add(nodeId)) {
                // Add all nodes reachable from this node
                getRoutesFrom(nodeId).forEach(route -> queue.add(route.getToNodeId()));
            }
        }
        
        return reachable;
    }

    private String generateWorkflowId() {
        return name + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Functional interface for extracting output from workflow state.
     */
    @FunctionalInterface
    public interface OutputExtractor<I, O> {
        O extractOutput(I input, WorkflowState state, Map<String, Object> context);
    }

    /**
     * Builder for creating StatefulWorkflowImpl instances.
     */
    public static class Builder<I, O> {
        private String name;
        private final Map<String, StatefulAgentNode<I>> nodes = new HashMap<>();
        private final List<WorkflowRoute<I>> routes = new ArrayList<>();
        private StatefulAgentNode<I> defaultEntryPoint;
        private OutputExtractor<I, O> outputExtractor;
        private int maxExecutionSteps = 1000;

        public Builder<I, O> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<I, O> addNode(StatefulAgentNode<I> node) {
            this.nodes.put(node.getNodeId(), node);
            return this;
        }

        public Builder<I, O> addRoute(WorkflowRoute<I> route) {
            this.routes.add(route);
            return this;
        }

        public Builder<I, O> addRoute(String id, String fromNodeId, String toNodeId) {
            return addRoute(WorkflowRoute.simple(id, fromNodeId, toNodeId));
        }

        public Builder<I, O> defaultEntryPoint(StatefulAgentNode<I> entryPoint) {
            this.defaultEntryPoint = entryPoint;
            return this;
        }

        public Builder<I, O> outputExtractor(OutputExtractor<I, O> extractor) {
            this.outputExtractor = extractor;
            return this;
        }

        public Builder<I, O> maxExecutionSteps(int maxSteps) {
            this.maxExecutionSteps = maxSteps;
            return this;
        }

        public StatefulWorkflowImpl<I, O> build() {
            if (name == null) {
                name = "StatefulWorkflow-" + System.currentTimeMillis();
            }
            if (outputExtractor == null) {
                // Default extractor that tries to cast the input to output type
                outputExtractor = (input, state, context) -> {
                    @SuppressWarnings("unchecked")
                    O result = (O) input;
                    return result;
                };
            }

            return new StatefulWorkflowImpl<>(name, nodes, routes, defaultEntryPoint, outputExtractor, maxExecutionSteps);
        }
    }

    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }
}