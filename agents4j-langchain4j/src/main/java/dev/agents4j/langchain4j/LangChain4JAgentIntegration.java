package dev.agents4j.langchain4j;

import dev.agents4j.api.AgentNode;
import dev.agents4j.langchain4j.LangChain4JAgentNode;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Integration layer for LangChain4J with Agents4J framework.
 * 
 * This class provides utilities and adapters to integrate LangChain4J
 * language models and AI services with the Agents4J agent framework.
 */
public class LangChain4JAgentIntegration {
    
    /**
     * Creates an AgentNode from a LangChain4J AI service interface.
     * 
     * @param <T> the AI service interface type
     * @param serviceClass the AI service interface class
     * @param chatModel the LangChain4J chat language model
     * @return an AgentNode wrapping the LangChain4J service
     */
    public static <T> AgentNode<String, String> createAgentNode(
            Class<T> serviceClass, 
            ChatModel chatModel) {
        
        T aiService = AiServices.builder(serviceClass)
                .chatModel(chatModel)
                .build();
        
        return new LangChain4JAgentNodeAdapter<>(aiService, serviceClass, chatModel, null);
    }
    
    /**
     * Creates an AgentNode from a LangChain4J AI service interface with memory.
     * 
     * @param <T> the AI service interface type
     * @param serviceClass the AI service interface class
     * @param chatModel the LangChain4J chat language model
     * @param chatMemory the chat memory for conversation history
     * @return an AgentNode wrapping the LangChain4J service
     */
    public static <T> AgentNode<String, String> createAgentNode(
            Class<T> serviceClass, 
            ChatModel chatModel,
            ChatMemory chatMemory) {
        
        T aiService = AiServices.builder(serviceClass)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .build();
        
        return new LangChain4JAgentNodeAdapter<>(aiService, serviceClass, chatModel, chatMemory);
    }
    
    /**
     * Builder for creating LangChain4J-based agent nodes with additional configuration.
     */
    public static class Builder<T> {
        private Class<T> serviceClass;
        private ChatModel chatModel;
        private ChatMemory chatMemory;
        
        public Builder(Class<T> serviceClass) {
            this.serviceClass = serviceClass;
        }
        
        public Builder<T> withChatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }
        
        public Builder<T> withMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }
        
        public Builder<T> withDefaultMemory() {
            this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);
            return this;
        }
        
        public AgentNode<String, String> build() {
            if (chatModel == null) {
                throw new IllegalStateException("Chat model is required");
            }
            
            if (chatMemory != null) {
                return createAgentNode(serviceClass, chatModel, chatMemory);
            } else {
                return createAgentNode(serviceClass, chatModel);
            }
        }
    }
    
    /**
     * Creates a builder for the specified AI service interface.
     * 
     * @param <T> the AI service interface type
     * @param serviceClass the AI service interface class
     * @return a builder for creating the agent node
     */
    public static <T> Builder<T> builder(Class<T> serviceClass) {
        return new Builder<>(serviceClass);
    }
}