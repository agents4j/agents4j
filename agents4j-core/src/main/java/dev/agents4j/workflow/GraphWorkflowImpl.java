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
import dev.agents4j.api.serialization.WorkflowStateSerializer;
import dev.agents4j.api.suspension.ResumeOptions;
import dev.agents4j.api.suspension.WorkflowSuspension;
import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.config.WorkflowConfiguration;
import dev.agents4j.workflow.context.WorkflowContextKeys;
import dev.agents4j.workflow.execution.GraphWorkflowExecutor;
import dev.agents4j.workflow.monitor.WorkflowMonitor;
import dev.agents4j.workflow.output.OutputExtractor;
import dev.agents4j.workflow.serialization.JsonGraphWorkflowStateSerializer;
import dev.agents4j.workflow.validation.GraphWorkflowValidator;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Enhanced implementation of GraphWorkflow using the modern type-safe
 * workflow architecture. Provides type-safe navigation, immutable state
 * management, comprehensive error handling, and enhanced capabilities.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class GraphWorkflowImpl<I, O> implements GraphWorkflow<I, O> {

    private static final Logger LOGGER = Logger.getLogger(
        GraphWorkflowImpl.class.getName()
    );

    private final GraphWorkflowExecutor<I, O> executor;
    private final GraphWorkflowValidator<I> validator;
    private final String name;
    private final String version;
    private final Class<I> inputType;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes;
    private final Map<EdgeId, GraphEdge> edges;
    private final Set<NodeId> entryPointIds;
    private final NodeId defaultEntryPointId;
    private final WorkflowMonitor monitor;
    private final Executor asyncExecutor;
    private WorkflowStateSerializer<
        GraphWorkflowState<I>
    > stateSerializer;

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
     * @param inputType The input type class for type safety
     */
    public GraphWorkflowImpl(
        String name,
        String version,
        Class<I> inputType,
        Map<NodeId, GraphWorkflowNode<I>> nodes,
        Map<EdgeId, GraphEdge> edges,
        Set<NodeId> entryPointIds,
        NodeId defaultEntryPointId,
        OutputExtractor<I, O> outputExtractor,
        WorkflowConfiguration configuration,
        WorkflowMonitor monitor,
        Executor asyncExecutor
    ) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.version = Objects.requireNonNull(
            version,
            "Version cannot be null"
        );
        this.inputType = Objects.requireNonNull(
            inputType,
            "Input type cannot be null"
        );
        this.nodes = Objects.requireNonNull(nodes, "Nodes cannot be null");
        this.edges = Objects.requireNonNull(edges, "Edges cannot be null");
        this.entryPointIds = Objects.requireNonNull(
            entryPointIds,
            "Entry point IDs cannot be null"
        );
        this.defaultEntryPointId = defaultEntryPointId;
        this.monitor = Objects.requireNonNull(
            monitor,
            "Monitor cannot be null"
        );
        this.asyncExecutor = Objects.requireNonNull(
            asyncExecutor,
            "Async executor cannot be null"
        );

        // Initialize components
        this.executor = new GraphWorkflowExecutor<>(
            name,
            nodes,
            edges,
            outputExtractor,
            configuration,
            monitor
        );

        this.validator = new GraphWorkflowValidator<>(
            name,
            nodes,
            edges,
            entryPointIds,
            defaultEntryPointId,
            outputExtractor
        );

        this.stateSerializer = new JsonGraphWorkflowStateSerializer<>(
            inputType,
            version
        );
    }

    /**
     * Creates a new enhanced workflow instance with custom serializer.
     */
    public GraphWorkflowImpl(
        String name,
        String version,
        Class<I> inputType,
        Map<NodeId, GraphWorkflowNode<I>> nodes,
        Map<EdgeId, GraphEdge> edges,
        Set<NodeId> entryPointIds,
        NodeId defaultEntryPointId,
        OutputExtractor<I, O> outputExtractor,
        WorkflowConfiguration configuration,
        WorkflowMonitor monitor,
        Executor asyncExecutor,
        WorkflowStateSerializer<GraphWorkflowState<I>> customSerializer
    ) {
        this(
            name,
            version,
            inputType,
            nodes,
            edges,
            entryPointIds,
            defaultEntryPointId,
            outputExtractor,
            configuration,
            monitor,
            asyncExecutor
        );
        
        this.stateSerializer = Objects.requireNonNull(
            customSerializer,
            "Custom serializer cannot be null"
        );
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public WorkflowStateSerializer<GraphWorkflowState<I>> getStateSerializer() {
        return stateSerializer;
    }

    /**
     * Gets a node by its ID.
     */
    public GraphWorkflowNode<I> getNode(NodeId nodeId) {
        return nodes.get(nodeId);
    }

    @Override
    public Map<NodeId, GraphWorkflowNode<I>> getNodes() {
        return Map.copyOf(nodes);
    }

    @Override
    public Map<EdgeId, GraphEdge> getEdges() {
        return Map.copyOf(edges);
    }

    @Override
    public Set<NodeId> getEntryPoints() {
        return Set.copyOf(entryPointIds);
    }

    @Override
    public Set<GraphEdge> getEdgesFrom(NodeId nodeId) {
        return edges
            .values()
            .stream()
            .filter(edge -> edge.fromNode().equals(nodeId))
            .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public StateValidationResult validateState(GraphWorkflowState<I> state) {
        if (state == null) {
            return StateValidationResult.invalid("State cannot be null");
        }

        // Check workflow ID compatibility
        if (!getName().equals(state.workflowId().value())) {
            return StateValidationResult.invalid(
                "Workflow ID mismatch: expected " +
                getName() +
                ", got " +
                state.workflowId().value()
            );
        }

        // Check if current node exists
        if (state.currentNode().isPresent()) {
            NodeId nodeId = state.currentNode().get();
            if (getNode(nodeId) == null) {
                return StateValidationResult.invalid(
                    "Unknown node: " + nodeId.value()
                );
            }
        }

        // Validate state data type
        if (state.data() != null && !inputType.isInstance(state.data())) {
            return StateValidationResult.invalid(
                "State data type mismatch: expected " +
                inputType.getName() +
                ", got " +
                state.data().getClass().getName()
            );
        }

        // Check for cycles if configured
        if (state.hasCycle()) {
            return StateValidationResult.invalid(
                "Cycle detected in workflow path"
            );
        }

        return StateValidationResult.valid();
    }

    @Override
    public WorkflowResult<O, WorkflowError> start(I input) {
        return start(input, WorkflowContext.empty());
    }

    @Override
    public WorkflowResult<O, WorkflowError> start(
        I input,
        WorkflowContext context
    ) {
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
            var result = executor.executeWorkflow(initialState);
            // If suspended, wrap in type-safe suspension
            if (result.isSuspended()) {
                Optional<
                    WorkflowSuspension<GraphWorkflowState<I>>
                > typedSuspension = extractTypedSuspension(result);

                if (typedSuspension.isPresent()) {
                    return createEnhancedSuspendedResult(typedSuspension.get());
                }
            }
            return result;
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
     * Creates a type-safe suspension result that preserves the suspension information
     * in a more accessible way while maintaining compatibility.
     */
    private WorkflowResult<O, WorkflowError> createEnhancedSuspendedResult(
        WorkflowSuspension<GraphWorkflowState<I>> suspension
    ) {
        return WorkflowResult.suspended(
            suspension.getSuspensionId(),
            suspension.getSuspendedState(),
            suspension.getReason()
        );
    }

    @Override
    public WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<I> state
    ) {
        // Issue deprecation warning for missing validation
        warnMissingValidation(getName());
        // Use enhanced API with safe defaults
        ResumeOptions options = ResumeOptions.development();
        return resumeWithOptions(state, options);
    }

    @Override
    public WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates
    ) {
        // Use enhanced API with safe defaults
        ResumeOptions options = ResumeOptions.development();
        return resumeWithOptions(state, contextUpdates, options);
    }

    /**
     * Resumes workflow with options.
     */
    public WorkflowResult<O, WorkflowError> resumeWithOptions(
        GraphWorkflowState<I> state,
        ResumeOptions options
    ) {
        return resumeWithOptions(state, WorkflowContext.empty(), options);
    }

    /**
     * Resumes workflow with context updates and options.
     */
    public WorkflowResult<O, WorkflowError> resumeWithOptions(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates,
        ResumeOptions options
    ) {
        if (options.shouldValidateState()) {
            StateValidationResult validation = validateState(state);
            if (!validation.isValid()) {
                return WorkflowResult.failure(
                    createValidationError(validation.getMessage())
                );
            }
        }

        // Merge context if provided
        GraphWorkflowState<I> updatedState = state;
        if (contextUpdates != null && !contextUpdates.isEmpty()) {
            WorkflowContext mergedContext = state
                .context()
                .merge(contextUpdates);
            updatedState = state.withContext(mergedContext);
        }

        monitor.onWorkflowResumed(state.workflowId(), updatedState);

        try {
            return executor.executeWorkflow(updatedState);
        } catch (Exception e) {
            WorkflowError error = SystemError.of(
                "WORKFLOW_RESUME_ERROR",
                "Unexpected error during workflow resume: " + e.getMessage(),
                name
            );
            monitor.onWorkflowError(state.workflowId(), error, updatedState, e);
            return WorkflowResult.failure(error);
        }
    }

    @Override
    public CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        I input
    ) {
        return CompletableFuture.supplyAsync(() -> start(input), asyncExecutor);
    }

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

    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state
    ) {
        return resumeWithOptionsAsync(state, ResumeOptions.development());
    }

    public CompletableFuture<WorkflowResult<O, WorkflowError>> resumeAsync(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates
    ) {
        return resumeWithOptionsAsync(
            state,
            contextUpdates,
            ResumeOptions.development()
        );
    }

    /**
     * Async resume with options.
     */
    public CompletableFuture<
        WorkflowResult<O, WorkflowError>
    > resumeWithOptionsAsync(
        GraphWorkflowState<I> state,
        ResumeOptions options
    ) {
        return CompletableFuture.supplyAsync(
            () -> resumeWithOptions(state, options),
            asyncExecutor
        );
    }

    /**
     * Async resume with context and options.
     */
    public CompletableFuture<
        WorkflowResult<O, WorkflowError>
    > resumeWithOptionsAsync(
        GraphWorkflowState<I> state,
        WorkflowContext contextUpdates,
        ResumeOptions options
    ) {
        return CompletableFuture.supplyAsync(
            () -> resumeWithOptions(state, contextUpdates, options),
            asyncExecutor
        );
    }

    /**
     * Extracts type-safe suspension with proper error handling.
     */
    @Override
    public Optional<
        WorkflowSuspension<GraphWorkflowState<I>>
    > extractTypedSuspension(WorkflowResult<O, WorkflowError> result) {
        if (!result.isSuspended()) {
            return Optional.empty();
        }

        try {
            var suspension = result.getSuspension().orElseThrow();

            // Check if suspension state is already a WorkflowSuspension
            if (
                suspension.suspensionState() instanceof
                WorkflowSuspension<?> existingSuspension
            ) {
                @SuppressWarnings("unchecked")
                WorkflowSuspension<GraphWorkflowState<I>> typedSuspension =
                    (WorkflowSuspension<
                            GraphWorkflowState<I>
                        >) existingSuspension;
                return Optional.of(typedSuspension);
            }

            // Otherwise, extract from raw suspension state
            @SuppressWarnings("unchecked")
            GraphWorkflowState<I> state = (GraphWorkflowState<
                    I
                >) suspension.suspensionState();

            return Optional.of(
                WorkflowSuspension.of(
                    suspension.suspensionId(),
                    state,
                    suspension.reason(),
                    getVersion()
                )
            );
        } catch (ClassCastException e) {
            // Log error but don't fail completely
            LOGGER.warning(
                "Failed to extract typed suspension: " + e.getMessage()
            );
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.warning(
                "Unexpected error extracting typed suspension: " +
                e.getMessage()
            );
            return Optional.empty();
        }
    }

    /**
     * Enhanced validation that includes version checking.
     */
    public ValidationResult validate() {
        ValidationResult baseResult = validator.validate();

        if (!baseResult.isValid()) {
            return baseResult;
        }

        // Additional enhanced validations
        try {
            // Test serializer
            Map<String, Object> metadata = stateSerializer.getMetadata();
            if (metadata.isEmpty()) {
                return ValidationResult.failure("Serializer metadata is empty");
            }

            // Validate version format
            if (version == null || version.trim().isEmpty()) {
                return ValidationResult.failure(
                    "Workflow version cannot be null or empty"
                );
            }

            // Validate input type
            if (inputType == null) {
                return ValidationResult.failure("Input type cannot be null");
            }

            return ValidationResult.success();
        } catch (Exception e) {
            return ValidationResult.failure(
                "Enhanced validation failed: " + e.getMessage()
            );
        }
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
     * Creates validation error.
     */
    private WorkflowError createValidationError(String message) {
        return SystemError.of(
            "VALIDATION_ERROR",
            "State validation failed: " + message,
            name
        );
    }

    /**
     * Simple warning for missing validation (placeholder for MigrationWarnings).
     */
    private void warnMissingValidation(String workflowName) {
        LOGGER.warning(
            "DEPRECATION WARNING: No state validation performed for workflow: " +
            workflowName
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
