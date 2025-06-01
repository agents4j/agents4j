package dev.agents4j.integration.examples;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Debug and health check resource for troubleshooting the quarkus-integration service.
 */
@Path("/api/debug")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DebugResource {

    private static final Logger LOG = Logger.getLogger(DebugResource.class);

    @Inject
    ChatModel chatModel;

    @ConfigProperty(name = "openai.api-key", defaultValue = "not-configured")
    String apiKey;

    @ConfigProperty(name = "openai.model-name", defaultValue = "not-configured")
    String modelName;

    /**
     * Basic health check endpoint
     */
    @GET
    @Path("/health")
    public Response health() {
        LOG.info("Health check requested");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        health.put("service", "agents4j-quarkus-integration");
        
        return Response.ok(health).build();
    }

    /**
     * Detailed system information for debugging
     */
    @GET
    @Path("/info")
    public Response info() {
        LOG.info("Debug info requested");
        
        Map<String, Object> info = new HashMap<>();
        info.put("timestamp", Instant.now().toString());
        info.put("service", "agents4j-quarkus-integration");
        
        // Java information
        Map<String, String> javaInfo = new HashMap<>();
        javaInfo.put("version", System.getProperty("java.version"));
        javaInfo.put("vendor", System.getProperty("java.vendor"));
        javaInfo.put("runtime", System.getProperty("java.runtime.name"));
        info.put("java", javaInfo);
        
        // Configuration information
        Map<String, Object> config = new HashMap<>();
        config.put("apiKeyConfigured", !apiKey.equals("not-configured") && !apiKey.equals("your-api-key-here"));
        config.put("apiKeyMasked", maskApiKey(apiKey));
        config.put("modelName", modelName);
        config.put("chatModelAvailable", chatModel != null);
        if (chatModel != null) {
            config.put("chatModelClass", chatModel.getClass().getSimpleName());
        }
        info.put("configuration", config);
        
        // Memory information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Long> memory = new HashMap<>();
        memory.put("totalMemory", runtime.totalMemory());
        memory.put("freeMemory", runtime.freeMemory());
        memory.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memory.put("maxMemory", runtime.maxMemory());
        info.put("memory", memory);
        
        return Response.ok(info).build();
    }

    /**
     * Test endpoint that validates request parsing without calling external APIs
     */
    @POST
    @Path("/validate-request")
    public Response validateRequest(SnarkyResponseResource.QuestionRequest request) {
        LOG.info("Request validation test requested");
        
        Map<String, Object> validation = new HashMap<>();
        validation.put("timestamp", Instant.now().toString());
        validation.put("requestReceived", request != null);
        
        if (request == null) {
            validation.put("error", "Request is null");
            validation.put("status", "FAILED");
            return Response.status(Response.Status.BAD_REQUEST).entity(validation).build();
        }
        
        validation.put("questionPresent", request.question() != null);
        validation.put("questionEmpty", request.question() == null || request.question().trim().isEmpty());
        
        if (request.question() != null) {
            validation.put("questionLength", request.question().length());
            validation.put("questionContent", request.question());
        }
        
        boolean isValid = request.question() != null && !request.question().trim().isEmpty();
        validation.put("status", isValid ? "VALID" : "INVALID");
        
        if (!isValid) {
            validation.put("error", "Question field is required and cannot be empty");
            return Response.status(Response.Status.BAD_REQUEST).entity(validation).build();
        }
        
        return Response.ok(validation).build();
    }

    /**
     * Test ChatModel availability without making external API calls
     */
    @GET
    @Path("/chatmodel-status")
    public Response chatModelStatus() {
        LOG.info("ChatModel status check requested");
        
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", Instant.now().toString());
        status.put("chatModelInjected", chatModel != null);
        
        if (chatModel != null) {
            status.put("chatModelClass", chatModel.getClass().getName());
            status.put("chatModelSimpleClass", chatModel.getClass().getSimpleName());
        }
        
        status.put("apiKeyConfigured", !apiKey.equals("not-configured") && !apiKey.equals("your-api-key-here"));
        status.put("modelNameConfigured", !modelName.equals("not-configured"));
        
        return Response.ok(status).build();
    }

    /**
     * Comprehensive system check
     */
    @GET
    @Path("/system-check")
    public Response systemCheck() {
        LOG.info("System check requested");
        
        Map<String, Object> systemCheck = new HashMap<>();
        systemCheck.put("timestamp", Instant.now().toString());
        
        // Check all critical components
        boolean allGood = true;
        Map<String, Object> checks = new HashMap<>();
        
        // ChatModel check
        boolean chatModelOk = chatModel != null;
        checks.put("chatModel", Map.of("status", chatModelOk ? "OK" : "FAILED", "available", chatModelOk));
        if (!chatModelOk) allGood = false;
        
        // Configuration check
        boolean configOk = !apiKey.equals("not-configured") && !apiKey.equals("your-api-key-here");
        checks.put("configuration", Map.of("status", configOk ? "OK" : "WARNING", "apiKeySet", configOk));
        
        // Memory check
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        boolean memoryOk = memoryUsagePercent < 90;
        checks.put("memory", Map.of("status", memoryOk ? "OK" : "WARNING", "usagePercent", memoryUsagePercent));
        
        systemCheck.put("checks", checks);
        systemCheck.put("overallStatus", allGood ? "HEALTHY" : "DEGRADED");
        
        return Response.ok(systemCheck).build();
    }

    /**
     * Masks the API key for safe logging
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.equals("not-configured")) {
            return "not-configured";
        }
        if (apiKey.equals("your-api-key-here")) {
            return "default-placeholder";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}