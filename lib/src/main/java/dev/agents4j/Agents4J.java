/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;

/**
 * Main library class for Agents4J that provides convenience methods
 * for creating and using agent workflows.
 */
public class Agents4J {

    /**
     * Creates a simple chain workflow with string input/output.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param systemPrompts System prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<String, String> createChainWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        return AgentWorkflowFactory.createStringChainWorkflow(
            name,
            model,
            systemPrompts
        );
    }

    /**
     * Creates a chain workflow with memory for maintaining conversation history.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param maxMessages Maximum number of messages to keep in memory
     * @param systemPrompts System prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<String, String> createConversationalWorkflow(
        String name,
        ChatModel model,
        int maxMessages,
        String... systemPrompts
    ) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(maxMessages)
            .build();

        return AgentWorkflowFactory.createStringChainWorkflowWithMemory(
            name,
            model,
            memory,
            systemPrompts
        );
    }

    /**
     * Execute a simple query using a single agent.
     *
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the agent
     * @param query The query to process
     * @return The response from the agent
     * @throws RuntimeException if workflow execution fails
     */
    public static String query(
        ChatModel model,
        String systemPrompt,
        String query
    ) {
        ChainWorkflow<String, String> workflow = createChainWorkflow(
            "SingleAgentWorkflow",
            model,
            systemPrompt
        );

        try {
            return workflow.execute(query);
        } catch (WorkflowExecutionException e) {
            throw new RuntimeException("Failed to execute query", e);
        }
    }

    /**
     * Execute a complex query using a chain of agents.
     *
     * @param model The ChatModel to use
     * @param query The query to process
     * @param systemPrompts The system prompts for each agent in the chain
     * @return The final response from the chain
     * @throws RuntimeException if workflow execution fails
     */
    public static String complexQuery(
        ChatModel model,
        String query,
        String... systemPrompts
    ) {
        ChainWorkflow<String, String> workflow = createChainWorkflow(
            "ComplexQueryWorkflow",
            model,
            systemPrompts
        );

        try {
            return workflow.execute(query);
        } catch (WorkflowExecutionException e) {
            throw new RuntimeException("Failed to execute complex query", e);
        }
    }

    /**
     * Creates a parallelization workflow for concurrent processing of multiple inputs.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new ParallelizationWorkflow instance
     */
    public static ParallelizationWorkflow createParallelizationWorkflow(
        String name,
        ChatModel model
    ) {
        return ParallelizationWorkflow.builder()
            .name(name)
            .chatModel(model)
            .build();
    }

    /**
     * Execute multiple inputs in parallel using the same prompt.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template to apply to each input
     * @param inputs The list of inputs to process in parallel
     * @param numWorkers The number of worker threads to use
     * @return List of responses in the same order as inputs
     * @throws RuntimeException if workflow execution fails
     */
    public static List<String> parallelQuery(
        ChatModel model,
        String prompt,
        List<String> inputs,
        int numWorkers
    ) {
        ParallelizationWorkflow workflow = createParallelizationWorkflow(
            "ParallelQueryWorkflow",
            model
        );

        ParallelizationWorkflow.ParallelInput parallelInput = 
            new ParallelizationWorkflow.ParallelInput(prompt, inputs, numWorkers);

        try {
            return workflow.execute(parallelInput);
        } catch (WorkflowExecutionException e) {
            throw new RuntimeException("Failed to execute parallel query", e);
        }
    }

    /**
     * Execute multiple inputs in parallel using the same prompt with default worker count.
     * Uses a default of 4 worker threads.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template to apply to each input
     * @param inputs The list of inputs to process in parallel
     * @return List of responses in the same order as inputs
     */
    public static List<String> parallelQuery(
        ChatModel model,
        String prompt,
        List<String> inputs
    ) {
        return parallelQuery(model, prompt, inputs, 4);
    }

    /**
     * Check if the Agents4J library is properly configured.
     *
     * @return true if the library is properly configured
     */
    public boolean isConfigured() {
        return true;
    }
}
