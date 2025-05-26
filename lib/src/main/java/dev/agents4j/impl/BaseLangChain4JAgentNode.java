package dev.agents4j.impl;

import dev.agents4j.api.LangChain4JAgentNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Base implementation of the LangChain4JAgentNode interface.
 * This class handles the common functionality for LangChain4J-based agent nodes.
 *
 * @param <I> The input type for the agent
 * @param <O> The output type from the agent
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
        this.name = Objects.requireNonNull(name, "Agent name cannot be null");
        this.model = Objects.requireNonNull(
            model,
            "ChatLanguageModel cannot be null"
        );
        this.memory = memory; // Memory can be null
        this.systemPrompt = systemPrompt; // System prompt can be null

        // If memory and system prompt are both provided, add the system message to memory
        if (memory != null && systemPrompt != null && !systemPrompt.isEmpty()) {
            memory.add(new SystemMessage(systemPrompt));
        }
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
    public String getName() {
        return name;
    }

    @Override
    public O process(I input, Map<String, Object> context) {
        // Convert input to user message
        UserMessage userMessage = createUserMessage(input, context);

        // Get the AI response
        AiMessage aiMessage = getAiResponse(userMessage, context);

        // Convert AI message to output
        return convertToOutput(aiMessage, context);
    }

    /**
     * Create a UserMessage from the input.
     *
     * @param input The input to convert
     * @param context Additional context information
     * @return A UserMessage representing the input
     */
    protected abstract UserMessage createUserMessage(
        I input,
        Map<String, Object> context
    );

    /**
     * Get the AI response for the given UserMessage.
     *
     * @param userMessage The user message to process
     * @param context Additional context information
     * @return The AiMessage response
     */
    protected AiMessage getAiResponse(
        UserMessage userMessage,
        Map<String, Object> context
    ) {
        // Add the user message to memory if available
        if (memory != null) {
            memory.add(userMessage);
        }

        // Get all messages from memory or create a new message list with the system prompt
        List<ChatMessage> messages = new ArrayList<>();

        if (memory != null) {
            memory.messages().forEach(messages::add);
        } else {
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            messages.add(userMessage);
        }

        // Get the response from the model
        AiMessage aiMessage = model.chat(messages).aiMessage();

        // Add the AI message to memory if available
        if (memory != null) {
            memory.add(aiMessage);
        }

        return aiMessage;
    }

    /**
     * Convert the AiMessage to the output type.
     *
     * @param aiMessage The AI message to convert
     * @param context Additional context information
     * @return The converted output
     */
    protected abstract O convertToOutput(
        AiMessage aiMessage,
        Map<String, Object> context
    );
}
