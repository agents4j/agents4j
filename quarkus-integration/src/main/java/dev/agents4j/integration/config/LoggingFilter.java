package dev.agents4j.integration.config;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS filter for logging HTTP requests and responses to aid in debugging.
 * This filter captures and logs detailed information about incoming requests and outgoing responses.
 */
@Provider
@Priority(1000)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);
    private static final String REQUEST_START_TIME = "REQUEST_START_TIME";
    private static final String REQUEST_BODY = "REQUEST_BODY";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!LOG.isDebugEnabled()) {
            return;
        }

        // Record start time
        requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());

        LOG.debug("=== HTTP REQUEST START ===");
        LOG.debug("Method: " + requestContext.getMethod());
        LOG.debug("URI: " + requestContext.getUriInfo().getRequestUri());
        LOG.debug("Path: " + requestContext.getUriInfo().getPath());
        
        // Log query parameters
        MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
        if (!queryParams.isEmpty()) {
            LOG.debug("Query Parameters:");
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                LOG.debug("  " + entry.getKey() + " = " + entry.getValue());
            }
        }

        // Log headers
        LOG.debug("Headers:");
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = entry.getValue();
            
            // Mask sensitive headers
            if (isSensitiveHeader(headerName)) {
                LOG.debug("  " + headerName + " = [MASKED]");
            } else {
                LOG.debug("  " + headerName + " = " + headerValues);
            }
        }

        // Log request body if present
        if (requestContext.hasEntity()) {
            try {
                InputStream entityStream = requestContext.getEntityStream();
                String requestBody = readEntityStream(entityStream);
                
                // Store the body for potential use in response logging
                requestContext.setProperty(REQUEST_BODY, requestBody);
                
                // Reset the stream for the actual request processing
                requestContext.setEntityStream(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));
                
                LOG.debug("Request Body:");
                LOG.debug(formatJsonForLogging(requestBody));
            } catch (Exception e) {
                LOG.debug("Could not read request body: " + e.getMessage());
            }
        } else {
            LOG.debug("Request Body: [empty]");
        }

        LOG.debug("=== HTTP REQUEST END ===");
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (!LOG.isDebugEnabled()) {
            return;
        }

        // Calculate request duration
        Long startTime = (Long) requestContext.getProperty(REQUEST_START_TIME);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : -1;

        LOG.debug("=== HTTP RESPONSE START ===");
        LOG.debug("Status: " + responseContext.getStatus() + " " + responseContext.getStatusInfo().getReasonPhrase());
        LOG.debug("Duration: " + duration + " ms");
        
        // Log response headers
        LOG.debug("Response Headers:");
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();
        for (Map.Entry<String, List<Object>> entry : responseHeaders.entrySet()) {
            LOG.debug("  " + entry.getKey() + " = " + entry.getValue());
        }

        // Log response body if present
        if (responseContext.hasEntity()) {
            try {
                Object entity = responseContext.getEntity();
                if (entity != null) {
                    LOG.debug("Response Body Type: " + entity.getClass().getSimpleName());
                    LOG.debug("Response Body:");
                    LOG.debug(formatEntityForLogging(entity));
                }
            } catch (Exception e) {
                LOG.debug("Could not log response body: " + e.getMessage());
            }
        } else {
            LOG.debug("Response Body: [empty]");
        }

        // Log original request info for correlation
        String originalRequestBody = (String) requestContext.getProperty(REQUEST_BODY);
        if (originalRequestBody != null) {
            LOG.debug("Original Request Body:");
            LOG.debug(formatJsonForLogging(originalRequestBody));
        }

        LOG.debug("=== HTTP RESPONSE END ===");
    }

    private String readEntityStream(InputStream entityStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = entityStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    private boolean isSensitiveHeader(String headerName) {
        if (headerName == null) return false;
        String lowerHeaderName = headerName.toLowerCase();
        return lowerHeaderName.contains("authorization") ||
               lowerHeaderName.contains("api-key") ||
               lowerHeaderName.contains("token") ||
               lowerHeaderName.contains("password") ||
               lowerHeaderName.contains("secret");
    }

    private String formatJsonForLogging(String json) {
        if (json == null || json.trim().isEmpty()) {
            return "[empty]";
        }
        
        // Basic JSON formatting for readability (simple indentation)
        try {
            // Remove extra whitespace and format basic structure
            json = json.trim();
            if (json.length() > 1000) {
                return json.substring(0, 1000) + "... [truncated]";
            }
            return json;
        } catch (Exception e) {
            return json; // Return as-is if formatting fails
        }
    }

    private String formatEntityForLogging(Object entity) {
        if (entity == null) {
            return "[null]";
        }
        
        try {
            String entityStr = entity.toString();
            if (entityStr.length() > 2000) {
                return entityStr.substring(0, 2000) + "... [truncated]";
            }
            return entityStr;
        } catch (Exception e) {
            return "[Could not serialize entity: " + e.getMessage() + "]";
        }
    }
}