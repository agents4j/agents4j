package dev.agents4j.workflow.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Parallel execution strategy that processes all agent nodes concurrently.
 * 
 * <p>This strategy executes all agent nodes simultaneously with the same input,
 * then aggregates their results. This is ideal for scenarios where multiple
 * independent processing operations need to be performed on the same data.</p>
 * 
 * <p><b>Characteristics:</b></p>
 * <ul>
 * <li>All nodes receive the same input</li>
 * <li>Nodes execute concurrently using a thread pool</li>
 * <li>Results are aggregated into a list maintaining node order</li>
 * <li>Failure in one node can optionally fail the entire execution</li>
 * <li>Significantly faster for independent operations</li>
 * </ul>
 * 
 * <p><b>Best for:</b></p>
 * <ul>
 * <li>Independent analysis operations (sentiment, classification, etc.)</li>
 * <li>Validation workflows with multiple validators</li>
 * <li>Content generation with multiple approaches</li>
 * <li>Scenarios where speed is critical and operations are independent</li>
 * </ul>
 * 
 * <p><b>Configuration Options:</b></p>
 * <ul>
 * <li><code>maxConcurrency</code> (Integer): Maximum number of concurrent threads</li>
 * <li><code>timeoutMs</code> (Long): Maximum time to wait for all nodes to complete</li>
 * <li><code>failFast</code> (Boolean): Whether to fail immediately when any node fails</li>
 * <li><code>aggregationStrategy</code> (String): How to combine results ("list", "map", "custom")</li>
 * </ul>
 *
 * @param <I> The input type for all nodes
 * @param <O> The aggregated output type
 */
public class ParallelExecutionStrategy<I, O> implements WorkflowExecutionStrategy<I, O> {

    private static final String STRATEGY_NAME = "Parallel";
    
    // Configuration keys
    public static final String MAX_CONCURRENCY = "maxConcurrency";
    public static final String TIMEOUT_MS = "timeoutMs";
    public static final String FAIL_FAST = "failFast";
    public static final String AGGREGATION_STRATEGY = "aggregationStrategy";
    
    // Default values
    private static final int DEFAULT_MAX_CONCURRENCY = Runtime.getRuntime().availableProcessors();
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final boolean DEFAULT_FAIL_FAST = true;
    private static final String DEFAULT_AGGREGATION = "list";

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
        int maxConcurrency = (Integer) context.getOrDefault(MAX_CONCURRENCY, DEFAULT_MAX_CONCURRENCY);
        long timeoutMs = (Long) context.getOrDefault(TIMEOUT_MS, DEFAULT_TIMEOUT_MS);
        boolean failFast = (Boolean) context.getOrDefault(FAIL_FAST, DEFAULT_FAIL_FAST);
        String aggregationStrategy = (String) context.getOrDefault(AGGREGATION_STRATEGY, DEFAULT_AGGREGATION);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(maxConcurrency, nodes.size()));
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Create futures for all nodes
            List<CompletableFuture<NodeResult>> futures = new ArrayList<>();
            
            for (int i = 0; i < nodes.size(); i++) {
                final int nodeIndex = i;
                final AgentNode<Object, Object> node = (AgentNode<Object, Object>) nodes.get(i);
                
                CompletableFuture<NodeResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Object result = node.process(input, context);
                        return new NodeResult(nodeIndex, node.getName(), result, null);
                    } catch (Exception e) {
                        return new NodeResult(nodeIndex, node.getName(), null, e);
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all futures to complete or timeout
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture<?>[0])
            );
            
            try {
                allFutures.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Cancel any remaining futures
                futures.forEach(future -> future.cancel(true));
                throw new WorkflowExecutionException(
                    "ParallelStrategy", 
                    "Parallel execution timed out or was interrupted", 
                    e
                );
            }
            
            // Collect results
            List<NodeResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            // Check for errors
            List<NodeResult> errorResults = results.stream()
                .filter(result -> result.error != null)
                .collect(Collectors.toList());
            
            if (!errorResults.isEmpty() && failFast) {
                StringBuilder errorMessage = new StringBuilder("Parallel execution failed for nodes: ");
                errorResults.forEach(result -> 
                    errorMessage.append(result.nodeName).append(" (").append(result.error.getMessage()).append("), ")
                );
                
                throw new WorkflowExecutionException(
                    "ParallelStrategy", 
                    errorMessage.toString(),
                    errorResults.get(0).error
                );
            }
            
            // Store execution metadata
            long endTime = System.currentTimeMillis();
            context.put("strategy_used", STRATEGY_NAME);
            context.put("total_nodes", nodes.size());
            context.put("successful_nodes", results.size() - errorResults.size());
            context.put("failed_nodes", errorResults.size());
            context.put("execution_time_ms", endTime - startTime);
            context.put("execution_successful", errorResults.isEmpty());
            
            // Store individual results in context
            for (NodeResult result : results) {
                if (result.error == null) {
                    context.put("result_" + result.nodeName, result.result);
                } else {
                    context.put("error_" + result.nodeName, result.error.getMessage());
                }
            }
            
            // Aggregate results based on strategy
            return aggregateResults(results, aggregationStrategy, context);
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<O> executeAsync(List<AgentNode<?, ?>> nodes, I input, Map<String, Object> context) {
        // Parallel strategy is already async, so we can optimize this
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(nodes, input, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async parallel execution failed", e);
            }
        });
    }

    /**
     * Aggregate node results based on the specified strategy.
     */
    @SuppressWarnings("unchecked")
    private O aggregateResults(List<NodeResult> results, String aggregationStrategy, Map<String, Object> context) {
        switch (aggregationStrategy.toLowerCase()) {
            case "list":
                List<Object> resultList = results.stream()
                    .filter(result -> result.error == null)
                    .map(result -> result.result)
                    .collect(Collectors.toList());
                return (O) resultList;
                
            case "map":
                Map<String, Object> resultMap = new HashMap<>();
                results.stream()
                    .filter(result -> result.error == null)
                    .forEach(result -> resultMap.put(result.nodeName, result.result));
                return (O) resultMap;
                
            case "first":
                return (O) results.stream()
                    .filter(result -> result.error == null)
                    .map(result -> result.result)
                    .findFirst()
                    .orElse(null);
                    
            default:
                // For custom aggregation, just return the first successful result
                return (O) results.stream()
                    .filter(result -> result.error == null)
                    .map(result -> result.result)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getStrategyConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("strategyName", STRATEGY_NAME);
        config.put("executionType", "parallel");
        config.put("defaultMaxConcurrency", DEFAULT_MAX_CONCURRENCY);
        config.put("defaultTimeoutMs", DEFAULT_TIMEOUT_MS);
        config.put("supportsFailFast", true);
        config.put("supportsAggregation", true);
        config.put("memoryIntensive", true);
        config.put("deterministic", false);
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
        characteristics.put("expectedMemoryUsage", "high");
        
        int maxConcurrency = (Integer) context.getOrDefault(MAX_CONCURRENCY, DEFAULT_MAX_CONCURRENCY);
        characteristics.put("concurrency", Math.min(maxConcurrency, nodes.size()));
        characteristics.put("deterministic", false);
        characteristics.put("speedup", "high");
        
        return characteristics;
    }

    /**
     * Helper class to store node execution results.
     */
    private static class NodeResult {
        final int index;
        final String nodeName;
        final Object result;
        final Exception error;
        
        NodeResult(int index, String nodeName, Object result, Exception error) {
            this.index = index;
            this.nodeName = nodeName;
            this.result = result;
            this.error = error;
        }
    }

    /**
     * Create a new instance of ParallelExecutionStrategy.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new ParallelExecutionStrategy instance
     */
    public static <I, O> ParallelExecutionStrategy<I, O> create() {
        return new ParallelExecutionStrategy<>();
    }
}