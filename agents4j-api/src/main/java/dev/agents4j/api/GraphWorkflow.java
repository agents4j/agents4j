package dev.agents4j.api;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.context.*;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.result.error.ValidationError;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.serialization.*;
import dev.agents4j.api.suspension.*;
import dev.agents4j.api.validation.*;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for graph-based workflow implementations.
 *
 * - Type-safe suspension results
 * - Built-in serialization support
 * - State validation before resume
 * - Configurable resume options
 * - Version-aware operations
 *
 * @param <S> The input state type for the workflow
 * @param <O> The output type for the workflow
 */
public interface GraphWorkflow<S, O> {
    /**
     * Starts a new workflow execution with the given input and empty context.
     *
     * @param input The workflow input
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(S input);

    /**
     * Starts a new workflow execution with the given input and context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(S input, WorkflowContext context);

    /**
     * Starts a new workflow execution with the given ID, input and context.
     *
     * @param workflowId The workflow ID to use
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return Result of the workflow execution
     */
    WorkflowResult<O, WorkflowError> start(
        WorkflowId workflowId,
        S input,
        WorkflowContext context
    );

    /**
     * Resumes a suspended workflow execution with the given state.
     *
     * @param state The suspended workflow state to resume
     * @return Result of the resumed workflow execution
     */
    WorkflowResult<O, WorkflowError> resume(GraphWorkflowState<S> state);

    /**
     * Resumes a suspended workflow execution with the given state and context updates.
     *
     * @param state The suspended workflow state to resume
     * @param contextUpdates Additional context updates to apply
     * @return Result of the resumed workflow execution
     */
    WorkflowResult<O, WorkflowError> resume(
        GraphWorkflowState<S> state,
        WorkflowContext contextUpdates
    );

    /**
     * Starts the workflow execution asynchronously.
     *
     * @param input The workflow input
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(S input);

    /**
     * Starts the workflow execution asynchronously with the given context.
     *
     * @param input The workflow input
     * @param context Initial context for the workflow
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<WorkflowResult<O, WorkflowError>> startAsync(
        S input,
        WorkflowContext context
    );

    /**
     * Gets the workflow version for compatibility checking.
     *
     * @return The current workflow version
     */
    String getVersion();

    /**
     * Gets the state serializer for this workflow.
     *
     * @return The workflow state serializer
     */
    WorkflowStateSerializer<GraphWorkflowState<S>> getStateSerializer();

    /**
     * Creates a type-safe suspension for the given state.
     *
     * @param state The workflow state to suspend
     * @param reason The reason for suspension
     * @return A type-safe WorkflowSuspension
     */
    default WorkflowSuspension<GraphWorkflowState<S>> createSuspension(
        GraphWorkflowState<S> state,
        String reason
    ) {
        return WorkflowSuspension.of(
            java.util.UUID.randomUUID().toString(),
            state,
            reason,
            getVersion()
        );
    }

    /**
     * Creates a type-safe suspension with timeout.
     *
     * @param state The workflow state to suspend
     * @param reason The reason for suspension
     * @param timeout The suspension timeout
     * @return A type-safe WorkflowSuspension
     */
    default WorkflowSuspension<
        GraphWorkflowState<S>
    > createSuspensionWithTimeout(
        GraphWorkflowState<S> state,
        String reason,
        java.time.Duration timeout
    ) {
        return WorkflowSuspension.withTimeout(
            java.util.UUID.randomUUID().toString(),
            state,
            reason,
            timeout,
            getVersion()
        );
    }

    /**
     * Creates a type-safe suspended WorkflowResult.
     *
     * @param state The workflow state to suspend
     * @param reason The reason for suspension
     * @return A type-safe suspended WorkflowResult
     */
    default WorkflowResult<O, WorkflowError> suspendWorkflow(
        GraphWorkflowState<S> state,
        String reason
    ) {
        var suspension = createSuspension(state, reason);
        return WorkflowResult.suspended(suspension);
    }

    /**
     * Creates a type-safe suspended WorkflowResult with timeout.
     *
     * @param state The workflow state to suspend
     * @param reason The reason for suspension
     * @param timeout The suspension timeout
     * @return A type-safe suspended WorkflowResult
     */
    default WorkflowResult<O, WorkflowError> suspendWorkflowWithTimeout(
        GraphWorkflowState<S> state,
        String reason,
        java.time.Duration timeout
    ) {
        var suspension = createSuspensionWithTimeout(state, reason, timeout);
        return WorkflowResult.suspended(suspension);
    }

    /**
     * Extracts a type-safe suspension from a WorkflowResult.
     *
     * @param result The workflow result that may contain a suspension
     * @return Optional type-safe suspension
     */
    default Optional<
        WorkflowSuspension<GraphWorkflowState<S>>
    > extractTypedSuspension(WorkflowResult<O, WorkflowError> result) {
        if (!result.isSuspended()) {
            return Optional.empty();
        }

        try {
            var suspension = result.getSuspension().orElseThrow();
            @SuppressWarnings("unchecked")
            GraphWorkflowState<S> state = (GraphWorkflowState<
                    S
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
            return Optional.empty();
        }
    }

    /**
     * Serializes a workflow state to string representation.
     *
     * @param state The state to serialize
     * @return Serialized state string
     * @throws WorkflowStateSerializer.SerializationException if serialization fails
     */
    default String serializeState(GraphWorkflowState<S> state)
        throws WorkflowStateSerializer.SerializationException {
        return getStateSerializer().serialize(state, getVersion());
    }

    /**
     * Deserializes a workflow state from string representation.
     *
     * @param serializedState The serialized state
     * @return Deserialized workflow state
     * @throws WorkflowStateSerializer.DeserializationException if deserialization fails
     * @throws WorkflowStateSerializer.VersionMismatchException if version is incompatible
     */
    default GraphWorkflowState<S> deserializeState(String serializedState)
        throws WorkflowStateSerializer.DeserializationException, WorkflowStateSerializer.VersionMismatchException {
        return getStateSerializer().deserialize(serializedState, getVersion());
    }

    /**
     * Validates a workflow state for compatibility with this workflow.
     *
     * @param state The state to validate
     * @return Validation result
     */
    default StateValidationResult validateState(GraphWorkflowState<S> state) {
        // Basic validation - subclasses can override for more sophisticated checks
        if (state == null) {
            return StateValidationResult.invalid("State cannot be null");
        }

        if (state.currentNode().isPresent()) {
            var nodeId = state.currentNode().get();
            if (getNode(nodeId) == null) {
                return StateValidationResult.invalid(
                    "Unknown node: " + nodeId.value()
                );
            }
        }

        return StateValidationResult.valid();
    }

    /**
     * Resumes a workflow with enhanced options and validation.
     *
     * @param state The suspended workflow state
     * @param options Resume options for controlling behavior
     * @return Result of the resumed workflow execution
     */
    default WorkflowResult<O, WorkflowError> resumeWithOptions(
        GraphWorkflowState<S> state,
        ResumeOptions options
    ) {
        // Validate state if requested
        if (options.shouldValidateState()) {
            StateValidationResult validation = validateState(state);
            if (!validation.isValid()) {
                return WorkflowResult.failure(
                    createValidationError(validation.getMessage())
                );
            }
        }

        // Use existing resume method - enhanced implementations can override
        return resume(state);
    }

    /**
     * Resumes a workflow with enhanced options, validation, and context merging.
     *
     * @param state The suspended workflow state
     * @param contextUpdates Additional context updates
     * @param options Resume options for controlling behavior
     * @return Result of the resumed workflow execution
     */
    default WorkflowResult<O, WorkflowError> resumeWithOptions(
        GraphWorkflowState<S> state,
        WorkflowContext contextUpdates,
        ResumeOptions options
    ) {
        // Validate state if requested
        if (options.shouldValidateState()) {
            StateValidationResult validation = validateState(state);
            if (!validation.isValid()) {
                return WorkflowResult.failure(
                    createValidationError(validation.getMessage())
                );
            }
        }

        // Merge contexts according to strategy
        ContextMergeStrategy.ContextMergeResult mergeResult = options
            .getContextMergeStrategy()
            .merge(state.context(), contextUpdates);

        // Check for conflicts if configured to fail on them
        if (
            options.shouldFailOnConflicts() &&
            mergeResult.hasUnresolvedConflicts()
        ) {
            return WorkflowResult.failure(
                createContextConflictError(mergeResult.getConflicts())
            );
        }

        // Create new state with merged context
        GraphWorkflowState<S> mergedState = state.withContext(
            mergeResult.getMergedContext()
        );

        // Use existing resume method
        return resume(mergedState);
    }

    /**
     * Resumes a workflow from serialized state with full validation and options.
     *
     * @param serializedState The serialized workflow state
     * @param workflowVersion The version of the serialized state
     * @param options Resume options
     * @return Result of the resumed workflow execution
     */
    default WorkflowResult<O, WorkflowError> resumeFromSerialized(
        String serializedState,
        String workflowVersion,
        ResumeOptions options
    ) {
        try {
            // Check version compatibility
            if (
                !options.shouldAllowVersionMismatch() &&
                !getStateSerializer()
                    .isCompatible(workflowVersion, getVersion())
            ) {
                return WorkflowResult.failure(
                    createVersionMismatchError(workflowVersion, getVersion())
                );
            }

            // Attempt migration if needed and enabled
            String stateToDeserialize = serializedState;
            if (
                options.shouldEnableMigration() &&
                !workflowVersion.equals(getVersion())
            ) {
                Optional<String> migrated = getStateSerializer()
                    .migrate(serializedState, workflowVersion, getVersion());
                if (migrated.isPresent()) {
                    stateToDeserialize = migrated.get();
                } else if (!options.shouldAllowVersionMismatch()) {
                    return WorkflowResult.failure(
                        createMigrationFailedError(
                            workflowVersion,
                            getVersion()
                        )
                    );
                }
            }

            // Deserialize state
            GraphWorkflowState<S> state = getStateSerializer()
                .deserialize(stateToDeserialize, getVersion());

            // Resume with options
            return resumeWithOptions(state, options);
        } catch (WorkflowStateSerializer.DeserializationException e) {
            return WorkflowResult.failure(createDeserializationError(e));
        } catch (WorkflowStateSerializer.VersionMismatchException e) {
            return WorkflowResult.failure(createVersionMismatchError(e));
        } catch (Exception e) {
            return WorkflowResult.failure(createGenericError(e));
        }
    }

    /**
     * Asynchronously resumes a workflow with enhanced options.
     *
     * @param state The suspended workflow state
     * @param options Resume options
     * @return CompletableFuture with the workflow result
     */
    default CompletableFuture<
        WorkflowResult<O, WorkflowError>
    > resumeWithOptionsAsync(
        GraphWorkflowState<S> state,
        ResumeOptions options
    ) {
        return CompletableFuture.supplyAsync(() ->
            resumeWithOptions(state, options)
        );
    }

    /**
     * Asynchronously resumes a workflow with enhanced options and context updates.
     *
     * @param state The suspended workflow state
     * @param contextUpdates Additional context updates
     * @param options Resume options
     * @return CompletableFuture with the workflow result
     */
    default CompletableFuture<
        WorkflowResult<O, WorkflowError>
    > resumeWithOptionsAsync(
        GraphWorkflowState<S> state,
        WorkflowContext contextUpdates,
        ResumeOptions options
    ) {
        return CompletableFuture.supplyAsync(() ->
            resumeWithOptions(state, contextUpdates, options)
        );
    }

    // Helper methods for creating specific error types

    private WorkflowError createValidationError(String message) {
        return ValidationError.of(
            "VALIDATION_ERROR",
            message,
            "state",
            "unknown"
        );
    }

    private WorkflowError createContextConflictError(
        java.util.List<ContextMergeStrategy.ContextConflict> conflicts
    ) {
        String message =
            "Context conflicts detected: " + conflicts.size() + " conflicts";
        return ValidationError.of(
            "CONTEXT_CONFLICT",
            message,
            "state",
            "unknown"
        );
    }

    private WorkflowError createVersionMismatchError(
        String serializedVersion,
        String currentVersion
    ) {
        String message = String.format(
            "Version mismatch: serialized=%s, current=%s",
            serializedVersion,
            currentVersion
        );
        return ValidationError.of(
            "VERSION_MISMATCH",
            message,
            "state",
            "unknown"
        );
    }

    private WorkflowError createVersionMismatchError(
        WorkflowStateSerializer.VersionMismatchException e
    ) {
        return ValidationError.of(
            "VERSION_MISMATCH",
            e.getMessage(),
            "state",
            "unknown"
        );
    }

    private WorkflowError createMigrationFailedError(
        String fromVersion,
        String toVersion
    ) {
        String message = String.format(
            "Migration failed: %s -> %s",
            fromVersion,
            toVersion
        );
        return ValidationError.of(
            "MIGRATION_FAILED",
            message,
            "state",
            "unknown"
        );
    }

    private WorkflowError createDeserializationError(
        WorkflowStateSerializer.DeserializationException e
    ) {
        return ValidationError.of(
            "DESERIALIZATION_ERROR",
            e.getMessage(),
            "state",
            "unknown"
        );
    }

    private WorkflowError createGenericError(Exception e) {
        return ValidationError.of(
            "INTERNAL_ERROR",
            e.getMessage(),
            "state",
            "unknown"
        );
    }

    /**
     * Result of state validation.
     */
    record StateValidationResult(boolean _valid, String message) {
        public static StateValidationResult valid() {
            return new StateValidationResult(true, "Valid");
        }

        public static StateValidationResult invalid(String message) {
            return new StateValidationResult(false, message);
        }

        public boolean isValid() {
            return _valid;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Gets the name of this workflow.
     *
     * @return The workflow name
     */
    String getName();

    /**
     * Gets all nodes in this workflow.
     *
     * @return Map of node IDs to nodes
     */
    Map<NodeId, GraphWorkflowNode<S>> getNodes();

    /**
     * Gets all edges in this workflow.
     *
     * @return Map of edge IDs to edges
     */
    Map<EdgeId, GraphEdge> getEdges();

    /**
     * Gets the node with the specified ID.
     *
     * @param nodeId The node ID
     * @return The node, or null if not found
     */
    GraphWorkflowNode<S> getNode(NodeId nodeId);

    /**
     * Gets all edges originating from the specified node.
     *
     * @param nodeId The source node ID
     * @return Set of edges from the node
     */
    Set<GraphEdge> getEdgesFrom(NodeId nodeId);

    /**
     * Gets all entry points for this workflow.
     *
     * @return Set of entry point node IDs
     */
    Set<NodeId> getEntryPoints();

    /**
     * Validates the workflow structure and configuration.
     *
     * @return The validation result
     */
    ValidationResult validate();
}
