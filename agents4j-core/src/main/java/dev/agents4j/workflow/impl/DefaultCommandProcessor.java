package dev.agents4j.workflow.impl;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.CommandHandler;
import dev.agents4j.workflow.api.CommandProcessor;
import dev.agents4j.workflow.api.ExecutionResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of CommandProcessor that manages command handlers.
 * Thread-safe implementation that supports multiple handlers per command type
 * with priority-based selection.
 */
public class DefaultCommandProcessor<I, O> implements CommandProcessor<I, O> {
    
    private final Map<WorkflowCommand.CommandType, List<CommandHandler<I, O>>> handlers = new ConcurrentHashMap<>();
    
    @Override
    public void registerHandler(CommandHandler<I, O> handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        for (WorkflowCommand.CommandType type : WorkflowCommand.CommandType.values()) {
            if (handler.canHandle(type)) {
                handlers.computeIfAbsent(type, k -> new ArrayList<>())
                        .add(handler);
                // Sort by priority (highest first)
                handlers.get(type).sort((h1, h2) -> Integer.compare(h2.getPriority(), h1.getPriority()));
            }
        }
    }
    
    @Override
    public boolean unregisterHandler(CommandHandler<I, O> handler) {
        if (handler == null) {
            return false;
        }
        
        boolean removed = false;
        for (WorkflowCommand.CommandType type : WorkflowCommand.CommandType.values()) {
            List<CommandHandler<I, O>> typeHandlers = handlers.get(type);
            if (typeHandlers != null) {
                removed |= typeHandlers.remove(handler);
                // Clean up empty lists
                if (typeHandlers.isEmpty()) {
                    handlers.remove(type);
                }
            }
        }
        return removed;
    }
    
    @Override
    public ExecutionResult<I, O> process(WorkflowCommand<I> command, 
                                        WorkflowState state, 
                                        Map<String, Object> context) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("State cannot be null");
        }
        if (context == null) {
            context = new HashMap<>();
        }
        
        List<CommandHandler<I, O>> typeHandlers = handlers.get(command.getType());
        
        if (typeHandlers == null || typeHandlers.isEmpty()) {
            return ExecutionResult.failure(new dev.agents4j.api.exception.WorkflowExecutionException(
                "No handler registered for command type: " + command.getType()));
        }
        
        // Use the highest priority handler
        CommandHandler<I, O> handler = typeHandlers.get(0);
        
        try {
            return handler.handle(command, state, context);
        } catch (Exception e) {
            return ExecutionResult.failure(new dev.agents4j.api.exception.WorkflowExecutionException(
                "Handler " + handler.getHandlerName() + " failed to process command: " + e.getMessage(), e));
        }
    }
    
    @Override
    public boolean hasHandlerFor(WorkflowCommand.CommandType commandType) {
        List<CommandHandler<I, O>> typeHandlers = handlers.get(commandType);
        return typeHandlers != null && !typeHandlers.isEmpty();
    }
    
    @Override
    public int getHandlerCount() {
        return handlers.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Gets all registered handlers for a specific command type.
     *
     * @param commandType The command type
     * @return Unmodifiable list of handlers for the command type
     */
    public List<CommandHandler<I, O>> getHandlersFor(WorkflowCommand.CommandType commandType) {
        List<CommandHandler<I, O>> typeHandlers = handlers.get(commandType);
        return typeHandlers != null ? Collections.unmodifiableList(typeHandlers) : Collections.emptyList();
    }
    
    /**
     * Clears all registered handlers.
     */
    public void clearAllHandlers() {
        handlers.clear();
    }
    
    /**
     * Gets a summary of registered handlers by command type.
     *
     * @return Map of command types to handler count
     */
    public Map<WorkflowCommand.CommandType, Integer> getHandlerSummary() {
        Map<WorkflowCommand.CommandType, Integer> summary = new HashMap<>();
        handlers.forEach((type, handlerList) -> summary.put(type, handlerList.size()));
        return Collections.unmodifiableMap(summary);
    }
}