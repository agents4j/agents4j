package dev.agents4j.workflow.impl;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.ExecutionResult;
import dev.agents4j.workflow.StatefulWorkflowImpl;

import java.util.Map;

/**
 * Handles COMPLETE commands by extracting the final output and completing the workflow.
 */
public class CompleteCommandHandler<I, O> implements CommandHandler<I, O> {
    
    private final StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor;
    
    public CompleteCommandHandler(StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
        this.outputExtractor = outputExtractor;
    }
    
    @Override
    public boolean canHandle(WorkflowCommand.CommandType type) {
        return type == WorkflowCommand.CommandType.COMPLETE;
    }
    
    @Override
    public ExecutionResult<I, O> handle(WorkflowCommand<I> command, 
                                       WorkflowState state, 
                                       Map<String, Object> context) {
        try {
            I input = command.getNextInput().orElse(null);
            
            // Apply any state updates from the command
            WorkflowState updatedState = state;
            if (!command.getStateUpdates().isEmpty()) {
                updatedState = state.withUpdates(command.getStateUpdates());
            }
            
            // Add any command metadata to context
            command.getMetadata().forEach(context::put);
            
            // Use original input for output extraction if available, otherwise use current input
            I outputInput = input;
            if (context.containsKey("original_input")) {
                try {
                    outputInput = (I) context.get("original_input");
                } catch (ClassCastException e) {
                    // Fall back to current input if original input type doesn't match
                    outputInput = input;
                }
            }
            
            // Extract the final output using the configured extractor
            O output = outputExtractor.extractOutput(outputInput, updatedState, context);
            
            return ExecutionResult.completed(output, updatedState);
            
        } catch (Exception e) {
            return ExecutionResult.failure(new WorkflowExecutionException(
                "Failed to extract output during completion: " + e.getMessage(), e));
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for completion handler
    }
    
    @Override
    public String getHandlerName() {
        return "CompleteCommandHandler";
    }
}