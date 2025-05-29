package dev.agents4j.api.workflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a command returned by a StatefulAgentNode that instructs the workflow
 * on how to proceed. Commands can contain state updates, routing instructions,
 * or suspension requests.
 *
 * @param <I> The input type for the workflow
 */
public class WorkflowCommand<I> {
    
    public enum CommandType {
        CONTINUE,    // Continue to next node(s) based on routes
        GOTO,        // Jump directly to a specific node
        SUSPEND,     // Suspend workflow execution
        COMPLETE,    // Complete workflow execution
        ERROR        // Indicate an error occurred
    }
    
    private final CommandType type;
    private final Map<String, Object> stateUpdates;
    private final String targetNodeId;
    private final I nextInput;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    
    private WorkflowCommand(CommandType type, Map<String, Object> stateUpdates, 
                           String targetNodeId, I nextInput, String errorMessage,
                           Map<String, Object> metadata) {
        this.type = Objects.requireNonNull(type, "Command type cannot be null");
        this.stateUpdates = Collections.unmodifiableMap(stateUpdates != null ? stateUpdates : Collections.emptyMap());
        this.targetNodeId = targetNodeId;
        this.nextInput = nextInput;
        this.errorMessage = errorMessage;
        this.metadata = Collections.unmodifiableMap(metadata != null ? metadata : Collections.emptyMap());
    }
    
    /**
     * Gets the command type.
     *
     * @return The command type
     */
    public CommandType getType() {
        return type;
    }
    
    /**
     * Gets the state updates to apply.
     *
     * @return Map of state updates
     */
    public Map<String, Object> getStateUpdates() {
        return stateUpdates;
    }
    
    /**
     * Gets the target node ID for GOTO commands.
     *
     * @return The target node ID, or empty if not applicable
     */
    public Optional<String> getTargetNodeId() {
        return Optional.ofNullable(targetNodeId);
    }
    
    /**
     * Gets the next input for the workflow.
     *
     * @return The next input, or empty if not provided
     */
    public Optional<I> getNextInput() {
        return Optional.ofNullable(nextInput);
    }
    
    /**
     * Gets the error message for ERROR commands.
     *
     * @return The error message, or empty if not applicable
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    /**
     * Gets the metadata associated with this command.
     *
     * @return Map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Creates a CONTINUE command with optional state updates.
     *
     * @param <I> The input type
     * @return A new command builder
     */
    public static <I> Builder<I> continueWith() {
        return new Builder<I>().type(CommandType.CONTINUE);
    }
    
    /**
     * Creates a GOTO command to jump to a specific node.
     *
     * @param nodeId The target node ID
     * @param <I> The input type
     * @return A new command builder
     */
    public static <I> Builder<I> goTo(String nodeId) {
        return new Builder<I>().type(CommandType.GOTO).targetNode(nodeId);
    }
    
    /**
     * Creates a SUSPEND command to pause workflow execution.
     *
     * @param <I> The input type
     * @return A new command builder
     */
    public static <I> Builder<I> suspend() {
        return new Builder<I>().type(CommandType.SUSPEND);
    }
    
    /**
     * Creates a COMPLETE command to finish workflow execution.
     *
     * @param <I> The input type
     * @return A new command builder
     */
    public static <I> Builder<I> complete() {
        return new Builder<I>().type(CommandType.COMPLETE);
    }
    
    /**
     * Creates an ERROR command to indicate an error occurred.
     *
     * @param errorMessage The error message
     * @param <I> The input type
     * @return A new command builder
     */
    public static <I> Builder<I> error(String errorMessage) {
        return new Builder<I>().type(CommandType.ERROR).errorMessage(errorMessage);
    }
    
    /**
     * Builder for creating WorkflowCommand instances.
     *
     * @param <I> The input type
     */
    public static class Builder<I> {
        private CommandType type;
        private final Map<String, Object> stateUpdates = new HashMap<>();
        private String targetNodeId;
        private I nextInput;
        private String errorMessage;
        private final Map<String, Object> metadata = new HashMap<>();
        
        private Builder<I> type(CommandType type) {
            this.type = type;
            return this;
        }
        
        private Builder<I> targetNode(String nodeId) {
            this.targetNodeId = nodeId;
            return this;
        }
        
        private Builder<I> errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        /**
         * Adds a state update.
         *
         * @param key The state key
         * @param value The state value
         * @return This builder
         */
        public Builder<I> updateState(String key, Object value) {
            this.stateUpdates.put(key, value);
            return this;
        }
        
        /**
         * Sets the next input for the workflow.
         *
         * @param input The next input
         * @return This builder
         */
        public Builder<I> withInput(I input) {
            this.nextInput = input;
            return this;
        }
        
        /**
         * Adds metadata.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder
         */
        public Builder<I> addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Builds the WorkflowCommand.
         *
         * @return A new WorkflowCommand instance
         */
        public WorkflowCommand<I> build() {
            return new WorkflowCommand<>(type, stateUpdates, targetNodeId, nextInput, errorMessage, metadata);
        }
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowCommand{type=%s, targetNode=%s, stateUpdates=%d}",
                type, targetNodeId, stateUpdates.size());
    }
}