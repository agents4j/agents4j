package dev.agents4j.api;

import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main interface for agent workflows providing simple execution capabilities.
 * This interface provides a simpler alternative to StatefulWorkflow for workflows
 * that don't require advanced state management, suspension, or resumption.
 *
 * A workflow coordinates the execution of multiple agent nodes and manages
 * the flow of information between them. Unlike StatefulWorkflow, this interface
 * returns direct output values rather than StatefulWorkflowResult objects.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public interface Workflow<I, O> {
    /**
     * Execute the workflow with the given input.
     *
     * @param input The input to the workflow
     * @return The output from the workflow
     * @throws WorkflowExecutionException if execution fails
     */
    O execute(I input) throws WorkflowExecutionException;

    /**
     * Execute the workflow with the given input and context.
     *
     * @param input The input to the workflow
     * @param context Additional context information for the workflow
     * @return The output from the workflow
     * @throws WorkflowExecutionException if execution fails
     */
    O execute(I input, Map<String, Object> context)
        throws WorkflowExecutionException;

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

    /**
     * Get the name of this workflow.
     *
     * @return The name of the workflow
     */
    String getName();

    /**
     * Get the configuration properties of this workflow.
     *
     * @return A map of configuration properties
     */
    Map<String, Object> getConfiguration();

    /**
     * Get a specific configuration property.
     *
     * @param key The property key
     * @param defaultValue The default value if property is not found
     * @param <T> The type of the property value
     * @return The property value or default value
     */
    <T> T getConfigurationProperty(String key, T defaultValue);
    // This interface provides a simplified workflow execution API
    // without the complexity of state management found in StatefulWorkflow
}
