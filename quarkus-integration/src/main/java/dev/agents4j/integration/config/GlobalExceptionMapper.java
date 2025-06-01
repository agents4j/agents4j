package dev.agents4j.integration.config;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception mapper for debugging HTTP errors and providing detailed error responses.
 * This mapper catches various exceptions and provides comprehensive debugging information.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("=== GlobalExceptionMapper caught exception ===");
        LOG.error("Exception type: " + exception.getClass().getSimpleName());
        LOG.error("Exception message: " + exception.getMessage());
        LOG.error("Full stack trace:", exception);

        // Log the full exception chain
        Throwable current = exception;
        int depth = 0;
        while (current != null && depth < 10) {
            LOG.error("  [" + depth + "] " + current.getClass().getSimpleName() + ": " + current.getMessage());
            current = current.getCause();
            depth++;
        }

        // Handle specific exception types
        if (exception instanceof JsonParseException) {
            return handleJsonParseException((JsonParseException) exception);
        }
        
        if (exception instanceof JsonMappingException) {
            return handleJsonMappingException((JsonMappingException) exception);
        }
        
        if (exception instanceof InvalidFormatException) {
            return handleInvalidFormatException((InvalidFormatException) exception);
        }
        
        if (exception instanceof MismatchedInputException) {
            return handleMismatchedInputException((MismatchedInputException) exception);
        }
        
        if (exception instanceof BadRequestException) {
            return handleBadRequestException((BadRequestException) exception);
        }
        
        if (exception instanceof WebApplicationException) {
            return handleWebApplicationException((WebApplicationException) exception);
        }
        
        if (exception instanceof IllegalArgumentException) {
            return handleIllegalArgumentException((IllegalArgumentException) exception);
        }
        
        if (exception instanceof IOException) {
            return handleIOException((IOException) exception);
        }

        // Handle generic exceptions
        return handleGenericException(exception);
    }

    private Response handleJsonParseException(JsonParseException e) {
        LOG.error("JSON Parse Error at line " + e.getLocation().getLineNr() + 
                 ", column " + e.getLocation().getColumnNr());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "JSON Parse Error");
        errorDetails.put("message", "Invalid JSON format in request body");
        errorDetails.put("details", e.getOriginalMessage());
        errorDetails.put("line", e.getLocation().getLineNr());
        errorDetails.put("column", e.getLocation().getColumnNr());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleJsonMappingException(JsonMappingException e) {
        LOG.error("JSON Mapping Error: " + e.getPathReference());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "JSON Mapping Error");
        errorDetails.put("message", "Failed to map JSON to object");
        errorDetails.put("details", e.getOriginalMessage());
        errorDetails.put("path", e.getPathReference());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleInvalidFormatException(InvalidFormatException e) {
        LOG.error("Invalid Format Error for field: " + e.getPathReference());
        LOG.error("Invalid value: " + e.getValue());
        LOG.error("Target type: " + e.getTargetType());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Invalid Format Error");
        errorDetails.put("message", "Invalid value format in request");
        errorDetails.put("field", e.getPathReference());
        errorDetails.put("invalidValue", e.getValue());
        errorDetails.put("expectedType", e.getTargetType().getSimpleName());
        errorDetails.put("details", e.getOriginalMessage());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleMismatchedInputException(MismatchedInputException e) {
        LOG.error("Mismatched Input Error for path: " + e.getPathReference());
        LOG.error("Target type: " + e.getTargetType());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Mismatched Input Error");
        errorDetails.put("message", "Input does not match expected format");
        errorDetails.put("path", e.getPathReference());
        errorDetails.put("expectedType", e.getTargetType().getSimpleName());
        errorDetails.put("details", e.getOriginalMessage());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleBadRequestException(BadRequestException e) {
        LOG.error("Bad Request Exception: " + e.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Bad Request");
        errorDetails.put("message", e.getMessage() != null ? e.getMessage() : "Invalid request");
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleWebApplicationException(WebApplicationException e) {
        LOG.error("Web Application Exception with status: " + e.getResponse().getStatus());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Web Application Error");
        errorDetails.put("message", e.getMessage() != null ? e.getMessage() : "Request processing failed");
        errorDetails.put("httpStatus", e.getResponse().getStatus());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(e.getResponse().getStatus())
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleIllegalArgumentException(IllegalArgumentException e) {
        LOG.error("Illegal Argument Exception: " + e.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Invalid Argument");
        errorDetails.put("message", e.getMessage() != null ? e.getMessage() : "Invalid argument provided");
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleIOException(IOException e) {
        LOG.error("IO Exception: " + e.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "IO Error");
        errorDetails.put("message", "Failed to process request input/output");
        errorDetails.put("details", e.getMessage());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response handleGenericException(Throwable e) {
        LOG.error("Generic Exception: " + e.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An unexpected error occurred");
        errorDetails.put("exceptionType", e.getClass().getSimpleName());
        errorDetails.put("details", e.getMessage());
        errorDetails.put("debugInfo", createDebugInfo(e));
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorDetails)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Map<String, Object> createDebugInfo(Throwable e) {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("exceptionClass", e.getClass().getName());
        debugInfo.put("timestamp", java.time.Instant.now().toString());
        
        // Add stack trace (first few lines)
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            StringBuilder stackSummary = new StringBuilder();
            int limit = Math.min(5, stackTrace.length);
            for (int i = 0; i < limit; i++) {
                stackSummary.append(stackTrace[i].toString());
                if (i < limit - 1) stackSummary.append("\n");
            }
            debugInfo.put("stackTrace", stackSummary.toString());
        }
        
        // Add root cause if different
        Throwable rootCause = e;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        if (rootCause != e) {
            Map<String, String> rootCauseInfo = new HashMap<>();
            rootCauseInfo.put("class", rootCause.getClass().getName());
            rootCauseInfo.put("message", rootCause.getMessage());
            debugInfo.put("rootCause", rootCauseInfo);
        }
        
        return debugInfo;
    }
}