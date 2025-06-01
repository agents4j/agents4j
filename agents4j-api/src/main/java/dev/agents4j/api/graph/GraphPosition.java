package dev.agents4j.api.graph;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the current position and traversal history within a graph workflow.
 * Tracks node visits, edge traversals, and path information for analysis and cycle detection.
 */
public record GraphPosition(
    NodeId currentNodeId,
    Optional<NodeId> previousNodeId,
    List<NodeId> visitedNodes,
    Map<EdgeId, Instant> edgeTraversalHistory,
    int depth
) {
    
    public GraphPosition {
        Objects.requireNonNull(currentNodeId, "Current node ID cannot be null");
        Objects.requireNonNull(previousNodeId, "Previous node ID optional cannot be null");
        Objects.requireNonNull(visitedNodes, "Visited nodes list cannot be null");
        Objects.requireNonNull(edgeTraversalHistory, "Edge traversal history cannot be null");
        
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be negative");
        }
        
        // Make collections immutable
        visitedNodes = List.copyOf(visitedNodes);
        edgeTraversalHistory = Map.copyOf(edgeTraversalHistory);
    }
    
    /**
     * Creates an initial graph position at the specified node.
     *
     * @param startNode The starting node ID
     * @return A new GraphPosition at the start node
     */
    public static GraphPosition at(NodeId startNode) {
        Objects.requireNonNull(startNode, "Start node cannot be null");
        return new GraphPosition(
            startNode,
            Optional.empty(),
            List.of(startNode),
            Map.of(),
            0
        );
    }
    
    /**
     * Creates a new position by moving to the specified node.
     *
     * @param nodeId The target node ID
     * @return A new GraphPosition at the target node
     */
    public GraphPosition moveTo(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        
        var newVisited = new java.util.ArrayList<>(visitedNodes);
        newVisited.add(nodeId);
        
        return new GraphPosition(
            nodeId,
            Optional.of(currentNodeId),
            newVisited,
            edgeTraversalHistory,
            depth + 1
        );
    }
    
    /**
     * Creates a new position by traversing the specified edge.
     *
     * @param edgeId The edge being traversed
     * @param targetNode The target node ID
     * @return A new GraphPosition after edge traversal
     */
    public GraphPosition traverseEdge(EdgeId edgeId, NodeId targetNode) {
        Objects.requireNonNull(edgeId, "Edge ID cannot be null");
        Objects.requireNonNull(targetNode, "Target node cannot be null");
        
        var newVisited = new java.util.ArrayList<>(visitedNodes);
        newVisited.add(targetNode);
        
        var newEdgeHistory = new java.util.HashMap<>(edgeTraversalHistory);
        newEdgeHistory.put(edgeId, Instant.now());
        
        return new GraphPosition(
            targetNode,
            Optional.of(currentNodeId),
            newVisited,
            newEdgeHistory,
            depth + 1
        );
    }
    
    /**
     * Checks if the current node has been visited before (cycle detection).
     *
     * @return true if the current node was previously visited
     */
    public boolean hasCycle() {
        return visitedNodes.indexOf(currentNodeId) < visitedNodes.size() - 1;
    }
    
    /**
     * Checks if a specific node has been visited.
     *
     * @param nodeId The node ID to check
     * @return true if the node has been visited
     */
    public boolean hasVisited(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        return visitedNodes.contains(nodeId);
    }
    
    /**
     * Gets the complete path taken through the graph.
     *
     * @return An immutable list of node IDs representing the path
     */
    public List<NodeId> getPath() {
        return visitedNodes;
    }
    
    /**
     * Gets the number of times a node has been visited.
     *
     * @param nodeId The node ID to count
     * @return The visit count for the node
     */
    public long getVisitCount(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        return visitedNodes.stream()
            .filter(nodeId::equals)
            .count();
    }
    
    /**
     * Gets the timestamp when an edge was last traversed.
     *
     * @param edgeId The edge ID
     * @return Optional containing the traversal timestamp
     */
    public Optional<Instant> getEdgeTraversalTime(EdgeId edgeId) {
        Objects.requireNonNull(edgeId, "Edge ID cannot be null");
        return Optional.ofNullable(edgeTraversalHistory.get(edgeId));
    }
    
    /**
     * Checks if an edge has been traversed.
     *
     * @param edgeId The edge ID to check
     * @return true if the edge has been traversed
     */
    public boolean hasTraversedEdge(EdgeId edgeId) {
        Objects.requireNonNull(edgeId, "Edge ID cannot be null");
        return edgeTraversalHistory.containsKey(edgeId);
    }
    
    /**
     * Gets the path as a string representation.
     *
     * @return String representation of the traversal path
     */
    public String getPathString() {
        return visitedNodes.stream()
            .map(NodeId::value)
            .reduce((a, b) -> a + " -> " + b)
            .orElse("(empty path)");
    }
    
    /**
     * Creates a new position by resetting to a specific node (e.g., for error recovery).
     *
     * @param nodeId The node to reset to
     * @return A new GraphPosition reset to the specified node
     */
    public GraphPosition resetTo(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "Node ID cannot be null");
        
        var newVisited = new java.util.ArrayList<>(visitedNodes);
        newVisited.add(nodeId);
        
        return new GraphPosition(
            nodeId,
            Optional.of(currentNodeId),
            newVisited,
            edgeTraversalHistory,
            depth + 1
        );
    }
    
    @Override
    public String toString() {
        return String.format("GraphPosition{current=%s, depth=%d, path=%s}", 
            currentNodeId.value(), depth, getPathString());
    }
}