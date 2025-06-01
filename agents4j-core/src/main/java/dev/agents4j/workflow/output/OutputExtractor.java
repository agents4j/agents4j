package dev.agents4j.workflow.output;

import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.result.error.WorkflowError;
import dev.agents4j.api.result.WorkflowResult;

/**
 * Interface for extracting output from workflow state after execution.
 * 
 * @param <I> The input type
 * @param <O> The output type
 */
public interface OutputExtractor<I, O> {
    /**
     * Extracts output from the workflow result and state.
     *
     * @param state The final workflow state
     * @return The extracted output
     */
    O extract(GraphWorkflowState<I> state);
    
    /**
     * Extracts output in case of workflow failure.
     *
     * @param state The final workflow state
     * @param error The workflow error
     * @return The extracted output or null
     */
    default O extractFromError(GraphWorkflowState<I> state, WorkflowError error) {
        return null;
    }
}