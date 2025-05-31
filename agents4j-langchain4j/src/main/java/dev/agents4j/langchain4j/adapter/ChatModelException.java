/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.langchain4j.adapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when chat model operations fail.
 * This exception provides structured error information specific to chat model operations.
 */
public class ChatModelException extends Exception {
    
    private final String providerName;
    private final String modelName;
    private final Map<String, Object> context;
    
    /**
     * Creates a new ChatModelException.
     *
     * @param providerName The name of the chat model provider
     * @param modelName The name of the model
     * @param message The error message
     */
    public ChatModelException(String providerName, String modelName, String message) {
        this(providerName, modelName, message, null, Collections.emptyMap());
    }
    
    /**
     * Creates a new ChatModelException with a cause.
     *
     * @param providerName The name of the chat model provider
     * @param modelName The name of the model
     * @param message The error message
     * @param cause The underlying cause
     */
    public ChatModelException(String providerName, String modelName, String message, Throwable cause) {
        this(providerName, modelName, message, cause, Collections.emptyMap());
    }
    
    /**
     * Creates a new ChatModelException with context.
     *
     * @param providerName The name of the chat model provider
     * @param modelName The name of the model
     * @param message The error message
     * @param cause The underlying cause
     * @param context Additional context information
     */
    public ChatModelException(String providerName, String modelName, String message, Throwable cause, Map<String, Object> context) {
        super(formatMessage(providerName, modelName, message), cause);
        this.providerName = providerName;
        this.modelName = modelName;
        this.context = new HashMap<>(context != null ? context : Collections.emptyMap());
        this.context.put("providerName", providerName);
        this.context.put("modelName", modelName);
        this.context.put("timestamp", System.currentTimeMillis());
    }
    
    /**
     * Gets the name of the chat model provider.
     *
     * @return The provider name
     */
    public String getProviderName() {
        return providerName;
    }
    
    /**
     * Gets the name of the model.
     *
     * @return The model name
     */
    public String getModelName() {
        return modelName;
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
    public ChatModelException addContext(String key, Object value) {
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
     * @param providerName The provider name
     * @param modelName The model name
     * @param message The original message
     * @return The formatted message
     */
    private static String formatMessage(String providerName, String modelName, String message) {
        return String.format("[Provider: %s, Model: %s] %s", 
            providerName != null ? providerName : "Unknown",
            modelName != null ? modelName : "Unknown",
            message);
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
    
    /**
     * Checks if this exception is due to rate limiting.
     *
     * @return true if this is a rate limit error
     */
    public boolean isRateLimitError() {
        return getContextValue("errorType", "").toString().toLowerCase().contains("rate");
    }
    
    /**
     * Checks if this exception is due to authentication issues.
     *
     * @return true if this is an authentication error
     */
    public boolean isAuthenticationError() {
        return getContextValue("errorType", "").toString().toLowerCase().contains("auth");
    }
    
    /**
     * Checks if this exception is due to quota exceeded.
     *
     * @return true if this is a quota error
     */
    public boolean isQuotaError() {
        return getContextValue("errorType", "").toString().toLowerCase().contains("quota");
    }
}