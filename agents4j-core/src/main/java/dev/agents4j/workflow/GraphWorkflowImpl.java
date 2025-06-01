package dev.agents4j.workflow;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.SystemError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.config.WorkflowConfiguration;
import dev.agents4j.workflow.context.WorkflowContextKeys;
import dev.agents4j.workflow.execution.GraphWorkflowExecutor;
import dev.agents4j.workflow.monitor.WorkflowMonitor;
import dev.agents4j.workflow.output.OutputExtractor;
import dev.agents4j.workflow.validation.GraphWorkflowValidator;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    
    // Component instances
    private final GraphWorkflowExecutor<I, O> executor;
    private final GraphWorkflowValidator<I> validator;

    /**
     * Creates a new workflow instance with the given components.
     * 
     * @param name The workflow name
     * @param nodes The workflow nodes
     * @param edges The workflow edges
     * @param entryPointIds The entry point node IDs
     * @param defaultEntryPointId The default entry point node ID
     * @param outputExtractor The output extractor
     * @param configuration The workflow configuration
     * @param monitor The workflow monitor
     * @param asyncExecutor The executor for async operations
     */
    public GraphWorkflowImpl(
        String name,
        Map<NodeId, GraphWorkflowNode<I>> nodes,
        Map<EdgeId, GraphEdge> edges,
        Set<NodeId> entryPointIds,
        NodeId defaultEntryPointId,
        OutputExtractor<I, O> outputExtractor,
        WorkflowConfiguration configuration,
        WorkflowMonitor monitor,
        Executor asyncExecutor
    ) {
        this.name = name;
        this.nodes = nodes;
        this.edges = edges;
        this.entryPointIds = entryPointIds;
        this.defaultEntryPointId = defaultEntryPointId;
        this.outputExtractor = outputExtractor;
        this.configuration = configuration;
        this.monitor = monitor;
        this.asyncExecutor = asyncExecutor;
        
        // Initialize components
        this.executor = new GraphWorkflowExecutor<>(
            name, nodes, edges, outputExtractor, configuration, monitor
        );
        
        this.validator = new GraphWorkflowValidator<>(
            name, nodes, edges, entryPointIds, defaultEntryPointId, outputExtractor
        );
    }

    @Override
    public WorkflowResult<O, WorkflowError> start(I input) {
        return start(input, WorkflowContext.empty());
    }

    @Override
    public WorkflowResult<O, WorkflowError> start(I input, WorkflowContext context) {
        // Generate workflow ID
        WorkflowId workflowId = WorkflowId.generate();
        return start(workflowId, input, context);
    }

    @Override
    public WorkflowResult<O, WorkflowError> start(
        WorkflowId workflowId, 
        I input, 
        WorkflowContext context
    ) {
        // Create initial context if not provided
        WorkflowContext initialContext = context != null ? context : WorkflowContext.empty();
        
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
            return executor.executeWorkflow(initialState);
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

    @Override
    public WorkflowResult<O, WorkflowError> resume(GraphWorkflowState<I> state) {
        return resume(state, WorkflowContext.empty());
    }

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
            updatedState.context()
                .with(WorkflowContextKeys.WORKFLOW_RESUMED_TIME, Instant.now())
                .with(WorkflowContextKeys.WORKFLOW_RESUMED_COUNT, 
                      updatedState.getContextOrDefault(WorkflowContextKeys.WORKFLOW_RESUMED_COUNT, 0) + 1)
        );
        
        monitor.onWorkflowResumed(state.workflowId(), updatedState);
        
        try {
            return executor.executeWorkflow(updatedState);
        } catch (Exception e) {
            WorkflowError error = SystemError.of(
                "WORKFLOW_RESUME_ERROR",
                "Unexpected error during workflow resumption: " + e.getMessage(),
                name
            );
            monitor.onWorkflowError(state.workflowId(), error, updatedState, e);
            return WorkflowResult.failure(error);
        }
    }

    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(I input) {
        return CompletableFuture.supplyAsync(() -> start(input), asyncExecutor);
    }

    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        I input, 
        WorkflowContext context
    ) {
        return CompletableFuture.supplyAsync(() -> start(input, context), asyncExecutor);
    }

    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state
    ) {
        return CompletableFuture.supplyAsync(() -> resume(state), asyncExecutor);
    }

    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state, 
        WorkflowContext contextUpdates
    ) {
        return CompletableFuture.supplyAsync(() -> resume(state, contextUpdates), asyncExecutor);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<NodeId, GraphWorkflowNode<I>> getNodes() {
        return nodes;
    }

    @Override
    public Map<EdgeId, GraphEdge> getEdges() {
        return edges;
    }

    @Override
    public GraphWorkflowNode<I> getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public Set<GraphEdge> getEdgesFrom(NodeId nodeId) {
        return edges.values().stream()
            .filter(edge -> edge.fromNode().equals(nodeId))
            .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<NodeId> getEntryPoints() {
        return entryPointIds;
    }

    @Override
    public ValidationResult validate() {
        return validator.validate();
    }

    /**
     * Determines the entry point for a workflow execution.
     * 
     * @param input The workflow input
     * @param context The workflow context
     * @return The entry point node ID
     */
    private NodeId determineEntryPoint(I input, WorkflowContext context) {
        // If there's only one entry point, use that
        if (entryPointIds.size() == 1) {
            return entryPointIds.iterator().next();
        }
        
        // Otherwise, use the default entry point
        if (defaultEntryPointId != null) {
            return defaultEntryPointId;
        }
        
        // This should not happen if validation is done, but just in case
        throw new IllegalStateException(
            "No entry point available for workflow: " + name
        );
    }

    /**
     * Creates a new builder for GraphWorkflowImpl.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new builder
     */
    public static <I, O> GraphWorkflowBuilder<I, O> builder() {
        return new GraphWorkflowBuilder<>();
    }
}