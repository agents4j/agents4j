/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.adapter;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LangChain4J implementation of ChatModelAdapter.
 * This adapter wraps LangChain4J ChatModel instances to provide a consistent interface
 * while following the Dependency Inversion Principle.
 */
public class LangChain4JChatModelAdapter implements ChatModelAdapter {

    private final ChatModel chatModel;
    private final String providerName;
    private final String modelName;
    private final Map<String, Object> configuration;

    /**
     * Creates a new LangChain4JChatModelAdapter.
     *
     * @param chatModel The LangChain4J ChatModel to wrap
     * @param providerName The name of the provider (e.g., "OpenAI", "Anthropic")
     * @param modelName The name of the specific model
     */
    public LangChain4JChatModelAdapter(
        ChatModel chatModel,
        String providerName,
        String modelName
    ) {
        this(chatModel, providerName, modelName, new HashMap<>());
    }

    /**
     * Creates a new LangChain4JChatModelAdapter with configuration.
     *
     * @param chatModel The LangChain4J ChatModel to wrap
     * @param providerName The name of the provider
     * @param modelName The name of the specific model
     * @param configuration Additional configuration for the adapter
     */
    public LangChain4JChatModelAdapter(
        ChatModel chatModel,
        String providerName,
        String modelName,
        Map<String, Object> configuration
    ) {
        if (chatModel == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Provider name cannot be null or empty"
            );
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Model name cannot be null or empty"
            );
        }

        this.chatModel = chatModel;
        this.providerName = providerName;
        this.modelName = modelName;
        this.configuration = new HashMap<>(
            configuration != null ? configuration : Map.of()
        );
    }

    @Override
    public String chat(String prompt, Map<String, Object> context)
        throws ChatModelException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ChatModelException(
                providerName,
                modelName,
                "Prompt cannot be null or empty"
            );
        }

        try {
            UserMessage userMessage = UserMessage.from(prompt);
            AiMessage response = chatModel.chat(userMessage).aiMessage();

            if (response == null) {
                throw new ChatModelException(
                    providerName,
                    modelName,
                    "Received null response from chat model",
                    null,
                    Map.of("prompt", prompt)
                );
            }

            return response.text();
        } catch (Exception e) {
            if (e instanceof ChatModelException) {
                throw e;
            }
            throw new ChatModelException(
                providerName,
                modelName,
                "Failed to generate chat response: " + e.getMessage(),
                e,
                Map.of("prompt", prompt, "errorType", determineErrorType(e))
            );
        }
    }

    @Override
    public CompletableFuture<String> chatAsync(
        String prompt,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chat(prompt, context);
            } catch (ChatModelException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public String chatWithSystem(
        String systemPrompt,
        String userPrompt,
        Map<String, Object> context
    ) throws ChatModelException {
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw new ChatModelException(
                providerName,
                modelName,
                "System prompt cannot be null or empty"
            );
        }
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new ChatModelException(
                providerName,
                modelName,
                "User prompt cannot be null or empty"
            );
        }

        try {
            SystemMessage systemMessage = SystemMessage.from(systemPrompt);
            UserMessage userMessage = UserMessage.from(userPrompt);

            AiMessage response = chatModel.chat(
                List.of(systemMessage, userMessage)
            ).aiMessage();

            if (response == null) {
                throw new ChatModelException(
                    providerName,
                    modelName,
                    "Received null response from chat model",
                    null,
                    Map.of(
                        "systemPrompt",
                        systemPrompt,
                        "userPrompt",
                        userPrompt
                    )
                );
            }

            return response.text();
        } catch (Exception e) {
            if (e instanceof ChatModelException) {
                throw e;
            }
            throw new ChatModelException(
                providerName,
                modelName,
                "Failed to generate chat response with system prompt: " +
                e.getMessage(),
                e,
                Map.of(
                    "systemPrompt",
                    systemPrompt,
                    "userPrompt",
                    userPrompt,
                    "errorType",
                    determineErrorType(e)
                )
            );
        }
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Try a simple test to see if the model is available
            validateConfiguration();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>(configuration);
        config.put("providerName", providerName);
        config.put("modelName", modelName);
        config.put("adapterType", "LangChain4J");
        return config;
    }

    @Override
    public void validateConfiguration() throws ChatModelException {
        if (chatModel == null) {
            throw new ChatModelException(
                providerName,
                modelName,
                "ChatModel is not initialized"
            );
        }

        // Additional validation can be added here based on specific provider requirements
        try {
            // Test with a minimal prompt to validate the model is working
            UserMessage testMessage = UserMessage.from("test");
            AiMessage response = chatModel.chat(testMessage).aiMessage();

            if (response == null) {
                throw new ChatModelException(
                    providerName,
                    modelName,
                    "Model validation failed: received null response"
                );
            }
        } catch (Exception e) {
            throw new ChatModelException(
                providerName,
                modelName,
                "Model validation failed: " + e.getMessage(),
                e,
                Map.of("errorType", determineErrorType(e))
            );
        }
    }

    @Override
    public int getMaxTokens() {
        // This would need to be configured based on the specific model
        return configuration.containsKey("maxTokens")
            ? ((Number) configuration.get("maxTokens")).intValue()
            : -1;
    }

    @Override
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Simple estimation: roughly 4 characters per token for most models
        // This is a rough approximation and should be replaced with proper tokenization
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Override
    public double getCostPerToken() {
        return configuration.containsKey("costPerToken")
            ? ((Number) configuration.get("costPerToken")).doubleValue()
            : -1.0;
    }

    /**
     * Gets the underlying LangChain4J ChatModel.
     * This method provides access to the wrapped model for advanced use cases.
     *
     * @return The wrapped ChatModel
     */
    public ChatModel getChatModel() {
        return chatModel;
    }

    /**
     * Creates a new adapter with additional configuration.
     *
     * @param additionalConfig Additional configuration to merge
     * @return A new adapter instance with merged configuration
     */
    public LangChain4JChatModelAdapter withConfiguration(
        Map<String, Object> additionalConfig
    ) {
        Map<String, Object> mergedConfig = new HashMap<>(this.configuration);
        if (additionalConfig != null) {
            mergedConfig.putAll(additionalConfig);
        }
        return new LangChain4JChatModelAdapter(
            chatModel,
            providerName,
            modelName,
            mergedConfig
        );
    }

    /**
     * Determines the error type based on the exception.
     *
     * @param exception The exception to analyze
     * @return A string describing the error type
     */
    private String determineErrorType(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return "unknown";
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("rate") || lowerMessage.contains("limit")) {
            return "rate_limit";
        } else if (
            lowerMessage.contains("auth") ||
            lowerMessage.contains("unauthorized") ||
            lowerMessage.contains("forbidden")
        ) {
            return "authentication";
        } else if (
            lowerMessage.contains("quota") || lowerMessage.contains("exceeded")
        ) {
            return "quota_exceeded";
        } else if (
            lowerMessage.contains("timeout") ||
            lowerMessage.contains("timed out")
        ) {
            return "timeout";
        } else if (
            lowerMessage.contains("network") ||
            lowerMessage.contains("connection")
        ) {
            return "network";
        } else {
            return "model_error";
        }
    }

    /**
     * Builder for creating LangChain4JChatModelAdapter instances.
     */
    public static class Builder {

        private ChatModel chatModel;
        private String providerName;
        private String modelName;
        private final Map<String, Object> configuration = new HashMap<>();

        /**
         * Sets the ChatModel to wrap.
         *
         * @param chatModel The ChatModel instance
         * @return This builder for method chaining
         */
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Sets the provider name.
         *
         * @param providerName The provider name
         * @return This builder for method chaining
         */
        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        /**
         * Sets the model name.
         *
         * @param modelName The model name
         * @return This builder for method chaining
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * Adds a configuration property.
         *
         * @param key The configuration key
         * @param value The configuration value
         * @return This builder for method chaining
         */
        public Builder configuration(String key, Object value) {
            this.configuration.put(key, value);
            return this;
        }

        /**
         * Sets the maximum tokens for this model.
         *
         * @param maxTokens The maximum token count
         * @return This builder for method chaining
         */
        public Builder maxTokens(int maxTokens) {
            return configuration("maxTokens", maxTokens);
        }

        /**
         * Sets the cost per token for this model.
         *
         * @param costPerToken The cost per token in USD
         * @return This builder for method chaining
         */
        public Builder costPerToken(double costPerToken) {
            return configuration("costPerToken", costPerToken);
        }

        /**
         * Builds the adapter instance.
         *
         * @return A new LangChain4JChatModelAdapter instance
         * @throws IllegalArgumentException if required parameters are missing
         */
        public LangChain4JChatModelAdapter build() {
            return new LangChain4JChatModelAdapter(
                chatModel,
                providerName,
                modelName,
                configuration
            );
        }
    }

    /**
     * Creates a new builder for LangChain4JChatModelAdapter.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
