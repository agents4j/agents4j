package dev.agents4j.workflow.impl;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.ExecutionResult;

import java.util.Map;

/**
 * Handles SUSPEND commands by suspending the workflow execution.
 */
public class SuspendCommandHandler<I, O> implements CommandHandler<I, O> {
    
    @Override
    public boolean canHandle(WorkflowCommand.CommandType type) {
        return type == WorkflowCommand.CommandType.SUSPEND;
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
        
        // Add suspension metadata to context
        context.put("suspended_at", System.currentTimeMillis());
        command.getMetadata().forEach(context::put);
        
        return ExecutionResult.suspended(updatedState);
    }
    
    @Override
    public int getPriority() {
        return 90; // High priority for suspension handler
    }
    
    @Override
    public String getHandlerName() {
        return "SuspendCommandHandler";
    }
}