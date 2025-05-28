package dev.agents4j.api.routing;

import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for content routing and classification logic.
 * 
 * <p>This interface defines the contract for routing systems that analyze
 * input content and determine the most appropriate route for processing.
 * Implementations can use various approaches including LLM-based classification,
 * rule-based routing, or hybrid strategies.</p>
 * 
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 * <li>Analyze input content to determine appropriate routing</li>
 * <li>Provide confidence scores and reasoning for routing decisions</li>
 * <li>Support multiple routing strategies and criteria</li>
 * <li>Enable caching and performance optimization</li>
 * </ul>
 * 
 * <p><b>Common Implementations:</b></p>
 * <ul>
 * <li><b>LLM-based</b>: Uses language models for intelligent classification</li>
 * <li><b>Rule-based</b>: Uses predefined patterns and rules</li>
 * <li><b>Hybrid</b>: Combines multiple classification approaches</li>
 * <li><b>Cached</b>: Adds caching layer for performance</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * ContentRouter<String> router = LLMContentRouter.builder()
 *     .model(chatModel)
 *     .classificationPrompt("Classify this support ticket...")
 *     .build();
 * 
 * Set<String> availableRoutes = Set.of("technical", "billing", "general");
 * RoutingDecision decision = router.route("My login is broken", availableRoutes, context);
 * }</pre>
 *
 * @param <I> The input type to be routed
 */
public interface ContentRouter<I> {

    /**
     * Routes the input content to the most appropriate route.
     * 
     * <p>This method analyzes the input content and selects the best route
     * from the available options. The routing decision includes confidence
     * metrics, reasoning, and alternative routes considered.</p>
     *
     * @param input The input content to route
     * @param availableRoutes Set of available route identifiers
     * @param context Additional context for routing decisions
     * @return A RoutingDecision containing the selected route and metadata
     * @throws WorkflowExecutionException if routing fails
     * @throws IllegalArgumentException if input is null or availableRoutes is empty
     */
    RoutingDecision route(I input, Set<String> availableRoutes, Map<String, Object> context) 
        throws WorkflowExecutionException;

    /**
     * Routes the input content asynchronously.
     * 
     * <p>Provides non-blocking routing for better performance in concurrent
     * scenarios. The default implementation wraps the synchronous route method
     * in a CompletableFuture, but implementations may override for more
     * efficient async processing.</p>
     *
     * @param input The input content to route
     * @param availableRoutes Set of available route identifiers
     * @param context Additional context for routing decisions
     * @return A CompletableFuture containing the RoutingDecision
     */
    default CompletableFuture<RoutingDecision> routeAsync(I input, Set<String> availableRoutes, 
                                                         Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return route(input, availableRoutes, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async routing failed", e);
            }
        });
    }

    /**
     * Get the name of this content router.
     * 
     * @return A descriptive name for this router implementation
     */
    String getRouterName();

    /**
     * Get configuration information for this router.
     * 
     * @return A map containing router configuration details
     */
    default Map<String, Object> getRouterConfiguration() {
        return Map.of("routerName", getRouterName());
    }

    /**
     * Validate whether this router can handle the given input and routes.
     * 
     * @param input The input to validate
     * @param availableRoutes The available routes to validate
     * @param context The routing context
     * @return true if this router can handle the input and routes, false otherwise
     */
    default boolean canRoute(I input, Set<String> availableRoutes, Map<String, Object> context) {
        return input != null && availableRoutes != null && !availableRoutes.isEmpty();
    }

    /**
     * Get performance characteristics for routing operations.
     * 
     * @param input The input to analyze
     * @param availableRoutes The available routes
     * @param context The routing context
     * @return A map containing performance characteristics
     */
    default Map<String, Object> getRoutingCharacteristics(I input, Set<String> availableRoutes, 
                                                          Map<String, Object> context) {
        Map<String, Object> characteristics = new java.util.HashMap<>();
        characteristics.put("routerName", getRouterName());
        characteristics.put("routeCount", availableRoutes != null ? availableRoutes.size() : 0);
        characteristics.put("inputType", input != null ? input.getClass().getSimpleName() : "null");
        return characteristics;
    }
}