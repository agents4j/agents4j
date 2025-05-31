/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when workflow provider operations fail.
 */
public class WorkflowProviderException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final String providerType;
    private final Map<String, Object> context;
    
    /**
     * Creates a new WorkflowProviderException.
     *
     * @param providerType The provider type that failed
     * @param message The error message
     */
    public WorkflowProviderException(String providerType, String message) {
        this(providerType, message, null, Collections.emptyMap());
    }
    
    /**
     * Creates a new WorkflowProviderException with a cause.
     *
     * @param providerType The provider type that failed
     * @param message The error message
     * @param cause The underlying cause
     */
    public WorkflowProviderException(String providerType, String message, Throwable cause) {
        this(providerType, message, cause, Collections.emptyMap());
    }
    
    /**
     * Creates a new WorkflowProviderException with context.
     *
     * @param providerType The provider type that failed
     * @param message The error message
     * @param cause The underlying cause
     * @param context Additional context information
     */
    public WorkflowProviderException(String providerType, String message, Throwable cause, Map<String, Object> context) {
        super(formatMessage(providerType, message), cause);
        this.providerType = providerType;
        this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
        this.context.put("providerType", providerType);
        this.context.put("timestamp", System.currentTimeMillis());
    }
    
    /**
     * Gets the provider type that failed.
     *
     * @return The provider type
     */
    public String getProviderType() {
        return providerType;
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
    public WorkflowProviderException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Creates a formatted error message.
     *
     * @param providerType The provider type
     * @param message The original message
     * @return The formatted message
     */
    private static String formatMessage(String providerType, String message) {
        return String.format("[ProviderType: %s] %s", providerType != null ? providerType : "Unknown", message);
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