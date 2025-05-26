package dev.agents4j.api;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a node in an agent workflow.
 * Each node is responsible for processing input and producing output.
 */
public interface AgentNode<I, O> {
    
    /**
     * Process the input and return the output synchronously.
     *
     * @param input The input to process
     * @param context Additional context information for the agent
     * @return The output from processing the input
     */
    O process(I input, Map<String, Object> context);
    
    /**
     * Process the input asynchronously and return a future that will complete with the output.
     *
     * @param input The input to process
     * @param context Additional context information for the agent
     * @return A CompletableFuture that will complete with the output
     */
    default CompletableFuture<O> processAsync(I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> process(input, context));
    }
    
    /**
     * Get the name of this agent node.
     *
     * @return The name of the agent node
     */
    String getName();
}