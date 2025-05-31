package dev.agents4j.workflow.nodes;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.Objects;

/**
 * Abstract base class for StatefulAgentNode implementations.
 * Provides common functionality and utility methods for building stateful nodes.
 *
 * @param <S> The type of the workflow state data
 */
public abstract class AbstractStatefulAgentNode<S> implements StatefulAgentNode<S> {
    
    private final String nodeId;
    private final String name;
    private final boolean canBeEntryPoint;
    private final boolean canSuspend;
    
    protected AbstractStatefulAgentNode(String nodeId, String name) {
        this(nodeId, name, false, true);
    }
    
    protected AbstractStatefulAgentNode(String nodeId, String name, boolean canBeEntryPoint) {
        this(nodeId, name, canBeEntryPoint, true);
    }
    
    protected AbstractStatefulAgentNode(String nodeId, String name, boolean canBeEntryPoint, boolean canSuspend) {
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.canBeEntryPoint = canBeEntryPoint;
        this.canSuspend = canSuspend;
    }
    
    @Override
    public String getNodeId() {
        return nodeId;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean canBeEntryPoint() {
        return canBeEntryPoint;
    }
    
    @Override
    public boolean canSuspend() {
        return canSuspend;
    }
    
    /**
     * Template method for processing. Subclasses should implement doProcess
     * instead of overriding this method directly.
     */
    @Override
    public final WorkflowCommand<S> process(WorkflowState<S> state) {
        try {
            return doProcess(state);
        } catch (Exception e) {
            return handleError(e, state);
        }
    }
    
    /**
     * Abstract method that subclasses must implement to define their processing logic.
     *
     * @param state The current workflow state containing data and context
     * @return A WorkflowCommand indicating how the workflow should proceed
     */
    protected abstract WorkflowCommand<S> doProcess(WorkflowState<S> state);
    
    /**
     * Handles errors that occur during processing. Can be overridden by subclasses
     * to provide custom error handling.
     *
     * @param error The exception that occurred
     * @param state The current workflow state
     * @return A WorkflowCommand representing the error
     */
    protected WorkflowCommand<S> handleError(Exception error, WorkflowState<S> state) {
        return WorkflowCommand.<S>error("Error in node " + nodeId + ": " + error.getMessage()).build();
    }
    
    /**
     * Utility method to create a continue command with state updates.
     *
     * @return A continue command builder
     */
    protected WorkflowCommand.Builder<S> continueWith() {
        return WorkflowCommand.continueWith();
    }
    
    /**
     * Utility method to create a goto command.
     *
     * @param targetNodeId The target node ID
     * @return A goto command builder
     */
    protected WorkflowCommand.Builder<S> goTo(String targetNodeId) {
        return WorkflowCommand.goTo(targetNodeId);
    }
    
    /**
     * Utility method to create a suspend command.
     *
     * @return A suspend command builder
     */
    protected WorkflowCommand.Builder<S> suspend() {
        return WorkflowCommand.suspend();
    }
    
    /**
     * Utility method to create a complete command.
     *
     * @return A complete command builder
     */
    protected WorkflowCommand.Builder<S> complete() {
        return WorkflowCommand.complete();
    }
    
    /**
     * Utility method to check if a context key exists and has a specific value.
     *
     * @param state The workflow state
     * @param key The context key
     * @param expectedValue The expected value
     * @return true if the context has the key with the expected value
     */
    protected boolean contextEquals(WorkflowState<S> state, String key, Object expectedValue) {
        return Objects.equals(state.getContextValue(key).orElse(null), expectedValue);
    }
    
    /**
     * Utility method to check if a context key exists.
     *
     * @param state The workflow state
     * @param key The context key
     * @return true if the context contains the key
     */
    protected boolean contextContains(WorkflowState<S> state, String key) {
        return state.getContextValue(key).isPresent();
    }
    
    /**
     * Utility method to get a context value with a default.
     *
     * @param state The workflow state
     * @param key The context key
     * @param defaultValue The default value
     * @param <T> The value type
     * @return The context value or default
     */
    protected <T> T getContextValue(WorkflowState<S> state, String key, T defaultValue) {
        return state.getContextValue(key, defaultValue);
    }
    
    /**
     * Utility method to increment a counter in the context.
     *
     * @param state The workflow state
     * @param counterKey The counter key
     * @return The new counter value
     */
    protected int incrementCounter(WorkflowState<S> state, String counterKey) {
        int currentValue = getContextValue(state, counterKey, 0);
        return currentValue + 1;
    }
    
    @Override
    public String toString() {
        return String.format("%s{nodeId='%s', name='%s'}", getClass().getSimpleName(), nodeId, name);
    }
}