package dev.agents4j.workflow.builder;

import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.workflow.GraphWorkflowImpl;
import dev.agents4j.workflow.config.WorkflowConfiguration;
import dev.agents4j.workflow.monitor.NoOpWorkflowMonitor;
import dev.agents4j.workflow.monitor.WorkflowMonitor;
import dev.agents4j.workflow.output.OutputExtractor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Builder for creating GraphWorkflowImpl instances.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class GraphWorkflowBuilder<I, O> {

    private String name;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes = new HashMap<>();
    private final Map<EdgeId, GraphEdge> edges = new HashMap<>();
    private final Set<NodeId> entryPointIds = new HashSet<>();
    private NodeId defaultEntryPointId;
    private OutputExtractor<I, O> outputExtractor;
    private WorkflowConfiguration configuration =
        WorkflowConfiguration.defaultConfiguration();
    private WorkflowMonitor monitor = NoOpWorkflowMonitor.INSTANCE;
    private Executor asyncExecutor = ForkJoinPool.commonPool();

    /**
     * Sets the name of the workflow.
     *
     * @param name The workflow name
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Adds a node to the workflow.
     *
     * @param node The node to add
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> addNode(GraphWorkflowNode<I> node) {
        this.nodes.put(node.getNodeId(), node);
        if (node.isEntryPoint()) {
            this.entryPointIds.add(node.getNodeId());
        }
        return this;
    }

    /**
     * Adds an edge to the workflow.
     *
     * @param edge The edge to add
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> addEdge(GraphEdge edge) {
        this.edges.put(edge.edgeId(), edge);
        return this;
    }

    /**
     * Adds an edge between two nodes.
     *
     * @param fromNodeId The source node ID
     * @param toNodeId The target node ID
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> addEdge(
        NodeId fromNodeId,
        NodeId toNodeId
    ) {
        return addEdge(GraphEdge.between(fromNodeId, toNodeId));
    }

    /**
     * Adds a conditional edge between two nodes.
     *
     * @param fromNodeId The source node ID
     * @param toNodeId The target node ID
     * @param condition The edge condition
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> addEdge(
        NodeId fromNodeId,
        NodeId toNodeId,
        EdgeCondition condition
    ) {
        return addEdge(GraphEdge.conditional(fromNodeId, toNodeId, condition));
    }

    /**
     * Sets the default entry point for the workflow.
     *
     * @param entryPointId The default entry point node ID
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> defaultEntryPoint(NodeId entryPointId) {
        this.defaultEntryPointId = entryPointId;
        // Also ensure it's in the entry points list
        this.entryPointIds.add(entryPointId);
        return this;
    }

    /**
     * Sets the output extractor for the workflow.
     *
     * @param outputExtractor The output extractor
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> outputExtractor(
        OutputExtractor<I, O> outputExtractor
    ) {
        this.outputExtractor = outputExtractor;
        return this;
    }

    /**
     * Sets the configuration for the workflow.
     *
     * @param configuration The workflow configuration
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> configuration(
        WorkflowConfiguration configuration
    ) {
        this.configuration = Objects.requireNonNull(
            configuration,
            "Configuration cannot be null"
        );
        return this;
    }

    /**
     * Sets the monitor for the workflow.
     *
     * @param monitor The workflow monitor
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> monitor(WorkflowMonitor monitor) {
        this.monitor = monitor != null ? monitor : NoOpWorkflowMonitor.INSTANCE;
        return this;
    }

    /**
     * Sets the executor for asynchronous operations.
     *
     * @param asyncExecutor The executor for async operations
     * @return This builder instance
     */
    public GraphWorkflowBuilder<I, O> asyncExecutor(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor != null
            ? asyncExecutor
            : ForkJoinPool.commonPool();
        return this;
    }

    /**
     * Builds a new GraphWorkflowImpl.
     *
     * @return The built workflow
     * @throws IllegalStateException if the workflow is invalid
     */
    public GraphWorkflowImpl<I, O> build() {
        // Set a default name if not provided
        if (name == null || name.isBlank()) {
            name = "Workflow-" + System.currentTimeMillis();
        }

        // Set default entry point if only one entry point exists
        if (defaultEntryPointId == null && entryPointIds.size() == 1) {
            defaultEntryPointId = entryPointIds.iterator().next();
        }

        return new GraphWorkflowImpl<>(
            name,
            nodes,
            edges,
            entryPointIds,
            defaultEntryPointId,
            outputExtractor,
            configuration,
            monitor,
            asyncExecutor
        );
    }
}
