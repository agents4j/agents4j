package dev.agents4j.workflow.routing;

import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;

/**
 * Factory for creating generalized routing workflows with common patterns and configurations.
 * 
 * <p>This factory provides convenient methods for constructing routing workflows
 * with flexible routing solutions using content routers and routes.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Generalized routing patterns for any use case</li>
 * <li>Fluent API for complex routing workflow construction</li>
 * <li>Support for both simple and complex routing scenarios</li>
 * <li>Flexible route and router configuration</li>
 * </ul>
 */
public class RoutingWorkflowFactory {

    /**
     * Creates a simple routing workflow with a single router and multiple routes.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @param routes The routes to add
     * @return A new RoutingWorkflow instance
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createSimpleRoutingWorkflow(
        String name,
        ContentRouter<I> router,
        Route<I, O>... routes
    ) {
        RoutingWorkflow.Builder<I, O> builder = RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router);

        for (Route<I, O> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    /**
     * Creates a routing workflow with confidence-based fallback behavior.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @param confidenceThreshold The minimum confidence threshold
     * @param fallbackRoute The fallback route for low confidence decisions
     * @param routes The primary routes to add
     * @return A new RoutingWorkflow instance with fallback configuration
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createRoutingWorkflowWithFallback(
        String name,
        ContentRouter<I> router,
        double confidenceThreshold,
        Route<I, O> fallbackRoute,
        Route<I, O>... routes
    ) {
        RoutingWorkflow.Builder<I, O> builder = RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router)
            .confidenceThreshold(confidenceThreshold)
            .fallbackRoute(fallbackRoute)
            .enableFallbackOnLowConfidence(true);

        for (Route<I, O> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    /**
     * Creates a routing workflow with analytics enabled.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @param enableAnalytics Whether to enable route analytics
     * @param routes The routes to add
     * @return A new RoutingWorkflow instance with analytics configuration
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createAnalyticsEnabledWorkflow(
        String name,
        ContentRouter<I> router,
        boolean enableAnalytics,
        Route<I, O>... routes
    ) {
        RoutingWorkflow.Builder<I, O> builder = RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router)
            .enableRouteAnalytics(enableAnalytics);

        for (Route<I, O> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    /**
     * Creates a basic routing workflow builder for custom configuration.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @return A new RoutingWorkflow.Builder instance for further configuration
     */
    public static <I, O> RoutingWorkflow.Builder<I, O> createBuilder(
        String name,
        ContentRouter<I> router
    ) {
        return RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router);
    }

    /**
     * Creates a routing workflow with full customization options.
     *
     * @param <I> The input type
     * @param <O> The output type
     * @param name The workflow name
     * @param router The content router to use
     * @param confidenceThreshold The minimum confidence threshold
     * @param fallbackRoute The fallback route (can be null)
     * @param enableFallbackOnLowConfidence Whether to enable fallback on low confidence
     * @param enableAnalytics Whether to enable route analytics
     * @param routes The routes to add
     * @return A new RoutingWorkflow instance with full configuration
     */
    @SafeVarargs
    public static <I, O> RoutingWorkflow<I, O> createCustomRoutingWorkflow(
        String name,
        ContentRouter<I> router,
        double confidenceThreshold,
        Route<I, O> fallbackRoute,
        boolean enableFallbackOnLowConfidence,
        boolean enableAnalytics,
        Route<I, O>... routes
    ) {
        RoutingWorkflow.Builder<I, O> builder = RoutingWorkflow.<I, O>builder()
            .name(name)
            .router(router)
            .confidenceThreshold(confidenceThreshold)
            .enableFallbackOnLowConfidence(enableFallbackOnLowConfidence)
            .enableRouteAnalytics(enableAnalytics);

        if (fallbackRoute != null) {
            builder.fallbackRoute(fallbackRoute);
        }

        for (Route<I, O> route : routes) {
            builder.addRoute(route);
        }

        return builder.build();
    }

    // Private constructor to prevent instantiation
    private RoutingWorkflowFactory() {
        throw new UnsupportedOperationException("RoutingWorkflowFactory is a utility class and should not be instantiated");
    }
}