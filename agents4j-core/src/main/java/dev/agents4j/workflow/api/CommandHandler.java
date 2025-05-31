package dev.agents4j.workflow.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Strategy interface for handling different workflow commands.
 * Follows Open/Closed Principle - open for extension, closed for modification.
 *
 * @param <S> The state type
 * @param <O> The output type
 */
public interface CommandHandler<S, O> {
    
    /**
     * Checks if this handler can handle the given command type.
     *
     * @param type The command type to check
     * @return true if this handler can process the command type
     */
    boolean canHandle(WorkflowCommand.CommandType type);
    
    /**
     * Handles the workflow command and returns the execution result.
     *
     * @param command The workflow command to handle
     * @param state The current workflow state
     * @return The execution result containing the outcome of processing the command
     */
    ExecutionResult<S, O> handle(WorkflowCommand<S> command, 
                                WorkflowState<S> state);
    
    /**
     * Gets the priority of this handler. Higher numbers indicate higher priority.
     * When multiple handlers can process the same command type, the one with
     * the highest priority will be selected.
     *
     * @return The priority level (default is 0)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Gets the name of this command handler for logging and debugging purposes.
     *
     * @return A descriptive name for this handler
     */
    default String getHandlerName() {
        return this.getClass().getSimpleName();
    }
}