package dev.agents4j.api.command;

import dev.agents4j.api.workflow.WorkflowCommand;
import java.util.Map;

/**
 * Factory interface for creating workflow commands.
 * Provides a clean abstraction for command creation and allows for
 * different command creation strategies or decorations.
 * 
 * <p>This interface enables dependency injection of command creation logic
 * and supports customization of command creation behavior without modifying
 * the core workflow or node logic.</p>
 * 
 * @param <S> The type of the workflow state data
 */
public interface CommandFactory<S> {
    
    /**
     * Creates a CONTINUE command with the specified parameters.
     *
     * @param nextStateData The new state data (can be null to keep current)
     * @param contextUpdates Context updates to apply (can be null or empty)
     * @param metadata Additional metadata for the command (can be null or empty)
     * @return A CONTINUE WorkflowCommand
     */
    WorkflowCommand<S> createContinueCommand(S nextStateData, Map<String, Object> contextUpdates, 
                                           Map<String, Object> metadata);
    
    /**
     * Creates a CONTINUE command with minimal parameters.
     *
     * @return A basic CONTINUE WorkflowCommand
     */
    default WorkflowCommand<S> createContinueCommand() {
        return createContinueCommand(null, null, null);
    }
    
    /**
     * Creates a CONTINUE command with only context updates.
     *
     * @param contextUpdates Context updates to apply
     * @return A CONTINUE WorkflowCommand
     */
    default WorkflowCommand<S> createContinueCommand(Map<String, Object> contextUpdates) {
        return createContinueCommand(null, contextUpdates, null);
    }
    
    /**
     * Creates a GOTO command to jump to a specific node.
     *
     * @param targetNodeId The target node identifier
     * @param nextStateData The new state data (can be null to keep current)
     * @param contextUpdates Context updates to apply (can be null or empty)
     * @param metadata Additional metadata for the command (can be null or empty)
     * @return A GOTO WorkflowCommand
     */
    WorkflowCommand<S> createGotoCommand(String targetNodeId, S nextStateData, 
                                       Map<String, Object> contextUpdates, 
                                       Map<String, Object> metadata);
    
    /**
     * Creates a GOTO command with minimal parameters.
     *
     * @param targetNodeId The target node identifier
     * @return A basic GOTO WorkflowCommand
     */
    default WorkflowCommand<S> createGotoCommand(String targetNodeId) {
        return createGotoCommand(targetNodeId, null, null, null);
    }
    
    /**
     * Creates a SUSPEND command to pause workflow execution.
     *
     * @param contextUpdates Context updates to apply (can be null or empty)
     * @param metadata Additional metadata for the command (can be null or empty)
     * @return A SUSPEND WorkflowCommand
     */
    WorkflowCommand<S> createSuspendCommand(Map<String, Object> contextUpdates, 
                                          Map<String, Object> metadata);
    
    /**
     * Creates a SUSPEND command with minimal parameters.
     *
     * @return A basic SUSPEND WorkflowCommand
     */
    default WorkflowCommand<S> createSuspendCommand() {
        return createSuspendCommand(null, null);
    }
    
    /**
     * Creates a COMPLETE command to finish workflow execution.
     *
     * @param finalStateData The final state data (can be null)
     * @param contextUpdates Context updates to apply (can be null or empty)
     * @param metadata Additional metadata for the command (can be null or empty)
     * @return A COMPLETE WorkflowCommand
     */
    WorkflowCommand<S> createCompleteCommand(S finalStateData, Map<String, Object> contextUpdates, 
                                           Map<String, Object> metadata);
    
    /**
     * Creates a COMPLETE command with minimal parameters.
     *
     * @return A basic COMPLETE WorkflowCommand
     */
    default WorkflowCommand<S> createCompleteCommand() {
        return createCompleteCommand(null, null, null);
    }
    
    /**
     * Creates a COMPLETE command with final state data.
     *
     * @param finalStateData The final state data
     * @return A COMPLETE WorkflowCommand
     */
    default WorkflowCommand<S> createCompleteCommand(S finalStateData) {
        return createCompleteCommand(finalStateData, null, null);
    }
    
    /**
     * Creates an ERROR command to indicate a failure.
     *
     * @param errorMessage The error message describing the failure
     * @param contextUpdates Context updates to apply (can be null or empty)
     * @param metadata Additional metadata for the command (can be null or empty)
     * @return An ERROR WorkflowCommand
     */
    WorkflowCommand<S> createErrorCommand(String errorMessage, Map<String, Object> contextUpdates, 
                                        Map<String, Object> metadata);
    
    /**
     * Creates an ERROR command with minimal parameters.
     *
     * @param errorMessage The error message describing the failure
     * @return A basic ERROR WorkflowCommand
     */
    default WorkflowCommand<S> createErrorCommand(String errorMessage) {
        return createErrorCommand(errorMessage, null, null);
    }
    
    /**
     * Gets information about this command factory.
     *
     * @return A map containing factory information
     */
    default Map<String, Object> getFactoryInfo() {
        return Map.of(
            "factoryType", getClass().getSimpleName(),
            "supportedCommands", new String[]{"CONTINUE", "GOTO", "SUSPEND", "COMPLETE", "ERROR"}
        );
    }
}