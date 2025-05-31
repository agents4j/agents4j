/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Unchecked exception thrown when agent execution fails.
 * This exception provides structured error information with context and workflow details.
 */
public class AgentExecutionException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String workflowName;
    private final Map<String, Object> context;
    
    /**
     * Creates a new AgentExecutionException.
     *
     * @param workflowName The name of the workflow that failed
     * @param message The error message
     */
    public AgentExecutionException(String workflowName, String message) {
        this(workflowName, message, null, Collections.emptyMap());
    }
    
    /**
     * Creates a new AgentExecutionException with a cause.
     *
     * @param workflowName The name of the workflow that failed
     * @param message The error message
     * @param cause The underlying cause
     */
    public AgentExecutionException(String workflowName, String message, Throwable cause) {
        this(workflowName, message, cause, Collections.emptyMap());
    }
    
    /**
     * Creates a new AgentExecutionException with context.
     *
     * @param workflowName The name of the workflow that failed
     * @param message The error message
     * @param cause The underlying cause
     * @param context Additional context information
     */
    public AgentExecutionException(String workflowName, String message, Throwable cause, Map<String, Object> context) {
        super(formatMessage(workflowName, message), cause);
        this.workflowName = workflowName;
        this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
        this.context.put("workflowName", workflowName);
        this.context.put("timestamp", System.currentTimeMillis());
    }
    
    /**
     * Gets the name of the workflow that failed.
     *
     * @return The workflow name
     */
    public String getWorkflowName() {
        return workflowName;
    }
    
    /**
     * Gets an unmodifiable view of the context map.
     *
     * @return The context map
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
    
    /**
     * Gets a context value.
     *
     * @param key The context key
     * @param <T> The expected type of the value
     * @return The context value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key) {
        return (T) context.get(key);
    }
    
    /**
     * Gets a context value with a default.
     *
     * @param key The context key
     * @param defaultValue The default value if key is not found
     * @param <T> The expected type of the value
     * @return The context value, or defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, T defaultValue) {
        return (T) context.getOrDefault(key, defaultValue);
    }
    
    /**
     * Adds context information to this exception.
     *
     * @param key The context key
     * @param value The context value
     * @return This exception instance for method chaining
     */
    public AgentExecutionException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Gets the timestamp when this exception was created.
     *
     * @return The timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return getContextValue("timestamp", 0L);
    }
    
    /**
     * Creates a formatted error message.
     *
     * @param workflowName The workflow name
     * @param message The original message
     * @return The formatted message
     */
    private static String formatMessage(String workflowName, String message) {
        return String.format("[Workflow: %s] %s", workflowName != null ? workflowName : "Unknown", message);
    }
    
    /**
     * Returns a detailed string representation of this exception.
     *
     * @return Detailed string representation
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessage());
        
        if (!context.isEmpty()) {
            sb.append("\nContext:");
            context.forEach((key, value) -> 
                sb.append(String.format("\n  %s: %s", key, value))
            );
        }
        
        return sb.toString();
    }
}