package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.AgentWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A workflow that chains multiple agent nodes together.
 * The output of each node becomes the input to the next node in the chain.
 *
 * @param <I> The input type for the first node in the chain
 * @param <O> The output type of the last node in the chain
 */
public class ChainWorkflow<I, O> implements AgentWorkflow<I, O> {

    private final String name;
    private final List<AgentNode<?, ?>> nodes;

    /**
     * Creates a new ChainWorkflow with the given name and nodes.
     *
     * @param name The name of the workflow
     * @param nodes The list of agent nodes that form the chain
     */
    private ChainWorkflow(String name, List<? extends AgentNode<?, ?>> nodes) {
        this.name = Objects.requireNonNull(
            name,
            "Workflow name cannot be null"
        );
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException(
                "Chain workflow must contain at least one node"
            );
        }
        this.nodes = new ArrayList<>(nodes);
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
    @SuppressWarnings("unchecked")
    @Override
    public O execute(I input, Map<String, Object> context) throws WorkflowExecutionException {
        Objects.requireNonNull(input, "Input cannot be null");

        try {
            // Start with the input to the first node
            Object currentInput = input;

            // Process each node in the chain sequentially
            for (int i = 0; i < nodes.size(); i++) {
                AgentNode<Object, Object> node = (AgentNode<
                        Object,
                        Object
                    >) nodes.get(i);
                currentInput = node.process(currentInput, context);

                // Store the intermediate result in the context for debugging/tracking
                context.put("result_" + node.getName(), currentInput);
            }

            // The output of the last node is the output of the workflow
            return (O) currentInput;
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("inputType", input.getClass().getSimpleName());
            errorContext.put("nodeCount", nodes.size());
            throw new WorkflowExecutionException(name, "Chain workflow execution failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(I input) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(
        I input,
        Map<String, Object> context
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Builder for creating ChainWorkflow instances.
     *
     * @param <I> The input type for the first node in the chain
     * @param <O> The output type of the last node in the chain
     */
    public static class Builder<I, O> {

        private String name;
        private final List<AgentNode<?, ?>> nodes = new ArrayList<>();

        /**
         * Set the name of the workflow.
         *
         * @param name The name of the workflow
         * @return This builder instance for method chaining
         */
        public Builder<I, O> name(String name) {
            this.name = name;
            return (Builder<I, O>) this;
        }

        /**
         * Add a node to the chain.
         *
         * @param node The agent node to add
         * @param <N> The output type of the node being added
         * @return A new builder instance with the updated generic types
         */
        public <N> Builder<I, O> node(AgentNode<? super I, ?> node) {
            nodes.add(node);
            return this;
        }

        /**
         * Add the first node to the chain.
         *
         * @param node The first agent node to add
         * @param <N> The output type of the first node
         * @return A new builder instance with the updated generic type
         */
        public <N> Builder<I, O> firstNode(AgentNode<? super I, N> node) {
            nodes.add(node);
            return this;
        }

        /**
         * Build the ChainWorkflow instance.
         *
         * @return A new ChainWorkflow instance
         */
        public ChainWorkflow<I, O> build() {
            if (name == null) {
                name = "ChainWorkflow-" + System.currentTimeMillis();
            }
            return new ChainWorkflow<>(name, nodes);
        }
    }

    /**
     * Create a new Builder to construct a ChainWorkflow.
     *
     * @param <I> The input type for the first node
     * @param <O> The output type for the last node
     * @return A new Builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<I, O>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("nodeCount", nodes.size());
        config.put("workflowType", "chain");
        config.put("nodes", nodes.stream().map(AgentNode::getName).toArray(String[]::new));
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
     * Get the list of nodes in this workflow.
     *
     * @return An unmodifiable list of the nodes in this workflow
     */
    public List<AgentNode<?, ?>> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
}
