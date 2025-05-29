package dev.agents4j.workflow.impl;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.ExecutionResult;

import java.util.Map;

/**
 * Handles CONTINUE commands by updating state and continuing workflow execution.
 */
public class ContinueCommandHandler<I, O> implements CommandHandler<I, O> {
    
    @Override
    public boolean canHandle(WorkflowCommand.CommandType type) {
        return type == WorkflowCommand.CommandType.CONTINUE;
    }
    
    @Override
    public ExecutionResult<I, O> handle(WorkflowCommand<I> command, 
                                       WorkflowState state, 
                                       Map<String, Object> context) {
        
        // Apply any state updates from the command
        WorkflowState updatedState = state;
        if (!command.getStateUpdates().isEmpty()) {
            updatedState = state.withUpdates(command.getStateUpdates());
        }
        
        // Get the next input for continuing execution
        I nextInput = command.getNextInput().orElse(null);
        
        // Add any command metadata to context
        command.getMetadata().forEach(context::put);
        
        return ExecutionResult.continueWith(updatedState, nextInput);
    }
    
    @Override
    public int getPriority() {
        return 80; // High priority for continue handler
    }
    
    @Override
    public String getHandlerName() {
        return "ContinueCommandHandler";
    }
}