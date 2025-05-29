package dev.agents4j.integration.examples.util;

import dev.agents4j.api.AgentNode;
import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A utility adapter that converts AgentOutput to AgentInput for use in chain workflows
 * where ComplexLangChain4JAgentNode instances need to be chained together.
 * 
 * This adapter preserves the content and metadata from the AgentOutput and creates
 * a new AgentInput that can be consumed by the next node in the chain.
 */
public class AgentOutputToInputAdapter implements AgentNode<AgentOutput, AgentInput> {
    
    private final String name;
    
    /**
     * Creates a new adapter with the given name.
     * 
     * @param name The name to assign to this adapter node
     */
    public AgentOutputToInputAdapter(String name) {
        this.name = name;
    }
    
    /**
     * Creates a new adapter with a default name.
     */
    public AgentOutputToInputAdapter() {
        this("AgentOutputToInputAdapter");
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public AgentInput process(AgentOutput input, Map<String, Object> context) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        // Convert AgentOutput to AgentInput, preserving content and metadata
        return AgentInput.builder(input.getContent())
            .withAllMetadata(input.getMetadata())
            .build();
    }
    
    @Override
    public CompletableFuture<AgentInput> processAsync(AgentOutput input, Map<String, Object> context) {
        return CompletableFuture.completedFuture(process(input, context));
    }
    
    /**
     * Creates a new adapter instance with the given name.
     * 
     * @param name The name for the adapter
     * @return A new AgentOutputToInputAdapter instance
     */
    public static AgentOutputToInputAdapter create(String name) {
        return new AgentOutputToInputAdapter(name);
    }
    
    /**
     * Creates a new adapter instance with a default name.
     * 
     * @return A new AgentOutputToInputAdapter instance
     */
    public static AgentOutputToInputAdapter create() {
        return new AgentOutputToInputAdapter();
    }
}