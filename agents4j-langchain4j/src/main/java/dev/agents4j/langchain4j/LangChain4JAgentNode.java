package dev.agents4j.langchain4j;

import dev.agents4j.api.AgentNode;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * An AgentNode implementation that uses LangChain4J's ChatModel and ChatMemory.
 * This interface allows for integration with various LangChain4J models.
 *
 * @param <I> The input type for the agent
 * @param <O> The output type for the agent
 */
public interface LangChain4JAgentNode<I, O> extends AgentNode<I, O> {
    /**
     * Gets the ChatModel used by this agent.
     *
     * @return The ChatModel instance
     */
    ChatModel getModel();

    /**
     * Gets the ChatMemory used by this agent, if any.
     *
     * @return An Optional containing the ChatMemory, or empty if no memory is used
     */
    Optional<ChatMemory> getMemory();

    /**
     * Process the input using the LangChain4J model and return the output.
     * This method should handle the conversion between the input/output types
     * and the appropriate message formats for LangChain4J.
     *
     * @param input The input to process
     * @param context Additional context information for the agent
     * @return The output from processing the input
     */
    @Override
    O process(I input, Map<String, Object> context);

    /**
     * Process the input asynchronously using the LangChain4J model.
     *
     * @param input The input to process
     * @param context Additional context information for the agent
     * @return A CompletableFuture that will complete with the output
     */
    @Override
    default CompletableFuture<O> processAsync(
        I input,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> process(input, context));
    }
}
