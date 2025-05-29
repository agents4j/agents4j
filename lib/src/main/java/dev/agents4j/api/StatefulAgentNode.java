package dev.agents4j.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a stateful agent node that can participate in a StatefulWorkflow.
 * Unlike regular AgentNodes, StatefulAgentNodes return WorkflowCommands that
 * can influence workflow execution flow and state.
 *
 * @param <I> The input type for this node
 */
public interface StatefulAgentNode<I> {
    
    /**
     * Process the input with access to the current workflow state and return a command
     * that instructs the workflow on how to proceed.
     *
     * @param input The input to process
     * @param state The current workflow state
     * @param context Additional context information
     * @return A WorkflowCommand indicating how the workflow should proceed
     */
    WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context);
    
    /**
     * Process the input asynchronously.
     *
     * @param input The input to process
     * @param state The current workflow state
     * @param context Additional context information
     * @return A CompletableFuture containing the WorkflowCommand
     */
    default CompletableFuture<WorkflowCommand<I>> processAsync(I input, WorkflowState state, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> process(input, state, context));
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