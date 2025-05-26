package dev.agents4j.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a workflow of agent nodes.
 * A workflow coordinates the execution of multiple agent nodes
 * and manages the flow of information between them.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface AgentWorkflow<I, O> {
    /**
     * Execute the workflow with the given input.
     *
     * @param input The input to the workflow
     * @return The output from the workflow
     */
    O execute(I input);

    /**
     * Execute the workflow with the given input and context.
     *
     * @param input The input to the workflow
     * @param context Additional context information for the workflow
     * @return The output from the workflow
     */
    O execute(I input, Map<String, Object> context);

    /**
     * Execute the workflow asynchronously with the given input.
     *
     * @param input The input to the workflow
     * @return A CompletableFuture that will complete with the output
     */
    default CompletableFuture<O> executeAsync(I input) {
        return CompletableFuture.supplyAsync(() -> execute(input));
    }

    /**
     * Execute the workflow asynchronously with the given input and context.
     *
     * @param input The input to the workflow
     * @param context Additional context information for the workflow
     * @return A CompletableFuture that will complete with the output
     */
    default CompletableFuture<O> executeAsync(
        I input,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> execute(input, context));
    }

    /**
     * Get the name of this workflow.
     *
     * @return The name of the workflow
     */
    String getName();
}
