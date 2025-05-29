/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.facade;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.exception.AgentExecutionException;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Focused facade for chain workflow operations.
 * This class follows the Single Responsibility Principle by handling only chain-related workflows.
 */
public final class ChainWorkflows {

    private ChainWorkflows() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a simple chain workflow with string input/output.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param systemPrompts System prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static ChainWorkflow<String, String> create(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        validateCreateParameters(name, model, systemPrompts);
        
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
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static ChainWorkflow<String, String> createConversational(
        String name,
        ChatModel model,
        int maxMessages,
        String... systemPrompts
    ) {
        validateCreateParameters(name, model, systemPrompts);
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be positive");
        }

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
     * @throws AgentExecutionException if workflow execution fails
     */
    public static String executeSimple(
        ChatModel model,
        String systemPrompt,
        String query
    ) {
        validateExecuteParameters(model, systemPrompt, query);
        
        ChainWorkflow<String, String> workflow = create(
            "SingleAgentWorkflow",
            model,
            systemPrompt
        );

        try {
            return workflow.execute(query);
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "SingleAgentWorkflow", 
                "Failed to execute simple query", 
                e,
                java.util.Map.of("query", query, "systemPrompt", systemPrompt)
            );
        }
    }

    /**
     * Execute a complex query using a chain of agents.
     *
     * @param model The ChatModel to use
     * @param query The query to process
     * @param systemPrompts The system prompts for each agent in the chain
     * @return The final response from the chain
     * @throws AgentExecutionException if workflow execution fails
     */
    public static String executeComplex(
        ChatModel model,
        String query,
        String... systemPrompts
    ) {
        validateExecuteParameters(model, query);
        if (systemPrompts == null || systemPrompts.length == 0) {
            throw new IllegalArgumentException("At least one system prompt is required");
        }

        ChainWorkflow<String, String> workflow = create(
            "ComplexQueryWorkflow",
            model,
            systemPrompts
        );

        try {
            return workflow.execute(query);
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "ComplexQueryWorkflow",
                "Failed to execute complex query",
                e,
                java.util.Map.of(
                    "query", query, 
                    "agentCount", systemPrompts.length,
                    "systemPrompts", java.util.Arrays.asList(systemPrompts)
                )
            );
        }
    }

    /**
     * Execute a conversational query with memory.
     *
     * @param model The ChatModel to use
     * @param query The query to process
     * @param maxMessages Maximum messages to keep in memory
     * @param systemPrompts The system prompts for each agent in the chain
     * @return The response from the conversational workflow
     * @throws AgentExecutionException if workflow execution fails
     */
    public static String executeConversational(
        ChatModel model,
        String query,
        int maxMessages,
        String... systemPrompts
    ) {
        validateExecuteParameters(model, query);
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be positive");
        }
        if (systemPrompts == null || systemPrompts.length == 0) {
            throw new IllegalArgumentException("At least one system prompt is required");
        }

        ChainWorkflow<String, String> workflow = createConversational(
            "ConversationalWorkflow",
            model,
            maxMessages,
            systemPrompts
        );

        try {
            return workflow.execute(query);
        } catch (WorkflowExecutionException e) {
            throw new AgentExecutionException(
                "ConversationalWorkflow",
                "Failed to execute conversational query",
                e,
                java.util.Map.of(
                    "query", query,
                    "maxMessages", maxMessages,
                    "agentCount", systemPrompts.length
                )
            );
        }
    }

    private static void validateCreateParameters(String name, ChatModel model, String... systemPrompts) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow name cannot be null or empty");
        }
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (systemPrompts == null || systemPrompts.length == 0) {
            throw new IllegalArgumentException("At least one system prompt is required");
        }
        for (int i = 0; i < systemPrompts.length; i++) {
            if (systemPrompts[i] == null || systemPrompts[i].trim().isEmpty()) {
                throw new IllegalArgumentException("System prompt at index " + i + " cannot be null or empty");
            }
        }
    }

    private static void validateExecuteParameters(ChatModel model, String query) {
        if (model == null) {
            throw new IllegalArgumentException("ChatModel cannot be null");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
    }

    private static void validateExecuteParameters(ChatModel model, String systemPrompt, String query) {
        validateExecuteParameters(model, query);
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("System prompt cannot be null or empty");
        }
    }
}