package dev.agents4j.workflow.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Processes workflow commands using registered handlers.
 * Follows Strategy pattern and Open/Closed Principle.
 *
 * @param <S> The state type
 * @param <O> The output type
 */
public interface CommandProcessor<S, O> {
    
    /**
     * Registers a command handler with this processor.
     *
     * @param handler The command handler to register
     * @throws IllegalArgumentException if the handler is null
     */
    void registerHandler(CommandHandler<S, O> handler);
    
    /**
     * Unregisters a command handler from this processor.
     *
     * @param handler The command handler to unregister
     * @return true if the handler was successfully removed, false if it wasn't registered
     */
    boolean unregisterHandler(CommandHandler<S, O> handler);
    
    /**
     * Processes a workflow command using the appropriate registered handler.
     *
     * @param command The workflow command to process
     * @param state The current workflow state
     * @return The execution result from processing the command
     * @throws IllegalArgumentException if no suitable handler is found for the command type
     */
    ExecutionResult<S, O> process(WorkflowCommand<S> command, 
                                 WorkflowState<S> state);
    
    /**
     * Checks if a handler is registered for the given command type.
     *
     * @param commandType The command type to check
     * @return true if a handler exists for the command type
     */
    boolean hasHandlerFor(WorkflowCommand.CommandType commandType);
    
    /**
     * Gets the number of registered handlers.
     *
     * @return The total number of registered handlers
     */
    int getHandlerCount();
}