package dev.agents4j.api.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for different workflow execution approaches.
 * 
 * <p>This interface defines the contract for workflow execution strategies,
 * allowing different execution patterns to be plugged into workflows dynamically.
 * The Strategy Pattern enables separation of execution logic from workflow structure,
 * making the system more flexible and extensible.</p>
 * 
 * <p><b>Key Benefits:</b></p>
 * <ul>
 * <li>Decouples execution logic from workflow structure</li>
 * <li>Enables runtime strategy selection</li>
 * <li>Supports easy addition of new execution patterns</li>
 * <li>Promotes code reuse across different workflows</li>
 * </ul>
 * 
 * <p><b>Common Execution Strategies:</b></p>
 * <ul>
 * <li><b>Sequential</b>: Execute nodes one after another in order</li>
 * <li><b>Parallel</b>: Execute all nodes concurrently</li>
 * <li><b>Conditional</b>: Execute nodes based on runtime conditions</li>
 * <li><b>Batch</b>: Process inputs in batches with configurable size</li>
 * <li><b>Pipeline</b>: Streaming execution with overlapping processing</li>
 * </ul>
 *
 * @param <I> The input type for the workflow
 * @param <O> The output type for the workflow
 * 
 * @see dev.agents4j.workflow.strategy.SequentialExecutionStrategy
 * @see dev.agents4j.workflow.strategy.ParallelExecutionStrategy
 * @see dev.agents4j.workflow.strategy.ConditionalExecutionStrategy
 */
public interface WorkflowExecutionStrategy<I, O> {

    /**
     * Execute the given list of agent nodes with the provided input and context.
     * 
     * <p>This method defines how a collection of agent nodes should be executed
     * to transform the input into the desired output. The specific execution
     * pattern (sequential, parallel, etc.) is determined by the concrete
     * implementation.</p>
     *
     * @param nodes The list of agent nodes to execute. Must not be null or empty.
     *              The order may or may not be significant depending on the strategy.
     * @param input The input to process. Must not be null.
     * @param context The execution context containing additional parameters and state.
     *                May contain strategy-specific configuration options.
     * @return The processed output after executing the strategy
     * @throws WorkflowExecutionException if execution fails at any stage
     * @throws IllegalArgumentException if nodes is null/empty or input is null
     */
    O execute(List<AgentNode<?, ?>> nodes, I input, Map<String, Object> context) 
        throws WorkflowExecutionException;

    /**
     * Execute the workflow asynchronously.
     * 
     * <p>Provides non-blocking execution of the workflow strategy. The default
     * implementation wraps the synchronous execute method in a CompletableFuture,
     * but strategies may override this for more efficient async implementations.</p>
     *
     * @param nodes The list of agent nodes to execute
     * @param input The input to process
     * @param context The execution context
     * @return A CompletableFuture containing the processed output
     */
    default CompletableFuture<O> executeAsync(List<AgentNode<?, ?>> nodes, I input, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(nodes, input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async execution failed", e);
            }
        });
    }

    /**
     * Get the name of this execution strategy.
     * 
     * @return A descriptive name for this strategy (e.g., "Sequential", "Parallel")
     */
    String getStrategyName();

    /**
     * Get strategy-specific configuration information.
     * 
     * @return A map containing configuration details for this strategy
     */
    default Map<String, Object> getStrategyConfiguration() {
        return Map.of("strategyName", getStrategyName());
    }

    /**
     * Validate whether this strategy can execute the given nodes.
     * 
     * @param nodes The list of nodes to validate
     * @param context The execution context
     * @return true if the strategy can execute these nodes, false otherwise
     */
    default boolean canExecute(List<AgentNode<?, ?>> nodes, Map<String, Object> context) {
        return nodes != null && !nodes.isEmpty();
    }

    /**
     * Get the estimated execution characteristics for this strategy.
     * 
     * @param nodes The nodes that would be executed
     * @param context The execution context
     * @return A map containing execution characteristics like estimated time, memory usage, etc.
     */
    default Map<String, Object> getExecutionCharacteristics(List<AgentNode<?, ?>> nodes, Map<String, Object> context) {
        Map<String, Object> characteristics = new java.util.HashMap<>();
        characteristics.put("nodeCount", nodes != null ? nodes.size() : 0);
        characteristics.put("strategyType", getStrategyName());
        return characteristics;
    }
}