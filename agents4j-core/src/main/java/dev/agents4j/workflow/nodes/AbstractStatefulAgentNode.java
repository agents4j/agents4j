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
 * @param <I> The input type for this node
 */
public abstract class AbstractStatefulAgentNode<I> implements StatefulAgentNode<I> {
    
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
     * Template method for processing input. Subclasses should implement doProcess
     * instead of overriding this method directly.
     */
    @Override
    public final WorkflowCommand<I> process(I input, WorkflowState state, Map<String, Object> context) {
        try {
            return doProcess(input, state, context);
        } catch (Exception e) {
            return handleError(e, input, state, context);
        }
    }
    
    /**
     * Abstract method that subclasses must implement to define their processing logic.
     *
     * @param input The input to process
     * @param state The current workflow state
     * @param context Additional context information
     * @return A WorkflowCommand indicating how the workflow should proceed
     */
    protected abstract WorkflowCommand<I> doProcess(I input, WorkflowState state, Map<String, Object> context);
    
    /**
     * Handles errors that occur during processing. Can be overridden by subclasses
     * to provide custom error handling.
     *
     * @param error The exception that occurred
     * @param input The input being processed
     * @param state The current workflow state
     * @param context The execution context
     * @return A WorkflowCommand representing the error
     */
    protected WorkflowCommand<I> handleError(Exception error, I input, WorkflowState state, Map<String, Object> context) {
        return WorkflowCommand.<I>error("Error in node " + nodeId + ": " + error.getMessage()).build();
    }
    
    /**
     * Utility method to create a continue command with state updates.
     *
     * @return A continue command builder
     */
    protected WorkflowCommand.Builder<I> continueWith() {
        return WorkflowCommand.continueWith();
    }
    
    /**
     * Utility method to create a goto command.
     *
     * @param targetNodeId The target node ID
     * @return A goto command builder
     */
    protected WorkflowCommand.Builder<I> goTo(String targetNodeId) {
        return WorkflowCommand.goTo(targetNodeId);
    }
    
    /**
     * Utility method to create a suspend command.
     *
     * @return A suspend command builder
     */
    protected WorkflowCommand.Builder<I> suspend() {
        return WorkflowCommand.suspend();
    }
    
    /**
     * Utility method to create a complete command.
     *
     * @return A complete command builder
     */
    protected WorkflowCommand.Builder<I> complete() {
        return WorkflowCommand.complete();
    }
    
    /**
     * Utility method to check if a state key exists and has a specific value.
     *
     * @param state The workflow state
     * @param key The state key
     * @param expectedValue The expected value
     * @return true if the state has the key with the expected value
     */
    protected boolean stateEquals(WorkflowState state, String key, Object expectedValue) {
        return Objects.equals(state.get(key).orElse(null), expectedValue);
    }
    
    /**
     * Utility method to check if a state key exists.
     *
     * @param state The workflow state
     * @param key The state key
     * @return true if the state contains the key
     */
    protected boolean stateContains(WorkflowState state, String key) {
        return state.get(key).isPresent();
    }
    
    /**
     * Utility method to get a state value with a default.
     *
     * @param state The workflow state
     * @param key The state key
     * @param defaultValue The default value
     * @param <T> The value type
     * @return The state value or default
     */
    protected <T> T getStateValue(WorkflowState state, String key, T defaultValue) {
        return state.get(key, defaultValue);
    }
    
    /**
     * Utility method to increment a counter in the state.
     *
     * @param state The workflow state
     * @param counterKey The counter key
     * @return The new counter value
     */
    protected int incrementCounter(WorkflowState state, String counterKey) {
        int currentValue = getStateValue(state, counterKey, 0);
        return currentValue + 1;
    }
    
    @Override
    public String toString() {
        return String.format("%s{nodeId='%s', name='%s'}", getClass().getSimpleName(), nodeId, name);
    }
}