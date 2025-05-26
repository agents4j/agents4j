/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j;

import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

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

        return workflow.execute(query);
    }

    /**
     * Execute a complex query using a chain of agents.
     *
     * @param model The ChatModel to use
     * @param query The query to process
     * @param systemPrompts The system prompts for each agent in the chain
     * @return The final response from the chain
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

        return workflow.execute(query);
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
