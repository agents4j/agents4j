package dev.agents4j.workflow.impl;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.ExecutionResult;

import java.util.Map;

/**
 * Handles ERROR commands by creating appropriate error execution results.
 */
public class ErrorCommandHandler<I, O> implements CommandHandler<I, O> {
    
    @Override
    public boolean canHandle(WorkflowCommand.CommandType type) {
        return type == WorkflowCommand.CommandType.ERROR;
    }
    
    @Override
    public ExecutionResult<I, O> handle(WorkflowCommand<I> command, 
                                       WorkflowState state, 
                                       Map<String, Object> context) {
        
        // Get the error message from the command
        String errorMessage = command.getErrorMessage()
                .orElse("Unknown error occurred during workflow execution");
        
        // Apply any state updates from the command before creating the error
        WorkflowState updatedState = state;
        if (!command.getStateUpdates().isEmpty()) {
            updatedState = state.withUpdates(command.getStateUpdates());
        }
        
        // Add error metadata to context for debugging
        context.put("error_timestamp", System.currentTimeMillis());
        context.put("error_message", errorMessage);
        command.getMetadata().forEach(context::put);
        
        // Create the workflow execution exception with context
        String workflowId = updatedState.getWorkflowId();
        String currentNodeId = updatedState.getCurrentNodeId().orElse("unknown");
        
        WorkflowExecutionException exception = new WorkflowExecutionException(
            errorMessage, workflowId, currentNodeId, updatedState);
        
        return ExecutionResult.failure(exception);
    }
    
    @Override
    public int getPriority() {
        return 95; // High priority for error handler
    }
    
    @Override
    public String getHandlerName() {
        return "ErrorCommandHandler";
    }
}