package dev.agents4j.workflow.strategy;

import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Factory for creating and managing workflow execution strategies.
 * 
 * <p>This factory provides a centralized way to create, register, and retrieve
 * workflow execution strategies. It supports both built-in strategies and
 * custom user-defined strategies.</p>
 * 
 * <p><b>Built-in Strategies:</b></p>
 * <ul>
 * <li><b>sequential</b>: Sequential execution of nodes</li>
 * <li><b>parallel</b>: Parallel execution of all nodes</li>
 * <li><b>conditional</b>: Conditional execution based on runtime conditions</li>
 * <li><b>batch</b>: Batch processing of inputs</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Get a built-in strategy
 * WorkflowExecutionStrategy<String, String> strategy = 
 *     StrategyFactory.getStrategy("sequential");
 * 
 * // Register a custom strategy
 * StrategyFactory.registerStrategy("custom", MyCustomStrategy::new);
 * 
 * // Create strategy with context
 * Map<String, Object> config = new HashMap<>();
 * config.put("maxConcurrency", 4);
 * WorkflowExecutionStrategy<String, List<String>> parallelStrategy = 
 *     StrategyFactory.createStrategy("parallel", config);
 * }</pre>
 */
public class StrategyFactory {

    private static final Map<String, Supplier<? extends WorkflowExecutionStrategy<?, ?>>> strategies = 
        new ConcurrentHashMap<>();
    
    // Initialize built-in strategies
    static {
        registerBuiltInStrategies();
    }

    /**
     * Register built-in strategies.
     */
    private static void registerBuiltInStrategies() {
        strategies.put("sequential", SequentialExecutionStrategy::create);
        strategies.put("parallel", ParallelExecutionStrategy::create);
        strategies.put("conditional", ConditionalExecutionStrategy::create);
        strategies.put("batch", BatchExecutionStrategy::create);
    }

    /**
     * Get a strategy by name using default configuration.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @param strategyName The name of the strategy
     * @return A new instance of the strategy
     * @throws IllegalArgumentException if the strategy is not found
     */
    @SuppressWarnings("unchecked")
    public static <I, O> WorkflowExecutionStrategy<I, O> getStrategy(String strategyName) {
        Objects.requireNonNull(strategyName, "Strategy name cannot be null");
        
        Supplier<? extends WorkflowExecutionStrategy<?, ?>> supplier = strategies.get(strategyName.toLowerCase());
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown strategy: " + strategyName + 
                ". Available strategies: " + getAvailableStrategies());
        }
        
        return (WorkflowExecutionStrategy<I, O>) supplier.get();
    }

    /**
     * Create a strategy with the given configuration context.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @param strategyName The name of the strategy
     * @param config Configuration context for the strategy
     * @return A new instance of the strategy
     * @throws IllegalArgumentException if the strategy is not found
     */
    public static <I, O> WorkflowExecutionStrategy<I, O> createStrategy(String strategyName, 
                                                                       Map<String, Object> config) {
        WorkflowExecutionStrategy<I, O> strategy = getStrategy(strategyName);
        
        // For strategies that support configuration, we could extend this
        // For now, the configuration is passed during execution
        return strategy;
    }

    /**
     * Register a custom strategy.
     * 
     * @param strategyName The name to register the strategy under
     * @param strategySupplier A supplier that creates new instances of the strategy
     * @throws IllegalArgumentException if strategyName is null or supplier is null
     */
    public static void registerStrategy(String strategyName, 
                                      Supplier<? extends WorkflowExecutionStrategy<?, ?>> strategySupplier) {
        Objects.requireNonNull(strategyName, "Strategy name cannot be null");
        Objects.requireNonNull(strategySupplier, "Strategy supplier cannot be null");
        
        strategies.put(strategyName.toLowerCase(), strategySupplier);
    }

    /**
     * Unregister a strategy.
     * 
     * @param strategyName The name of the strategy to unregister
     * @return true if the strategy was removed, false if it didn't exist
     */
    public static boolean unregisterStrategy(String strategyName) {
        if (strategyName == null) {
            return false;
        }
        return strategies.remove(strategyName.toLowerCase()) != null;
    }

    /**
     * Check if a strategy is registered.
     * 
     * @param strategyName The name of the strategy to check
     * @return true if the strategy is registered, false otherwise
     */
    public static boolean isStrategyRegistered(String strategyName) {
        return strategyName != null && strategies.containsKey(strategyName.toLowerCase());
    }

    /**
     * Get the set of available strategy names.
     * 
     * @return A set containing all registered strategy names
     */
    public static Set<String> getAvailableStrategies() {
        return Set.copyOf(strategies.keySet());
    }

    /**
     * Get configuration information for a strategy.
     * 
     * @param strategyName The name of the strategy
     * @return A map containing strategy configuration, or empty map if strategy not found
     */
    public static Map<String, Object> getStrategyConfiguration(String strategyName) {
        try {
            WorkflowExecutionStrategy<?, ?> strategy = getStrategy(strategyName);
            return strategy.getStrategyConfiguration();
        } catch (IllegalArgumentException e) {
            return Map.of();
        }
    }

    /**
     * Get information about all available strategies.
     * 
     * @return A map where keys are strategy names and values are their configurations
     */
    public static Map<String, Map<String, Object>> getAllStrategyConfigurations() {
        Map<String, Map<String, Object>> allConfigs = new HashMap<>();
        
        for (String strategyName : strategies.keySet()) {
            try {
                allConfigs.put(strategyName, getStrategyConfiguration(strategyName));
            } catch (Exception e) {
                // Skip strategies that fail to instantiate
                allConfigs.put(strategyName, Map.of("error", "Failed to get configuration: " + e.getMessage()));
            }
        }
        
        return allConfigs;
    }

    /**
     * Create a sequential strategy with default configuration.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new SequentialExecutionStrategy
     */
    public static <I, O> SequentialExecutionStrategy<I, O> sequential() {
        return SequentialExecutionStrategy.create();
    }

    /**
     * Create a parallel strategy with default configuration.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new ParallelExecutionStrategy
     */
    public static <I, O> ParallelExecutionStrategy<I, O> parallel() {
        return ParallelExecutionStrategy.create();
    }

    /**
     * Create a conditional strategy with default configuration.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new ConditionalExecutionStrategy
     */
    public static <I, O> ConditionalExecutionStrategy<I, O> conditional() {
        return ConditionalExecutionStrategy.create();
    }

    /**
     * Create a batch strategy with default configuration.
     * 
     * @param <I> The input type
     * @param <O> The output type
     * @return A new BatchExecutionStrategy
     */
    public static <I, O> BatchExecutionStrategy<I, O> batch() {
        return BatchExecutionStrategy.create();
    }

    /**
     * Builder for creating strategies with fluent configuration.
     */
    public static class StrategyBuilder {
        private final String strategyName;
        private final Map<String, Object> config = new HashMap<>();

        private StrategyBuilder(String strategyName) {
            this.strategyName = strategyName;
        }

        /**
         * Add a configuration property.
         * 
         * @param key The configuration key
         * @param value The configuration value
         * @return This builder for method chaining
         */
        public StrategyBuilder config(String key, Object value) {
            config.put(key, value);
            return this;
        }

        /**
         * Add multiple configuration properties.
         * 
         * @param configMap The configuration map to add
         * @return This builder for method chaining
         */
        public StrategyBuilder config(Map<String, Object> configMap) {
            config.putAll(configMap);
            return this;
        }

        /**
         * Build the strategy with the configured settings.
         * 
         * @param <I> The input type
         * @param <O> The output type
         * @return A new strategy instance
         */
        public <I, O> WorkflowExecutionStrategy<I, O> build() {
            return createStrategy(strategyName, config);
        }
    }

    /**
     * Create a strategy builder for fluent configuration.
     * 
     * @param strategyName The name of the strategy to build
     * @return A new StrategyBuilder instance
     */
    public static StrategyBuilder builder(String strategyName) {
        return new StrategyBuilder(strategyName);
    }

    /**
     * Validate strategy compatibility with given node types.
     * 
     * @param strategyName The strategy to validate
     * @param nodeTypes The types of nodes that will be executed
     * @return true if the strategy can handle the nodes, false otherwise
     */
    public static boolean isStrategyCompatible(String strategyName, Class<?>... nodeTypes) {
        if (!isStrategyRegistered(strategyName)) {
            return false;
        }
        
        try {
            WorkflowExecutionStrategy<?, ?> strategy = getStrategy(strategyName);
            // For now, all strategies are compatible with all node types
            // This could be extended in the future for more specific validation
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get recommended strategy for given execution characteristics.
     * 
     * @param characteristics Execution characteristics map
     * @return Recommended strategy name
     */
    public static String getRecommendedStrategy(Map<String, Object> characteristics) {
        // Simple recommendation logic - can be made more sophisticated
        
        Integer nodeCount = (Integer) characteristics.get("nodeCount");
        Boolean independentNodes = (Boolean) characteristics.get("independentNodes");
        Boolean memoryConstrained = (Boolean) characteristics.get("memoryConstrained");
        Boolean requiresBranching = (Boolean) characteristics.get("requiresBranching");
        Integer dataSize = (Integer) characteristics.get("dataSize");
        
        if (requiresBranching != null && requiresBranching) {
            return "conditional";
        }
        
        if (dataSize != null && dataSize > 1000) {
            return "batch";
        }
        
        if (independentNodes != null && independentNodes && 
            (nodeCount == null || nodeCount > 1)) {
            return "parallel";
        }
        
        return "sequential"; // Default fallback
    }

    // Private constructor to prevent instantiation
    private StrategyFactory() {
        throw new UnsupportedOperationException("StrategyFactory is a utility class and should not be instantiated");
    }
}