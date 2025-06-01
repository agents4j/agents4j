package dev.agents4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.SystemError;
import dev.agents4j.api.result.error.ValidationError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.validation.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Modern implementation of GraphWorkflow using the enhanced graph-based
 * workflow architecture. Provides type-safe navigation, immutable state
 * management, and comprehensive error handling.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class GraphWorkflowImpl<I, O> implements GraphWorkflow<I, O> {

    private final String name;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes;
    private final Map<EdgeId, GraphEdge> edges;
    private final Set<NodeId> entryPointIds;
    private final NodeId defaultEntryPointId;
    private final OutputExtractor<I, O> outputExtractor;
    private final WorkflowConfiguration configuration;
    private final WorkflowMonitor monitor;
    private final Executor asyncExecutor;

    private GraphWorkflowImpl(Builder<I, O> builder) {
        this.name = builder.name;
        this.nodes = Collections.unmodifiableMap(new HashMap<>(builder.nodes));
        this.edges = Collections.unmodifiableMap(new HashMap<>(builder.edges));
        this.entryPointIds = Collections.unmodifiableSet(builder.entryPointIds);
        this.defaultEntryPointId = builder.defaultEntryPointId;
        this.outputExtractor = builder.outputExtractor;
        this.configuration = builder.configuration;
        this.monitor = builder.monitor;
        this.asyncExecutor = builder.asyncExecutor != null
            ? builder.asyncExecutor
            : ForkJoinPool.commonPool();
    }

    /**
     * Starts a new workflow execution with the given input and initial context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    @Override
    public WorkflowResult<O, WorkflowError> start(
        I input,
        WorkflowContext context
    ) {
        // Generate workflow ID
        WorkflowId workflowId = WorkflowId.generate();

        // Create initial context if not provided
        WorkflowContext initialContext = context != null
            ? context
            : WorkflowContext.empty();

        // Add workflow metadata to context
        initialContext = initialContext
            .with(WorkflowContextKeys.WORKFLOW_ID, workflowId.value())
            .with(WorkflowContextKeys.WORKFLOW_NAME, name)
            .with(WorkflowContextKeys.WORKFLOW_START_TIME, Instant.now());

        // Determine entry point
        NodeId entryPoint = determineEntryPoint(input, initialContext);

        // Create initial state
        GraphWorkflowState<I> initialState = GraphWorkflowState.create(
            workflowId,
            input,
            entryPoint,
            initialContext
        );

        monitor.onWorkflowStarted(workflowId, name, initialState);

        try {
            return executeWorkflow(initialState);
        } catch (Exception e) {
            WorkflowError error = SystemError.of(
                "WORKFLOW_EXECUTION_ERROR",
                "Unexpected error during workflow execution: " + e.getMessage(),
                name
            );
            monitor.onWorkflowError(workflowId, error, initialState, e);
            return WorkflowResult.failure(error);
        }
    }

    /**
     * Starts a new workflow execution with the given input and empty context.
     *
     * @param input The workflow input
     * @return Result of the workflow execution
     */
    @Override
    public WorkflowResult<O, WorkflowError> start(I input) {
        return start(input, WorkflowContext.empty());
    }

    /**
     * Starts the workflow execution with custom workflow ID.
     *
     * @param workflowId Custom workflow ID
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    public WorkflowResult<O, WorkflowError> start(
        WorkflowId workflowId,
        I input,
        WorkflowContext context
    ) {
        // Create initial context if not provided
        WorkflowContext initialContext = context != null
            ? context
            : WorkflowContext.empty();

        // Add workflow metadata to context
        initialContext = initialContext
            .with(WorkflowContextKeys.WORKFLOW_ID, workflowId.value())
            .with(WorkflowContextKeys.WORKFLOW_NAME, name)
            .with(WorkflowContextKeys.WORKFLOW_START_TIME, Instant.now());

        // Determine entry point
        NodeId entryPoint = determineEntryPoint(input, initialContext);

        // Create initial state
        GraphWorkflowState<I> initialState = GraphWorkflowState.create(
            workflowId,
            input,
            entryPoint,
            initialContext
        );

        monitor.onWorkflowStarted(workflowId, name, initialState);

        try {
            return executeWorkflow(initialState);
        } catch (Exception e) {
            WorkflowError error = SystemError.of(
                "WORKFLOW_EXECUTION_ERROR",
                "Unexpected error during workflow execution: " + e.getMessage(),
                name
            );
            monitor.onWorkflowError(workflowId, error, initialState, e);
            return WorkflowResult.failure(error);
        }
    }

    /**
     * Resumes a suspended workflow execution with the given state.
     *
     * @param state The suspended workflow state to resume
     * @return Result of the resumed workflow execution
     */
    @Override
    public WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<I> state
    ) {
        return resume(state, WorkflowContext.empty());
    }

    /**
     * Resumes a suspended workflow with the given state and additional context updates.
     *
     * @param state The suspended workflow state to resume
     * @param contextUpdates Additional context updates to apply
     * @return Result of the resumed workflow execution
     */
    @Override
    public WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates
    ) {
        // Apply context updates
        GraphWorkflowState<I> updatedState = state;
        if (contextUpdates != null && !contextUpdates.isEmpty()) {
            updatedState = state.withContext(contextUpdates);
        }

        // Add resume metadata
        updatedState = updatedState.withContext(
            updatedState
                .context()
                .with(WorkflowContextKeys.WORKFLOW_RESUMED_TIME, Instant.now())
                .with(
                    WorkflowContextKeys.WORKFLOW_RESUMED_COUNT,
                    updatedState.getContextOrDefault(
                        WorkflowContextKeys.WORKFLOW_RESUMED_COUNT,
                        0
                    ) +
                    1
                )
        );

        monitor.onWorkflowResumed(state.workflowId(), updatedState);

        try {
            return executeWorkflow(updatedState);
        } catch (Exception e) {
            WorkflowError error = SystemError.of(
                "WORKFLOW_RESUME_ERROR",
                "Unexpected error during workflow resumption: " +
                e.getMessage(),
                name
            );
            monitor.onWorkflowError(state.workflowId(), error, updatedState, e);
            return WorkflowResult.failure(error);
        }
    }

    /**
     * Starts the workflow execution asynchronously.
     *
     * @param input The workflow input
     * @return CompletableFuture with the workflow result
     */
    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        I input
    ) {
        return CompletableFuture.supplyAsync(() -> start(input), asyncExecutor);
    }

    /**
     * Starts the workflow execution asynchronously with the given context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return CompletableFuture with the workflow result
     */
    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        I input,
        WorkflowContext context
    ) {
        return CompletableFuture.supplyAsync(
            () -> start(input, context),
            asyncExecutor
        );
    }

    /**
     * Resumes a suspended workflow asynchronously.
     *
     * @param state The suspended workflow state
     * @return CompletableFuture with the workflow result
     */
    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state
    ) {
        return CompletableFuture.supplyAsync(
            () -> resume(state),
            asyncExecutor
        );
    }

    /**
     * Resumes a suspended workflow asynchronously with additional context updates.
     *
     * @param state The suspended workflow state
     * @param contextUpdates Additional context updates to apply
     * @return CompletableFuture with the workflow result
     */
    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates
    ) {
        return CompletableFuture.supplyAsync(
            () -> resume(state, contextUpdates),
            asyncExecutor
        );
    }

    /**
     * Gets the name of this workflow.
     *
     * @return The workflow name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets all nodes in this workflow.
     *
     * @return Map of node IDs to nodes
     */
    public Map<NodeId, GraphWorkflowNode<I>> getNodes() {
        return nodes;
    }

    /**
     * Gets all edges in this workflow.
     *
     * @return Map of edge IDs to edges
     */
    public Map<EdgeId, GraphEdge> getEdges() {
        return edges;
    }

    /**
     * Gets the node with the specified ID.
     *
     * @param nodeId The node ID
     * @return The node, or null if not found
     */
    public GraphWorkflowNode<I> getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Gets all edges originating from the specified node.
     *
     * @param nodeId The source node ID
     * @return Set of edges from the node
     */
    public Set<GraphEdge> getEdgesFrom(NodeId nodeId) {
        return edges
            .values()
            .stream()
            .filter(edge -> edge.fromNode().equals(nodeId))
            .collect(Collectors.toSet());
    }

    /**
     * Gets all entry points for this workflow.
     *
     * @return Set of entry point node IDs
     */
    public Set<NodeId> getEntryPoints() {
        return entryPointIds;
    }

    /**
     * Validates the workflow configuration.
     *
     * @return ValidationResult containing any validation errors
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        // Check for basic configuration
        if (name == null || name.trim().isEmpty()) {
            errors.add("Workflow name is required");
        }

        if (nodes.isEmpty()) {
            errors.add("Workflow must have at least one node");
        }

        if (entryPointIds.isEmpty()) {
            errors.add("Workflow must have at least one entry point");
        }

        if (defaultEntryPointId == null && entryPointIds.size() > 1) {
            errors.add(
                "Default entry point required when multiple entry points exist"
            );
        }

        if (outputExtractor == null) {
            errors.add("Output extractor is required");
        }

        // Validate node references
        for (EdgeId edgeId : edges.keySet()) {
            GraphEdge edge = edges.get(edgeId);

            if (!nodes.containsKey(edge.fromNode())) {
                errors.add(
                    "Edge " +
                    edgeId.value() +
                    " references non-existent source node: " +
                    edge.fromNode().value()
                );
            }

            if (!nodes.containsKey(edge.toNode())) {
                errors.add(
                    "Edge " +
                    edgeId.value() +
                    " references non-existent target node: " +
                    edge.toNode().value()
                );
            }
        }

        // Dont validate reachability
        if (errors.size() > 0) {
            return ValidationResult.failure(errors);
        } else {
            return ValidationResult.success();
        }
    }

    /**
     * Executes the workflow logic with the given state.
     *
     * @param state The current workflow state
     * @return Result of the workflow execution
     */
    private WorkflowResult<O, WorkflowError> executeWorkflow(
        GraphWorkflowState<I> state
    ) {
        GraphWorkflowState<I> currentState = state;
        int stepCount = 0;
        Instant startTime = Instant.now();
        Set<String> visitedNodes = new HashSet<>();

        while (stepCount < configuration.getMaxExecutionSteps()) {
            stepCount++;

            // Check execution timeout
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(configuration.getMaxExecutionTime()) > 0) {
                WorkflowError error = SystemError.of(
                    "EXECUTION_TIMEOUT",
                    "Workflow execution exceeded maximum allowed time of " +
                    configuration.getMaxExecutionTime(),
                    name
                );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );
                return WorkflowResult.failure(error);
            }

            // Get current node
            NodeId currentNodeId = currentState
                .currentNode()
                .orElseThrow(() ->
                    new IllegalStateException(
                        "No current node in workflow state"
                    )
                );

            GraphWorkflowNode<I> currentNode = nodes.get(currentNodeId);
            if (currentNode == null) {
                WorkflowError error = SystemError.of(
                    "NODE_NOT_FOUND",
                    "Node not found: " + currentNodeId.value(),
                    name
                );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );
                return WorkflowResult.failure(error);
            }

            // Check for cycles if configured
            if (configuration.isDetectCycles()) {
                String nodeKey = currentNodeId.value();
                if (
                    visitedNodes.contains(nodeKey) &&
                    !configuration.isAllowCycles()
                ) {
                    WorkflowError error = SystemError.of(
                        "CYCLE_DETECTED",
                        "Cycle detected in workflow execution at node: " +
                        currentNodeId.value(),
                        name
                    );
                    monitor.onWorkflowError(
                        currentState.workflowId(),
                        error,
                        currentState,
                        null
                    );
                    return WorkflowResult.failure(error);
                }
                visitedNodes.add(nodeKey);
            }

            // Monitor node start
            monitor.onNodeStarted(
                currentState.workflowId(),
                currentNodeId,
                currentState
            );
            Instant nodeStartTime = Instant.now();

            // Process node with lifecycle management
            WorkflowResult<GraphCommand<I>, WorkflowError> commandResult;
            try {
                commandResult = currentNode.processWithLifecycle(currentState);
            } catch (Exception e) {
                WorkflowError error = SystemError.of(
                    "NODE_EXECUTION_ERROR",
                    "Error executing node " +
                    currentNodeId.value() +
                    ": " +
                    e.getMessage(),
                    name
                );
                monitor.onNodeError(
                    currentState.workflowId(),
                    currentNodeId,
                    error,
                    currentState,
                    e
                );
                return WorkflowResult.failure(error);
            }

            // Monitor node completion
            Duration nodeExecutionTime = Duration.between(
                nodeStartTime,
                Instant.now()
            );
            monitor.onNodeCompleted(
                currentState.workflowId(),
                currentNodeId,
                nodeExecutionTime,
                currentState
            );

            // Handle command result
            if (commandResult.isFailure()) {
                WorkflowError error = commandResult
                    .getError()
                    .orElse(
                        SystemError.of(
                            "UNKNOWN_ERROR",
                            "Unknown error occurred in node " +
                            currentNodeId.value(),
                            name
                        )
                    );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );
                return WorkflowResult.failure(error);
            } else if (commandResult.isSuspended()) {
                var suspension = commandResult.getSuspension().get();
                monitor.onWorkflowSuspended(
                    currentState.workflowId(),
                    suspension.suspensionId(),
                    suspension.reason(),
                    currentState
                );
                return WorkflowResult.suspended(
                    suspension.suspensionId(),
                    currentState,
                    suspension.reason()
                );
            }

            // Process the command
            GraphCommand<I> command = commandResult
                .getValue()
                .orElseThrow(() ->
                    new IllegalStateException("Command result has no value")
                );

            if (command instanceof GraphCommand.Complete) {
                // Extract output and complete the workflow
                GraphCommand.Complete<I> completeCommand =
                    (GraphCommand.Complete<I>) command;

                // Apply any final state updates
                if (completeCommand.getContextUpdates().isPresent()) {
                    currentState = currentState.withContext(
                        completeCommand.getContextUpdates().get()
                    );
                }

                if (completeCommand.getStateData().isPresent()) {
                    currentState = currentState.withData(
                        completeCommand.getStateData().get()
                    );
                }

                // Extract output
                O output;
                try {
                    output = outputExtractor.extractOutput(
                        completeCommand.result(),
                        currentState
                    );
                } catch (Exception e) {
                    WorkflowError error = SystemError.of(
                        "OUTPUT_EXTRACTION_ERROR",
                        "Error extracting output: " + e.getMessage(),
                        name
                    );
                    monitor.onWorkflowError(
                        currentState.workflowId(),
                        error,
                        currentState,
                        e
                    );
                    return WorkflowResult.failure(error);
                }

                monitor.onWorkflowCompleted(
                    currentState.workflowId(),
                    currentState
                );
                return WorkflowResult.success(output);
            } else if (command instanceof GraphCommand.Traverse) {
                // Navigate to next node
                GraphCommand.Traverse<I> traverseCommand =
                    (GraphCommand.Traverse<I>) command;
                NodeId targetNodeId = traverseCommand.targetNode();

                // Verify target node exists
                if (!nodes.containsKey(targetNodeId)) {
                    WorkflowError error = SystemError.of(
                        "INVALID_TRAVERSE",
                        "Cannot traverse to non-existent node: " +
                        targetNodeId.value(),
                        name
                    );
                    monitor.onWorkflowError(
                        currentState.workflowId(),
                        error,
                        currentState,
                        null
                    );
                    return WorkflowResult.failure(error);
                }

                // Apply any context updates
                if (traverseCommand.getContextUpdates().isPresent()) {
                    currentState = currentState.withContext(
                        traverseCommand.getContextUpdates().get()
                    );
                }

                // Apply any data updates
                if (traverseCommand.getStateData().isPresent()) {
                    currentState = currentState.withData(
                        traverseCommand.getStateData().get()
                    );
                }

                // Check if an edge exists and if its condition is satisfied
                EdgeId edgeId = findEdgeBetween(currentNodeId, targetNodeId);
                if (edgeId != null) {
                    GraphEdge edge = edges.get(edgeId);

                    // Check edge condition if present
                    if (
                        edge.hasCondition() &&
                        !edge.condition().evaluate(currentState)
                    ) {
                        WorkflowError error = SystemError.of(
                            "EDGE_CONDITION_FAILED",
                            "Edge condition not satisfied for edge: " +
                            edgeId.value(),
                            name
                        );
                        monitor.onWorkflowError(
                            currentState.workflowId(),
                            error,
                            currentState,
                            null
                        );
                        return WorkflowResult.failure(error);
                    }

                    // Update edge traversal metadata
                    currentState = currentState.withContext(
                        currentState
                            .context()
                            .with(
                                WorkflowContextKeys.LAST_EDGE_ID,
                                edgeId.value()
                            )
                            .with(
                                WorkflowContextKeys.LAST_EDGE_TIME,
                                Instant.now()
                            )
                    );
                }

                // Move to target node
                currentState = currentState.moveToNode(targetNodeId);

                monitor.onNodeTransition(
                    currentState.workflowId(),
                    currentNodeId,
                    targetNodeId,
                    currentState
                );
            } else if (command instanceof GraphCommand.Suspend) {
                // Suspend workflow execution
                GraphCommand.Suspend<I> suspendCommand = (GraphCommand.Suspend<
                        I
                    >) command;

                // Apply any context or data updates
                if (suspendCommand.getContextUpdates().isPresent()) {
                    currentState = currentState.withContext(
                        suspendCommand.getContextUpdates().get()
                    );
                }

                if (suspendCommand.getStateData().isPresent()) {
                    currentState = currentState.withData(
                        suspendCommand.getStateData().get()
                    );
                }

                monitor.onWorkflowSuspended(
                    currentState.workflowId(),
                    suspendCommand.suspensionId(),
                    suspendCommand.reason(),
                    currentState
                );

                return WorkflowResult.suspended(
                    suspendCommand.suspensionId(),
                    currentState,
                    suspendCommand.reason()
                );
            } else if (command instanceof GraphCommand.Error) {
                // Handle error command
                GraphCommand.Error<I> errorCommand = (GraphCommand.Error<
                        I
                    >) command;

                // Apply any context or data updates
                if (errorCommand.getContextUpdates().isPresent()) {
                    currentState = currentState.withContext(
                        errorCommand.getContextUpdates().get()
                    );
                }

                if (errorCommand.getStateData().isPresent()) {
                    currentState = currentState.withData(
                        errorCommand.getStateData().get()
                    );
                }

                // Check if there's a fallback node
                if (errorCommand.fallbackNode().isPresent()) {
                    NodeId fallbackNode = errorCommand.fallbackNode().get();

                    // Verify fallback node exists
                    if (!nodes.containsKey(fallbackNode)) {
                        WorkflowError error = SystemError.of(
                            "INVALID_FALLBACK",
                            "Fallback node does not exist: " +
                            fallbackNode.value(),
                            name
                        );
                        monitor.onWorkflowError(
                            currentState.workflowId(),
                            error,
                            currentState,
                            null
                        );
                        return WorkflowResult.failure(error);
                    }

                    // Move to fallback node
                    currentState = currentState.moveToNode(fallbackNode);

                    monitor.onNodeTransition(
                        currentState.workflowId(),
                        currentNodeId,
                        fallbackNode,
                        currentState
                    );
                } else {
                    // No fallback, propagate the error
                    WorkflowError error = errorCommand.error();
                    monitor.onWorkflowError(
                        currentState.workflowId(),
                        error,
                        currentState,
                        null
                    );
                    return WorkflowResult.failure(error);
                }
            } else if (command instanceof GraphCommand.Fork) {
                // Fork execution into multiple branches
                GraphCommand.Fork<I> forkCommand = (GraphCommand.Fork<
                        I
                    >) command;

                // This implementation doesn't handle parallel execution within a single workflow
                // Instead, it follows the first branch and logs a warning
                Set<NodeId> targetNodes = forkCommand.targetNodes();
                if (targetNodes.isEmpty()) {
                    WorkflowError error = SystemError.of(
                        "INVALID_FORK",
                        "Fork command with no target nodes",
                        name
                    );
                    monitor.onWorkflowError(
                        currentState.workflowId(),
                        error,
                        currentState,
                        null
                    );
                    return WorkflowResult.failure(error);
                }

                // Take the first branch
                NodeId firstTarget = targetNodes.iterator().next();

                // Apply any context or data updates
                if (forkCommand.getContextUpdates().isPresent()) {
                    currentState = currentState.withContext(
                        forkCommand.getContextUpdates().get()
                    );
                }

                if (forkCommand.getStateData().isPresent()) {
                    currentState = currentState.withData(
                        forkCommand.getStateData().get()
                    );
                }

                // Move to first target node
                currentState = currentState.moveToNode(firstTarget);

                monitor.onNodeTransition(
                    currentState.workflowId(),
                    currentNodeId,
                    firstTarget,
                    currentState
                );

                // Log warning about unsupported parallel execution
                monitor.onWarning(
                    currentState.workflowId(),
                    "Parallel fork execution not supported. Following first branch: " +
                    firstTarget.value(),
                    currentState
                );
            } else {
                // Unsupported command type
                WorkflowError error = SystemError.of(
                    "UNSUPPORTED_COMMAND",
                    "Unsupported command type: " +
                    command.getClass().getSimpleName(),
                    name
                );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );
                return WorkflowResult.failure(error);
            }
        }

        // If we get here, we've exceeded the maximum execution steps
        WorkflowError error = SystemError.of(
            "MAX_STEPS_EXCEEDED",
            "Workflow execution exceeded maximum allowed steps: " +
            configuration.getMaxExecutionSteps(),
            name
        );
        monitor.onWorkflowError(
            currentState.workflowId(),
            error,
            currentState,
            null
        );
        return WorkflowResult.failure(error);
    }

    /**
     * Determines the entry point for a workflow execution.
     *
     * @param input The workflow input
     * @param context The workflow context
     * @return The selected entry point node ID
     */
    private NodeId determineEntryPoint(I input, WorkflowContext context) {
        // If there's only one entry point, use it
        if (entryPointIds.size() == 1) {
            return entryPointIds.iterator().next();
        }

        // If a default entry point is specified, use it
        if (defaultEntryPointId != null) {
            return defaultEntryPointId;
        }

        // If we get here, there are multiple entry points and no default
        // This should have been caught by validation, but just in case
        throw new IllegalStateException(
            "Multiple entry points exist but no default entry point specified"
        );
    }

    /**
     * Finds all nodes reachable from entry points.
     *
     * @return Set of all reachable node IDs
     */
    private Set<NodeId> findReachableNodes() {
        Set<NodeId> reachable = new HashSet<>();
        Set<NodeId> toProcess = new HashSet<>(entryPointIds);

        while (!toProcess.isEmpty()) {
            NodeId current = toProcess.iterator().next();
            toProcess.remove(current);

            if (reachable.contains(current)) {
                continue;
            }

            reachable.add(current);

            // Find all edges from this node
            for (GraphEdge edge : getEdgesFrom(current)) {
                if (!reachable.contains(edge.toNode())) {
                    toProcess.add(edge.toNode());
                }
            }
        }

        return reachable;
    }

    /**
     * Finds an edge between two nodes if one exists.
     *
     * @param fromNode The source node ID
     * @param toNode The target node ID
     * @return The edge ID, or null if no edge exists
     */
    private EdgeId findEdgeBetween(NodeId fromNode, NodeId toNode) {
        return edges
            .values()
            .stream()
            .filter(
                edge ->
                    edge.fromNode().equals(fromNode) &&
                    edge.toNode().equals(toNode)
            )
            .map(edge -> edge.edgeId())
            .findFirst()
            .orElse(null);
    }

    /**
     * Interface for extracting output from workflow results.
     *
     * @param <I> The input type
     * @param <O> The output type
     */
    public interface OutputExtractor<I, O> {
        /**
         * Extracts output from the workflow result and state.
         *
         * @param result The workflow result object
         * @param state The final workflow state
         * @return The extracted output
         */
        O extractOutput(Object result, GraphWorkflowState<I> state);
    }

    /**
     * Configuration for workflow execution.
     */
    public static class WorkflowConfiguration {

        private final int maxExecutionSteps;
        private final Duration maxExecutionTime;
        private final boolean detectCycles;
        private final boolean allowCycles;

        private WorkflowConfiguration(
            int maxExecutionSteps,
            Duration maxExecutionTime,
            boolean detectCycles,
            boolean allowCycles
        ) {
            this.maxExecutionSteps = maxExecutionSteps;
            this.maxExecutionTime = maxExecutionTime;
            this.detectCycles = detectCycles;
            this.allowCycles = allowCycles;
        }

        public int getMaxExecutionSteps() {
            return maxExecutionSteps;
        }

        public Duration getMaxExecutionTime() {
            return maxExecutionTime;
        }

        public boolean isDetectCycles() {
            return detectCycles;
        }

        public boolean isAllowCycles() {
            return allowCycles;
        }

        public static WorkflowConfiguration defaultConfiguration() {
            return new WorkflowConfiguration(
                1000, // default max steps
                Duration.ofMinutes(5), // default max execution time
                true, // detect cycles by default
                false // don't allow cycles by default
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            private int maxExecutionSteps = 1000;
            private Duration maxExecutionTime = Duration.ofMinutes(5);
            private boolean detectCycles = true;
            private boolean allowCycles = false;

            public Builder maxExecutionSteps(int maxExecutionSteps) {
                this.maxExecutionSteps = maxExecutionSteps;
                return this;
            }

            public Builder maxExecutionTime(Duration maxExecutionTime) {
                this.maxExecutionTime = maxExecutionTime;
                return this;
            }

            public Builder detectCycles(boolean detectCycles) {
                this.detectCycles = detectCycles;
                return this;
            }

            public Builder allowCycles(boolean allowCycles) {
                this.allowCycles = allowCycles;
                return this;
            }

            public WorkflowConfiguration build() {
                return new WorkflowConfiguration(
                    maxExecutionSteps,
                    maxExecutionTime,
                    detectCycles,
                    allowCycles
                );
            }
        }
    }

    /**
     * Workflow monitoring interface for tracking execution events.
     */
    public interface WorkflowMonitor {
        void onWorkflowStarted(
            WorkflowId workflowId,
            String name,
            GraphWorkflowState<?> state
        );
        void onWorkflowResumed(
            WorkflowId workflowId,
            GraphWorkflowState<?> state
        );
        void onWorkflowCompleted(
            WorkflowId workflowId,
            GraphWorkflowState<?> state
        );
        void onWorkflowSuspended(
            WorkflowId workflowId,
            String suspensionId,
            String reason,
            GraphWorkflowState<?> state
        );
        void onWorkflowError(
            WorkflowId workflowId,
            WorkflowError error,
            GraphWorkflowState<?> state,
            Throwable cause
        );
        void onNodeStarted(
            WorkflowId workflowId,
            NodeId nodeId,
            GraphWorkflowState<?> state
        );
        void onNodeCompleted(
            WorkflowId workflowId,
            NodeId nodeId,
            Duration executionTime,
            GraphWorkflowState<?> state
        );
        void onNodeError(
            WorkflowId workflowId,
            NodeId nodeId,
            WorkflowError error,
            GraphWorkflowState<?> state,
            Throwable cause
        );
        void onNodeTransition(
            WorkflowId workflowId,
            NodeId fromNodeId,
            NodeId toNodeId,
            GraphWorkflowState<?> state
        );
        void onWarning(
            WorkflowId workflowId,
            String message,
            GraphWorkflowState<?> state
        );
    }

    /**
     * Default implementation of WorkflowMonitor that does nothing.
     */
    public static class NoOpWorkflowMonitor implements WorkflowMonitor {

        public static final NoOpWorkflowMonitor INSTANCE =
            new NoOpWorkflowMonitor();

        @Override
        public void onWorkflowStarted(
            WorkflowId workflowId,
            String name,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onWorkflowResumed(
            WorkflowId workflowId,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onWorkflowCompleted(
            WorkflowId workflowId,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onWorkflowSuspended(
            WorkflowId workflowId,
            String suspensionId,
            String reason,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onWorkflowError(
            WorkflowId workflowId,
            WorkflowError error,
            GraphWorkflowState<?> state,
            Throwable cause
        ) {}

        @Override
        public void onNodeStarted(
            WorkflowId workflowId,
            NodeId nodeId,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onNodeCompleted(
            WorkflowId workflowId,
            NodeId nodeId,
            Duration executionTime,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onNodeError(
            WorkflowId workflowId,
            NodeId nodeId,
            WorkflowError error,
            GraphWorkflowState<?> state,
            Throwable cause
        ) {}

        @Override
        public void onNodeTransition(
            WorkflowId workflowId,
            NodeId fromNodeId,
            NodeId toNodeId,
            GraphWorkflowState<?> state
        ) {}

        @Override
        public void onWarning(
            WorkflowId workflowId,
            String message,
            GraphWorkflowState<?> state
        ) {}
    }

    /**
     * Context keys used by the workflow implementation.
     */
    public static class WorkflowContextKeys {

        public static final ContextKey<String> WORKFLOW_ID =
            ContextKey.stringKey("workflow.id");
        public static final ContextKey<String> WORKFLOW_NAME =
            ContextKey.stringKey("workflow.name");
        public static final ContextKey<Instant> WORKFLOW_START_TIME =
            ContextKey.of("workflow.start_time", Instant.class);
        public static final ContextKey<Instant> WORKFLOW_RESUMED_TIME =
            ContextKey.of("workflow.resumed_time", Instant.class);
        public static final ContextKey<Integer> WORKFLOW_RESUMED_COUNT =
            ContextKey.intKey("workflow.resumed_count");
        public static final ContextKey<String> LAST_EDGE_ID =
            ContextKey.stringKey("workflow.last_edge_id");
        public static final ContextKey<Instant> LAST_EDGE_TIME = ContextKey.of(
            "workflow.last_edge_time",
            Instant.class
        );
    }

    /**
     * Builder for creating GraphWorkflowImpl instances.
     */
    public static class Builder<I, O> {

        private String name;
        private final Map<NodeId, GraphWorkflowNode<I>> nodes = new HashMap<>();
        private final Map<EdgeId, GraphEdge> edges = new HashMap<>();
        private final Set<NodeId> entryPointIds = new HashSet<>();
        private NodeId defaultEntryPointId;
        private OutputExtractor<I, O> outputExtractor;
        private WorkflowConfiguration configuration =
            WorkflowConfiguration.defaultConfiguration();
        private WorkflowMonitor monitor = NoOpWorkflowMonitor.INSTANCE;
        private Executor asyncExecutor;

        /**
         * Sets the name of the workflow.
         *
         * @param name The workflow name
         * @return This builder instance
         */
        public Builder<I, O> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds a node to the workflow.
         *
         * @param node The node to add
         * @return This builder instance
         */
        public Builder<I, O> addNode(GraphWorkflowNode<I> node) {
            this.nodes.put(node.getNodeId(), node);
            if (node.isEntryPoint()) {
                this.entryPointIds.add(node.getNodeId());
            }
            return this;
        }

        /**
         * Adds an edge to the workflow.
         *
         * @param edge The edge to add
         * @return This builder instance
         */
        public Builder<I, O> addEdge(GraphEdge edge) {
            this.edges.put(edge.edgeId(), edge);
            return this;
        }

        /**
         * Adds an edge between two nodes.
         *
         * @param fromNodeId The source node ID
         * @param toNodeId The target node ID
         * @return This builder instance
         */
        public Builder<I, O> addEdge(NodeId fromNodeId, NodeId toNodeId) {
            return addEdge(GraphEdge.between(fromNodeId, toNodeId));
        }

        /**
         * Adds a conditional edge between two nodes.
         *
         * @param fromNodeId The source node ID
         * @param toNodeId The target node ID
         * @param condition The edge condition
         * @return This builder instance
         */
        public Builder<I, O> addEdge(
            NodeId fromNodeId,
            NodeId toNodeId,
            EdgeCondition condition
        ) {
            return addEdge(
                GraphEdge.conditional(fromNodeId, toNodeId, condition)
            );
        }

        /**
         * Sets the default entry point for the workflow.
         *
         * @param entryPointId The default entry point node ID
         * @return This builder instance
         */
        public Builder<I, O> defaultEntryPoint(NodeId entryPointId) {
            this.defaultEntryPointId = entryPointId;
            this.entryPointIds.add(entryPointId);
            return this;
        }

        /**
         * Sets the output extractor for the workflow.
         *
         * @param extractor The output extractor
         * @return This builder instance
         */
        public Builder<I, O> outputExtractor(OutputExtractor<I, O> extractor) {
            this.outputExtractor = extractor;
            return this;
        }

        /**
         * Sets the configuration for the workflow.
         *
         * @param configuration The workflow configuration
         * @return This builder instance
         */
        public Builder<I, O> configuration(
            WorkflowConfiguration configuration
        ) {
            this.configuration = Objects.requireNonNull(configuration);
            return this;
        }

        /**
         * Sets the monitor for the workflow.
         *
         * @param monitor The workflow monitor
         * @return This builder instance
         */
        public Builder<I, O> monitor(WorkflowMonitor monitor) {
            this.monitor = Objects.requireNonNull(monitor);
            return this;
        }

        /**
         * Sets the executor for asynchronous operations.
         *
         * @param executor The executor
         * @return This builder instance
         */
        public Builder<I, O> asyncExecutor(Executor executor) {
            this.asyncExecutor = executor;
            return this;
        }

        /**
         * Adds a router node with outgoing edges to target nodes.
         *
         * @param router The content router
         * @param targets The target node IDs
         * @return This builder instance
         */
        public Builder<I, O> addRouter(
            ContentRouter<I> router,
            Set<NodeId> targets
        ) {
            // Add the router as a node
            addNode(router);

            // Add edges from router to all target nodes
            for (NodeId target : targets) {
                addEdge(router.getNodeId(), target);
            }

            return this;
        }

        /**
         * Adds a router node with conditional edges based on routing decisions.
         *
         * @param router The content router
         * @param targets The target node IDs
         * @return This builder instance
         */
        public Builder<I, O> addDynamicRouter(
            ContentRouter<I> router,
            Set<NodeId> targets
        ) {
            // Add the router as a node
            addNode(router);

            // For each target, create an edge with a condition that checks if it was selected
            for (NodeId target : targets) {
                EdgeCondition condition = EdgeCondition.whenContextEquals(
                    ContextKey.stringKey("routing.selected_route"),
                    target.value()
                );

                addEdge(router.getNodeId(), target, condition);
            }

            return this;
        }

        /**
         * Builds the workflow instance.
         *
         * @return A new GraphWorkflowImpl instance
         * @throws IllegalStateException If validation fails
         */
        public GraphWorkflowImpl<I, O> build() {
            // Set default name if not provided
            if (name == null || name.trim().isEmpty()) {
                name =
                    "Workflow-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // Find entry points if not explicitly set
            if (entryPointIds.isEmpty()) {
                for (GraphWorkflowNode<I> node : nodes.values()) {
                    if (node.isEntryPoint()) {
                        entryPointIds.add(node.getNodeId());
                    }
                }
            }

            // Require at least one entry point
            if (entryPointIds.isEmpty()) {
                throw new IllegalStateException(
                    "Workflow must have at least one entry point"
                );
            }

            // Set default entry point if not specified
            if (defaultEntryPointId == null && entryPointIds.size() == 1) {
                defaultEntryPointId = entryPointIds.iterator().next();
            }

            // Require output extractor
            if (outputExtractor == null) {
                throw new IllegalStateException("Output extractor is required");
            }

            // Create workflow instance
            GraphWorkflowImpl<I, O> workflow = new GraphWorkflowImpl<>(this);

            // Validate the workflow
            ValidationResult validation = workflow.validate();
            if (!validation.isValid()) {
                throw new IllegalStateException(
                    "Invalid workflow configuration: " + validation
                );
            }

            return workflow;
        }
    }

    /**
     * Creates a new builder for a GraphWorkflowImpl.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @return A new builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }
}
