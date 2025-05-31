package dev.agents4j.langchain4j;

import dev.agents4j.langchain4j.LangChain4JAgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter that wraps a LangChain4J AI service to implement the LangChain4JAgentNode interface.
 * 
 * @param <T> the LangChain4J AI service interface type
 */
public class LangChain4JAgentNodeAdapter<T> implements LangChain4JAgentNode<String, String> {
    
    private final T aiService;
    private final Class<T> serviceClass;
    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final Method primaryMethod;
    private final String nodeName;
    
    public LangChain4JAgentNodeAdapter(T aiService, Class<T> serviceClass, ChatModel chatModel, ChatMemory chatMemory) {
        this.aiService = aiService;
        this.serviceClass = serviceClass;
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.primaryMethod = findPrimaryMethod();
        this.nodeName = "LangChain4J-" + serviceClass.getSimpleName();
    }
    
    @Override
    public String process(String input, Map<String, Object> context) {
        try {
            Object result = primaryMethod.invoke(aiService, input);
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            throw new RuntimeException("Failed to process input with LangChain4J service: " + nodeName, e);
        }
    }
    
    @Override
    public CompletableFuture<String> processAsync(String input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> process(input, context));
    }
    
    @Override
    public ChatModel getModel() {
        return chatModel;
    }
    
    @Override
    public Optional<ChatMemory> getMemory() {
        return Optional.ofNullable(chatMemory);
    }
    
    @Override
    public String getName() {
        return nodeName;
    }
    
    /**
     * Gets the wrapped AI service instance.
     * 
     * @return the AI service instance
     */
    public T getAiService() {
        return aiService;
    }
    
    /**
     * Gets the service class type.
     * 
     * @return the service class
     */
    public Class<T> getServiceClass() {
        return serviceClass;
    }
    
    /**
     * Finds the primary method to invoke on the AI service.
     * Looks for methods that take a String parameter and return a String.
     */
    private Method findPrimaryMethod() {
        Method[] methods = serviceClass.getDeclaredMethods();
        
        // First, look for methods named "chat", "ask", "process", or "generate"
        for (String methodName : new String[]{"chat", "ask", "process", "generate"}) {
            for (Method method : methods) {
                if (method.getName().equals(methodName) && 
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0] == String.class) {
                    return method;
                }
            }
        }
        
        // If no preferred method found, look for any method with String parameter
        for (Method method : methods) {
            if (method.getParameterCount() == 1 &&
                method.getParameterTypes()[0] == String.class) {
                return method;
            }
        }
        
        throw new IllegalArgumentException("No suitable method found in " + serviceClass.getName() + 
            " that takes a String parameter");
    }
}