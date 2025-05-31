package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.Workflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A workflow implementation that uses pluggable execution strategies.
 *
 * <p>This workflow delegates the actual execution logic to a configurable
 * {@link WorkflowExecutionStrategy}, enabling different execution patterns
 * to be applied to the same set of nodes. This implements the Strategy Pattern
 * to separate workflow structure from execution behavior.</p>
 *
 * <p><b>Key Benefits:</b></p>
 * <ul>
 * <li>Flexible execution patterns through strategy injection</li>
 * <li>Runtime strategy selection based on conditions</li>
 * <li>Reusable node configurations with different execution strategies</li>
 * <li>Easy testing with mock strategies</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Create workflow with sequential strategy
 * StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
 *     .name("MyWorkflow")
 *     .strategy(SequentialExecutionStrategy.create())
 *     .addNode(node1)
 *     .addNode(node2)
 *     .build();
 *
 * // Execute with custom context
 * Map<String, Object> context = new HashMap<>();
 * context.put("storeIntermediateResults", true);
 * String result = workflow.execute("input", context);
 * }</pre>
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class StrategyWorkflow<I, O> implements Workflow<I, O> {

    private final String name;
    private final List<AgentNode<?, ?>> nodes;
    private final WorkflowExecutionStrategy<I, O> strategy;
    private final Map<String, Object> defaultContext;

    /**
     * Creates a new StrategyWorkflow with the given configuration.
     *
     * @param name The name of the workflow
     * @param nodes The list of agent nodes to execute
     * @param strategy The execution strategy to use
     * @param defaultContext Default context values for execution
     */
    private StrategyWorkflow(
        String name,
        List<AgentNode<?, ?>> nodes,
        WorkflowExecutionStrategy<I, O> strategy,
        Map<String, Object> defaultContext
    ) {
        this.name = Objects.requireNonNull(
            name,
            "Workflow name cannot be null"
        );
        this.nodes = Collections.unmodifiableList(
            Objects.requireNonNull(nodes, "Nodes cannot be null")
        );
        this.strategy = Objects.requireNonNull(
            strategy,
            "Strategy cannot be null"
        );
        this.defaultContext = new HashMap<>(
            defaultContext != null ? defaultContext : Collections.emptyMap()
        );

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                "Workflow must contain at least one node"
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public O execute(I input) throws WorkflowExecutionException {
        return execute(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public O execute(I input, Map<String, Object> context)
        throws WorkflowExecutionException {
        Objects.requireNonNull(input, "Input cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        // Merge default context with provided context
        Map<String, Object> mergedContext = new HashMap<>(defaultContext);
        mergedContext.putAll(context);

        // Add workflow metadata to context
        mergedContext.put("workflow_name", name);
        mergedContext.put("workflow_type", "strategy");
        mergedContext.put("strategy_name", strategy.getStrategyName());
        mergedContext.put("node_count", nodes.size());

        try {
            // Validate that the strategy can execute these nodes
            if (!strategy.canExecute(nodes, mergedContext)) {
                throw new WorkflowExecutionException(
                    name,
                    "Strategy " +
                    strategy.getStrategyName() +
                    " cannot execute the configured nodes"
                );
            }

            long startTime = System.currentTimeMillis();

            // Execute using the strategy
            O result = strategy.execute(nodes, input, mergedContext);

            long endTime = System.currentTimeMillis();
            mergedContext.put("total_execution_time_ms", endTime - startTime);

            return result;
        } catch (WorkflowExecutionException e) {
            // Re-throw workflow exceptions as-is
            throw e;
        } catch (Exception e) {
            // Wrap other exceptions
            throw new WorkflowExecutionException(
                name,
                "Strategy workflow execution failed with strategy: " +
                strategy.getStrategyName(),
                e
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(I input) {
        return executeAsync(input, new HashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(
        I input,
        Map<String, Object> context
    ) {
        Objects.requireNonNull(input, "Input cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");

        // Merge contexts
        Map<String, Object> mergedContext = new HashMap<>(defaultContext);
        mergedContext.putAll(context);

        // Use the strategy's async execution
        return strategy.executeAsync(nodes, input, mergedContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("workflowType", "strategy");
        config.put("strategyName", strategy.getStrategyName());
        config.put("nodeCount", nodes.size());
        config.put(
            "nodes",
            nodes.stream().map(AgentNode::getName).toArray(String[]::new)
        );

        // Include strategy configuration
        Map<String, Object> strategyConfig =
            strategy.getStrategyConfiguration();
        config.put("strategyConfiguration", strategyConfig);

        // Include default context (excluding sensitive data)
        Map<String, Object> safeContext = new HashMap<>(defaultContext);
        safeContext.remove("apiKey");
        safeContext.remove("password");
        safeContext.remove("secret");
        config.put("defaultContext", safeContext);

        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfigurationProperty(String key, T defaultValue) {
        return (T) getConfiguration().getOrDefault(key, defaultValue);
    }

    /**
     * Get the execution strategy used by this workflow.
     *
     * @return The execution strategy
     */
    public WorkflowExecutionStrategy<I, O> getStrategy() {
        return strategy;
    }

    /**
     * Get the list of nodes in this workflow.
     *
     * @return An unmodifiable list of the nodes
     */
    public List<AgentNode<?, ?>> getNodes() {
        return nodes;
    }

    /**
     * Get the default context for this workflow.
     *
     * @return A copy of the default context
     */
    public Map<String, Object> getDefaultContext() {
        return new HashMap<>(defaultContext);
    }

    /**
     * Get execution characteristics for this workflow.
     *
     * @param context The execution context
     * @return A map containing execution characteristics
     */
    public Map<String, Object> getExecutionCharacteristics(
        Map<String, Object> context
    ) {
        Map<String, Object> mergedContext = new HashMap<>(defaultContext);
        if (context != null) {
            mergedContext.putAll(context);
        }
        return strategy.getExecutionCharacteristics(nodes, mergedContext);
    }

    /**
     * Builder for creating StrategyWorkflow instances.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     */
    public static class Builder<I, O> {

        private String name;
        private final List<AgentNode<?, ?>> nodes = new java.util.ArrayList<>();
        private WorkflowExecutionStrategy<I, O> strategy;
        private final Map<String, Object> defaultContext = new HashMap<>();

        /**
         * Set the name of the workflow.
         *
         * @param name The workflow name
         * @return This builder instance for method chaining
         */
        public Builder<I, O> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the execution strategy.
         *
         * @param strategy The execution strategy to use
         * @return This builder instance for method chaining
         */
        public Builder<I, O> strategy(
            WorkflowExecutionStrategy<I, O> strategy
        ) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Add a node to the workflow.
         *
         * @param node The agent node to add
         * @return This builder instance for method chaining
         */
        public Builder<I, O> addNode(AgentNode<?, ?> node) {
            this.nodes.add(node);
            return this;
        }

        /**
         * Add multiple nodes to the workflow.
         *
         * @param nodes The agent nodes to add
         * @return This builder instance for method chaining
         */
        public Builder<I, O> addNodes(List<AgentNode<?, ?>> nodes) {
            this.nodes.addAll(nodes);
            return this;
        }

        /**
         * Add a default context value.
         *
         * @param key The context key
         * @param value The context value
         * @return This builder instance for method chaining
         */
        public Builder<I, O> defaultContext(String key, Object value) {
            this.defaultContext.put(key, value);
            return this;
        }

        /**
         * Add multiple default context values.
         *
         * @param context The context map to add
         * @return This builder instance for method chaining
         */
        public Builder<I, O> defaultContext(Map<String, Object> context) {
            this.defaultContext.putAll(context);
            return this;
        }

        /**
         * Build the StrategyWorkflow instance.
         *
         * @return A new StrategyWorkflow instance
         * @throws IllegalStateException if required fields are not set
         */
        public StrategyWorkflow<I, O> build() {
            if (name == null) {
                name = "StrategyWorkflow-" + System.currentTimeMillis();
            }
            if (strategy == null) {
                throw new IllegalStateException("Strategy must be set");
            }
            if (nodes.isEmpty()) {
                throw new IllegalStateException(
                    "At least one node must be added"
                );
            }

            return new StrategyWorkflow<>(
                name,
                nodes,
                strategy,
                defaultContext
            );
        }
    }

    /**
     * Create a new Builder to construct a StrategyWorkflow.
     *
     * @param <I> The input type for the workflow
     * @param <O> The output type for the workflow
     * @return A new Builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }

    /**
     * Creates a new StrategyWorkflow with the specified parameters.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param strategy The execution strategy
     * @param nodes The agent nodes
     * @return A new StrategyWorkflow instance
     */
    public static <I, O> StrategyWorkflow<I, O> create(
        String name,
        WorkflowExecutionStrategy<I, O> strategy,
        AgentNode<?, ?>... nodes
    ) {
        return StrategyWorkflow.<I, O>builder()
            .name(name)
            .strategy(strategy)
            .addNodes(List.of(nodes))
            .build();
    }
}
