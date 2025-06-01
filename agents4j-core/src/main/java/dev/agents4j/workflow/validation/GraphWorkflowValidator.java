package dev.agents4j.workflow.validation;

import dev.agents4j.api.graph.EdgeId;
import dev.agents4j.api.graph.GraphEdge;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.validation.ValidationResult;
import dev.agents4j.workflow.output.OutputExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for graph workflows.
 * Checks workflow structure and configuration for validity.
 *
 * @param <I> The input type for the workflow
 */
public class GraphWorkflowValidator<I> {

    private final String name;
    private final Map<NodeId, GraphWorkflowNode<I>> nodes;
    private final Map<EdgeId, GraphEdge> edges;
    private final Set<NodeId> entryPointIds;
    private final NodeId defaultEntryPointId;
    private final OutputExtractor<I, ?> outputExtractor;

    /**
     * Creates a new validator for the given workflow components.
     *
     * @param name The workflow name
     * @param nodes The workflow nodes
     * @param edges The workflow edges
     * @param entryPointIds The entry point node IDs
     * @param defaultEntryPointId The default entry point node ID
     * @param outputExtractor The output extractor
     */
    public GraphWorkflowValidator(
        String name,
        Map<NodeId, GraphWorkflowNode<I>> nodes,
        Map<EdgeId, GraphEdge> edges,
        Set<NodeId> entryPointIds,
        NodeId defaultEntryPointId,
        OutputExtractor<I, ?> outputExtractor
    ) {
        this.name = name;
        this.nodes = nodes;
        this.edges = edges;
        this.entryPointIds = entryPointIds;
        this.defaultEntryPointId = defaultEntryPointId;
        this.outputExtractor = outputExtractor;
    }

    /**
     * Validates the workflow configuration.
     *
     * @return ValidationResult containing any validation errors
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();

        // Basic structural validation
        if (nodes.isEmpty()) {
            errors.add("Workflow must have at least one node");
        }

        if (entryPointIds.isEmpty()) {
            errors.add("Workflow must have at least one entry point");
        }

        if (defaultEntryPointId == null && entryPointIds.size() > 1) {
            errors.add(
                "Default entry point required when multiple entry points exist"
            );
        }

        if (outputExtractor == null) {
            errors.add("Output extractor is required");
        }

        // Validate node references
        for (EdgeId edgeId : edges.keySet()) {
            GraphEdge edge = edges.get(edgeId);

            if (!nodes.containsKey(edge.fromNode())) {
                errors.add(
                    "Edge " +
                    edgeId.value() +
                    " references non-existent source node: " +
                    edge.fromNode().value()
                );
            }

            if (!nodes.containsKey(edge.toNode())) {
                errors.add(
                    "Edge " +
                    edgeId.value() +
                    " references non-existent target node: " +
                    edge.toNode().value()
                );
            }
        }

        // Validate reachability from entry points
        if (errors.isEmpty()) {
            validateReachability(errors);
        }

        if (errors.size() > 0) {
            return ValidationResult.failure(errors);
        } else {
            return ValidationResult.success();
        }
    }

    /**
     * Validates that all nodes are reachable from an entry point.
     *
     * @param errors List to add validation errors to
     */
    private void validateReachability(List<String> errors) {
        // Only validate if we have entry points and nodes
        if (entryPointIds.isEmpty() || nodes.isEmpty()) {
            return;
        }

        Set<NodeId> reachableNodes = findReachableNodes();
        
        Set<NodeId> unreachableNodes = nodes.keySet()
            .stream()
            .filter(nodeId -> !reachableNodes.contains(nodeId))
            .collect(Collectors.toSet());
            
        if (!unreachableNodes.isEmpty()) {
            errors.add(
                "The following nodes are unreachable from any entry point: " +
                unreachableNodes.stream()
                    .map(NodeId::value)
                    .collect(Collectors.joining(", "))
            );
        }
    }

    /**
     * Finds all nodes reachable from entry points.
     *
     * @return Set of reachable node IDs
     */
    private Set<NodeId> findReachableNodes() {
        Set<NodeId> reachable = new HashSet<>();
        Set<NodeId> toProcess = new HashSet<>(entryPointIds);
        
        while (!toProcess.isEmpty()) {
            NodeId nodeId = toProcess.iterator().next();
            toProcess.remove(nodeId);
            
            if (reachable.contains(nodeId)) {
                continue;
            }
            
            reachable.add(nodeId);
            
            // Find edges from this node
            for (GraphEdge edge : edges.values()) {
                if (edge.fromNode().equals(nodeId)) {
                    NodeId targetNode = edge.toNode();
                    if (!reachable.contains(targetNode)) {
                        toProcess.add(targetNode);
                    }
                }
            }
        }
        
        return reachable;
    }
}