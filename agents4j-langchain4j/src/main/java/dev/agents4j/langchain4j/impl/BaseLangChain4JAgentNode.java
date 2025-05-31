package dev.agents4j.langchain4j.impl;

import dev.agents4j.api.AgentNode;
import dev.agents4j.langchain4j.LangChain4JAgentNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Base implementation of LangChain4JAgentNode that provides common functionality
 * for working with LangChain4J chat models and memory.
 * 
 * @param <I> Input type
 * @param <O> Output type
 */
public abstract class BaseLangChain4JAgentNode<I, O> 
    implements LangChain4JAgentNode<I, O> {

    private final String name;
    private final ChatModel model;
    private final ChatMemory memory;
    private final String systemPrompt;

    /**
     * Creates a new BaseLangChain4JAgentNode.
     *
     * @param name The name of the agent node
     * @param model The ChatModel to use
     * @param memory The ChatMemory to use, can be null if no memory is needed
     * @param systemPrompt The system prompt to use, can be null if no system prompt is needed
     */
    protected BaseLangChain4JAgentNode(
        String name,
        ChatModel model,
        ChatMemory memory,
        String systemPrompt
    ) {
        this.name = name;
        this.model = model;
        this.memory = memory;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ChatModel getModel() {
        return model;
    }

    @Override
    public Optional<ChatMemory> getMemory() {
        return Optional.ofNullable(memory);
    }

    @Override
    public O process(I input, Map<String, Object> context) {
        try {
            // Create user message from input
            UserMessage userMessage = createUserMessage(input, context);
            
            // Get AI response
            AiMessage aiMessage = getAiResponse(userMessage, context);
            
            // Convert AI response to output
            return createOutput(aiMessage, input, context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process input with LangChain4J agent: " + name, e);
        }
    }

    @Override
    public CompletableFuture<O> processAsync(I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> process(input, context));
    }

    /**
     * Creates a UserMessage from the input.
     * Subclasses must implement this to convert their input type to a UserMessage.
     *
     * @param input The input to convert
     * @param context Additional context information
     * @return UserMessage for the LangChain4J model
     */
    protected abstract UserMessage createUserMessage(I input, Map<String, Object> context);

    /**
     * Gets the AI response from the model.
     * This method handles the interaction with the ChatModel and ChatMemory.
     *
     * @param userMessage The user message to send
     * @param context Additional context information
     * @return The AI response
     */
    protected AiMessage getAiResponse(
        UserMessage userMessage,
        Map<String, Object> context
    ) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // Add system message if configured
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        
        // Add messages from memory if available
        if (memory != null) {
            messages.addAll(memory.messages());
        }
        
        // Add the current user message
        messages.add(userMessage);
        
        // Get AI response
        AiMessage aiMessage = model.generate(messages);
        
        // Store messages in memory if available
        if (memory != null) {
            memory.add(userMessage);
            memory.add(aiMessage);
        }
        
        return aiMessage;
    }

    /**
     * Creates the output from the AI response.
     * Subclasses must implement this to convert the AiMessage to their output type.
     *
     * @param aiMessage The AI response
     * @param originalInput The original input for reference
     * @param context Additional context information
     * @return The converted output
     */
    protected abstract O createOutput(
        AiMessage aiMessage,
        I originalInput,
        Map<String, Object> context
    );

    /**
     * Gets the system prompt being used.
     *
     * @return The system prompt, or null if none is configured
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }
}