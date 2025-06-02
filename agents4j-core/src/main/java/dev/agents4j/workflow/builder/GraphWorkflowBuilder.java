package dev.agents4j.workflow.builder;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.*;
import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.serialization.WorkflowStateSerializer;
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
 * Builder for creating enhanced GraphWorkflow instances with type safety.
 * By default creates EnhancedGraphWorkflowImpl instances with full type safety
 * and enhanced suspension/resumption capabilities.
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 */
public class GraphWorkflowBuilder<I, O> {

    private String name;
    private String version = "1.0.0";
    private Class<I> inputType;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes = new HashMap<>();
    private final Map<EdgeId, GraphEdge> edges = new HashMap<>();
    private final Set<NodeId> entryPointIds = new HashSet<>();
    private NodeId defaultEntryPointId;
    private OutputExtractor<I, O> outputExtractor;
    private WorkflowConfiguration configuration =
        WorkflowConfiguration.defaultConfiguration();
    private WorkflowMonitor monitor = NoOpWorkflowMonitor.INSTANCE;
    private Executor asyncExecutor = ForkJoinPool.commonPool();

    private WorkflowStateSerializer<GraphWorkflowState<I>> customSerializer;

    /**
     * Creates a new builder with the specified input type for enhanced type safety.
     *
     * @param inputType The input type class
     * @param <I> The input type
     * @param <O> The output type
     * @return A new builder instance
     */
    public static <I, O> GraphWorkflowBuilder<I, O> create(Class<I> inputType) {
        return new GraphWorkflowBuilder<I, O>().inputType(inputType);
    }

    /**
     * Sets the name of the workflow.
     *
     * @param name The workflow name
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the version of the workflow for compatibility tracking.
     *
     * @param version The workflow version
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> version(String version) {
        this.version = Objects.requireNonNull(
            version,
            "Version cannot be null"
        );
        return this;
    }

    /**
     * Sets the input type for enhanced type safety.
     *
     * @param inputType The input type class
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> inputType(Class<I> inputType) {
        this.inputType = Objects.requireNonNull(
            inputType,
            "Input type cannot be null"
        );
        return this;
    }

    /**
     * Sets a custom serializer for workflow state.
     *
     * @param customSerializer The custom serializer
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> customSerializer(
        WorkflowStateSerializer<GraphWorkflowState<I>> customSerializer
    ) {
        this.customSerializer = Objects.requireNonNull(
            customSerializer,
            "Custom serializer cannot be null"
        );
        return this;
    }

    /**
     * Adds a node to the workflow.
     *
     * @param node The node to add
     * @return This builder
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
     * @return This builder
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
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> addEdge(
        NodeId fromNodeId,
        NodeId toNodeId
    ) {
        return addEdge(GraphEdge.between(fromNodeId, toNodeId));
    }

    /**
     * Sets the default entry point for the workflow.
     *
     * @param entryPointId The default entry point node ID
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> defaultEntryPoint(NodeId entryPointId) {
        this.defaultEntryPointId = entryPointId;
        this.entryPointIds.add(entryPointId);
        return this;
    }

    /**
     * Sets the output extractor for the workflow.
     *
     * @param outputExtractor The output extractor
     * @return This builder
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
     * @return This builder
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
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> monitor(WorkflowMonitor monitor) {
        this.monitor = monitor != null ? monitor : NoOpWorkflowMonitor.INSTANCE;
        return this;
    }

    /**
     * Sets the executor for asynchronous operations.
     *
     * @param asyncExecutor The executor for async operations
     * @return This builder
     */
    public GraphWorkflowBuilder<I, O> asyncExecutor(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor != null
            ? asyncExecutor
            : ForkJoinPool.commonPool();
        return this;
    }

    /**
     * Builds an enhanced workflow instance with type safety.
     *
     * @return An GraphWorkflowImpl instance
     */
    public GraphWorkflow<I, O> build() {
        validateConfiguration();

        if (inputType == null) {
            throw new IllegalStateException(
                "Input type is required for enhanced workflow"
            );
        }

        if (customSerializer != null) {
            return new GraphWorkflowImpl<>(
                name,
                version,
                inputType,
                Map.copyOf(nodes),
                Map.copyOf(edges),
                Set.copyOf(entryPointIds),
                defaultEntryPointId,
                outputExtractor,
                configuration,
                monitor,
                asyncExecutor,
                customSerializer
            );
        }

        return new GraphWorkflowImpl<>(
            name,
            version,
            inputType,
            Map.copyOf(nodes),
            Map.copyOf(edges),
            Set.copyOf(entryPointIds),
            defaultEntryPointId,
            outputExtractor,
            configuration,
            monitor,
            asyncExecutor
        );
    }

    private void validateConfiguration() {
        if (name == null || name.trim().isEmpty()) {
            name = "Workflow-" + System.currentTimeMillis();
        }

        if (entryPointIds.isEmpty()) {
            throw new IllegalStateException(
                "At least one entry point is required"
            );
        }

        if (defaultEntryPointId == null && entryPointIds.size() == 1) {
            defaultEntryPointId = entryPointIds.iterator().next();
        }

        if (defaultEntryPointId == null) {
            throw new IllegalStateException(
                "Default entry point is required when multiple entry points exist"
            );
        }

        if (!entryPointIds.contains(defaultEntryPointId)) {
            throw new IllegalStateException(
                "Default entry point must be one of the entry points"
            );
        }

        if (outputExtractor == null) {
            throw new IllegalStateException("Output extractor is required");
        }

        // Validate that all edge endpoints exist as nodes
        for (GraphEdge edge : edges.values()) {
            if (!nodes.containsKey(edge.fromNode())) {
                throw new IllegalStateException(
                    "Edge source node not found: " + edge.fromNode()
                );
            }
            if (!nodes.containsKey(edge.toNode())) {
                throw new IllegalStateException(
                    "Edge target node not found: " + edge.toNode()
                );
            }
        }
    }

    public static <I, O> GraphWorkflowBuilder<I, O> builder() {
        return new GraphWorkflowBuilder<>();
    }
}
