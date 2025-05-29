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

    /**
     * Produces a ChatModel instance configured with OpenAI settings.
     */
    @Produces
    @Singleton
    public ChatModel chatModel() {
        LOG.info("Creating ChatModel with model: " + modelName);
        
        if ("your-api-key-here".equals(apiKey)) {
            LOG.warn("Using default API key. Set OPENAI_API_KEY environment variable for production use.");
        }

        try {
            ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(false)
                .logResponses(false)
                .build();

            LOG.info("ChatModel created successfully");
            return model;
            
        } catch (Exception e) {
            LOG.error("Failed to create ChatModel", e);
            throw new RuntimeException("ChatModel configuration failed", e);
        }
    }
}