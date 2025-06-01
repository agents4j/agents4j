package dev.agents4j.workflow.execution;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.*;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.SystemError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.workflow.config.WorkflowConfiguration;
import dev.agents4j.workflow.context.WorkflowContextKeys;
import dev.agents4j.workflow.monitor.WorkflowMonitor;
import dev.agents4j.workflow.output.OutputExtractor;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Executor for graph workflows.
 * Handles the execution of workflow nodes and transitions between them.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class GraphWorkflowExecutor<I, O> {

    private final String workflowName;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes;
    private final Map<EdgeId, GraphEdge> edges;
    private final OutputExtractor<I, O> outputExtractor;
    private final WorkflowConfiguration configuration;
    private final WorkflowMonitor monitor;

    /**
     * Creates a new executor for the given workflow components.
     *
     * @param workflowName The workflow name
     * @param nodes The workflow nodes
     * @param edges The workflow edges
     * @param outputExtractor The output extractor
     * @param configuration The workflow configuration
     * @param monitor The workflow monitor
     */
    public GraphWorkflowExecutor(
        String workflowName,
        Map<NodeId, GraphWorkflowNode<I>> nodes,
        Map<EdgeId, GraphEdge> edges,
        OutputExtractor<I, O> outputExtractor,
        WorkflowConfiguration configuration,
        WorkflowMonitor monitor
    ) {
        this.workflowName = workflowName;
        this.nodes = nodes;
        this.edges = edges;
        this.outputExtractor = outputExtractor;
        this.configuration = configuration;
        this.monitor = monitor;
    }

    /**
     * Executes a workflow from the given state.
     *
     * @param state The workflow state to execute from
     * @return The workflow execution result
     */
    public WorkflowResult<O, WorkflowError> executeWorkflow(
        GraphWorkflowState<I> state
    ) {
        Instant startTime = Instant.now();
        int stepCount = 0;
        Set<String> visitedNodes = new HashSet<>();
        GraphWorkflowState<I> currentState = state;

        while (true) {
            // Check step count limit
            if (stepCount >= configuration.getMaxExecutionSteps()) {
                WorkflowError error = SystemError.of(
                    "MAX_STEPS_EXCEEDED",
                    "Workflow execution exceeded maximum allowed steps: " +
                    configuration.getMaxExecutionSteps(),
                    workflowName
                );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );
                return WorkflowResult.failure(error);
            }

            stepCount++;

            // Check execution timeout
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(configuration.getMaxExecutionTime()) > 0) {
                WorkflowError error = SystemError.of(
                    "EXECUTION_TIMEOUT",
                    "Workflow execution exceeded maximum allowed time of " +
                    configuration.getMaxExecutionTime(),
                    workflowName
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
                    workflowName
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
                        workflowName
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
                    workflowName
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
            long processingTime = Duration.between(
                nodeStartTime,
                Instant.now()
            ).toMillis();
            monitor.onNodeCompleted(
                currentState.workflowId(),
                currentNodeId,
                currentState,
                processingTime
            );

            // Handle node execution result
            if (commandResult.isFailure()) {
                // Node execution failed
                WorkflowError error = commandResult
                    .getError()
                    .orElseThrow(() -> new RuntimeException("Unexpected error")
                    );
                monitor.onWorkflowError(
                    currentState.workflowId(),
                    error,
                    currentState,
                    null
                );

                // Try to extract output even from error state
                O output = outputExtractor.extractFromError(
                    currentState,
                    error
                );
                if (output != null) {
                    return WorkflowResult.failure(error, output);
                }
                return WorkflowResult.failure(error);
            }

            // Process the command from the node
            GraphCommand<I> command = commandResult.getOrThrow();
            if (command instanceof GraphCommandComplete) {
                // Workflow is complete
                monitor.onWorkflowCompleted(
                    currentState.workflowId(),
                    currentState
                );
                O output = outputExtractor.extract(currentState);
                return WorkflowResult.success(output);
            } else if (command instanceof GraphCommandSuspend) {
                // Workflow is suspended
                monitor.onWorkflowSuspended(
                    currentState.workflowId(),
                    currentState
                );
                return WorkflowResult.suspended(
                    UUID.randomUUID().toString(),
                    currentState,
                    "Suspended"
                );
            } else if (command instanceof GraphCommandTraverse) {
                // Transition to another node
                NodeId targetNodeId =
                    ((GraphCommandTraverse) command).targetNode();

                // Find edge between current node and target node
                Optional<GraphEdge> edge = findEdgeBetween(
                    currentNodeId,
                    targetNodeId
                );

                // Update state with new node and context updates
                GraphWorkflowState<I> nextState;
                if (edge.isPresent()) {
                    EdgeId edgeId = edge.get().edgeId();

                    // Add edge information to context
                    WorkflowContext edgeContext = WorkflowContext.empty()
                        .with(WorkflowContextKeys.LAST_EDGE_ID, edgeId.value())
                        .with(
                            WorkflowContextKeys.LAST_EDGE_TIME,
                            Instant.now()
                        );

                    // Combine edge context with command context updates
                    WorkflowContext combinedUpdates = command
                        .getContextUpdates()
                        .get()
                        .merge(edgeContext);

                    // Create new state with edge traversal
                    nextState = currentState
                        .withData(command.getStateData().get())
                        .withContext(combinedUpdates)
                        .traverseEdge(edgeId, targetNodeId);

                    // Monitor the transition
                    monitor.onNodeTransition(
                        currentState.workflowId(),
                        edgeId,
                        currentNodeId,
                        targetNodeId,
                        nextState
                    );
                } else {
                    // No explicit edge, just move to the node
                    nextState = currentState
                        .withData(command.getStateData().get())
                        .withContext(command.getContextUpdates().get())
                        .moveToNode(targetNodeId);

                    // Log a warning about missing edge
                    monitor.onWarning(
                        currentState.workflowId(),
                        "No explicit edge found for transition from " +
                        currentNodeId.value() +
                        " to " +
                        targetNodeId.value(),
                        nextState
                    );
                }

                // Update current state for next iteration
                currentState = nextState;
            } else {
                // No next node specified, but not complete or suspended
                WorkflowError error = SystemError.of(
                    "INVALID_COMMAND",
                    "Node " +
                    currentNodeId.value() +
                    " returned invalid command: " +
                    "not complete, not suspended, and no next node specified",
                    workflowName
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
    }

    /**
     * Finds an edge between two nodes.
     *
     * @param fromNodeId The source node ID
     * @param toNodeId The target node ID
     * @return Optional containing the edge if found
     */
    private Optional<GraphEdge> findEdgeBetween(
        NodeId fromNodeId,
        NodeId toNodeId
    ) {
        return edges
            .values()
            .stream()
            .filter(
                edge ->
                    edge.fromNode().equals(fromNodeId) &&
                    edge.toNode().equals(toNodeId)
            )
            .findFirst();
    }
}
