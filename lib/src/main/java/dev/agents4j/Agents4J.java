/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j;

import dev.agents4j.facade.ChainWorkflows;
import dev.agents4j.facade.OrchestratorWorkflows;
import dev.agents4j.facade.ParallelWorkflows;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.agents4j.workflow.OrchestratorWorkersWorkflow;
import dev.agents4j.workflow.ParallelizationWorkflow;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;

/**
 * Main library class for Agents4J that provides convenience methods
 * for creating and using agent workflows.
 * 
 * @deprecated This class delegates to focused facade classes for better organization.
 * Use {@link ChainWorkflows}, {@link ParallelWorkflows}, or {@link OrchestratorWorkflows} directly.
 */
@Deprecated(since = "2.0.0", forRemoval = true)
public class Agents4J {

    /**
     * Creates a simple chain workflow with string input/output.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param systemPrompts System prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     * @deprecated Use {@link ChainWorkflows#create(String, ChatModel, String...)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static ChainWorkflow<String, String> createChainWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        return ChainWorkflows.create(name, model, systemPrompts);
    }

    /**
     * Creates a chain workflow with memory for maintaining conversation history.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param maxMessages Maximum number of messages to keep in memory
     * @param systemPrompts System prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     * @deprecated Use {@link ChainWorkflows#createConversational(String, ChatModel, int, String...)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static ChainWorkflow<String, String> createConversationalWorkflow(
        String name,
        ChatModel model,
        int maxMessages,
        String... systemPrompts
    ) {
        return ChainWorkflows.createConversational(name, model, maxMessages, systemPrompts);
    }

    /**
     * Execute a simple query using a single agent.
     *
     * @param model The ChatModel to use
     * @param systemPrompt The system prompt for the agent
     * @param query The query to process
     * @return The response from the agent
     * @throws RuntimeException if workflow execution fails
     * @deprecated Use {@link ChainWorkflows#executeSimple(ChatModel, String, String)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static String query(
        ChatModel model,
        String systemPrompt,
        String query
    ) {
        return ChainWorkflows.executeSimple(model, systemPrompt, query);
    }

    /**
     * Execute a complex query using a chain of agents.
     *
     * @param model The ChatModel to use
     * @param query The query to process
     * @param systemPrompts The system prompts for each agent in the chain
     * @return The final response from the chain
     * @throws RuntimeException if workflow execution fails
     * @deprecated Use {@link ChainWorkflows#executeComplex(ChatModel, String, String...)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static String complexQuery(
        ChatModel model,
        String query,
        String... systemPrompts
    ) {
        return ChainWorkflows.executeComplex(model, query, systemPrompts);
    }

    /**
     * Creates a parallelization workflow for concurrent processing of multiple inputs.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new ParallelizationWorkflow instance
     * @deprecated Use {@link ParallelWorkflows#create(String, ChatModel)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static ParallelizationWorkflow createParallelizationWorkflow(
        String name,
        ChatModel model
    ) {
        return ParallelWorkflows.create(name, model);
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
     * @deprecated Use {@link ParallelWorkflows#execute(ChatModel, String, List, int)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static List<String> parallelQuery(
        ChatModel model,
        String prompt,
        List<String> inputs,
        int numWorkers
    ) {
        return ParallelWorkflows.execute(model, prompt, inputs, numWorkers);
    }

    /**
     * Execute multiple inputs in parallel using the same prompt with default worker count.
     * Uses a default of 4 worker threads.
     *
     * @param model The ChatModel to use
     * @param prompt The prompt template to apply to each input
     * @param inputs The list of inputs to process in parallel
     * @return List of responses in the same order as inputs
     * @deprecated Use {@link ParallelWorkflows#execute(ChatModel, String, List)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static List<String> parallelQuery(
        ChatModel model,
        String prompt,
        List<String> inputs
    ) {
        return ParallelWorkflows.execute(model, prompt, inputs);
    }

    /**
     * Creates an orchestrator-workers workflow with standard worker types.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new OrchestratorWorkersWorkflow instance
     * @deprecated Use {@link OrchestratorWorkflows#create(String, ChatModel)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static OrchestratorWorkersWorkflow createOrchestratorWorkersWorkflow(
        String name,
        ChatModel model
    ) {
        return OrchestratorWorkflows.create(name, model);
    }

    /**
     * Creates an orchestrator-workers workflow with custom workers.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param workers Worker definitions
     * @return A new OrchestratorWorkersWorkflow instance
     * @deprecated Use {@link OrchestratorWorkflows#createCustom(String, ChatModel, AgentWorkflowFactory.WorkerDefinition...)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static OrchestratorWorkersWorkflow createCustomOrchestratorWorkersWorkflow(
        String name,
        ChatModel model,
        AgentWorkflowFactory.WorkerDefinition... workers
    ) {
        return OrchestratorWorkflows.createCustom(name, model, workers);
    }

    /**
     * Execute a complex task using orchestrator-workers pattern.
     * The orchestrator will analyze the task and delegate to appropriate workers.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @return The synthesized response from all workers
     * @throws RuntimeException if workflow execution fails
     * @deprecated Use {@link OrchestratorWorkflows#execute(ChatModel, String)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static String orchestratedQuery(
        ChatModel model,
        String taskDescription
    ) {
        return OrchestratorWorkflows.execute(model, taskDescription);
    }

    /**
     * Execute a complex task using orchestrator-workers pattern with custom workers.
     *
     * @param model The ChatModel to use
     * @param taskDescription The complex task to process
     * @param workers Custom worker definitions
     * @return The synthesized response from all workers
     * @throws RuntimeException if workflow execution fails
     * @deprecated Use {@link OrchestratorWorkflows#executeCustom(ChatModel, String, AgentWorkflowFactory.WorkerDefinition...)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static String customOrchestratedQuery(
        ChatModel model,
        String taskDescription,
        AgentWorkflowFactory.WorkerDefinition... workers
    ) {
        return OrchestratorWorkflows.executeCustom(model, taskDescription, workers);
    }

    /**
     * Helper method to create a worker definition.
     *
     * @param type The worker type identifier
     * @param description A description of what this worker does
     * @param systemPrompt The system prompt for this worker
     * @return A new WorkerDefinition instance
     * @deprecated Use {@link OrchestratorWorkflows#worker(String, String, String)} instead
     */
    @Deprecated(since = "2.0.0", forRemoval = true)
    public static AgentWorkflowFactory.WorkerDefinition worker(String type, String description, String systemPrompt) {
        return OrchestratorWorkflows.worker(type, description, systemPrompt);
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
