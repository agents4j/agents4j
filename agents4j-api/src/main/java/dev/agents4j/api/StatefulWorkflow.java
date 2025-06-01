package dev.agents4j.api;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.concurrent.CompletableFuture;

/**
 * Modern stateful workflow interface that directly uses type-safe components.
 * This replaces the old StatefulWorkflow interface with a fully modernized approach
 * that eliminates bridge adapters and provides compile-time safety throughout.
 *
 * @param <S> The type of the workflow state data
 * @param <O> The output type for the workflow
 */
public interface StatefulWorkflow<S, O> {
    /**
     * Gets the unique identifier for this workflow.
     *
     * @return The workflow identifier
     */
    String getWorkflowId();

    /**
     * Gets the human-readable name of this workflow.
     *
     * @return The workflow name
     */
    String getName();

    /**
     * Gets the version of this workflow.
     *
     * @return The workflow version
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Starts a new workflow execution with empty context.
     *
     * @return A WorkflowResult containing the execution outcome
     */
    default WorkflowResult<WorkflowExecution<S, O>, WorkflowError> start() {
        return start(null, WorkflowContext.empty());
    }

    /**
     * Starts a new workflow execution with initial state data.
     *
     * @param initialStateData The initial state data
     * @return A WorkflowResult containing the execution outcome
     */
    default WorkflowResult<WorkflowExecution<S, O>, WorkflowError> start(
        S initialStateData
    ) {
        return start(initialStateData, WorkflowContext.empty());
    }

    /**
     * Starts a new workflow execution with initial state data and context.
     *
     * @param initialStateData The initial state data
     * @param initialContext The initial workflow context
     * @return A WorkflowResult containing the execution outcome
     */
    WorkflowResult<WorkflowExecution<S, O>, WorkflowError> start(
        S initialStateData,
        WorkflowContext initialContext
    );

    /**
     * Resumes a suspended workflow execution.
     *
     * @param execution The suspended workflow execution
     * @return A WorkflowResult containing the execution outcome
     */
    WorkflowResult<WorkflowExecution<S, O>, WorkflowError> resume(
        WorkflowExecution<S, O> execution
    );

    /**
     * Resumes a suspended workflow execution with updated context.
     *
     * @param execution The suspended workflow execution
     * @param contextUpdates Additional context updates to apply
     * @return A WorkflowResult containing the execution outcome
     */
    WorkflowResult<WorkflowExecution<S, O>, WorkflowError> resume(
        WorkflowExecution<S, O> execution,
        WorkflowContext contextUpdates
    );

    /**
     * Starts a workflow execution asynchronously.
     *
     * @param initialStateData The initial state data
     * @param initialContext The initial workflow context
     * @return A CompletableFuture containing the execution result
     */
    default CompletableFuture<
        WorkflowResult<WorkflowExecution<S, O>, WorkflowError>
    > startAsync(S initialStateData, WorkflowContext initialContext) {
        return CompletableFuture.supplyAsync(() ->
            start(initialStateData, initialContext)
        );
    }

    /**
     * Resumes a workflow execution asynchronously.
     *
     * @param execution The suspended workflow execution
     * @return A CompletableFuture containing the execution result
     */
    default CompletableFuture<
        WorkflowResult<WorkflowExecution<S, O>, WorkflowError>
    > resumeAsync(WorkflowExecution<S, O> execution) {
        return CompletableFuture.supplyAsync(() -> resume(execution));
    }

    /**
     * Validates that the workflow can process the given state data type.
     *
     * @param stateData The state data to validate
     * @return A WorkflowResult indicating validation success or failure
     */
    default WorkflowResult<S, WorkflowError> validate(S stateData) {
        if (stateData == null) {
            return WorkflowResult.suspended(
                "validation-failed",
                null,
                "Workflow state data is required"
            );
        }
        return WorkflowResult.success(stateData);
    }

    /**
     * Gets metadata about this workflow for introspection and tooling.
     *
     * @return Workflow metadata
     */
    default WorkflowMetadata getMetadata() {
        return new WorkflowMetadata(
            getWorkflowId(),
            getName(),
            getVersion(),
            getSupportedStateType(),
            getSupportedOutputType()
        );
    }

    /**
     * Gets the supported state data type for this workflow.
     *
     * @return The Class representing the state type
     */
    Class<S> getSupportedStateType();

    /**
     * Gets the supported output type for this workflow.
     *
     * @return The Class representing the output type
     */
    Class<O> getSupportedOutputType();

    /**
     * Represents a workflow execution state that can be completed, suspended, or failed.
     * This replaces the old StatefulWorkflowResult with a more functional approach.
     *
     * @param <S> The type of the workflow state data
     * @param <O> The output type for the workflow
     */
    sealed interface WorkflowExecution<S, O>
        permits
            WorkflowExecution.Completed,
            WorkflowExecution.Suspended,
            WorkflowExecution.Failed {
        /**
         * Gets the current state data.
         *
         * @return The current state data
         */
        S getStateData();

        /**
         * Gets the current workflow context.
         *
         * @return The current context
         */
        WorkflowContext getContext();

        /**
         * Gets the workflow execution ID.
         *
         * @return The execution ID
         */
        String getExecutionId();

        /**
         * Gets the timestamp when this execution state was created.
         *
         * @return The timestamp
         */
        java.time.Instant getTimestamp();

        /**
         * Checks if this execution is completed.
         *
         * @return true if completed
         */
        default boolean isCompleted() {
            return this instanceof Completed;
        }

        /**
         * Checks if this execution is suspended.
         *
         * @return true if suspended
         */
        default boolean isSuspended() {
            return this instanceof Suspended;
        }

        /**
         * Checks if this execution has failed.
         *
         * @return true if failed
         */
        default boolean isFailed() {
            return this instanceof Failed;
        }

        /**
         * Represents a completed workflow execution.
         */
        record Completed<S, O>(
            S stateData,
            WorkflowContext context,
            String executionId,
            java.time.Instant timestamp,
            O output
        )
            implements WorkflowExecution<S, O> {
            public Completed {
                java.util.Objects.requireNonNull(
                    stateData,
                    "State data cannot be null"
                );
                java.util.Objects.requireNonNull(
                    context,
                    "Context cannot be null"
                );
                java.util.Objects.requireNonNull(
                    executionId,
                    "Execution ID cannot be null"
                );
                java.util.Objects.requireNonNull(
                    timestamp,
                    "Timestamp cannot be null"
                );
                java.util.Objects.requireNonNull(
                    output,
                    "Output cannot be null"
                );
            }

            @Override
            public S getStateData() {
                return stateData;
            }

            @Override
            public WorkflowContext getContext() {
                return context;
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public java.time.Instant getTimestamp() {
                return timestamp;
            }
        }

        /**
         * Represents a suspended workflow execution that can be resumed.
         */
        record Suspended<S, O>(
            S stateData,
            WorkflowContext context,
            String executionId,
            java.time.Instant timestamp,
            String suspensionId,
            String reason,
            java.util.Optional<java.time.Duration> timeout
        )
            implements WorkflowExecution<S, O> {
            public Suspended {
                java.util.Objects.requireNonNull(
                    stateData,
                    "State data cannot be null"
                );
                java.util.Objects.requireNonNull(
                    context,
                    "Context cannot be null"
                );
                java.util.Objects.requireNonNull(
                    executionId,
                    "Execution ID cannot be null"
                );
                java.util.Objects.requireNonNull(
                    timestamp,
                    "Timestamp cannot be null"
                );
                java.util.Objects.requireNonNull(
                    suspensionId,
                    "Suspension ID cannot be null"
                );
                java.util.Objects.requireNonNull(
                    reason,
                    "Suspension reason cannot be null"
                );
                java.util.Objects.requireNonNull(
                    timeout,
                    "Timeout optional cannot be null"
                );
            }

            @Override
            public S getStateData() {
                return stateData;
            }

            @Override
            public WorkflowContext getContext() {
                return context;
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public java.time.Instant getTimestamp() {
                return timestamp;
            }
        }

        /**
         * Represents a failed workflow execution.
         */
        record Failed<S, O>(
            S stateData,
            WorkflowContext context,
            String executionId,
            java.time.Instant timestamp,
            WorkflowError error,
            boolean isRecoverable
        )
            implements WorkflowExecution<S, O> {
            public Failed {
                java.util.Objects.requireNonNull(
                    stateData,
                    "State data cannot be null"
                );
                java.util.Objects.requireNonNull(
                    context,
                    "Context cannot be null"
                );
                java.util.Objects.requireNonNull(
                    executionId,
                    "Execution ID cannot be null"
                );
                java.util.Objects.requireNonNull(
                    timestamp,
                    "Timestamp cannot be null"
                );
                java.util.Objects.requireNonNull(error, "Error cannot be null");
            }

            @Override
            public S getStateData() {
                return stateData;
            }

            @Override
            public WorkflowContext getContext() {
                return context;
            }

            @Override
            public String getExecutionId() {
                return executionId;
            }

            @Override
            public java.time.Instant getTimestamp() {
                return timestamp;
            }
        }

        /**
         * Creates a completed execution.
         *
         * @param stateData The final state data
         * @param context The final context
         * @param executionId The execution ID
         * @param output The workflow output
         * @param <S> The state type
         * @param <O> The output type
         * @return A completed execution
         */
        static <S, O> WorkflowExecution<S, O> completed(
            S stateData,
            WorkflowContext context,
            String executionId,
            O output
        ) {
            return new Completed<>(
                stateData,
                context,
                executionId,
                java.time.Instant.now(),
                output
            );
        }

        /**
         * Creates a suspended execution.
         *
         * @param stateData The current state data
         * @param context The current context
         * @param executionId The execution ID
         * @param suspensionId The suspension ID
         * @param reason The suspension reason
         * @param <S> The state type
         * @param <O> The output type
         * @return A suspended execution
         */
        static <S, O> WorkflowExecution<S, O> suspended(
            S stateData,
            WorkflowContext context,
            String executionId,
            String suspensionId,
            String reason
        ) {
            return new Suspended<>(
                stateData,
                context,
                executionId,
                java.time.Instant.now(),
                suspensionId,
                reason,
                java.util.Optional.empty()
            );
        }

        /**
         * Creates a suspended execution with timeout.
         *
         * @param stateData The current state data
         * @param context The current context
         * @param executionId The execution ID
         * @param suspensionId The suspension ID
         * @param reason The suspension reason
         * @param timeout The suspension timeout
         * @param <S> The state type
         * @param <O> The output type
         * @return A suspended execution
         */
        static <S, O> WorkflowExecution<S, O> suspendedWithTimeout(
            S stateData,
            WorkflowContext context,
            String executionId,
            String suspensionId,
            String reason,
            java.time.Duration timeout
        ) {
            return new Suspended<>(
                stateData,
                context,
                executionId,
                java.time.Instant.now(),
                suspensionId,
                reason,
                java.util.Optional.of(timeout)
            );
        }

        /**
         * Creates a failed execution.
         *
         * @param stateData The current state data
         * @param context The current context
         * @param executionId The execution ID
         * @param error The workflow error
         * @param <S> The state type
         * @param <O> The output type
         * @return A failed execution
         */
        static <S, O> WorkflowExecution<S, O> failed(
            S stateData,
            WorkflowContext context,
            String executionId,
            WorkflowError error
        ) {
            return new Failed<>(
                stateData,
                context,
                executionId,
                java.time.Instant.now(),
                error,
                error.isRecoverable()
            );
        }
    }

    /**
     * Metadata about a workflow for introspection and tooling.
     */
    record WorkflowMetadata(
        String workflowId,
        String name,
        String version,
        Class<?> stateType,
        Class<?> outputType
    ) {
        public WorkflowMetadata {
            java.util.Objects.requireNonNull(
                workflowId,
                "Workflow ID cannot be null"
            );
            java.util.Objects.requireNonNull(
                name,
                "Workflow name cannot be null"
            );
            java.util.Objects.requireNonNull(
                version,
                "Workflow version cannot be null"
            );
            java.util.Objects.requireNonNull(
                stateType,
                "State type cannot be null"
            );
            java.util.Objects.requireNonNull(
                outputType,
                "Output type cannot be null"
            );
        }
    }
}
