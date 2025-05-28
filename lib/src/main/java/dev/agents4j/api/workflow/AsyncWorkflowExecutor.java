package dev.agents4j.api.workflow;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for asynchronous workflow execution.
 */
public interface AsyncWorkflowExecutor<I, O> {
    
    /**
     * Execute the workflow asynchronously with the given input.
     *
     * @param input The input to the workflow
     * @return A CompletableFuture that will complete with the output
     */
    CompletableFuture<O> executeAsync(I input);

    /**
     * Execute the workflow asynchronously with the given input and context.
     *
     * @param input The input to the workflow
     * @param context Additional context information for the workflow
     * @return A CompletableFuture that will complete with the output
     */
    CompletableFuture<O> executeAsync(I input, Map<String, Object> context);
}