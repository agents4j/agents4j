package dev.agents4j.api.workflow;

import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.Map;

/**
 * Interface for synchronous workflow execution.
 */
public interface WorkflowExecutor<I, O> {
    
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
    O execute(I input, Map<String, Object> context) throws WorkflowExecutionException;
}