/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.events;

/**
 * Interface for listening to workflow execution events.
 * Implementations can use this to monitor workflow progress, collect metrics,
 * or implement custom logging and debugging features.
 */
public interface WorkflowEventListener {

    /**
     * Called when a workflow starts execution.
     *
     * @param event The workflow started event
     */
    default void onWorkflowStarted(WorkflowStartedEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when an individual node within a workflow executes.
     *
     * @param event The node execution event
     */
    default void onNodeExecuted(NodeExecutionEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when a workflow completes successfully.
     *
     * @param event The workflow completed event
     */
    default void onWorkflowCompleted(WorkflowCompletedEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when a workflow fails with an error.
     *
     * @param event The workflow failure event
     */
    default void onWorkflowFailed(WorkflowFailedEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when a workflow is cancelled or interrupted.
     *
     * @param event The workflow cancelled event
     */
    default void onWorkflowCancelled(WorkflowCancelledEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called before a node starts executing.
     *
     * @param event The node starting event
     */
    default void onNodeStarting(NodeStartingEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when a node fails during execution.
     *
     * @param event The node failure event
     */
    default void onNodeFailed(NodeFailedEvent event) {
        // Default implementation does nothing
    }

    /**
     * Called when workflow context is updated.
     *
     * @param event The context updated event
     */
    default void onContextUpdated(ContextUpdatedEvent event) {
        // Default implementation does nothing
    }
}