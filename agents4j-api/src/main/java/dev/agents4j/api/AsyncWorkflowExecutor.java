package dev.agents4j.api;

import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for asynchronous workflow execution.
 * Separated from synchronous execution following ISP.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface AsyncWorkflowExecutor<I, O> {
    
    /**
     * Starts workflow execution asynchronously.
     *
     * @param input The initial input
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input);
    
    /**
     * Starts workflow execution asynchronously with context.
     *
     * @param input The initial input
     * @param context Additional context
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> startAsync(I input, Map<String, Object> context);
    
    /**
     * Resumes workflow execution asynchronously.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state);
    
    /**
     * Resumes workflow execution asynchronously with context.
     *
     * @param input The input for resumption
     * @param state The saved workflow state
     * @param context Additional context
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<O>> resumeAsync(I input, WorkflowState state, Map<String, Object> context);
}