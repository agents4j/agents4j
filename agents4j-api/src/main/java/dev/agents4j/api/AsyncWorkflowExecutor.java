package dev.agents4j.api;

import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for asynchronous workflow execution.
 * Separated from synchronous execution following ISP.
 *
 * @param <S> The type of the workflow state data
 * @param <O> The output type for the workflow
 */
public interface AsyncWorkflowExecutor<S, O> {
    
    /**
     * Starts workflow execution asynchronously.
     *
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<S, O>> startAsync();
    
    /**
     * Starts workflow execution asynchronously with initial state data.
     *
     * @param initialStateData The initial state data
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<S, O>> startAsync(S initialStateData);
    
    /**
     * Resumes workflow execution asynchronously.
     *
     * @param state The saved workflow state
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<S, O>> resumeAsync(WorkflowState<S> state);
    
    /**
     * Resumes workflow execution asynchronously with updated state.
     *
     * @param state The saved workflow state with any updates
     * @return CompletableFuture with the workflow result
     */
    CompletableFuture<StatefulWorkflowResult<S, O>> resumeAsyncWithUpdates(WorkflowState<S> state);
}