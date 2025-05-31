package dev.agents4j.api.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all agent-related errors.
 * Provides structured error information with context and error codes.
 */
public abstract class AgentException extends Exception {
    
    private final ErrorCode errorCode;
    private final Map<String, Object> context;
    
    protected AgentException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, Collections.emptyMap());
    }
    
    protected AgentException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, Collections.emptyMap());
    }
    
    protected AgentException(ErrorCode errorCode, String message, Map<String, Object> context) {
        this(errorCode, message, null, context);
    }
    
    protected AgentException(ErrorCode errorCode, String message, Throwable cause, Map<String, Object> context) {
        super(formatMessage(errorCode, message), cause);
        this.errorCode = errorCode;
        this.context = new HashMap<>(context);
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }
    
    public AgentException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
    
    private static String formatMessage(ErrorCode errorCode, String message) {
        return String.format("[%s] %s", errorCode.getCode(), message);
    }
}