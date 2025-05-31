package dev.agents4j.workflow;

import dev.agents4j.api.StatefulAgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.workflow.WorkflowRoute;
import dev.agents4j.api.workflow.WorkflowState;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Factory class for creating StatefulWorkflow instances with a fluent API.
 * Provides convenient methods for building common workflow patterns.
 */
public class StatefulWorkflowFactory {
    
    private StatefulWorkflowFactory() {
        // Utility class
    }
    
    /**
     * Creates a new workflow builder.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @return A new workflow builder
     */
    public static <I, O> StatefulWorkflowImpl.Builder<I, O> builder() {
        return StatefulWorkflowImpl.builder();
    }
    
    /**
     * Creates a simple sequential workflow where nodes are connected in order.
     *
     * @param name The workflow name
     * @param nodes The nodes in execution order
     * @param outputExtractor Function to extract output from final state
     * @param <I> The input type
     * @param <O> The output type
     * @return A configured workflow
     */
    @SafeVarargs
    public static <I, O> StatefulWorkflow<I, O> sequential(String name, 
                                                          StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor,
                                                          StatefulAgentNode<I>... nodes) {
        StatefulWorkflowImpl.Builder<I, O> builder = StatefulWorkflowImpl.<I, O>builder()
                .name(name)
                .outputExtractor(outputExtractor);
        
        // Add all nodes
        for (StatefulAgentNode<I> node : nodes) {
            builder.addNode(node);
        }
        
        // Set first node as entry point
        if (nodes.length > 0) {
            builder.defaultEntryPoint(nodes[0]);
        }
        
        // Create sequential routes
        for (int i = 0; i < nodes.length - 1; i++) {
            String routeId = "route-" + i + "-to-" + (i + 1);
            builder.addRoute(routeId, nodes[i].getNodeId(), nodes[i + 1].getNodeId());
        }
        
        return builder.build();
    }
    
    /**
     * Creates a conditional workflow where routing depends on state conditions.
     *
     * @param name The workflow name
     * @param entryPoint The entry point node
     * @param outputExtractor Function to extract output from final state
     * @param <I> The input type
     * @param <O> The output type
     * @return A workflow builder for adding conditional routes
     */
    public static <I, O> ConditionalWorkflowBuilder<I, O> conditional(String name,
                                                                      StatefulAgentNode<I> entryPoint,
                                                                      StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
        return new ConditionalWorkflowBuilder<>(name, entryPoint, outputExtractor);
    }
    
    /**
     * Creates a workflow with parallel execution paths that merge at a join node.
     *
     * @param name The workflow name
     * @param splitterNode Node that splits execution
     * @param joinNode Node that joins parallel paths
     * @param outputExtractor Function to extract output from final state
     * @param parallelNodes Nodes that execute in parallel
     * @param <I> The input type
     * @param <O> The output type
     * @return A configured workflow
     */
    @SafeVarargs
    public static <I, O> StatefulWorkflow<I, O> parallel(String name,
                                                         StatefulAgentNode<I> splitterNode,
                                                         StatefulAgentNode<I> joinNode,
                                                         StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor,
                                                         StatefulAgentNode<I>... parallelNodes) {
        StatefulWorkflowImpl.Builder<I, O> builder = StatefulWorkflowImpl.<I, O>builder()
                .name(name)
                .defaultEntryPoint(splitterNode)
                .outputExtractor(outputExtractor);
        
        // Add splitter and join nodes
        builder.addNode(splitterNode).addNode(joinNode);
        
        // Add parallel nodes
        for (StatefulAgentNode<I> node : parallelNodes) {
            builder.addNode(node);
        }
        
        // Create routes from splitter to parallel nodes
        for (int i = 0; i < parallelNodes.length; i++) {
            String routeId = "split-to-" + i;
            builder.addRoute(routeId, splitterNode.getNodeId(), parallelNodes[i].getNodeId());
        }
        
        // Create routes from parallel nodes to join
        for (int i = 0; i < parallelNodes.length; i++) {
            String routeId = "parallel-" + i + "-to-join";
            builder.addRoute(routeId, parallelNodes[i].getNodeId(), joinNode.getNodeId());
        }
        
        return builder.build();
    }
    
    /**
     * Creates a state machine workflow where transitions are based on state values.
     *
     * @param name The workflow name
     * @param initialState The initial state key
     * @param outputExtractor Function to extract output from final state
     * @param <I> The input type
     * @param <O> The output type
     * @return A state machine builder
     */
    public static <I, O> StateMachineBuilder<I, O> stateMachine(String name,
                                                               String initialState,
                                                               StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
        return new StateMachineBuilder<>(name, initialState, outputExtractor);
    }
    
    /**
     * Builder for conditional workflows.
     */
    public static class ConditionalWorkflowBuilder<I, O> {
        private final StatefulWorkflowImpl.Builder<I, O> builder;
        
        private ConditionalWorkflowBuilder(String name, StatefulAgentNode<I> entryPoint,
                                          StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
            this.builder = StatefulWorkflowImpl.<I, O>builder()
                    .name(name)
                    .defaultEntryPoint(entryPoint)
                    .outputExtractor(outputExtractor)
                    .addNode(entryPoint);
        }
        
        public ConditionalWorkflowBuilder<I, O> addNode(StatefulAgentNode<I> node) {
            builder.addNode(node);
            return this;
        }
        
        public ConditionalWorkflowBuilder<I, O> addConditionalRoute(String routeId,
                                                                    String fromNodeId,
                                                                    String toNodeId,
                                                                    BiPredicate<I, WorkflowState> condition) {
            WorkflowRoute<I> route = WorkflowRoute.<I>builder()
                    .id(routeId)
                    .from(fromNodeId)
                    .to(toNodeId)
                    .condition(condition)
                    .build();
            builder.addRoute(route);
            return this;
        }
        
        public ConditionalWorkflowBuilder<I, O> addDefaultRoute(String routeId,
                                                               String fromNodeId,
                                                               String toNodeId) {
            WorkflowRoute<I> route = WorkflowRoute.<I>builder()
                    .id(routeId)
                    .from(fromNodeId)
                    .to(toNodeId)
                    .asDefault()
                    .build();
            builder.addRoute(route);
            return this;
        }
        
        public ConditionalWorkflowBuilder<I, O> addRoute(WorkflowRoute<I> route) {
            builder.addRoute(route);
            return this;
        }
        
        public ConditionalWorkflowBuilder<I, O> maxExecutionSteps(int maxSteps) {
            builder.configuration(WorkflowExecutionConfiguration.builder()
                .maxExecutionSteps(maxSteps)
                .build());
            return this;
        }
        
        public StatefulWorkflow<I, O> build() {
            return builder.build();
        }
    }
    
    /**
     * Builder for state machine workflows.
     */
    public static class StateMachineBuilder<I, O> {
        private final StatefulWorkflowImpl.Builder<I, O> builder;
        private final String initialStateKey;
        
        private StateMachineBuilder(String name, String initialStateKey,
                                   StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
            this.builder = StatefulWorkflowImpl.<I, O>builder()
                    .name(name)
                    .outputExtractor(outputExtractor);
            this.initialStateKey = initialStateKey;
        }
        
        public StateMachineBuilder<I, O> addState(String stateValue, StatefulAgentNode<I> node) {
            builder.addNode(node);
            
            // Set as entry point if this is the initial state
            if (stateValue.equals(initialStateKey)) {
                builder.defaultEntryPoint(node);
            }
            
            return this;
        }
        
        public StateMachineBuilder<I, O> addTransition(String fromState,
                                                      String toState,
                                                      String stateKey,
                                                      Object expectedValue) {
            // Find nodes for the states (assuming state value is part of node ID)
            String routeId = "transition-" + fromState + "-to-" + toState;
            BiPredicate<I, WorkflowState> condition = (input, state) -> 
                    expectedValue.equals(state.get(stateKey).orElse(null));
            
            WorkflowRoute<I> route = WorkflowRoute.<I>builder()
                    .id(routeId)
                    .from(fromState)
                    .to(toState)
                    .condition(condition)
                    .build();
            
            builder.addRoute(route);
            return this;
        }
        
        public StateMachineBuilder<I, O> maxExecutionSteps(int maxSteps) {
            builder.configuration(WorkflowExecutionConfiguration.builder()
                .maxExecutionSteps(maxSteps)
                .build());
            return this;
        }
        
        public StatefulWorkflow<I, O> build() {
            return builder.build();
        }
    }
    
    /**
     * Convenience method to create a simple two-node approval workflow.
     */
    public static <I, O> StatefulWorkflow<I, O> approval(String name,
                                                         StatefulAgentNode<I> reviewNode,
                                                         StatefulAgentNode<I> approveNode,
                                                         StatefulAgentNode<I> rejectNode,
                                                         StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
        return conditional(name, reviewNode, outputExtractor)
                .addNode(approveNode)
                .addNode(rejectNode)
                .addConditionalRoute("review-to-approve", reviewNode.getNodeId(), approveNode.getNodeId(),
                        (input, state) -> Boolean.TRUE.equals(state.get("approved").orElse(false)))
                .addConditionalRoute("review-to-reject", reviewNode.getNodeId(), rejectNode.getNodeId(),
                        (input, state) -> Boolean.FALSE.equals(state.get("approved").orElse(false)))
                .addDefaultRoute("review-to-reject-default", reviewNode.getNodeId(), rejectNode.getNodeId())
                .build();
    }
    
    /**
     * Convenience method to create a retry workflow with maximum attempts.
     */
    public static <I, O> StatefulWorkflow<I, O> retry(String name,
                                                      StatefulAgentNode<I> processNode,
                                                      StatefulAgentNode<I> successNode,
                                                      StatefulAgentNode<I> failureNode,
                                                      int maxRetries,
                                                      StatefulWorkflowImpl.OutputExtractor<I, O> outputExtractor) {
        return conditional(name, processNode, outputExtractor)
                .addNode(successNode)
                .addNode(failureNode)
                .addConditionalRoute("process-to-success", processNode.getNodeId(), successNode.getNodeId(),
                        (input, state) -> Boolean.TRUE.equals(state.get("success").orElse(false)))
                .addConditionalRoute("process-retry", processNode.getNodeId(), processNode.getNodeId(),
                        (input, state) -> {
                            boolean failed = Boolean.FALSE.equals(state.get("success").orElse(false));
                            int attempts = state.get("attempts", 0);
                            return failed && attempts < maxRetries;
                        })
                .addConditionalRoute("process-to-failure", processNode.getNodeId(), failureNode.getNodeId(),
                        (input, state) -> {
                            boolean failed = Boolean.FALSE.equals(state.get("success").orElse(false));
                            int attempts = state.get("attempts", 0);
                            return failed && attempts >= maxRetries;
                        })
                .build();
    }
}