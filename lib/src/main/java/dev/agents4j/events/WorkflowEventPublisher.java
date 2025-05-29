/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for publishing workflow events to registered listeners.
 * Implementations should handle event dispatch and listener management.
 */
public interface WorkflowEventPublisher {

    /**
     * Adds a listener to receive workflow events.
     *
     * @param listener The listener to add
     */
    void addListener(WorkflowEventListener listener);

    /**
     * Removes a listener from receiving workflow events.
     *
     * @param listener The listener to remove
     * @return true if the listener was removed, false if it wasn't registered
     */
    boolean removeListener(WorkflowEventListener listener);

    /**
     * Gets all registered listeners.
     *
     * @return List of registered listeners
     */
    List<WorkflowEventListener> getListeners();

    /**
     * Publishes a workflow event to all registered listeners.
     *
     * @param event The event to publish
     */
    void publish(WorkflowEvent event);

    /**
     * Publishes a workflow event asynchronously to all registered listeners.
     *
     * @param event The event to publish
     * @return CompletableFuture that completes when all listeners have processed the event
     */
    CompletableFuture<Void> publishAsync(WorkflowEvent event);

    /**
     * Removes all registered listeners.
     */
    void clearListeners();

    /**
     * Gets the number of registered listeners.
     *
     * @return The listener count
     */
    int getListenerCount();

    /**
     * Convenience method to publish a workflow started event.
     *
     * @param workflowName The workflow name
     * @param input The workflow input
     * @param metadata Additional metadata
     */
    default void publishWorkflowStarted(String workflowName, Object input, Map<String, Object> metadata) {
        publish(new WorkflowStartedEvent(workflowName, input, metadata));
    }

    /**
     * Convenience method to publish a workflow completed event.
     *
     * @param workflowName The workflow name
     * @param output The workflow output
     * @param executionTimeMs The execution time in milliseconds
     * @param metadata Additional metadata
     */
    default void publishWorkflowCompleted(String workflowName, Object output, long executionTimeMs, Map<String, Object> metadata) {
        publish(new WorkflowCompletedEvent(workflowName, output, executionTimeMs, metadata));
    }

    /**
     * Convenience method to publish a workflow failed event.
     *
     * @param workflowName The workflow name
     * @param error The error that caused the failure
     * @param executionTimeMs The execution time in milliseconds
     * @param metadata Additional metadata
     */
    default void publishWorkflowFailed(String workflowName, Throwable error, long executionTimeMs, Map<String, Object> metadata) {
        publish(new WorkflowFailedEvent(workflowName, error, executionTimeMs, metadata));
    }

    /**
     * Convenience method to publish a node execution event.
     *
     * @param workflowName The workflow name
     * @param nodeName The node name
     * @param input The node input
     * @param output The node output
     * @param executionTimeMs The execution time in milliseconds
     * @param metadata Additional metadata
     */
    default void publishNodeExecuted(String workflowName, String nodeName, Object input, Object output, long executionTimeMs, Map<String, Object> metadata) {
        publish(new NodeExecutionEvent(workflowName, nodeName, input, output, executionTimeMs, metadata));
    }

    /**
     * Convenience method to publish a node failed event.
     *
     * @param workflowName The workflow name
     * @param nodeName The node name
     * @param input The node input
     * @param error The error that caused the failure
     * @param executionTimeMs The execution time in milliseconds
     * @param metadata Additional metadata
     */
    default void publishNodeFailed(String workflowName, String nodeName, Object input, Throwable error, long executionTimeMs, Map<String, Object> metadata) {
        publish(new NodeFailedEvent(workflowName, nodeName, input, error, executionTimeMs, metadata));
    }

    /**
     * Creates a new default implementation of WorkflowEventPublisher.
     *
     * @return A new DefaultWorkflowEventPublisher instance
     */
    static WorkflowEventPublisher create() {
        return new DefaultWorkflowEventPublisher();
    }

    /**
     * Creates a new async-only implementation of WorkflowEventPublisher.
     * This implementation always processes events asynchronously.
     *
     * @return A new AsyncWorkflowEventPublisher instance
     */
    static WorkflowEventPublisher createAsync() {
        return new AsyncWorkflowEventPublisher();
    }
}