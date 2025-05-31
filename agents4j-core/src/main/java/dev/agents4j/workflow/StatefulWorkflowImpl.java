package dev.agents4j.workflow;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowState;

import dev.agents4j.workflow.api.CommandProcessor;
import dev.agents4j.workflow.api.ExecutionResult;
import dev.agents4j.workflow.api.NodeRoutingStrategy;
import dev.agents4j.workflow.api.WorkflowExecutionMonitor;
import dev.agents4j.workflow.impl.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Refactored implementation of StatefulWorkflow following SOLID principles.
 * Uses composition and dependency injection for better separation of concerns.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class StatefulWorkflowImpl<I, O> implements StatefulWorkflow<I, O> {
    
    private final String name;
    private final Map<String, StatefulAgentNode<I>> nodes;
    private final List<WorkflowRoute<I>> routes;
    private final List<StatefulAgentNode<I>> entryPoints;
    private final StatefulAgentNode<I> defaultEntryPoint;
    private final OutputExtractor<I, O> outputExtractor;
    private final WorkflowExecutionConfiguration configuration;
    
    // Strategy dependencies
    private final NodeRoutingStrategy<I> routingStrategy;
    private final CommandProcessor<I, O> commandProcessor;
    private final WorkflowExecutionMonitor monitor;
    private final Executor asyncExecutor;

    private StatefulWorkflowImpl(Builder<I, O> builder) {
        this.name = builder.name;
        this.nodes = Collections.unmodifiableMap(new HashMap<>(builder.nodes));
        this.routes = Collections.unmodifiableList(new ArrayList<>(builder.routes));
        this.defaultEntryPoint = builder.defaultEntryPoint;
        this.outputExtractor = builder.outputExtractor;
        this.configuration = builder.configuration;
        this.routingStrategy = builder.routingStrategy;
        this.commandProcessor = builder.commandProcessor;
        this.monitor = builder.monitor;
        this.asyncExecutor = builder.asyncExecutor;
        
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
        
        if (context == null) {
            context = new HashMap<>();
        }
        
        monitor.onWorkflowStarted(initialState.getWorkflowId(), name, context);
        
        try {
            // Determine entry point
            StatefulAgentNode<I> entryNode = determineEntryPoint(input, initialState, context);
            
            // Update state with entry point
            WorkflowState stateWithEntryPoint = initialState.withCurrentNode(entryNode.getNodeId());
            
            return executeWorkflow(input, stateWithEntryPoint, context);
        } catch (Exception e) {
            monitor.onWorkflowError(initialState.getWorkflowId(), e.getMessage(), initialState, e);
            throw e;
        }
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
        
        if (context == null) {
            context = new HashMap<>();
        }
        
        monitor.onWorkflowResumed(state.getWorkflowId(), state);
        
        try {
            return executeWorkflow(input, state, context);
        } catch (Exception e) {
            monitor.onWorkflowError(state.getWorkflowId(), e.getMessage(), state, e);
            throw e;
        }
    }

    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return start(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return resume(input, state, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
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
        return routingStrategy instanceof DefaultNodeRoutingStrategy ? 
            ((DefaultNodeRoutingStrategy<I>) routingStrategy).getRoutesFrom(fromNodeId) :
            routes.stream()
                .filter(route -> route.getFromNodeId().equals(fromNodeId))
                .collect(Collectors.toList());
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
        Instant startTime = Instant.now();
        
        while (stepCount < configuration.getMaxExecutionSteps()) {
            stepCount++;
            
            // Check execution timeout
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(configuration.getMaxExecutionTime()) > 0) {
                monitor.onWorkflowSuspended(currentState.getWorkflowId(), currentState, "Execution timeout exceeded");
                return StatefulWorkflowResult.suspended(currentState);
            }
            
            // Get current node
            String currentNodeId = currentState.getCurrentNodeId()
                    .orElseThrow(() -> new WorkflowExecutionException(name, "No current node in workflow state"));
            
            StatefulAgentNode<I> currentNode = nodes.get(currentNodeId);
            if (currentNode == null) {
                throw new WorkflowExecutionException(name, "Current node not found: " + currentNodeId);
            }
            
            // Monitor node start
            monitor.onNodeStarted(currentState.getWorkflowId(), currentNodeId, currentState);
            Instant nodeStartTime = Instant.now();
            
            // Execute current node
            WorkflowCommand<I> command;
            try {
                command = currentNode.process(currentInput, currentState, context);
            } catch (Exception e) {
                monitor.onWorkflowError(currentState.getWorkflowId(), "Node execution failed: " + e.getMessage(), currentState, e);
                return StatefulWorkflowResult.error("Node execution failed: " + e.getMessage(), currentState);
            }
            
            // Monitor node completion
            Duration nodeExecutionTime = Duration.between(nodeStartTime, Instant.now());
            
            // Process command using command processor
            ExecutionResult<I, O> result = commandProcessor.process(command, currentState, context);
            
            if (result.isFailure()) {
                WorkflowExecutionException error = result.getError().orElse(
                    new WorkflowExecutionException("Unknown execution error"));
                monitor.onWorkflowError(currentState.getWorkflowId(), error.getMessage(), currentState, error);
                return StatefulWorkflowResult.error(error.getMessage(), currentState);
            }
            
            // Update state
            WorkflowState oldState = currentState;
            currentState = result.getState();
            monitor.onNodeCompleted(currentState.getWorkflowId(), currentNodeId, nodeExecutionTime, currentState);
            monitor.onStateUpdated(currentState.getWorkflowId(), oldState, currentState);
            
            // Handle result type
            switch (result.getType()) {
                case COMPLETED:
                    Duration totalTime = Duration.between(startTime, Instant.now());
                    monitor.onWorkflowCompleted(currentState.getWorkflowId(), totalTime, currentState);
                    return result.toWorkflowResult();
                    
                case SUSPENDED:
                    monitor.onWorkflowSuspended(currentState.getWorkflowId(), currentState, "Workflow suspended by command");
                    return result.toWorkflowResult();
                    
                case GOTO:
                    String targetNodeId = result.getTargetNodeId()
                            .orElseThrow(() -> new WorkflowExecutionException(name, "GOTO result missing target node"));
                    
                    if (!nodes.containsKey(targetNodeId)) {
                        return StatefulWorkflowResult.error("GOTO target node not found: " + targetNodeId, currentState);
                    }
                    
                    currentState = currentState.withCurrentNode(targetNodeId);
                    currentInput = result.getNextInput().orElse(currentInput);
                    break;
                    
                case CONTINUE:
                    Optional<StatefulAgentNode<I>> nextNode = routingStrategy.findNextNode(currentNode, currentInput, currentState);
                    if (!nextNode.isPresent()) {
                        // No more nodes to execute - complete the workflow
                        O continueOutput = outputExtractor.extractOutput(currentInput, currentState, context);
                        Duration completionTime = Duration.between(startTime, Instant.now());
                        monitor.onWorkflowCompleted(currentState.getWorkflowId(), completionTime, currentState);
                        return StatefulWorkflowResult.completed(continueOutput, currentState);
                    }
                    
                    currentState = currentState.withCurrentNode(nextNode.get().getNodeId());
                    currentInput = result.getNextInput().orElse(currentInput);
                    break;
                    
                default:
                    return StatefulWorkflowResult.error("Unknown result type: " + result.getType(), currentState);
            }
        }
        
        // Max steps exceeded - suspend workflow
        monitor.onWorkflowSuspended(currentState.getWorkflowId(), currentState, "Max execution steps exceeded");
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
     * Builder for creating StatefulWorkflowImpl instances with improved architecture.
     */
    public static class Builder<I, O> {
        private String name;
        private final Map<String, StatefulAgentNode<I>> nodes = new HashMap<>();
        private final List<WorkflowRoute<I>> routes = new ArrayList<>();
        private StatefulAgentNode<I> defaultEntryPoint;
        private OutputExtractor<I, O> outputExtractor;
        private WorkflowExecutionConfiguration configuration = WorkflowExecutionConfiguration.defaultConfiguration();
        private NodeRoutingStrategy<I> routingStrategy;
        private CommandProcessor<I, O> commandProcessor;
        private WorkflowExecutionMonitor monitor = NoOpWorkflowExecutionMonitor.INSTANCE;
        private Executor asyncExecutor;

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

        public Builder<I, O> configuration(WorkflowExecutionConfiguration config) {
            this.configuration = Objects.requireNonNull(config, "Configuration cannot be null");
            return this;
        }

        public Builder<I, O> routingStrategy(NodeRoutingStrategy<I> strategy) {
            this.routingStrategy = strategy;
            return this;
        }

        public Builder<I, O> commandProcessor(CommandProcessor<I, O> processor) {
            this.commandProcessor = processor;
            return this;
        }

        public Builder<I, O> monitor(WorkflowExecutionMonitor monitor) {
            this.monitor = Objects.requireNonNull(monitor, "Monitor cannot be null");
            return this;
        }

        public Builder<I, O> asyncExecutor(Executor executor) {
            this.asyncExecutor = executor;
            return this;
        }

        @Deprecated
        public Builder<I, O> maxExecutionSteps(int maxSteps) {
            this.configuration = WorkflowExecutionConfiguration.builder()
                .maxExecutionSteps(maxSteps)
                .build();
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
            if (routingStrategy == null) {
                routingStrategy = new DefaultNodeRoutingStrategy<>(routes, nodes);
            }
            if (commandProcessor == null) {
                commandProcessor = createDefaultCommandProcessor();
            }
            if (asyncExecutor == null) {
                asyncExecutor = configuration.getAsyncExecutor();
            }

            return new StatefulWorkflowImpl<>(this);
        }

        private CommandProcessor<I, O> createDefaultCommandProcessor() {
            DefaultCommandProcessor<I, O> processor = new DefaultCommandProcessor<>();
            
            // Register default command handlers
            processor.registerHandler(new CompleteCommandHandler<>(outputExtractor));
            processor.registerHandler(new SuspendCommandHandler<>());
            processor.registerHandler(new ContinueCommandHandler<>());
            processor.registerHandler(new GotoCommandHandler<>());
            processor.registerHandler(new ErrorCommandHandler<>());
            
            return processor;
        }
    }

    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }
}