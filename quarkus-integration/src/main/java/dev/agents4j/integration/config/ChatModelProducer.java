package dev.agents4j.integration.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;

/**
 * CDI producer for ChatModel instances.
 * Provides configured ChatModel beans for dependency injection.
 */
@ApplicationScoped
public class ChatModelProducer {

    private static final Logger LOG = Logger.getLogger(ChatModelProducer.class);

    @ConfigProperty(name = "openai.api-key", defaultValue = "your-api-key-here")
    String apiKey;

    @ConfigProperty(name = "openai.model-name", defaultValue = "gpt-3.5-turbo")
    String modelName;

    @ConfigProperty(name = "openai.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "openai.max-tokens", defaultValue = "2048")
    Integer maxTokens;

    @ConfigProperty(name = "openai.timeout", defaultValue = "60s")
    Duration timeout;

    public ChatModelProducer() {
        LOG.info("=== ChatModelProducer Constructor ===");
    }

    /**
     * Produces a ChatModel instance configured with OpenAI settings.
     */
    @Produces
    @Singleton
    public ChatModel chatModel() {
        LOG.info("=== ChatModelProducer.chatModel() START ===");
        LOG.info("Configuration values:");
        LOG.info("  API Key: " + (apiKey != null ? maskApiKey(apiKey) : "NULL"));
        LOG.info("  Model Name: " + modelName);
        LOG.info("  Temperature: " + temperature);
        LOG.info("  Max Tokens: " + maxTokens);
        LOG.info("  Timeout: " + timeout);
        
        // Validate configuration
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOG.error("API key is null or empty");
            throw new RuntimeException("OpenAI API key is required but not configured");
        }
        
        if ("your-api-key-here".equals(apiKey)) {
            LOG.warn("Using default placeholder API key. This will cause API failures.");
            LOG.warn("Set OPENAI_API_KEY environment variable or configure openai.api-key property.");
        }
        
        if (modelName == null || modelName.trim().isEmpty()) {
            LOG.error("Model name is null or empty");
            throw new RuntimeException("OpenAI model name is required but not configured");
        }
        
        if (temperature == null || temperature < 0.0 || temperature > 2.0) {
            LOG.warn("Invalid temperature value: " + temperature + ". Using default 0.7");
            temperature = 0.7;
        }
        
        if (maxTokens == null || maxTokens <= 0) {
            LOG.warn("Invalid max tokens value: " + maxTokens + ". Using default 2048");
            maxTokens = 2048;
        }
        
        if (timeout == null) {
            LOG.warn("Timeout is null. Using default 60s");
            timeout = Duration.ofSeconds(60);
        }

        try {
            LOG.info("Building OpenAI ChatModel...");
            ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(true)  // Enable request logging for debugging
                .logResponses(true) // Enable response logging for debugging
                .build();

            LOG.info("ChatModel created successfully");
            LOG.info("ChatModel class: " + model.getClass().getSimpleName());
            LOG.info("=== ChatModelProducer.chatModel() SUCCESS ===");
            return model;
            
        } catch (Exception e) {
            LOG.error("=== ChatModelProducer.chatModel() ERROR ===");
            LOG.error("Failed to create ChatModel with configuration:");
            LOG.error("  API Key: " + maskApiKey(apiKey));
            LOG.error("  Model: " + modelName);
            LOG.error("  Temperature: " + temperature);
            LOG.error("  Max Tokens: " + maxTokens);
            LOG.error("  Timeout: " + timeout);
            LOG.error("Exception type: " + e.getClass().getSimpleName());
            LOG.error("Exception message: " + e.getMessage());
            LOG.error("Full stack trace:", e);
            
            // Log root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            if (rootCause != e) {
                LOG.error("Root cause: " + rootCause.getClass().getSimpleName() + " - " + rootCause.getMessage());
            }
            
            throw new RuntimeException("ChatModel configuration failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Masks the API key for logging purposes, showing only first and last few characters
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null) return "NULL";
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}