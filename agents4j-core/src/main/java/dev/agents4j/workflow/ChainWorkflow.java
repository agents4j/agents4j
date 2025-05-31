package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.Workflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.workflow.adapters.AgentNodeAdapter;
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
 * This implementation uses StatefulWorkflow internally for enhanced capabilities
 * including state tracking, error handling, and debugging support.
 *
 * @param <I> The input type for the first node in the chain
 * @param <O> The output type of the last node in the chain
 */
public class ChainWorkflow<I, O> implements Workflow<I, O> {

    private final String name;
    private final List<AgentNode<?, ?>> nodes;
    private final StatefulWorkflow<I, O> statefulWorkflow;

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

        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.statefulWorkflow = createStatefulWorkflow(name, nodes);
    }

    /**
     * Creates the internal StatefulWorkflow from the list of AgentNodes.
     */
    @SuppressWarnings("unchecked")
    private StatefulWorkflow<I, O> createStatefulWorkflow(
        String name,
        List<? extends AgentNode<?, ?>> nodes
    ) {
        StatefulWorkflowImpl.Builder<I, O> builder = StatefulWorkflowImpl.<
                I,
                O
            >builder()
            .name(name + "_stateful")
            .outputExtractor((input, state, context) -> {
                // Extract final output from state
                @SuppressWarnings("unchecked")
                O finalOutput = (O) state.get("final_output").orElse(null);
                if (finalOutput != null) {
                    return finalOutput;
                }
                // Fallback to input if no final output (shouldn't happen in normal flow)
                return (O) input;
            })
            .configuration(
                WorkflowExecutionConfiguration.builder()
                    .maxExecutionSteps(nodes.size() + 10) // Allow some buffer for error handling
                    .build()
            );

        // Convert each AgentNode to StatefulAgentNode and build the chain
        AgentNodeAdapter<I> entryPoint = null;

        for (int i = 0; i < nodes.size(); i++) {
            AgentNode<?, ?> node = nodes.get(i);
            String nodeId = "chain_node_" + i;

            boolean isEntryPoint = (i == 0);
            boolean isLastNode = (i == nodes.size() - 1);

            @SuppressWarnings("unchecked")
            AgentNode<I, ?> typedNode = (AgentNode<I, ?>) node;

            AgentNodeAdapter<I> adapter = new AgentNodeAdapter<>(
                typedNode,
                nodeId,
                isEntryPoint,
                isLastNode
            );

            builder.addNode(adapter);

            if (isEntryPoint) {
                entryPoint = adapter;
                builder.defaultEntryPoint(adapter);
            }

            // Add route to next node (except for last node)
            if (!isLastNode) {
                String nextNodeId = "chain_node_" + (i + 1);
                WorkflowRoute<I> route = WorkflowRoute.<I>builder()
                    .id("chain_route_" + i + "_to_" + (i + 1))
                    .from(nodeId)
                    .to(nextNodeId)
                    .description(
                        "Sequential chain route from node " +
                        i +
                        " to " +
                        (i + 1)
                    )
                    .build();
                builder.addRoute(route);
            }
        }

        return builder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public O execute(I input) throws WorkflowExecutionException {
        return execute(input, new HashMap<>());
    }

    @Override
    public O execute(I input, Map<String, Object> context)
        throws WorkflowExecutionException {
        Objects.requireNonNull(input, "Input cannot be null");

        try {
            StatefulWorkflowResult<O> result = statefulWorkflow.start(
                input,
                context
            );

            if (result.isCompleted()) {
                return result
                    .getOutput()
                    .orElseThrow(() ->
                        new WorkflowExecutionException(
                            name,
                            "Workflow completed but no output available"
                        )
                    );
            } else if (result.isError()) {
                String errorMessage = result
                    .getErrorMessage()
                    .orElse("Unknown error occurred");
                throw new WorkflowExecutionException(
                    name,
                    "Chain workflow execution failed: " + errorMessage
                );
            } else if (result.isSuspended()) {
                throw new WorkflowExecutionException(
                    name,
                    "Chain workflow unexpectedly suspended - this should not happen in normal execution"
                );
            } else {
                throw new WorkflowExecutionException(
                    name,
                    "Chain workflow completed with unexpected status: " +
                    result.getStatus()
                );
            }
        } catch (WorkflowExecutionException e) {
            throw e; // Re-throw workflow exceptions as-is
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("inputType", input.getClass().getSimpleName());
            errorContext.put("nodeCount", nodes.size());
            errorContext.put("contextKeys", context.keySet());
            throw new WorkflowExecutionException(
                name,
                "Chain workflow execution failed",
                e
            );
        }
    }

    @Override
    public CompletableFuture<O> executeAsync(I input) {
        return executeAsync(input, new HashMap<>());
    }

    @Override
    public CompletableFuture<O> executeAsync(
        I input,
        Map<String, Object> context
    ) {
        return statefulWorkflow
            .startAsync(input, context)
            .thenApply(result -> {
                if (result.isCompleted()) {
                    return result
                        .getOutput()
                        .orElseThrow(() ->
                            new RuntimeException(
                                "Workflow completed but no output available"
                            )
                        );
                } else if (result.isError()) {
                    String errorMessage = result
                        .getErrorMessage()
                        .orElse("Unknown error occurred");
                    throw new RuntimeException(
                        "Chain workflow execution failed: " + errorMessage
                    );
                } else {
                    throw new RuntimeException(
                        "Chain workflow completed with unexpected status: " +
                        result.getStatus()
                    );
                }
            });
    }

    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("nodeCount", nodes.size());
        config.put("workflowType", "chain");
        config.put("implementation", "stateful");
        config.put(
            "nodes",
            nodes.stream().map(AgentNode::getName).toArray(String[]::new)
        );
        config.put("statefulNodes", statefulWorkflow.getNodes().size());
        config.put("routes", statefulWorkflow.getRoutes().size());
        return config;
    }

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
        return nodes;
    }

    /**
     * Get access to the underlying StatefulWorkflow for advanced operations.
     * This allows access to state inspection, route information, and other
     * advanced StatefulWorkflow features.
     *
     * @return The underlying StatefulWorkflow instance
     */
    public StatefulWorkflow<I, O> getStatefulWorkflow() {
        return statefulWorkflow;
    }

    /**
     * Execute the workflow and return the full StatefulWorkflowResult.
     * This provides access to execution state, intermediate results, and metadata.
     *
     * @param input The input to process
     * @param context Additional context
     * @return The complete StatefulWorkflowResult
     * @throws WorkflowExecutionException if execution fails
     */
    public StatefulWorkflowResult<O> executeWithState(
        I input,
        Map<String, Object> context
    ) throws WorkflowExecutionException {
        return statefulWorkflow.start(input, context);
    }

    /**
     * Execute the workflow and return the full StatefulWorkflowResult.
     * This provides access to execution state, intermediate results, and metadata.
     *
     * @param input The input to process
     * @return The complete StatefulWorkflowResult
     * @throws WorkflowExecutionException if execution fails
     */
    public StatefulWorkflowResult<O> executeWithState(I input)
        throws WorkflowExecutionException {
        return executeWithState(input, new HashMap<>());
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
            return this;
        }

        /**
         * Add a node to the chain.
         *
         * @param node The agent node to add
         * @return This builder instance for method chaining
         */
        public Builder<I, O> node(AgentNode<?, ?> node) {
            Objects.requireNonNull(node, "Node cannot be null");
            nodes.add(node);
            return this;
        }

        /**
         * Add the first node to the chain.
         * This is equivalent to node() but provides semantic clarity.
         *
         * @param node The first agent node to add
         * @return This builder instance for method chaining
         */
        public Builder<I, O> firstNode(AgentNode<? super I, ?> node) {
            return node(node);
        }

        /**
         * Add multiple nodes to the workflow in the order specified.
         *
         * @param nodes The nodes to add
         * @return This builder instance for method chaining
         */
        public final Builder<I, O> nodes(AgentNode<?, ?>... nodes) {
            for (AgentNode<?, ?> node : nodes) {
                node(node);
            }
            return this;
        }

        /**
         * Build the ChainWorkflow instance.
         *
         * @return A new ChainWorkflow instance
         * @throws IllegalStateException if no nodes have been added
         */
        public ChainWorkflow<I, O> build() {
            if (nodes.isEmpty()) {
                throw new IllegalStateException(
                    "Chain workflow must contain at least one node"
                );
            }

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
        return new Builder<>();
    }
}
