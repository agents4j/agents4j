package dev.agents4j.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a workflow node that can participate in a StatefulWorkflow.
 * WorkflowNodes return WorkflowCommands that can influence workflow 
 * execution flow and state.
 *
 * @param <S> The type of the workflow state data
 */
public interface WorkflowNode<S> {
    
    /**
     * Process with access to the current workflow state and return a command
     * that instructs the workflow on how to proceed.
     *
     * @param state The current workflow state containing data and context
     * @return A WorkflowCommand indicating how the workflow should proceed
     */
    WorkflowCommand<S> process(WorkflowState<S> state);
    
    /**
     * Process asynchronously.
     *
     * @param state The current workflow state containing data and context
     * @return A CompletableFuture containing the WorkflowCommand
     */
    default CompletableFuture<WorkflowCommand<S>> processAsync(WorkflowState<S> state) {
        return CompletableFuture.supplyAsync(() -> process(state));
    }
    
    /**
     * Get the unique identifier for this node.
     *
     * @return The node ID
     */
    String getNodeId();
    
    /**
     * Get the human-readable name of this node.
     *
     * @return The node name
     */
    String getName();
    
    /**
     * Check if this node can be used as an entry point to the workflow.
     *
     * @return true if this node can be an entry point
     */
    default boolean canBeEntryPoint() {
        return false;
    }
    
    /**
     * Check if this node can suspend the workflow.
     *
     * @return true if this node can suspend the workflow
     */
    default boolean canSuspend() {
        return true;
    }
}