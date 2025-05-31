package dev.agents4j.api;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;

/**
 * Core interface for synchronous workflow execution.
 * Follows the Interface Segregation Principle by focusing only on execution concerns.
 *
 * @param <S> The type of the workflow state data
 * @param <O> The output type for the workflow
 */
public interface WorkflowExecutor<S, O> {
    /**
     * Starts a new workflow execution.
     *
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<S, O> start() throws WorkflowExecutionException;

    /**
     * Starts a new workflow execution with initial state data.
     *
     * @param initialStateData The initial state data
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<S, O> start(S initialStateData)
        throws WorkflowExecutionException;

    /**
     * Starts a new workflow execution with initial state.
     *
     * @param initialState The initial workflow state
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<S, O> start(WorkflowState<S> initialState)
        throws WorkflowExecutionException;

    /**
     * Resumes a suspended workflow execution.
     *
     * @param state The saved workflow state
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<S, O> resume(WorkflowState<S> state)
        throws WorkflowExecutionException;

    /**
     * Resumes a suspended workflow execution with updated state.
     *
     * @param state The saved workflow state with any updates
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<S, O> resumeWithUpdates(WorkflowState<S> state)
        throws WorkflowExecutionException;
}
