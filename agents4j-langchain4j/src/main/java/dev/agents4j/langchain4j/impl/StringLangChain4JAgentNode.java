package dev.agents4j.langchain4j.impl;

import dev.agents4j.langchain4j.LangChain4JAgentNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Map;

/**
 * A simple implementation of LangChain4JAgentNode that processes string inputs and produces string outputs.
 * This agent node simply passes the string input to the model and returns the model's response as a string.
 */
public class StringLangChain4JAgentNode
    extends BaseLangChain4JAgentNode<String, String> {

    /**
     * Creates a new StringLangChain4JAgentNode.
     *
     * @param name The name of the agent node
     * @param model The ChatModel to use
     * @param memory The ChatMemory to use, can be null if no memory is needed
     * @param systemPrompt The system prompt to use, can be null if no system prompt is needed
     */
    public StringLangChain4JAgentNode(
        String name,
        ChatModel model,
        ChatMemory memory,
        String systemPrompt
    ) {
        super(name, model, memory, systemPrompt);
    }

    /**
     * Builder for creating StringLangChain4JAgentNode instances.
     */
    public static class Builder {

        private String name;
        private ChatModel model;
        private ChatMemory memory;
        private String systemPrompt;

        /**
         * Set the name of the agent node.
         *
         * @param name The name of the agent node
         * @return This builder instance for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the ChatModel to use.
         *
         * @param model The ChatModel to use
         * @return This builder instance for method chaining
         */
        public Builder model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * Set the ChatMemory to use.
         *
         * @param memory The ChatMemory to use
         * @return This builder instance for method chaining
         */
        public Builder memory(ChatMemory memory) {
            this.memory = memory;
            return this;
        }

        /**
         * Set the system prompt to use.
         *
         * @param systemPrompt The system prompt to use
         * @return This builder instance for method chaining
         */
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        /**
         * Build the StringLangChain4JAgentNode instance.
         *
         * @return A new StringLangChain4JAgentNode instance
         */
        public StringLangChain4JAgentNode build() {
            return new StringLangChain4JAgentNode(
                name,
                model,
                memory,
                systemPrompt
            );
        }
    }

    /**
     * Create a new Builder to construct a StringLangChain4JAgentNode.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected UserMessage createUserMessage(
        String input,
        Map<String, Object> context
    ) {
        return UserMessage.from(input);
    }

    @Override
    protected String convertToOutput(
        AiMessage aiMessage,
        Map<String, Object> context
    ) {
        return aiMessage.text();
    }
}
