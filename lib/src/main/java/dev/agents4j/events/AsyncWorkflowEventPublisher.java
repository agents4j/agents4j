/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.events;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Async-only implementation of WorkflowEventPublisher.
 * This implementation always processes events asynchronously for better performance
 * and non-blocking behavior in high-throughput scenarios.
 */
public class AsyncWorkflowEventPublisher implements WorkflowEventPublisher {

    private final List<WorkflowEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "async-workflow-event-publisher");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void addListener(WorkflowEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public boolean removeListener(WorkflowEventListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public List<WorkflowEventListener> getListeners() {
        return List.copyOf(listeners);
    }

    @Override
    public void publish(WorkflowEvent event) {
        // Delegate to async publishing - fire and forget
        publishAsync(event);
    }

    @Override
    public CompletableFuture<Void> publishAsync(WorkflowEvent event) {
        if (event == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = listeners.stream()
            .map(listener -> CompletableFuture.runAsync(() -> {
                try {
                    dispatchEvent(listener, event);
                } catch (Exception e) {
                    // Log error but don't fail the future
                    System.err.println("Error dispatching event to listener asynchronously: " + e.getMessage());
                }
            }, asyncExecutor))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @Override
    public void clearListeners() {
        listeners.clear();
    }

    @Override
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Dispatches an event to a specific listener based on the event type.
     *
     * @param listener The listener to notify
     * @param event The event to dispatch
     */
    private void dispatchEvent(WorkflowEventListener listener, WorkflowEvent event) {
        switch (event.getEventType()) {
            case "WORKFLOW_STARTED" -> listener.onWorkflowStarted((WorkflowStartedEvent) event);
            case "WORKFLOW_COMPLETED" -> listener.onWorkflowCompleted((WorkflowCompletedEvent) event);
            case "WORKFLOW_FAILED" -> listener.onWorkflowFailed((WorkflowFailedEvent) event);
            case "WORKFLOW_CANCELLED" -> listener.onWorkflowCancelled((WorkflowCancelledEvent) event);
            case "NODE_STARTING" -> listener.onNodeStarting((NodeStartingEvent) event);
            case "NODE_EXECUTED" -> listener.onNodeExecuted((NodeExecutionEvent) event);
            case "NODE_FAILED" -> listener.onNodeFailed((NodeFailedEvent) event);
            case "CONTEXT_UPDATED" -> listener.onContextUpdated((ContextUpdatedEvent) event);
            default -> {
                // Unknown event type - ignore or log
                System.err.println("Unknown event type: " + event.getEventType());
            }
        }
    }

    /**
     * Shuts down the async executor.
     * Should be called when the publisher is no longer needed.
     */
    public void shutdown() {
        asyncExecutor.shutdown();
    }
}