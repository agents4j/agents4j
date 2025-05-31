/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when workflow registration fails.
 */
public class WorkflowRegistrationException extends Exception {
    
    private final String workflowType;
    private final Map<String, Object> context;
    
    /**
     * Creates a new WorkflowRegistrationException.
     *
     * @param workflowType The workflow type that failed to register
     * @param message The error message
     */
    public WorkflowRegistrationException(String workflowType, String message) {
        this(workflowType, message, null, Collections.emptyMap());
    }
    
    /**
     * Creates a new WorkflowRegistrationException with a cause.
     *
     * @param workflowType The workflow type that failed to register
     * @param message The error message
     * @param cause The underlying cause
     */
    public WorkflowRegistrationException(String workflowType, String message, Throwable cause) {
        this(workflowType, message, cause, Collections.emptyMap());
    }
    
    /**
     * Creates a new WorkflowRegistrationException with context.
     *
     * @param workflowType The workflow type that failed to register
     * @param message The error message
     * @param cause The underlying cause
     * @param context Additional context information
     */
    public WorkflowRegistrationException(String workflowType, String message, Throwable cause, Map<String, Object> context) {
        super(formatMessage(workflowType, message), cause);
        this.workflowType = workflowType;
        this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
        this.context.put("workflowType", workflowType);
        this.context.put("timestamp", System.currentTimeMillis());
    }
    
    /**
     * Gets the workflow type that failed to register.
     *
     * @return The workflow type
     */
    public String getWorkflowType() {
        return workflowType;
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
     * Adds context information to this exception.
     *
     * @param key The context key
     * @param value The context value
     * @return This exception instance for method chaining
     */
    public WorkflowRegistrationException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Creates a formatted error message.
     *
     * @param workflowType The workflow type
     * @param message The original message
     * @return The formatted message
     */
    private static String formatMessage(String workflowType, String message) {
        return String.format("[WorkflowType: %s] %s", workflowType != null ? workflowType : "Unknown", message);
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