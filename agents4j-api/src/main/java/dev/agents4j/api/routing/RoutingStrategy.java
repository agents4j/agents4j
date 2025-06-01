package dev.agents4j.api.routing;

import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.context.ContextKey;

import java.util.Set;
import java.util.Optional;
import java.util.Map;

/**
 * Defines the strategy and configuration for content routing operations.
 * 
 * <p>RoutingStrategy encapsulates the configuration and behavior patterns
 * for how a ContentRouter should analyze content and make routing decisions.
 * It includes confidence thresholds, fallback behavior, context key mappings,
 * and content type support.</p>
 * 
 * <p>Key responsibilities:</p>
 * <ul>
 * <li>Define supported content types and routing criteria</li>
 * <li>Specify confidence thresholds and fallback strategies</li>
 * <li>Map routing data to workflow context keys</li>
 * <li>Provide default routes and error handling behavior</li>
 * </ul>
 * 
 * <p>Example implementation:</p>
 * <pre>{@code
 * public class CustomerSupportRoutingStrategy implements RoutingStrategy {
 *     
 *     @Override
 *     public String getDescription() {
 *         return "Routes customer messages based on content analysis and urgency";
 *     }
 *     
 *     @Override
 *     public boolean supportsContentType(Class<?> contentType) {
 *         return CustomerMessage.class.isAssignableFrom(contentType);
 *     }
 *     
 *     @Override
 *     public Set<NodeId> getDefaultRoutes() {
 *         return Set.of(
 *             NodeId.of("technical-support"),
 *             NodeId.of("billing-support"),
 *             NodeId.of("general-inquiry")
 *         );
 *     }
 * }
 * }</pre>
 */
public interface RoutingStrategy {
    
    /**
     * Gets a human-readable description of this routing strategy.
     * 
     * @return Description of the routing strategy and its behavior
     */
    String getDescription();
    
    /**
     * Checks if this strategy supports routing for the given content type.
     * 
     * @param contentType The content type to check
     * @return true if this strategy can route the content type
     */
    boolean supportsContentType(Class<?> contentType);
    
    /**
     * Gets the default set of routes this strategy can route to.
     * 
     * <p>This set represents the standard routes that this strategy
     * is configured to handle. It may be used as a fallback when
     * dynamic route discovery fails.</p>
     * 
     * @return Set of default route node IDs
     */
    Set<NodeId> getDefaultRoutes();
    
    /**
     * Gets the fallback node to use when routing fails.
     * 
     * <p>When content cannot be classified or confidence is too low,
     * the router can route to this fallback node instead of suspending
     * the workflow.</p>
     * 
     * @return Optional fallback node ID, empty if no fallback is configured
     */
    default Optional<NodeId> getFallbackNode() {
        return Optional.empty();
    }
    
    /**
     * Gets the minimum confidence threshold for routing decisions.
     * 
     * <p>Routing decisions with confidence below this threshold will
     * trigger fallback behavior or workflow suspension.</p>
     * 
     * @return Minimum confidence threshold (0.0 to 1.0)
     */
    default double getMinimumConfidence() {
        return 0.6;
    }
    
    /**
     * Gets the context key for storing routing confidence.
     * 
     * @return Context key for routing confidence values
     */
    default ContextKey<Double> getConfidenceContextKey() {
        return ContextKey.of("routing.confidence", Double.class);
    }
    
    /**
     * Gets the context key for storing routing reasoning.
     * 
     * @return Context key for routing reasoning text
     */
    default ContextKey<String> getReasoningContextKey() {
        return ContextKey.stringKey("routing.reasoning");
    }
    
    /**
     * Gets the context key for storing available routes.
     * 
     * @return Context key for available routes set
     */
    default ContextKey<Object> getRoutesContextKey() {
        return ContextKey.of("routing.available_routes", Object.class);
    }
    
    /**
     * Gets the context key for storing the selected route.
     * 
     * @return Context key for selected route string
     */
    default ContextKey<String> getSelectedRouteContextKey() {
        return ContextKey.stringKey("routing.selected_route");
    }
    
    /**
     * Gets the context key for storing routing processing time.
     * 
     * @return Context key for processing time in milliseconds
     */
    default ContextKey<Long> getProcessingTimeContextKey() {
        return ContextKey.of("routing.processing_time_ms", Long.class);
    }
    
    /**
     * Creates a context key for storing routing metadata.
     * 
     * @param metadataKey The metadata key name
     * @return Context key for the specific metadata value
     */
    default ContextKey<Object> getMetadataContextKey(String metadataKey) {
        return ContextKey.of("routing.metadata." + metadataKey, Object.class);
    }
    
    /**
     * Gets routing criteria configuration for this strategy.
     * 
     * <p>Returns a map of criteria names to their configuration.
     * This can include weights, thresholds, and other parameters
     * used during content analysis.</p>
     * 
     * @return Map of routing criteria configuration
     */
    default Map<String, Object> getRoutingCriteria() {
        return Map.of(
            "confidence_threshold", getMinimumConfidence(),
            "fallback_enabled", getFallbackNode().isPresent(),
            "default_route_count", getDefaultRoutes().size()
        );
    }
    
    /**
     * Determines if this strategy should use asynchronous processing.
     * 
     * <p>Strategies that perform complex analysis (like ML inference)
     * should return true to enable asynchronous processing and avoid
     * blocking the workflow thread.</p>
     * 
     * @return true if asynchronous processing is preferred
     */
    default boolean preferAsyncProcessing() {
        return false;
    }
    
    /**
     * Gets the maximum processing time allowed for routing decisions.
     * 
     * <p>If routing takes longer than this time, it may be considered
     * failed and fallback strategies should be used.</p>
     * 
     * @return Maximum processing time in milliseconds
     */
    default long getMaxProcessingTimeMs() {
        return 5000; // 5 seconds default
    }
    
    /**
     * Validates routing decision quality and consistency.
     * 
     * <p>This method can be used to validate that a routing decision
     * meets the strategy's quality standards before it's applied.</p>
     * 
     * @param decision The routing decision to validate
     * @return true if the decision meets quality standards
     */
    default boolean validateRoutingDecision(RoutingDecision decision) {
        return decision != null &&
               decision.getConfidence() >= getMinimumConfidence() &&
               (getDefaultRoutes().isEmpty() || 
                getDefaultRoutes().contains(decision.getSelectedRoute()));
    }
    
    /**
     * Creates a routing strategy with basic configuration.
     * 
     * @param description Strategy description
     * @param supportedType Supported content type
     * @param defaultRoutes Set of default routes
     * @return A basic routing strategy implementation
     */
    static RoutingStrategy basic(String description, Class<?> supportedType, Set<NodeId> defaultRoutes) {
        return new RoutingStrategy() {
            @Override
            public String getDescription() {
                return description;
            }
            
            @Override
            public boolean supportsContentType(Class<?> contentType) {
                return supportedType.isAssignableFrom(contentType);
            }
            
            @Override
            public Set<NodeId> getDefaultRoutes() {
                return defaultRoutes;
            }
        };
    }
    
    /**
     * Creates a routing strategy with fallback configuration.
     * 
     * @param description Strategy description
     * @param supportedType Supported content type
     * @param defaultRoutes Set of default routes
     * @param fallbackNode Fallback node for routing failures
     * @param minConfidence Minimum confidence threshold
     * @return A routing strategy with fallback support
     */
    static RoutingStrategy withFallback(String description, Class<?> supportedType, 
                                      Set<NodeId> defaultRoutes, NodeId fallbackNode, 
                                      double minConfidence) {
        return new RoutingStrategy() {
            @Override
            public String getDescription() {
                return description;
            }
            
            @Override
            public boolean supportsContentType(Class<?> contentType) {
                return supportedType.isAssignableFrom(contentType);
            }
            
            @Override
            public Set<NodeId> getDefaultRoutes() {
                return defaultRoutes;
            }
            
            @Override
            public Optional<NodeId> getFallbackNode() {
                return Optional.of(fallbackNode);
            }
            
            @Override
            public double getMinimumConfidence() {
                return minConfidence;
            }
        };
    }
}