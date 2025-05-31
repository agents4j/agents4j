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
 * @param <S> The type of the workflow state data
 */
public class WorkflowCommand<S> {
    
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
    private final S nextStateData;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    
    private WorkflowCommand(CommandType type, Map<String, Object> stateUpdates, 
                           String targetNodeId, S nextStateData, String errorMessage,
                           Map<String, Object> metadata) {
        this.type = Objects.requireNonNull(type, "Command type cannot be null");
        this.stateUpdates = Collections.unmodifiableMap(stateUpdates != null ? stateUpdates : Collections.emptyMap());
        this.targetNodeId = targetNodeId;
        this.nextStateData = nextStateData;
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
     * Gets the next state data for the workflow.
     *
     * @return The next state data, or empty if not provided
     */
    public Optional<S> getNextStateData() {
        return Optional.ofNullable(nextStateData);
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
     * @param <S> The state type
     * @return A new command builder
     */
    public static <S> Builder<S> continueWith() {
        return new Builder<S>().type(CommandType.CONTINUE);
    }
    
    /**
     * Creates a GOTO command to jump to a specific node.
     *
     * @param nodeId The target node ID
     * @param <S> The state type
     * @return A new command builder
     */
    public static <S> Builder<S> goTo(String nodeId) {
        return new Builder<S>().type(CommandType.GOTO).targetNode(nodeId);
    }
    
    /**
     * Creates a SUSPEND command to pause workflow execution.
     *
     * @param <S> The state type
     * @return A new command builder
     */
    public static <S> Builder<S> suspend() {
        return new Builder<S>().type(CommandType.SUSPEND);
    }
    
    /**
     * Creates a COMPLETE command to finish workflow execution.
     *
     * @param <S> The state type
     * @return A new command builder
     */
    public static <S> Builder<S> complete() {
        return new Builder<S>().type(CommandType.COMPLETE);
    }
    
    /**
     * Creates an ERROR command to indicate an error occurred.
     *
     * @param errorMessage The error message
     * @param <S> The state type
     * @return A new command builder
     */
    public static <S> Builder<S> error(String errorMessage) {
        return new Builder<S>().type(CommandType.ERROR).errorMessage(errorMessage);
    }
    
    /**
     * Builder for creating WorkflowCommand instances.
     *
     * @param <S> The state type
     */
    public static class Builder<S> {
        private CommandType type;
        private final Map<String, Object> stateUpdates = new HashMap<>();
        private String targetNodeId;
        private S nextStateData;
        private String errorMessage;
        private final Map<String, Object> metadata = new HashMap<>();
        
        private Builder<S> type(CommandType type) {
            this.type = type;
            return this;
        }
        
        private Builder<S> targetNode(String nodeId) {
            this.targetNodeId = nodeId;
            return this;
        }
        
        private Builder<S> errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        /**
         * Adds a context update.
         *
         * @param key The context key
         * @param value The context value
         * @return This builder
         */
        public Builder<S> updateState(String key, Object value) {
            this.stateUpdates.put(key, value);
            return this;
        }
        
        /**
         * Sets the next state data for the workflow.
         *
         * @param stateData The next state data
         * @return This builder
         */
        public Builder<S> withStateData(S stateData) {
            this.nextStateData = stateData;
            return this;
        }
        
        /**
         * Adds metadata.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder
         */
        public Builder<S> addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Builds the WorkflowCommand.
         *
         * @return A new WorkflowCommand instance
         */
        public WorkflowCommand<S> build() {
            return new WorkflowCommand<>(type, stateUpdates, targetNodeId, nextStateData, errorMessage, metadata);
        }
    }
    
    @Override
    public String toString() {
        return String.format("WorkflowCommand{type=%s, targetNode=%s, stateUpdates=%d}",
                type, targetNodeId, stateUpdates.size());
    }
}