package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import dev.agents4j.impl.StringLangChain4JAgentNode;
import dev.agents4j.workflow.routing.LLMContentRouter;
import dev.agents4j.workflow.routing.RuleBasedContentRouter;
import dev.agents4j.workflow.routing.RoutingWorkflow;
import dev.agents4j.workflow.routing.RoutingWorkflowFactory;
import dev.agents4j.workflow.strategy.StrategyFactory;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating various types of agent workflows.
 * This factory provides convenient methods for constructing
 * common workflow patterns.
 */
public class AgentWorkflowFactory {

    /**
     * Creates a chain workflow with the given agent nodes.
     *
     * @param <I> The input type for the first node in the chain
     * @param <O> The output type of the last node in the chain
     * @param name The name of the workflow
     * @param firstNode The first node in the chain
     * @param restNodes The remaining nodes in the chain
     * @return A new ChainWorkflow instance
     */
    @SafeVarargs
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <I, O> ChainWorkflow<I, O> createChainWorkflow(
        String name,
        AgentNode<I, ?> firstNode,
        AgentNode<?, ?>... restNodes
    ) {
        // Start building the chain with the first node
        ChainWorkflow.Builder<I, ?> builder = ChainWorkflow.<I, Object>builder()
            .name(name)
            .firstNode(firstNode);

        // Use type erasure to add the remaining nodes
        for (AgentNode<?, ?> node : restNodes) {
            // This is safe because at runtime, the generic type information is erased
            builder = (ChainWorkflow.Builder<
                    I,
                    ?
                >) ((ChainWorkflow.Builder) builder).node(node);
        }

        // Cast the final result to the expected type
        return (ChainWorkflow<I, O>) builder.build();
    }

    /**
     * Creates a chain workflow of string-processing LangChain4J agents.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param systemPrompts The system prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<String, String> createStringChainWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<String, String>> nodes = new ArrayList<>();

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node =
                StringLangChain4JAgentNode.builder()
                    .name(nodeName)
                    .model(model)
                    .systemPrompt(systemPrompts[i])
                    .build();
            nodes.add(node);
        }

        return createChainWorkflow(
            name,
            nodes.get(0),
            nodes.subList(1, nodes.size()).toArray(new AgentNode[0])
        );
    }

    /**
     * Creates a chain workflow of string-processing LangChain4J agents with memory.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param memory The ChatMemory to use for all agents
     * @param systemPrompts The system prompts for each agent in the chain
     * @return A new ChainWorkflow instance
     */
    public static ChainWorkflow<
        String,
        String
    > createStringChainWorkflowWithMemory(
        String name,
        ChatModel model,
        ChatMemory memory,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<String, String>> nodes = new ArrayList<>();

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node =
                StringLangChain4JAgentNode.builder()
                    .name(nodeName)
                    .model(model)
                    .memory(memory)
                    .systemPrompt(systemPrompts[i])
                    .build();
            nodes.add(node);
        }

        return createChainWorkflow(
            name,
            nodes.get(0),
            nodes.subList(1, nodes.size()).toArray(new AgentNode[0])
        );
    }

    /**
     * Creates a parallelization workflow for concurrent processing of multiple inputs.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all parallel operations
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
     * Creates a parallelization workflow with a default name.
     *
     * @param model The ChatModel to use for all parallel operations
     * @return A new ParallelizationWorkflow instance
     */
    public static ParallelizationWorkflow createParallelizationWorkflow(
        ChatModel model
    ) {
        return createParallelizationWorkflow(
            "ParallelizationWorkflow-" + System.currentTimeMillis(),
            model
        );
    }

    /**
     * Creates a strategy-based workflow with the specified execution strategy.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The name of the workflow
     * @param strategyName The name of the execution strategy to use
     * @param nodes The agent nodes to include in the workflow
     * @return A new StrategyWorkflow instance
     */
    @SafeVarargs
    public static <I, O> StrategyWorkflow<I, O> createStrategyWorkflow(
        String name,
        String strategyName,
        AgentNode<?, ?>... nodes
    ) {
        WorkflowExecutionStrategy<I, O> strategy = StrategyFactory.getStrategy(strategyName);
        return StrategyWorkflow.create(name, strategy, nodes);
    }

    /**
     * Creates a strategy-based workflow with the specified execution strategy and configuration.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The name of the workflow
     * @param strategyName The name of the execution strategy to use
     * @param config Configuration map for the strategy
     * @param nodes The agent nodes to include in the workflow
     * @return A new StrategyWorkflow instance
     */
    @SafeVarargs
    public static <I, O> StrategyWorkflow<I, O> createStrategyWorkflow(
        String name,
        String strategyName,
        Map<String, Object> config,
        AgentNode<?, ?>... nodes
    ) {
        WorkflowExecutionStrategy<I, O> strategy = StrategyFactory.createStrategy(strategyName, config);
        return StrategyWorkflow.<I, O>builder()
            .name(name)
            .strategy(strategy)
            .defaultContext(config)
            .addNodes(List.of(nodes))
            .build();
    }

    /**
     * Creates a sequential strategy workflow for string processing.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param systemPrompts The system prompts for each agent in the sequence
     * @return A new StrategyWorkflow instance with sequential execution
     */
    public static StrategyWorkflow<String, String> createSequentialStringWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<?, ?>> nodes = new ArrayList<>();
        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node = StringLangChain4JAgentNode.builder()
                .name(nodeName)
                .model(model)
                .systemPrompt(systemPrompts[i])
                .build();
            nodes.add(node);
        }

        return StrategyWorkflow.<String, String>builder()
            .name(name)
            .strategy(StrategyFactory.sequential())
            .addNodes(nodes)
            .build();
    }

    /**
     * Creates a parallel strategy workflow for string processing.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use for all agents
     * @param systemPrompts The system prompts for each agent to run in parallel
     * @return A new StrategyWorkflow instance with parallel execution
     */
    public static StrategyWorkflow<String, List<String>> createParallelStringWorkflow(
        String name,
        ChatModel model,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException(
                "At least one system prompt is required"
            );
        }

        List<AgentNode<?, ?>> nodes = new ArrayList<>();
        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = name + "-Node" + (i + 1);
            StringLangChain4JAgentNode node = StringLangChain4JAgentNode.builder()
                .name(nodeName)
                .model(model)
                .systemPrompt(systemPrompts[i])
                .build();
            nodes.add(node);
        }

        return StrategyWorkflow.<String, List<String>>builder()
            .name(name)
            .strategy(StrategyFactory.parallel())
            .defaultContext("aggregationStrategy", "list")
            .addNodes(nodes)
            .build();
    }

    /**
     * Creates a batch strategy workflow for processing large datasets.
     *
     * @param name The name of the workflow
     * @param batchSize The size of each batch
     * @param nodes The agent nodes to process each batch
     * @return A new StrategyWorkflow instance with batch execution
     */
    @SafeVarargs
    public static <I, O> StrategyWorkflow<I, O> createBatchWorkflow(
        String name,
        int batchSize,
        AgentNode<?, ?>... nodes
    ) {
        return StrategyWorkflow.<I, O>builder()
            .name(name)
            .strategy(StrategyFactory.batch())
            .defaultContext("batchSize", batchSize)
            .addNodes(List.of(nodes))
            .build();
    }

    /**
     * Creates a conditional strategy workflow with branching logic.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The name of the workflow
     * @param conditionalConfig Configuration for conditional execution
     * @param nodes The agent nodes to conditionally execute
     * @return A new StrategyWorkflow instance with conditional execution
     */
    @SafeVarargs
    public static <I, O> StrategyWorkflow<I, O> createConditionalWorkflow(
        String name,
        Map<String, Object> conditionalConfig,
        AgentNode<?, ?>... nodes
    ) {
        return StrategyWorkflow.<I, O>builder()
            .name(name)
            .strategy(StrategyFactory.conditional())
            .defaultContext(conditionalConfig)
            .addNodes(List.of(nodes))
            .build();
    }

    // =================== ROUTING WORKFLOW METHODS ===================







    /**
     * Creates a routing workflow with LLM-based content router.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The workflow name
     * @param model The ChatModel for routing
     * @param classificationPrompt The classification prompt
     * @param routes The routes to add to the workflow
     * @return A new RoutingWorkflow instance
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createLLMRoutingWorkflow(
        String name,
        ChatModel model,
        String classificationPrompt,
        Route<I, O>... routes
    ) {
        ContentRouter<I> router = LLMContentRouter.<I>builder()
            .model(model)
            .classificationPrompt(classificationPrompt)
            .includeConfidenceScoring(true)
            .includeReasoning(true)
            .build();

        return RoutingWorkflowFactory.createSimpleRoutingWorkflow(name, router, routes);
    }

    /**
     * Creates a routing workflow with rule-based content router.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The workflow name
     * @param keywordRules Map of route IDs to their keyword lists
     * @param routes The routes to add to the workflow
     * @return A new RoutingWorkflow instance
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createRuleBasedRoutingWorkflow(
        String name,
        Map<String, List<String>> keywordRules,
        Route<I, O>... routes
    ) {
        ContentRouter<I> router = RuleBasedContentRouter.createWithKeywords(keywordRules);
        return RoutingWorkflowFactory.createSimpleRoutingWorkflow(name, router, routes);
    }

    /**
     * Creates a simple routing workflow with custom router.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @param name The workflow name
     * @param router The content router to use
     * @param routes The routes to add to the workflow
     * @return A new RoutingWorkflow instance
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createRoutingWorkflow(
        String name,
        ContentRouter<I> router,
        Route<I, O>... routes
    ) {
        return RoutingWorkflowFactory.createSimpleRoutingWorkflow(name, router, routes);
    }

    /**
     * Creates a string processing route with sequential strategy.
     *
     * @param routeId The route identifier
     * @param description The route description
     * @param model The ChatModel to use
     * @param systemPrompts The system prompts for the route nodes
     * @return A new Route instance
     */
    public static Route<String, String> createStringRoute(
        String routeId,
        String description,
        ChatModel model,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException("At least one system prompt is required");
        }

        Route.Builder<String, String> builder = Route.<String, String>builder()
            .id(routeId)
            .description(description)
            .strategy(StrategyFactory.sequential());

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = routeId + "-Node" + (i + 1);
            StringLangChain4JAgentNode node = StringLangChain4JAgentNode.builder()
                .name(nodeName)
                .model(model)
                .systemPrompt(systemPrompts[i])
                .build();
            builder.addNode(node);
        }

        return builder.build();
    }

    /**
     * Creates a string processing route with custom strategy.
     *
     * @param routeId The route identifier
     * @param description The route description
     * @param model The ChatModel to use
     * @param strategy The execution strategy to use
     * @param systemPrompts The system prompts for the route nodes
     * @return A new Route instance
     */
    public static Route<String, String> createStringRouteWithStrategy(
        String routeId,
        String description,
        ChatModel model,
        WorkflowExecutionStrategy<String, String> strategy,
        String... systemPrompts
    ) {
        if (systemPrompts.length == 0) {
            throw new IllegalArgumentException("At least one system prompt is required");
        }

        Route.Builder<String, String> builder = Route.<String, String>builder()
            .id(routeId)
            .description(description)
            .strategy(strategy);

        for (int i = 0; i < systemPrompts.length; i++) {
            String nodeName = routeId + "-Node" + (i + 1);
            StringLangChain4JAgentNode node = StringLangChain4JAgentNode.builder()
                .name(nodeName)
                .model(model)
                .systemPrompt(systemPrompts[i])
                .build();
            builder.addNode(node);
        }

        return builder.build();
    }

    /**
     * Creates an orchestrator-workers workflow with the specified workers.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param workerConfigs Map of worker configurations
     * @return A new OrchestratorWorkersWorkflow instance
     */
    public static OrchestratorWorkersWorkflow createOrchestratorWorkersWorkflow(
        String name,
        ChatModel model,
        Map<String, OrchestratorWorkersWorkflow.WorkerConfig> workerConfigs
    ) {
        OrchestratorWorkersWorkflow.Builder builder = OrchestratorWorkersWorkflow.builder()
            .name(name)
            .chatModel(model);
        
        for (OrchestratorWorkersWorkflow.WorkerConfig config : workerConfigs.values()) {
            builder.addWorker(config);
        }
        
        return builder.build();
    }

    /**
     * Creates an orchestrator-workers workflow with predefined worker types.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @return A new OrchestratorWorkersWorkflow instance with common worker types
     */
    public static OrchestratorWorkersWorkflow createStandardOrchestratorWorkersWorkflow(
        String name,
        ChatModel model
    ) {
        return OrchestratorWorkersWorkflow.builder()
            .name(name)
            .chatModel(model)
            .addWorker("analyst", "Analyzes data and provides insights", 
                "You are a data analyst. Analyze the given information and provide detailed insights, patterns, and conclusions.")
            .addWorker("writer", "Creates written content and documentation", 
                "You are a professional writer. Create clear, engaging, and well-structured written content based on the given requirements.")
            .addWorker("researcher", "Conducts research and fact-checking", 
                "You are a researcher. Investigate the given topic thoroughly and provide accurate, well-sourced information.")
            .addWorker("summarizer", "Summarizes and condenses information", 
                "You are a content summarizer. Create concise, accurate summaries that capture the key points and essential information.")
            .build();
    }

    /**
     * Creates a simple orchestrator-workers workflow with custom workers.
     *
     * @param name The name of the workflow
     * @param model The ChatModel to use
     * @param workers Variable arguments of worker configurations (type, description, prompt)
     * @return A new OrchestratorWorkersWorkflow instance
     */
    public static OrchestratorWorkersWorkflow createCustomOrchestratorWorkersWorkflow(
        String name,
        ChatModel model,
        WorkerDefinition... workers
    ) {
        if (workers.length == 0) {
            throw new IllegalArgumentException("At least one worker must be provided");
        }

        OrchestratorWorkersWorkflow.Builder builder = OrchestratorWorkersWorkflow.builder()
            .name(name)
            .chatModel(model);

        for (WorkerDefinition worker : workers) {
            builder.addWorker(worker.type, worker.description, worker.systemPrompt);
        }

        return builder.build();
    }

    /**
     * Helper class for defining workers in a compact way.
     */
    public static class WorkerDefinition {
        public final String type;
        public final String description;
        public final String systemPrompt;

        public WorkerDefinition(String type, String description, String systemPrompt) {
            this.type = type;
            this.description = description;
            this.systemPrompt = systemPrompt;
        }
    }

    /**
     * Creates a worker definition.
     */
    public static WorkerDefinition worker(String type, String description, String systemPrompt) {
        return new WorkerDefinition(type, description, systemPrompt);
    }
}
