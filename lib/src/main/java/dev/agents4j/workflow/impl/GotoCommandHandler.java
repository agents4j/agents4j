package dev.agents4j.workflow.impl;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.ExecutionResult;

import java.util.Map;

/**
 * Handles GOTO commands by directing workflow execution to a specific target node.
 */
public class GotoCommandHandler<I, O> implements CommandHandler<I, O> {
    
    @Override
    public boolean canHandle(WorkflowCommand.CommandType type) {
        return type == WorkflowCommand.CommandType.GOTO;
    }
    
    @Override
    public ExecutionResult<I, O> handle(WorkflowCommand<I> command, 
                                       WorkflowState state, 
                                       Map<String, Object> context) {
        
        // Validate that the GOTO command has a target node
        String targetNodeId = command.getTargetNodeId()
                .orElseThrow(() -> new IllegalArgumentException("GOTO command missing target node ID"));
        
        // Apply any state updates from the command
        WorkflowState updatedState = state;
        if (!command.getStateUpdates().isEmpty()) {
            updatedState = state.withUpdates(command.getStateUpdates());
        }
        
        // Get the next input for the target node
        I nextInput = command.getNextInput().orElse(null);
        
        // Add routing metadata to context
        context.put("goto_target", targetNodeId);
        context.put("goto_timestamp", System.currentTimeMillis());
        command.getMetadata().forEach(context::put);
        
        return ExecutionResult.goTo(targetNodeId, updatedState, nextInput);
    }
    
    @Override
    public int getPriority() {
        return 85; // High priority for goto handler
    }
    
    @Override
    public String getHandlerName() {
        return "GotoCommandHandler";
    }
}