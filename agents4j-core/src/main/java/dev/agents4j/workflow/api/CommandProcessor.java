package dev.agents4j.workflow.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;

/**
 * Processes workflow commands using registered handlers.
 * Follows Strategy pattern and Open/Closed Principle.
 *
 * @param <I> The input type
 * @param <O> The output type
 */
public interface CommandProcessor<I, O> {
    
    /**
     * Registers a command handler with this processor.
     *
     * @param handler The command handler to register
     * @throws IllegalArgumentException if the handler is null
     */
    void registerHandler(CommandHandler<I, O> handler);
    
    /**
     * Unregisters a command handler from this processor.
     *
     * @param handler The command handler to unregister
     * @return true if the handler was successfully removed, false if it wasn't registered
     */
    boolean unregisterHandler(CommandHandler<I, O> handler);
    
    /**
     * Processes a workflow command using the appropriate registered handler.
     *
     * @param command The workflow command to process
     * @param state The current workflow state
     * @param context Additional context information
     * @return The execution result from processing the command
     * @throws IllegalArgumentException if no suitable handler is found for the command type
     */
    ExecutionResult<I, O> process(WorkflowCommand<I> command, 
                                 WorkflowState state, 
                                 Map<String, Object> context);
    
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