package dev.agents4j.workflow.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Batch execution strategy that processes inputs in configurable batches.
 * 
 * <p>This strategy divides the input into smaller batches and processes each batch
 * through the workflow nodes. This is particularly useful for handling large datasets
 * efficiently while managing memory usage and providing incremental progress updates.</p>
 * 
 * <p><b>Characteristics:</b></p>
 * <ul>
 * <li>Processes data in configurable batch sizes</li>
 * <li>Memory efficient for large datasets</li>
 * <li>Provides progress tracking and intermediate results</li>
 * <li>Supports both sequential and parallel batch processing</li>
 * <li>Enables partial success handling</li>
 * </ul>
 * 
 * <p><b>Best for:</b></p>
 * <ul>
 * <li>Large dataset processing</li>
 * <li>Memory-constrained environments</li>
 * <li>Long-running operations with progress tracking</li>
 * <li>Scenarios requiring incremental result delivery</li>
 * <li>ETL (Extract, Transform, Load) workflows</li>
 * </ul>
 * 
 * <p><b>Configuration Options:</b></p>
 * <ul>
 * <li><code>batchSize</code> (Integer): Number of items per batch</li>
 * <li><code>parallelBatches</code> (Boolean): Whether to process batches in parallel</li>
 * <li><code>continueOnBatchError</code> (Boolean): Whether to continue if a batch fails</li>
 * <li><code>maxConcurrentBatches</code> (Integer): Maximum number of parallel batches</li>
 * <li><code>progressCallback</code> (Function): Callback for progress updates</li>
 * </ul>
 *
 * @param <I> The input type (expected to be a collection or list)
 * @param <O> The aggregated output type
 */
public class BatchExecutionStrategy<I, O> implements WorkflowExecutionStrategy<I, O> {

    private static final String STRATEGY_NAME = "Batch";
    
    // Configuration keys
    public static final String BATCH_SIZE = "batchSize";
    public static final String PARALLEL_BATCHES = "parallelBatches";
    public static final String CONTINUE_ON_BATCH_ERROR = "continueOnBatchError";
    public static final String MAX_CONCURRENT_BATCHES = "maxConcurrentBatches";
    public static final String PROGRESS_CALLBACK = "progressCallback";
    public static final String INPUT_SPLITTER = "inputSplitter";
    public static final String RESULT_AGGREGATOR = "resultAggregator";
    
    // Default values
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final boolean DEFAULT_PARALLEL_BATCHES = false;
    private static final boolean DEFAULT_CONTINUE_ON_ERROR = true;
    private static final int DEFAULT_MAX_CONCURRENT_BATCHES = 3;

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
        int batchSize = (Integer) context.getOrDefault(BATCH_SIZE, DEFAULT_BATCH_SIZE);
        boolean parallelBatches = (Boolean) context.getOrDefault(PARALLEL_BATCHES, DEFAULT_PARALLEL_BATCHES);
        boolean continueOnError = (Boolean) context.getOrDefault(CONTINUE_ON_BATCH_ERROR, DEFAULT_CONTINUE_ON_ERROR);
        int maxConcurrentBatches = (Integer) context.getOrDefault(MAX_CONCURRENT_BATCHES, DEFAULT_MAX_CONCURRENT_BATCHES);
        
        try {
            // Split input into batches
            List<Object> inputItems = splitInput(input, context);
            List<List<Object>> batches = createBatches(inputItems, batchSize);
            
            if (batches.isEmpty()) {
                throw new WorkflowExecutionException("BatchStrategy", "No batches created from input");
            }
            
            // Store batch metadata
            context.put("total_batches", batches.size());
            context.put("total_items", inputItems.size());
            context.put("batch_size", batchSize);
            
            List<Object> allResults = new ArrayList<>();
            List<String> failedBatches = new ArrayList<>();
            int processedBatches = 0;
            
            if (parallelBatches) {
                // Process batches in parallel
                allResults.addAll(processeBatchesParallel(nodes, batches, context, continueOnError, 
                    maxConcurrentBatches, failedBatches));
            } else {
                // Process batches sequentially
                for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                    List<Object> batch = batches.get(batchIndex);
                    
                    try {
                        Object batchResult = processSingleBatch(nodes, batch, context, batchIndex);
                        allResults.add(batchResult);
                        processedBatches++;
                        
                        // Update progress
                        updateProgress(context, processedBatches, batches.size());
                        
                    } catch (Exception e) {
                        String batchId = "batch_" + batchIndex;
                        failedBatches.add(batchId);
                        context.put("error_" + batchId, e.getMessage());
                        
                        if (!continueOnError) {
                            throw new WorkflowExecutionException(
                                "BatchStrategy", 
                                "Batch " + batchIndex + " failed and continueOnError is false", 
                                e
                            );
                        }
                    }
                }
            }
            
            // Store execution metadata
            context.put("strategy_used", STRATEGY_NAME);
            context.put("processed_batches", processedBatches);
            context.put("failed_batches", failedBatches);
            context.put("success_rate", (double) processedBatches / batches.size());
            context.put("execution_successful", failedBatches.isEmpty());
            
            // Aggregate final results
            O finalResult = aggregateResults(allResults, context);
            context.put("final_result", finalResult);
            
            return finalResult;
            
        } catch (WorkflowExecutionException e) {
            context.put("execution_successful", false);
            throw e;
        } catch (Exception e) {
            context.put("execution_successful", false);
            throw new WorkflowExecutionException(
                "BatchStrategy", 
                "Batch execution failed", 
                e
            );
        }
    }

    /**
     * Split the input into individual items for batching.
     */
    @SuppressWarnings("unchecked")
    private List<Object> splitInput(I input, Map<String, Object> context) {
        // Check for custom input splitter
        java.util.function.Function<I, List<Object>> customSplitter = 
            (java.util.function.Function<I, List<Object>>) context.get(INPUT_SPLITTER);
        
        if (customSplitter != null) {
            return customSplitter.apply(input);
        }
        
        // Default splitting logic
        if (input instanceof List) {
            return new ArrayList<>((List<Object>) input);
        } else if (input instanceof Object[]) {
            return List.of((Object[]) input);
        } else if (input instanceof String) {
            // Split string by lines or custom delimiter
            String delimiter = (String) context.getOrDefault("inputDelimiter", "\n");
            return List.of(((String) input).split(delimiter));
        } else {
            // Treat as single item
            return List.of(input);
        }
    }

    /**
     * Create batches from the input items.
     */
    private List<List<Object>> createBatches(List<Object> items, int batchSize) {
        List<List<Object>> batches = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, endIndex)));
        }
        
        return batches;
    }

    /**
     * Process a single batch through all nodes.
     */
    @SuppressWarnings("unchecked")
    private Object processSingleBatch(List<AgentNode<?, ?>> nodes, List<Object> batch, 
                                    Map<String, Object> context, int batchIndex) throws Exception {
        
        Object currentInput = batch;
        
        // Create batch-specific context
        Map<String, Object> batchContext = new HashMap<>(context);
        batchContext.put("current_batch_index", batchIndex);
        batchContext.put("current_batch_size", batch.size());
        
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            AgentNode<Object, Object> node = (AgentNode<Object, Object>) nodes.get(nodeIndex);
            currentInput = node.process(currentInput, batchContext);
            
            // Store intermediate result for this batch
            batchContext.put("batch_" + batchIndex + "_step_" + nodeIndex + "_result", currentInput);
        }
        
        return currentInput;
    }

    /**
     * Process batches in parallel.
     */
    private List<Object> processeBatchesParallel(List<AgentNode<?, ?>> nodes, List<List<Object>> batches,
                                               Map<String, Object> context, boolean continueOnError,
                                               int maxConcurrentBatches, List<String> failedBatches) {
        
        java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newFixedThreadPool(
                Math.min(maxConcurrentBatches, batches.size())
            );
        
        try {
            List<java.util.concurrent.CompletableFuture<Object>> futures = new ArrayList<>();
            
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                final int finalBatchIndex = batchIndex;
                final List<Object> batch = batches.get(batchIndex);
                
                java.util.concurrent.CompletableFuture<Object> future = 
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            return processSingleBatch(nodes, batch, context, finalBatchIndex);
                        } catch (Exception e) {
                            if (continueOnError) {
                                failedBatches.add("batch_" + finalBatchIndex);
                                return null;
                            } else {
                                throw new RuntimeException("Batch " + finalBatchIndex + " failed", e);
                            }
                        }
                    }, executor);
                
                futures.add(future);
            }
            
            // Wait for all futures and collect results
            return futures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Update progress if a callback is configured.
     */
    @SuppressWarnings("unchecked")
    private void updateProgress(Map<String, Object> context, int processedBatches, int totalBatches) {
        java.util.function.BiConsumer<Integer, Integer> progressCallback = 
            (java.util.function.BiConsumer<Integer, Integer>) context.get(PROGRESS_CALLBACK);
        
        if (progressCallback != null) {
            progressCallback.accept(processedBatches, totalBatches);
        }
        
        // Store progress in context
        context.put("progress_processed", processedBatches);
        context.put("progress_total", totalBatches);
        context.put("progress_percentage", (double) processedBatches / totalBatches * 100);
    }

    /**
     * Aggregate all batch results into final output.
     */
    @SuppressWarnings("unchecked")
    private O aggregateResults(List<Object> batchResults, Map<String, Object> context) {
        // Check for custom result aggregator
        java.util.function.Function<List<Object>, O> customAggregator = 
            (java.util.function.Function<List<Object>, O>) context.get(RESULT_AGGREGATOR);
        
        if (customAggregator != null) {
            return customAggregator.apply(batchResults);
        }
        
        // Default aggregation: return as list
        return (O) batchResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getStrategyConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("strategyName", STRATEGY_NAME);
        config.put("executionType", "batch");
        config.put("defaultBatchSize", DEFAULT_BATCH_SIZE);
        config.put("supportsParallelBatches", true);
        config.put("supportsProgressTracking", true);
        config.put("memoryEfficient", true);
        config.put("supportsPartialFailure", true);
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
        characteristics.put("expectedMemoryUsage", "medium");
        
        int batchSize = (Integer) context.getOrDefault(BATCH_SIZE, DEFAULT_BATCH_SIZE);
        boolean parallelBatches = (Boolean) context.getOrDefault(PARALLEL_BATCHES, DEFAULT_PARALLEL_BATCHES);
        
        characteristics.put("batchSize", batchSize);
        characteristics.put("parallelProcessing", parallelBatches);
        characteristics.put("memoryFootprint", "O(batchSize)");
        
        if (parallelBatches) {
            int maxConcurrent = (Integer) context.getOrDefault(MAX_CONCURRENT_BATCHES, DEFAULT_MAX_CONCURRENT_BATCHES);
            characteristics.put("concurrency", maxConcurrent);
        } else {
            characteristics.put("concurrency", 1);
        }
        
        return characteristics;
    }

    /**
     * Create a new instance of BatchExecutionStrategy.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new BatchExecutionStrategy instance
     */
    public static <I, O> BatchExecutionStrategy<I, O> create() {
        return new BatchExecutionStrategy<>();
    }
}