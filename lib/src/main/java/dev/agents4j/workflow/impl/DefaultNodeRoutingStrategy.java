package dev.agents4j.workflow.impl;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.workflow.api.NodeRoutingStrategy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of NodeRoutingStrategy that uses configured routes
 * to determine the next node in workflow execution.
 */
public class DefaultNodeRoutingStrategy<I> implements NodeRoutingStrategy<I> {
    
    private final Map<String, List<WorkflowRoute<I>>> routesByFromNode;
    private final Map<String, StatefulAgentNode<I>> nodes;
    
    public DefaultNodeRoutingStrategy(List<WorkflowRoute<I>> routes, 
                                     Map<String, StatefulAgentNode<I>> nodes) {
        this.nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
        
        // Build route index for fast lookup
        this.routesByFromNode = routes.stream()
                .collect(Collectors.groupingBy(WorkflowRoute::getFromNodeId));
    }
    
    @Override
    public Optional<StatefulAgentNode<I>> findNextNode(StatefulAgentNode<I> currentNode, 
                                                       I input, 
                                                       WorkflowState state) {
        if (currentNode == null) {
            return Optional.empty();
        }
        
        String currentNodeId = currentNode.getNodeId();
        List<WorkflowRoute<I>> routes = routesByFromNode.get(currentNodeId);
        
        if (routes == null || routes.isEmpty()) {
            return Optional.empty();
        }
        
        // Find the first route that matches the current conditions
        for (WorkflowRoute<I> route : routes) {
            if (route.matches(input, state)) {
                String targetNodeId = route.getToNodeId();
                StatefulAgentNode<I> targetNode = nodes.get(targetNodeId);
                
                if (targetNode != null) {
                    return Optional.of(targetNode);
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public String getStrategyName() {
        return "DefaultNodeRoutingStrategy";
    }
    
    @Override
    public boolean canRoute(StatefulAgentNode<I> currentNode) {
        if (currentNode == null) {
            return false;
        }
        
        List<WorkflowRoute<I>> routes = routesByFromNode.get(currentNode.getNodeId());
        return routes != null && !routes.isEmpty();
    }
    
    @Override
    public int getPriority() {
        return 50; // Medium priority - allows custom strategies to override
    }
    
    /**
     * Gets all routes from a specific node.
     *
     * @param nodeId The source node ID
     * @return Unmodifiable list of routes from the node
     */
    public List<WorkflowRoute<I>> getRoutesFrom(String nodeId) {
        List<WorkflowRoute<I>> routes = routesByFromNode.get(nodeId);
        return routes != null ? Collections.unmodifiableList(routes) : Collections.emptyList();
    }
    
    /**
     * Checks if there are any routes configured from the given node.
     *
     * @param nodeId The source node ID
     * @return true if routes exist from the node
     */
    public boolean hasRoutesFrom(String nodeId) {
        List<WorkflowRoute<I>> routes = routesByFromNode.get(nodeId);
        return routes != null && !routes.isEmpty();
    }
    
    /**
     * Gets the total number of configured routes.
     *
     * @return The total route count
     */
    public int getRouteCount() {
        return routesByFromNode.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}