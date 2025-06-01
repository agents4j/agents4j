package dev.agents4j.api.routing;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * A specialized graph workflow node that performs content routing decisions.
 *
 * <p>ContentRouter extends GraphWorkflowNode to integrate routing capabilities
 * directly into the graph workflow architecture. It analyzes input data and
 * determines which graph node to traverse to next based on content classification.</p>
 *
 * <p>Key capabilities:</p>
 * <ul>
 * <li>Content analysis and classification</li>
 * <li>Dynamic route selection based on content</li>
 * <li>Confidence-based routing with fallback strategies</li>
 * <li>Integration with graph edge conditions</li>
 * <li>Asynchronous routing support</li>
 * <li>Multi-criteria routing decisions</li>
 * </ul>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * public class CustomerSupportRouter implements ContentRouter<CustomerMessage> {
 *
 *     @Override
 *     public WorkflowResult<GraphCommand<CustomerMessage>, WorkflowError> process(
 *             ModernGraphWorkflowState<CustomerMessage> state) {
 *
 *         var message = state.data();
 *         var routingDecision = analyzeContent(message);
 *         var targetNode = NodeId.of(routingDecision.getSelectedRoute());
 *
 *         return WorkflowResult.success(
 *             GraphCommand.Traverse.toWithContext(targetNode,
 *                 state.context().with(ROUTING_CONFIDENCE, routingDecision.getConfidence()))
 *         );
 *     }
 * }
 * }</pre>
 */
public interface ContentRouter<T> extends GraphWorkflowNode<T> {
    /**
     * Analyzes content and makes a routing decision.
     *
     * <p>This method performs content analysis to determine the most appropriate
     * route. It returns a comprehensive routing decision including confidence
     * metrics, reasoning, and alternative routes.</p>
     *
     * @param content The content to analyze and route
     * @param availableRoutes Set of available route node IDs
     * @param context Current workflow context for routing decisions
     * @return A routing decision with selected route and metadata
     * @throws IllegalArgumentException if content is null or availableRoutes is empty
     */
    RoutingDecision analyzeContent(
        T content,
        Set<NodeId> availableRoutes,
        WorkflowContext context
    );

    /**
     * Analyzes content asynchronously for better performance.
     *
     * <p>Default implementation wraps the synchronous analyzeContent method
     * in a CompletableFuture. Implementations should override for true
     * asynchronous processing when possible.</p>
     *
     * @param content The content to analyze and route
     * @param availableRoutes Set of available route node IDs
     * @param context Current workflow context for routing decisions
     * @return A CompletableFuture containing the routing decision
     */
    default CompletableFuture<RoutingDecision> analyzeContentAsync(
        T content,
        Set<NodeId> availableRoutes,
        WorkflowContext context
    ) {
        return CompletableFuture.supplyAsync(() ->
            analyzeContent(content, availableRoutes, context)
        );
    }

    /**
     * Gets the routing strategy configuration for this router.
     *
     * @return Configuration describing the routing strategy and parameters
     */
    RoutingStrategy getRoutingStrategy();

    /**
     * Gets the minimum confidence threshold for routing decisions.
     *
     * <p>If a routing decision doesn't meet this threshold, the router
     * should either use a fallback strategy or return a suspension command
     * for manual review.</p>
     *
     * @return The minimum confidence threshold (0.0 to 1.0)
     */
    default double getMinimumConfidenceThreshold() {
        return 0.5;
    }

    /**
     * Creates edge conditions for dynamic routing based on content analysis.
     *
     * <p>This method allows the router to define conditional edges that
     * depend on the content being routed. The conditions can examine
     * workflow context set by the routing decision.</p>
     *
     * @param targetNode The target node for the edge condition
     * @param routingDecision The routing decision that led to this target
     * @return An edge condition for traversing to the target node
     */
    default EdgeCondition createRoutingCondition(
        NodeId targetNode,
        RoutingDecision routingDecision
    ) {
        return EdgeCondition.whenContextGreaterThan(
            getRoutingStrategy().getConfidenceContextKey(),
            getMinimumConfidenceThreshold()
        );
    }

    /**
     * Handles routing failures and implements fallback strategies.
     *
     * <p>Called when routing confidence is below threshold or when
     * content cannot be classified. Implementations can define
     * fallback behavior such as routing to a default node or
     * suspending for manual review.</p>
     *
     * @param content The content that failed to route
     * @param availableRoutes The available routes that were considered
     * @param context The current workflow context
     * @param reason The reason for routing failure
     * @return A fallback routing decision or suspension command
     */
    default GraphCommand<T> handleRoutingFailure(
        T content,
        Set<NodeId> availableRoutes,
        WorkflowContext context,
        String reason
    ) {
        var fallbackNode = getRoutingStrategy().getFallbackNode();
        if (fallbackNode.isPresent()) {
            return GraphCommand.Traverse.toWithReason(
                fallbackNode.get(),
                "Routing failed: " + reason + ". Using fallback route."
            );
        }

        return GraphCommand.Suspend.withId(
            "routing-failure",
            "Content routing failed: " + reason + ". Manual review required."
        );
    }

    /**
     * Validates whether this router can handle the given content and routes.
     *
     * @param content The content to validate
     * @param availableRoutes The available routes to validate against
     * @param context The current workflow context
     * @return true if this router can handle the content and routes
     */
    default boolean canRoute(
        T content,
        Set<NodeId> availableRoutes,
        WorkflowContext context
    ) {
        return (
            content != null &&
            availableRoutes != null &&
            !availableRoutes.isEmpty() &&
            getRoutingStrategy().supportsContentType(content.getClass())
        );
    }

    /**
     * Gets performance characteristics for routing operations.
     *
     * @param content The content to analyze for performance estimation
     * @param availableRoutes The available routes
     * @param context The routing context
     * @return Performance characteristics including estimated processing time
     */
    default RoutingPerformance getRoutingPerformance(
        T content,
        Set<NodeId> availableRoutes,
        WorkflowContext context
    ) {
        return RoutingPerformance.builder()
            .routerName(getRouterName())
            .contentType(
                content != null ? content.getClass().getSimpleName() : "null"
            )
            .routeCount(availableRoutes != null ? availableRoutes.size() : 0)
            .estimatedProcessingTimeMs(
                estimateProcessingTime(content, availableRoutes)
            )
            .build();
    }

    /**
     * Estimates the processing time for routing the given content.
     *
     * @param content The content to analyze
     * @param availableRoutes The available routes
     * @return Estimated processing time in milliseconds
     */
    default long estimateProcessingTime(
        T content,
        Set<NodeId> availableRoutes
    ) {
        // Default implementation provides a simple estimate
        int baseTime = 50; // Base processing time in ms
        int routeComplexity = availableRoutes != null
            ? availableRoutes.size() * 10
            : 0;
        return baseTime + routeComplexity;
    }

    /**
     * Default implementation of GraphWorkflowNode.process that integrates routing logic.
     *
     * <p>This method:</p>
     * <ol>
     * <li>Extracts available routes from the workflow state or context</li>
     * <li>Performs content analysis using analyzeContent</li>
     * <li>Validates routing confidence against threshold</li>
     * <li>Returns appropriate graph command based on routing decision</li>
     * </ol>
     */
    @Override
    default WorkflowResult<GraphCommand<T>, WorkflowError> process(
        GraphWorkflowState<T> state
    ) {
        try {
            var content = state.data();
            var context = state.context();
            var availableRoutes = extractAvailableRoutes(state);

            // Validate routing capability
            if (!canRoute(content, availableRoutes, context)) {
                return WorkflowResult.success(
                    handleRoutingFailure(
                        content,
                        availableRoutes,
                        context,
                        "Router cannot handle this content or route configuration"
                    )
                );
            }

            // Perform content analysis
            var routingDecision = analyzeContent(
                content,
                availableRoutes,
                context
            );

            // Check confidence threshold
            if (
                !routingDecision.meetsThreshold(getMinimumConfidenceThreshold())
            ) {
                return WorkflowResult.success(
                    handleRoutingFailure(
                        content,
                        availableRoutes,
                        context,
                        String.format(
                            "Confidence %.2f below threshold %.2f",
                            routingDecision.getConfidence(),
                            getMinimumConfidenceThreshold()
                        )
                    )
                );
            }

            // Create routing command with updated context
            var targetNode = routingDecision.getSelectedRoute();
            var updatedContext = context
                .with(
                    getRoutingStrategy().getConfidenceContextKey(),
                    routingDecision.getConfidence()
                )
                .with(
                    getRoutingStrategy().getReasoningContextKey(),
                    routingDecision.getReasoning()
                );

            // Add routing metadata to context
            for (Map.Entry<String, Object> entry : routingDecision
                .getMetadata()
                .entrySet()) {
                updatedContext = updatedContext.with(
                    getRoutingStrategy().getMetadataContextKey(entry.getKey()),
                    entry.getValue()
                );
            }

            var traverseCommand = GraphCommand.Traverse.toWithUpdates(
                targetNode,
                updatedContext,
                content
            );
            return WorkflowResult.success(traverseCommand);
        } catch (Exception e) {
            return WorkflowResult.success(
                handleRoutingFailure(
                    state.data(),
                    extractAvailableRoutes(state),
                    state.context(),
                    "Routing failed with exception: " + e.getMessage()
                )
            );
        }
    }

    /**
     * Extracts available routes from the workflow state.
     *
     * <p>Default implementation looks for routes in the workflow context
     * under standard keys. Implementations can override to provide
     * custom route extraction logic.</p>
     *
     * @param state The current workflow state
     * @return Set of available route node IDs
     */
    default Set<NodeId> extractAvailableRoutes(GraphWorkflowState<T> state) {
        var routesContext = state.getContext(
            getRoutingStrategy().getRoutesContextKey()
        );
        if (
            routesContext.isPresent() && routesContext.get() instanceof Set<?>
        ) {
            @SuppressWarnings("unchecked")
            Set<String> routeStrings = (Set<String>) routesContext.get();
            return routeStrings
                .stream()
                .map(NodeId::of)
                .collect(java.util.stream.Collectors.toSet());
        }

        // Fallback: extract from node metadata or configuration
        return getRoutingStrategy().getDefaultRoutes();
    }

    /**
     * Gets the router name for identification and debugging.
     *
     * @return A descriptive name for this router implementation
     */
    String getRouterName();

    /**
     * Gets the description for this router node.
     *
     * @return A description explaining the router's purpose and behavior
     */
    @Override
    default String getDescription() {
        return String.format(
            "Content router: %s - Routes content based on %s",
            getRouterName(),
            getRoutingStrategy().getDescription()
        );
    }
}
