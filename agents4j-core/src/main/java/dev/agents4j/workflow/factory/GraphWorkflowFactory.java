package dev.agents4j.workflow.factory;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.workflow.GraphWorkflowImpl;
import dev.agents4j.workflow.builder.GraphWorkflowBuilder;
import dev.agents4j.workflow.config.WorkflowConfiguration;
import dev.agents4j.workflow.monitor.NoOpWorkflowMonitor;
import dev.agents4j.workflow.monitor.WorkflowMonitor;
import dev.agents4j.workflow.output.OutputExtractor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Factory for creating graph workflows with simplified APIs.
 * Provides convenience methods for common workflow patterns.
 */
public class GraphWorkflowFactory {

    /**
     * Creates a linear workflow that executes nodes in sequence.
     *
     * @param name The workflow name
     * @param nodes The nodes to execute in sequence
     * @param outputExtractor The output extractor
     * @param <I> The input type
     * @param <O> The output type
     * @return The created workflow
     */
    public static <I, O> GraphWorkflow<I, O> createLinearWorkflow(
        String name,
        GraphWorkflowNode<I>[] nodes,
        OutputExtractor<I, O> outputExtractor
    ) {
        if (nodes.length == 0) {
            throw new IllegalArgumentException("At least one node is required");
        }

        GraphWorkflowBuilder<I, O> builder = GraphWorkflowImpl.<I, O>builder()
            .name(name)
            .outputExtractor(outputExtractor);

        // Add all nodes
        for (GraphWorkflowNode<I> node : nodes) {
            builder.addNode(node);
        }

        // Add edges between nodes in sequence
        for (int i = 0; i < nodes.length - 1; i++) {
            builder.addEdge(nodes[i].getNodeId(), nodes[i + 1].getNodeId());
        }

        // Set first node as entry point
        builder.defaultEntryPoint(nodes[0].getNodeId());

        return builder.build();
    }

    /**
     * Creates a branching workflow with a router that directs to different branches.
     *
     * @param name The workflow name
     * @param router The router node
     * @param branches The branches to route to
     * @param outputExtractor The output extractor
     * @param <I> The input type
     * @param <O> The output type
     * @return The created workflow
     */
    public static <I, O> GraphWorkflow<I, O> createBranchingWorkflow(
        String name,
        GraphWorkflowNode<I> router,
        Branch<I>[] branches,
        OutputExtractor<I, O> outputExtractor
    ) {
        GraphWorkflowBuilder<I, O> builder = GraphWorkflowImpl.<I, O>builder()
            .name(name)
            .outputExtractor(outputExtractor)
            .addNode(router)
            .defaultEntryPoint(router.getNodeId());

        // Add each branch
        for (Branch<I> branch : branches) {
            // Add all nodes in the branch
            for (GraphWorkflowNode<I> node : branch.nodes) {
                builder.addNode(node);
            }

            // Connect router to the first node in the branch
            builder.addEdge(
                router.getNodeId(),
                branch.nodes[0].getNodeId(),
                branch.condition
            );

            // Connect nodes in the branch
            for (int i = 0; i < branch.nodes.length - 1; i++) {
                builder.addEdge(
                    branch.nodes[i].getNodeId(),
                    branch.nodes[i + 1].getNodeId()
                );
            }
        }

        return builder.build();
    }

    /**
     * Creates a workflow from a fluent specification.
     *
     * @param name The workflow name
     * @param specification The workflow specification
     * @param <I> The input type
     * @param <O> The output type
     * @return The created workflow
     */
    public static <I, O> GraphWorkflow<I, O> createWorkflow(
        String name,
        Consumer<WorkflowSpecification<I, O>> specification
    ) {
        WorkflowSpecification<I, O> spec = new WorkflowSpecification<>(name);
        specification.accept(spec);
        return spec.build();
    }

    /**
     * Represents a branch in a branching workflow.
     *
     * @param <I> The input type
     */
    public static class Branch<I> {
        private final EdgeCondition condition;
        private final GraphWorkflowNode<I>[] nodes;

        /**
         * Creates a new branch with the given condition and nodes.
         *
         * @param condition The condition for taking this branch
         * @param nodes The nodes in this branch
         */
        @SafeVarargs
        public Branch(EdgeCondition condition, GraphWorkflowNode<I>... nodes) {
            if (nodes.length == 0) {
                throw new IllegalArgumentException("Branch must have at least one node");
            }
            this.condition = condition;
            this.nodes = nodes;
        }
    }

    /**
     * Fluent specification for building workflows.
     *
     * @param <I> The input type
     * @param <O> The output type
     */
    public static class WorkflowSpecification<I, O> {
        private final GraphWorkflowBuilder<I, O> builder;

        /**
         * Creates a new workflow specification with the given name.
         *
         * @param name The workflow name
         */
        public WorkflowSpecification(String name) {
            this.builder = GraphWorkflowImpl.<I, O>builder().name(name);
        }

        /**
         * Adds a node to the workflow.
         *
         * @param node The node to add
         * @return This specification
         */
        public WorkflowSpecification<I, O> withNode(GraphWorkflowNode<I> node) {
            builder.addNode(node);
            return this;
        }

        /**
         * Adds an edge between two nodes.
         *
         * @param from The source node ID
         * @param to The target node ID
         * @return This specification
         */
        public WorkflowSpecification<I, O> withEdge(NodeId from, NodeId to) {
            builder.addEdge(from, to);
            return this;
        }

        /**
         * Adds a conditional edge between two nodes.
         *
         * @param from The source node ID
         * @param to The target node ID
         * @param condition The edge condition
         * @return This specification
         */
        public WorkflowSpecification<I, O> withEdge(
            NodeId from,
            NodeId to,
            EdgeCondition condition
        ) {
            builder.addEdge(from, to, condition);
            return this;
        }

        /**
         * Sets the entry point for the workflow.
         *
         * @param nodeId The entry point node ID
         * @return This specification
         */
        public WorkflowSpecification<I, O> withEntryPoint(NodeId nodeId) {
            builder.defaultEntryPoint(nodeId);
            return this;
        }

        /**
         * Sets the output extractor for the workflow.
         *
         * @param extractor The output extractor
         * @return This specification
         */
        public WorkflowSpecification<I, O> withOutputExtractor(
            OutputExtractor<I, O> extractor
        ) {
            builder.outputExtractor(extractor);
            return this;
        }

        /**
         * Sets the configuration for the workflow.
         *
         * @param configuration The workflow configuration
         * @return This specification
         */
        public WorkflowSpecification<I, O> withConfiguration(
            WorkflowConfiguration configuration
        ) {
            builder.configuration(configuration);
            return this;
        }

        /**
         * Sets the monitor for the workflow.
         *
         * @param monitor The workflow monitor
         * @return This specification
         */
        public WorkflowSpecification<I, O> withMonitor(WorkflowMonitor monitor) {
            builder.monitor(monitor);
            return this;
        }

        /**
         * Sets the executor for asynchronous operations.
         *
         * @param executor The executor
         * @return This specification
         */
        public WorkflowSpecification<I, O> withAsyncExecutor(Executor executor) {
            builder.asyncExecutor(executor);
            return this;
        }

        /**
         * Adds a router node to the workflow.
         *
         * @param nodeId The node ID
         * @param router The content router
         * @return This specification
         */
        public WorkflowSpecification<I, O> withRouter(
            NodeId nodeId,
            ContentRouter<I> router
        ) {
            builder.addRouter(nodeId, router);
            return this;
        }

        /**
         * Adds a dynamic router node to the workflow.
         *
         * @param nodeId The node ID
         * @param routingFunction The routing function
         * @return This specification
         */
        public WorkflowSpecification<I, O> withDynamicRouter(
            NodeId nodeId,
            Function<I, NodeId> routingFunction
        ) {
            builder.addDynamicRouter(nodeId, routingFunction);
            return this;
        }

        /**
         * Builds the workflow.
         *
         * @return The built workflow
         */
        public GraphWorkflow<I, O> build() {
            return builder.build();
        }
    }
}