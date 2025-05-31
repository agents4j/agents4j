package dev.agents4j.api;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Core interface for synchronous workflow execution.
 * Follows the Interface Segregation Principle by focusing only on execution concerns.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface WorkflowExecutor<I, O> {
    
    /**
     * Starts a new workflow execution with the given input.
     *
     * @param input The initial input
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input) throws WorkflowExecutionException;
    
    /**
     * Starts a new workflow execution with input and context.
     *
     * @param input The initial input
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input, Map<String, Object> context) throws WorkflowExecutionException;
    
    /**
     * Starts a new workflow execution with initial state.
     *
     * @param input The initial input
     * @param initialState The initial workflow state
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> start(I input, WorkflowState initialState, Map<String, Object> context) 
            throws WorkflowExecutionException;
    
    /**
     * Resumes a suspended workflow execution.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> resume(I input, WorkflowState state) throws WorkflowExecutionException;
    
    /**
     * Resumes a suspended workflow execution with context.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @param context Additional context
     * @return The workflow execution result
     * @throws WorkflowExecutionException if execution fails
     */
    StatefulWorkflowResult<O> resume(I input, WorkflowState state, Map<String, Object> context) 
            throws WorkflowExecutionException;
}