package dev.agents4j.workflow.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Sequential execution strategy that processes agent nodes one after another.
 * 
 * <p>This strategy executes agent nodes in the order they appear in the list,
 * passing the output of each node as input to the next node. This creates
 * a pipeline where data flows sequentially through each processing stage.</p>
 * 
 * <p><b>Characteristics:</b></p>
 * <ul>
 * <li>Deterministic execution order</li>
 * <li>Output of node N becomes input to node N+1</li>
 * <li>Failure in any node stops the entire execution</li>
 * <li>Memory efficient - only current data in memory</li>
 * <li>Easy to debug and trace execution flow</li>
 * </ul>
 * 
 * <p><b>Best for:</b></p>
 * <ul>
 * <li>Pipelines where each step depends on the previous</li>
 * <li>Data transformation workflows</li>
 * <li>Simple, linear processing flows</li>
 * <li>Scenarios where order matters</li>
 * </ul>
 * 
 * <p><b>Configuration Options:</b></p>
 * <ul>
 * <li><code>storeIntermediateResults</code> (Boolean): Whether to store intermediate results in context</li>
 * <li><code>continueOnError</code> (Boolean): Whether to continue processing if a node fails</li>
 * <li><code>timeoutMs</code> (Long): Maximum time to wait for each node (optional)</li>
 * </ul>
 *
 * @param <I> The input type for the first node
 * @param <O> The output type from the last node
 */
public class SequentialExecutionStrategy<I, O> implements WorkflowExecutionStrategy<I, O> {

    private static final String STRATEGY_NAME = "Sequential";
    
    // Configuration keys
    public static final String STORE_INTERMEDIATE_RESULTS = "storeIntermediateResults";
    public static final String CONTINUE_ON_ERROR = "continueOnError";
    public static final String TIMEOUT_MS = "timeoutMs";

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

        boolean storeIntermediateResults = (Boolean) context.getOrDefault(STORE_INTERMEDIATE_RESULTS, true);
        boolean continueOnError = (Boolean) context.getOrDefault(CONTINUE_ON_ERROR, false);
        
        Object currentInput = input;
        Object lastValidOutput = input;
        
        try {
            for (int i = 0; i < nodes.size(); i++) {
                AgentNode<Object, Object> node = (AgentNode<Object, Object>) nodes.get(i);
                
                try {
                    // Execute the current node
                    Object output = node.process(currentInput, context);
                    
                    // Store intermediate result if configured
                    if (storeIntermediateResults) {
                        context.put("step_" + i + "_input", currentInput);
                        context.put("step_" + i + "_output", output);
                        context.put("step_" + i + "_node", node.getName());
                    }
                    
                    // Update for next iteration
                    currentInput = output;
                    lastValidOutput = output;
                    
                } catch (Exception e) {
                    // Store error information
                    context.put("error_step", i);
                    context.put("error_node", node.getName());
                    context.put("error_input", currentInput);
                    
                    if (continueOnError) {
                        // Continue with the last valid output
                        currentInput = lastValidOutput;
                        context.put("step_" + i + "_skipped", true);
                        context.put("step_" + i + "_error", e.getMessage());
                    } else {
                        throw new WorkflowExecutionException(
                            "SequentialStrategy", 
                            "Failed at step " + i + " (node: " + node.getName() + ")", 
                            e
                        );
                    }
                }
            }
            
            // Store final execution metadata
            context.put("strategy_used", STRATEGY_NAME);
            context.put("total_steps", nodes.size());
            context.put("execution_successful", true);
            
            return (O) currentInput;
            
        } catch (WorkflowExecutionException e) {
            context.put("execution_successful", false);
            throw e;
        } catch (Exception e) {
            context.put("execution_successful", false);
            throw new WorkflowExecutionException(
                "SequentialStrategy", 
                "Sequential execution failed", 
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
        config.put("executionType", "sequential");
        config.put("supportsIntermediateResults", true);
        config.put("supportsContinueOnError", true);
        config.put("memoryEfficient", true);
        config.put("deterministic", true);
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
        characteristics.put("deterministic", true);
        
        // Estimate execution time as sum of individual node times
        boolean storeIntermediate = (Boolean) context.getOrDefault(STORE_INTERMEDIATE_RESULTS, true);
        characteristics.put("additionalOverhead", storeIntermediate ? "low" : "minimal");
        
        return characteristics;
    }

    /**
     * Create a new instance of SequentialExecutionStrategy.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new SequentialExecutionStrategy instance
     */
    public static <I, O> SequentialExecutionStrategy<I, O> create() {
        return new SequentialExecutionStrategy<>();
    }
}