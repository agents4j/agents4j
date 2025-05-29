/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.adapter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction layer for chat model providers.
 * This interface follows the Dependency Inversion Principle by allowing
 * different LLM providers to be plugged in without changing core workflow logic.
 */
public interface ChatModelAdapter {

    /**
     * Sends a chat message and returns the response synchronously.
     *
     * @param prompt The input prompt or message
     * @param context Additional context information for the chat
     * @return The response from the chat model
     * @throws ChatModelException if the chat operation fails
     */
    String chat(String prompt, Map<String, Object> context) throws ChatModelException;

    /**
     * Sends a chat message and returns the response asynchronously.
     *
     * @param prompt The input prompt or message
     * @param context Additional context information for the chat
     * @return CompletableFuture containing the response from the chat model
     */
    CompletableFuture<String> chatAsync(String prompt, Map<String, Object> context);

    /**
     * Sends a chat message with system prompt and returns the response.
     *
     * @param systemPrompt The system prompt to set context for the model
     * @param userPrompt The user's input prompt
     * @param context Additional context information for the chat
     * @return The response from the chat model
     * @throws ChatModelException if the chat operation fails
     */
    String chatWithSystem(String systemPrompt, String userPrompt, Map<String, Object> context) throws ChatModelException;

    /**
     * Gets the provider name for this adapter.
     *
     * @return The provider name (e.g., "OpenAI", "Anthropic", "Local")
     */
    String getProviderName();

    /**
     * Gets the model name or identifier.
     *
     * @return The model name
     */
    String getModelName();

    /**
     * Checks if the adapter is available and ready to use.
     *
     * @return true if the adapter is ready, false otherwise
     */
    boolean isAvailable();

    /**
     * Gets configuration information for this adapter.
     *
     * @return Configuration map
     */
    Map<String, Object> getConfiguration();

    /**
     * Validates that the adapter is properly configured.
     *
     * @throws ChatModelException if configuration is invalid
     */
    void validateConfiguration() throws ChatModelException;

    /**
     * Gets the maximum number of tokens this model can handle.
     *
     * @return Maximum token count, or -1 if unknown
     */
    default int getMaxTokens() {
        return -1;
    }

    /**
     * Estimates the token count for the given text.
     *
     * @param text The text to count tokens for
     * @return Estimated token count, or -1 if estimation is not supported
     */
    default int estimateTokenCount(String text) {
        return -1;
    }

    /**
     * Gets the cost per token for this model (in USD).
     *
     * @return Cost per token, or -1 if cost information is not available
     */
    default double getCostPerToken() {
        return -1.0;
    }
}