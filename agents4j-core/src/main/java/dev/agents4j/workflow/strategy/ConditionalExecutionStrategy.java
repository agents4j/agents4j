package dev.agents4j.workflow.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Conditional execution strategy that executes nodes based on runtime conditions.
 * 
 * <p>This strategy allows for dynamic workflow execution where nodes are executed
 * only when specific conditions are met. This enables branching logic, early
 * termination, and adaptive processing based on input data or intermediate results.</p>
 * 
 * <p><b>Characteristics:</b></p>
 * <ul>
 * <li>Nodes execute only when their conditions are satisfied</li>
 * <li>Supports complex branching and decision logic</li>
 * <li>Can implement early termination patterns</li>
 * <li>Enables adaptive workflows that respond to data</li>
 * <li>Maintains execution order among conditional nodes</li>
 * </ul>
 * 
 * <p><b>Best for:</b></p>
 * <ul>
 * <li>Workflows with branching logic</li>
 * <li>Multi-step validation with early exit</li>
 * <li>Adaptive processing based on content type</li>
 * <li>A/B testing scenarios</li>
 * <li>Progressive enhancement workflows</li>
 * </ul>
 * 
 * <p><b>Configuration Options:</b></p>
 * <ul>
 * <li><code>defaultCondition</code> (Function): Default condition for nodes without explicit conditions</li>
 * <li><code>shortCircuit</code> (Boolean): Whether to stop execution after first successful node</li>
 * <li><code>requireAtLeastOne</code> (Boolean): Whether at least one node must execute</li>
 * <li><code>storeSkippedNodes</code> (Boolean): Whether to track which nodes were skipped</li>
 * </ul>
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type from the workflow
 */
public class ConditionalExecutionStrategy<I, O> implements WorkflowExecutionStrategy<I, O> {

    private static final String STRATEGY_NAME = "Conditional";
    
    // Configuration keys
    public static final String DEFAULT_CONDITION = "defaultCondition";
    public static final String SHORT_CIRCUIT = "shortCircuit";
    public static final String REQUIRE_AT_LEAST_ONE = "requireAtLeastOne";
    public static final String STORE_SKIPPED_NODES = "storeSkippedNodes";
    public static final String NODE_CONDITIONS = "nodeConditions";
    public static final String RESULT_SELECTOR = "resultSelector";
    
    // Default condition that always returns true
    private static final BiPredicate<Object, Map<String, Object>> DEFAULT_TRUE_CONDITION = (input, context) -> true;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public O execute(List<AgentNode<?, ?>> nodes, I input, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        Objects.requireNonNull(nodes, "Nodes list cannot be null");
        Objects.requireNonNull(input, "Input cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list cannot be empty");
        }

        // Extract configuration
        boolean shortCircuit = (Boolean) context.getOrDefault(SHORT_CIRCUIT, false);
        boolean requireAtLeastOne = (Boolean) context.getOrDefault(REQUIRE_AT_LEAST_ONE, true);
        boolean storeSkippedNodes = (Boolean) context.getOrDefault(STORE_SKIPPED_NODES, true);
        
        Map<String, BiPredicate<Object, Map<String, Object>>> nodeConditions = 
            (Map<String, BiPredicate<Object, Map<String, Object>>>) context.get(NODE_CONDITIONS);
        
        Function<List<Object>, O> resultSelector = 
            (Function<List<Object>, O>) context.get(RESULT_SELECTOR);
        
        BiPredicate<Object, Map<String, Object>> defaultCondition = 
            (BiPredicate<Object, Map<String, Object>>) context.getOrDefault(DEFAULT_CONDITION, DEFAULT_TRUE_CONDITION);

        try {
            Object currentInput = input;
            Object lastResult = null;
            List<Object> allResults = new java.util.ArrayList<>();
            List<String> executedNodes = new java.util.ArrayList<>();
            List<String> skippedNodes = new java.util.ArrayList<>();
            
            for (int i = 0; i < nodes.size(); i++) {
                AgentNode<Object, Object> node = (AgentNode<Object, Object>) nodes.get(i);
                String nodeName = node.getName();
                
                // Determine the condition for this node
                BiPredicate<Object, Map<String, Object>> condition = 
                    (nodeConditions != null && nodeConditions.containsKey(nodeName)) 
                        ? nodeConditions.get(nodeName) 
                        : defaultCondition;
                
                // Check if the condition is satisfied
                boolean shouldExecute = false;
                try {
                    shouldExecute = condition.test(currentInput, context);
                } catch (Exception e) {
                    context.put("condition_error_node", nodeName);
                    context.put("condition_error", e.getMessage());
                    // If condition evaluation fails, default to not executing
                    shouldExecute = false;
                }
                
                if (shouldExecute) {
                    try {
                        // Execute the node
                        Object result = node.process(currentInput, context);
                        
                        // Track execution
                        executedNodes.add(nodeName);
                        allResults.add(result);
                        lastResult = result;
                        
                        // Store intermediate result
                        context.put("result_" + nodeName, result);
                        context.put("step_" + i + "_executed", true);
                        
                        // Update input for potential next nodes (chain-like behavior)
                        currentInput = result;
                        
                        // Short-circuit if configured
                        if (shortCircuit) {
                            break;
                        }
                        
                    } catch (Exception e) {
                        context.put("error_node", nodeName);
                        context.put("error_step", i);
                        throw new WorkflowExecutionException(
                            "ConditionalStrategy", 
                            "Failed executing node: " + nodeName, 
                            e
                        );
                    }
                } else {
                    // Node was skipped
                    skippedNodes.add(nodeName);
                    context.put("step_" + i + "_skipped", true);
                    context.put("step_" + i + "_reason", "Condition not met");
                }
            }
            
            // Check if at least one node executed when required
            if (requireAtLeastOne && executedNodes.isEmpty()) {
                throw new WorkflowExecutionException(
                    "ConditionalStrategy", 
                    "No nodes were executed, but at least one execution is required"
                );
            }
            
            // Store execution metadata
            context.put("strategy_used", STRATEGY_NAME);
            context.put("total_nodes", nodes.size());
            context.put("executed_nodes", executedNodes);
            context.put("executed_count", executedNodes.size());
            
            if (storeSkippedNodes) {
                context.put("skipped_nodes", skippedNodes);
                context.put("skipped_count", skippedNodes.size());
            }
            
            context.put("execution_successful", true);
            
            // Determine the final result
            O finalResult;
            if (resultSelector != null) {
                // Use custom result selector
                finalResult = resultSelector.apply(allResults);
            } else if (!allResults.isEmpty()) {
                // Default: return the last result
                finalResult = (O) lastResult;
            } else {
                // No nodes executed
                finalResult = (O) input;
            }
            
            context.put("final_result", finalResult);
            return finalResult;
            
        } catch (WorkflowExecutionException e) {
            context.put("execution_successful", false);
            throw e;
        } catch (Exception e) {
            context.put("execution_successful", false);
            throw new WorkflowExecutionException(
                "ConditionalStrategy", 
                "Conditional execution failed", 
                e
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getStrategyConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("strategyName", STRATEGY_NAME);
        config.put("executionType", "conditional");
        config.put("supportsBranching", true);
        config.put("supportsEarlyTermination", true);
        config.put("supportsCustomConditions", true);
        config.put("memoryEfficient", true);
        config.put("deterministic", "depends on conditions");
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getExecutionCharacteristics(List<AgentNode<?, ?>> nodes, Map<String, Object> context) {
        Map<String, Object> characteristics = new HashMap<>();
        characteristics.put("nodeCount", nodes.size());
        characteristics.put("strategyType", getStrategyName());
        characteristics.put("expectedMemoryUsage", "low");
        characteristics.put("concurrency", 1);
        
        boolean shortCircuit = (Boolean) context.getOrDefault(SHORT_CIRCUIT, false);
        characteristics.put("canShortCircuit", shortCircuit);
        characteristics.put("deterministic", "depends on conditions");
        
        // Estimate potential nodes to execute (conservative estimate)
        characteristics.put("maxNodesExecuted", shortCircuit ? 1 : nodes.size());
        
        return characteristics;
    }

    /**
     * Builder for creating ConditionalExecutionStrategy with pre-configured conditions.
     */
    public static class Builder<I, O> {
        private final Map<String, BiPredicate<Object, Map<String, Object>>> nodeConditions = new HashMap<>();
        private BiPredicate<Object, Map<String, Object>> defaultCondition = DEFAULT_TRUE_CONDITION;
        private boolean shortCircuit = false;
        private boolean requireAtLeastOne = true;
        private Function<List<Object>, O> resultSelector = null;

        /**
         * Add a condition for a specific node.
         */
        public Builder<I, O> addCondition(String nodeName, BiPredicate<Object, Map<String, Object>> condition) {
            nodeConditions.put(nodeName, condition);
            return this;
        }

        /**
         * Set the default condition for nodes without explicit conditions.
         */
        public Builder<I, O> defaultCondition(BiPredicate<Object, Map<String, Object>> condition) {
            this.defaultCondition = condition;
            return this;
        }

        /**
         * Enable short-circuit execution (stop after first successful node).
         */
        public Builder<I, O> shortCircuit(boolean enabled) {
            this.shortCircuit = enabled;
            return this;
        }

        /**
         * Set whether at least one node must execute.
         */
        public Builder<I, O> requireAtLeastOne(boolean required) {
            this.requireAtLeastOne = required;
            return this;
        }

        /**
         * Set a custom result selector function.
         */
        public Builder<I, O> resultSelector(Function<List<Object>, O> selector) {
            this.resultSelector = selector;
            return this;
        }

        /**
         * Build the strategy and configure the execution context.
         */
        public ConditionalExecutionStrategy<I, O> build() {
            return new ConditionalExecutionStrategy<>();
        }

        /**
         * Build the strategy and return a pre-configured context map.
         */
        public Map<String, Object> buildContext() {
            Map<String, Object> context = new HashMap<>();
            context.put(NODE_CONDITIONS, nodeConditions);
            context.put(DEFAULT_CONDITION, defaultCondition);
            context.put(SHORT_CIRCUIT, shortCircuit);
            context.put(REQUIRE_AT_LEAST_ONE, requireAtLeastOne);
            if (resultSelector != null) {
                context.put(RESULT_SELECTOR, resultSelector);
            }
            return context;
        }
    }

    /**
     * Create a new instance of ConditionalExecutionStrategy.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new ConditionalExecutionStrategy instance
     */
    public static <I, O> ConditionalExecutionStrategy<I, O> create() {
        return new ConditionalExecutionStrategy<>();
    }

    /**
     * Create a builder for configuring conditional execution.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new Builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }
}