package dev.agents4j.langchain4j.workflow.factory;

import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.agents4j.langchain4j.workflow.routing.GraphLLMContentRouter;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Factory for creating content routing components in graph-based workflows.
 * This factory provides convenient methods for constructing routing components 
 * compatible with the GraphWorkflow API.
 */
public class ContentRouterFactory {

    /**
     * Creates a builder for constructing GraphLLMContentRouter instances.
     *
     * @param <T> The type of content to be routed
     * @param nodeId The node ID for the router
     * @param model The ChatModel to use for content classification
     * @return A new ContentRouterBuilder instance
     */
    public static <T> ContentRouterBuilder<T> contentRouter(String nodeId, ChatModel model) {
        return new ContentRouterBuilder<T>(nodeId, model);
    }

    /**
     * Builder for creating GraphLLMContentRouter instances with fluent API.
     *
     * @param <T> The type of content to be routed
     */
    public static class ContentRouterBuilder<T> {
        private final String nodeId;
        private final ChatModel model;
        private Set<NodeId> routes;
        private RoutingStrategy strategy;

        private ContentRouterBuilder(String nodeId, ChatModel model) {
            this.nodeId = nodeId;
            this.model = model;
        }

        /**
         * Sets the available routes as NodeId objects.
         *
         * @param routes The available routes
         * @return This builder
         */
        public ContentRouterBuilder<T> routes(Set<NodeId> routes) {
            this.routes = routes;
            return this;
        }

        /**
         * Sets the available routes as string names (will be converted to NodeId objects).
         *
         * @param routeNames The available routes as strings
         * @return This builder
         */
        public ContentRouterBuilder<T> routes(String... routeNames) {
            this.routes = Arrays.stream(routeNames)
                .map(NodeId::of)
                .collect(Collectors.toSet());
            return this;
        }

        /**
         * Sets a custom routing strategy.
         *
         * @param strategy The routing strategy to use
         * @return This builder
         */
        public ContentRouterBuilder<T> strategy(RoutingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Builds the GraphLLMContentRouter with the configured properties.
         *
         * @return A new GraphLLMContentRouter instance
         * @throws IllegalStateException if required properties are not set
         */
        public GraphLLMContentRouter<T> build() {
            if (routes == null || routes.isEmpty()) {
                throw new IllegalStateException("Routes must be specified");
            }

            RoutingStrategy effectiveStrategy = strategy != null ? strategy :
                RoutingStrategy.basic("LLM content classification", Object.class, routes);

            return GraphLLMContentRouter.<T>builder()
                .nodeId(nodeId)
                .model(model)
                .strategy(effectiveStrategy)
                .build();
        }
    }
}