package dev.agents4j.langchain4j.workflow.routing;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.result.error.ExecutionError;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.RoutingDecision;
import dev.agents4j.api.routing.RoutingStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A content router implementation that uses LangChain4J's ChatModel to classify content
 * and make routing decisions within a graph workflow.
 *
 * <p>This router analyzes content using LLM-based classification and determines
 * the most appropriate graph node to route the content to based on the analysis.
 * It supports confidence scoring, alternative routes, and reasoning.</p>
 *
 * @param <T> The type of content being routed
 */
public class GraphLLMContentRouter<T> implements ContentRouter<T> {

    private static final String ROUTER_NAME = "LLM-Based Router";
    
    // Response parsing patterns
    private static final Pattern ROUTE_PATTERN = Pattern.compile("\"route\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern REASONING_PATTERN = Pattern.compile("\"reasoning\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ALTERNATIVES_PATTERN = Pattern.compile("\"alternatives\"\\s*:\\s*\\[(.*?)\\]");
    private static final Pattern ALTERNATIVE_ITEM_PATTERN = Pattern.compile("\\{\\s*\"route\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"score\"\\s*:\\s*([0-9]*\\.?[0-9]+)\\s*\\}");

    private final NodeId nodeId;
    private final ChatModel model;
    private final String classificationPrompt;
    private final boolean includeConfidence;
    private final boolean includeAlternatives;
    private final boolean includeReasoning;
    private final int maxAlternatives;
    private final RoutingStrategy strategy;

    /**
     * Creates a new GraphLLMContentRouter with the specified configuration.
     *
     * @param nodeId The node ID for this router
     * @param model The ChatModel to use for content classification
     * @param classificationPrompt The prompt template for classification
     * @param includeConfidence Whether to include confidence scores
     * @param includeAlternatives Whether to include alternative routes
     * @param includeReasoning Whether to include reasoning for decisions
     * @param maxAlternatives Maximum number of alternatives to include
     * @param strategy The routing strategy to use
     */
    public GraphLLMContentRouter(
            NodeId nodeId,
            ChatModel model,
            String classificationPrompt,
            boolean includeConfidence,
            boolean includeAlternatives,
            boolean includeReasoning,
            int maxAlternatives,
            RoutingStrategy strategy) {
        this.nodeId = Objects.requireNonNull(nodeId, "Node ID cannot be null");
        this.model = Objects.requireNonNull(model, "ChatModel cannot be null");
        this.classificationPrompt = classificationPrompt != null ? 
                classificationPrompt : "Analyze the following input and classify it into one of the available categories.";
        this.includeConfidence = includeConfidence;
        this.includeAlternatives = includeAlternatives;
        this.includeReasoning = includeReasoning;
        this.maxAlternatives = Math.max(1, maxAlternatives);
        this.strategy = Objects.requireNonNull(strategy, "RoutingStrategy cannot be null");
    }

    @Override
    public String getRouterName() {
        return ROUTER_NAME;
    }

    @Override
    public NodeId getNodeId() {
        return nodeId;
    }

    @Override
    public String getName() {
        return ROUTER_NAME;
    }

    @Override
    public RoutingStrategy getRoutingStrategy() {
        return strategy;
    }

    @Override
    public RoutingDecision analyzeContent(T content, Set<NodeId> availableRoutes, WorkflowContext context) {
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(availableRoutes, "Available routes cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        if (availableRoutes.isEmpty()) {
            throw new IllegalArgumentException("Available routes cannot be empty");
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // Build the classification prompt
            String fullPrompt = buildClassificationPrompt(content, availableRoutes, context);
            
            // Query the LLM
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(classificationPrompt));
            messages.add(UserMessage.from(fullPrompt));
            
            AiMessage response = model.chat(messages).aiMessage();
            String responseText = response.text();
            
            // Parse the response
            RoutingDecision decision = parseResponse(responseText, availableRoutes, startTime);
            
            // Create a builder with the parsed results
            RoutingDecision.Builder builder = RoutingDecision.builder()
                    .selectedRoute(decision.getSelectedRoute())
                    .confidence(decision.getConfidence())
                    .processingTimeMs(System.currentTimeMillis() - startTime);
            
            if (decision.getReasoning() != null) {
                builder.reasoning(decision.getReasoning());
            }
            
            if (decision.getAlternatives() != null && !decision.getAlternatives().isEmpty()) {
                builder.alternatives(decision.getAlternatives());
            }
            
            // Add metadata
            builder.addMetadata("prompt", fullPrompt);
            builder.addMetadata("response", responseText);
            builder.addMetadata("modelProvider", model.getClass().getSimpleName());
            
            return builder.build();
            
        } catch (Exception e) {
            // In case of error, build a fallback decision with error information
            Map<String, Object> errorMetadata = new HashMap<>();
            errorMetadata.put("error", e.getMessage());
            errorMetadata.put("errorType", e.getClass().getName());
            errorMetadata.put("timestamp", Instant.now().toString());
            
            if (!availableRoutes.isEmpty()) {
                NodeId fallback = strategy.getFallbackNode().orElse(availableRoutes.iterator().next());
                return RoutingDecision.builder()
                        .selectedRoute(fallback)
                        .confidence(0.0)
                        .reasoning("Error during content analysis: " + e.getMessage())
                        .processingTimeMs(System.currentTimeMillis() - startTime)
                        .metadata(errorMetadata)
                        .build();
            } else {
                throw new IllegalStateException("No available routes and routing failed", e);
            }
        }
    }

    @Override
    public CompletableFuture<RoutingDecision> analyzeContentAsync(T content, Set<NodeId> availableRoutes, WorkflowContext context) {
        return CompletableFuture.supplyAsync(() -> analyzeContent(content, availableRoutes, context));
    }

    /**
     * Builds the classification prompt for the LLM based on the content and available routes.
     *
     * @param content The content to classify
     * @param availableRoutes The available route options
     * @param context The workflow context
     * @return The formatted prompt for the LLM
     */
    private String buildClassificationPrompt(T content, Set<NodeId> availableRoutes, WorkflowContext context) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Add content description
        promptBuilder.append("Content to analyze:\n\n");
        promptBuilder.append(content.toString());
        promptBuilder.append("\n\n");
        
        // Add available routes
        promptBuilder.append("Available routes:\n");
        for (NodeId route : availableRoutes) {
            promptBuilder.append("- ").append(route.value()).append("\n");
        }
        promptBuilder.append("\n");
        
        // Instructions for response format
        promptBuilder.append("Instructions:\n");
        promptBuilder.append("1. Analyze the content and determine the most appropriate route.\n");
        promptBuilder.append("2. Return your analysis in JSON format with the following structure:\n\n");
        
        promptBuilder.append("{\n");
        promptBuilder.append("  \"route\": \"[selected route name]\",\n");
        
        if (includeConfidence) {
            promptBuilder.append("  \"confidence\": [value between 0.0 and 1.0],\n");
        }
        
        if (includeReasoning) {
            promptBuilder.append("  \"reasoning\": \"[explanation for why this route was selected]\",\n");
        }
        
        if (includeAlternatives) {
            promptBuilder.append("  \"alternatives\": [\n");
            promptBuilder.append("    { \"route\": \"[alternative route name]\", \"score\": [value between 0.0 and 1.0] },\n");
            promptBuilder.append("    ...\n");
            promptBuilder.append("  ],\n");
        }
        
        promptBuilder.append("}\n\n");
        
        promptBuilder.append("Make sure the selected route is one of the available routes listed above.\n");
        
        return promptBuilder.toString();
    }

    /**
     * Parses the LLM response to extract routing information.
     *
     * @param responseText The LLM response text
     * @param availableRoutes The available routes for validation
     * @param startTime The processing start time
     * @return A RoutingDecision based on the LLM response
     */
    private RoutingDecision parseResponse(String responseText, Set<NodeId> availableRoutes, long startTime) {
        // Extract route from response
        Matcher routeMatcher = ROUTE_PATTERN.matcher(responseText);
        if (!routeMatcher.find()) {
            throw new IllegalArgumentException("Invalid response format: missing route");
        }
        
        String routeName = routeMatcher.group(1);
        NodeId selectedRoute = NodeId.of(routeName);
        
        // Validate the route
        boolean validRoute = availableRoutes.stream()
                .anyMatch(r -> r.value().equals(routeName));
        
        if (!validRoute) {
            throw new IllegalArgumentException("Selected route '" + routeName + 
                    "' is not in the available routes: " + 
                    availableRoutes.stream().map(NodeId::value).collect(Collectors.joining(", ")));
        }
        
        // Extract confidence if available
        double confidence = 1.0;
        Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(responseText);
        if (confidenceMatcher.find()) {
            try {
                confidence = Double.parseDouble(confidenceMatcher.group(1));
                confidence = Math.max(0.0, Math.min(1.0, confidence));
            } catch (NumberFormatException e) {
                // Use default confidence if parsing fails
            }
        }
        
        // Extract reasoning if available
        String reasoning = null;
        Matcher reasoningMatcher = REASONING_PATTERN.matcher(responseText);
        if (reasoningMatcher.find()) {
            reasoning = reasoningMatcher.group(1);
        }
        
        // Extract alternatives if available
        Map<NodeId, Double> alternatives = new HashMap<>();
        Matcher alternativesMatcher = ALTERNATIVES_PATTERN.matcher(responseText);
        if (alternativesMatcher.find()) {
            String alternativesText = alternativesMatcher.group(1);
            Matcher alternativeItemMatcher = ALTERNATIVE_ITEM_PATTERN.matcher(alternativesText);
                
            while (alternativeItemMatcher.find() && alternatives.size() < maxAlternatives) {
                String altRoute = alternativeItemMatcher.group(1);
                double altScore = Double.parseDouble(alternativeItemMatcher.group(2));
                    
                // Only include valid routes in alternatives
                if (availableRoutes.stream().anyMatch(r -> r.value().equals(altRoute))) {
                    alternatives.put(NodeId.of(altRoute), Math.max(0.0, Math.min(1.0, altScore)));
                }
            }
        }
        
        // Calculate processing time
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("responseLength", responseText.length());
        metadata.put("processingTimeMs", processingTime);
        
        // Build the routing decision
        RoutingDecision.Builder builder = RoutingDecision.builder()
                .selectedRoute(selectedRoute)
                .confidence(confidence)
                .processingTimeMs(processingTime)
                .metadata(metadata);
        
        if (reasoning != null) {
            builder.reasoning(reasoning);
        }
        
        if (!alternatives.isEmpty()) {
            // Convert the map to a list of alternative entries
            alternatives.forEach((nodeId, score) -> 
                builder.addAlternative(nodeId.value(), score));
        }
        
        return builder.build();
    }

    /**
     * Creates a new builder for GraphLLMContentRouter.
     *
     * @param <T> The type of content to route
     * @return A new builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    @Override
    public boolean isEntryPoint() {
        return false;
    }

    /**
     * Builder for creating GraphLLMContentRouter instances.
     *
     * @param <T> The type of content to route
     */
    public static class Builder<T> {
        private NodeId nodeId;
        private ChatModel model;
        private String classificationPrompt;
        private boolean includeConfidence = true;
        private boolean includeAlternatives = true;
        private boolean includeReasoning = true;
        private int maxAlternatives = 3;
        private RoutingStrategy strategy;

        /**
         * Sets the node ID for the router.
         *
         * @param nodeId The node ID
         * @return This builder instance
         */
        public Builder<T> nodeId(NodeId nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        /**
         * Sets the node ID for the router.
         *
         * @param nodeId The node ID string
         * @return This builder instance
         */
        public Builder<T> nodeId(String nodeId) {
            this.nodeId = NodeId.of(nodeId);
            return this;
        }

        /**
         * Sets the ChatModel to use.
         *
         * @param model The ChatModel
         * @return This builder instance
         */
        public Builder<T> model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the classification prompt.
         *
         * @param prompt The classification prompt
         * @return This builder instance
         */
        public Builder<T> classificationPrompt(String prompt) {
            this.classificationPrompt = prompt;
            return this;
        }

        /**
         * Sets whether to include confidence scoring.
         *
         * @param include Whether to include confidence scoring
         * @return This builder instance
         */
        public Builder<T> includeConfidenceScoring(boolean include) {
            this.includeConfidence = include;
            return this;
        }

        /**
         * Sets whether to include alternative routes.
         *
         * @param include Whether to include alternative routes
         * @return This builder instance
         */
        public Builder<T> includeAlternatives(boolean include) {
            this.includeAlternatives = include;
            return this;
        }

        /**
         * Sets whether to include reasoning.
         *
         * @param include Whether to include reasoning
         * @return This builder instance
         */
        public Builder<T> includeReasoning(boolean include) {
            this.includeReasoning = include;
            return this;
        }

        /**
         * Sets the maximum number of alternatives.
         *
         * @param max The maximum number of alternatives
         * @return This builder instance
         */
        public Builder<T> maxAlternatives(int max) {
            this.maxAlternatives = max;
            return this;
        }

        /**
         * Sets the routing strategy.
         *
         * @param strategy The routing strategy
         * @return This builder instance
         */
        public Builder<T> strategy(RoutingStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        /**
         * Builds a new GraphLLMContentRouter instance.
         *
         * @return A new GraphLLMContentRouter instance
         * @throws IllegalStateException if required fields are not set
         */
        public GraphLLMContentRouter<T> build() {
            if (nodeId == null) {
                nodeId = NodeId.of("llm-content-router-" + System.currentTimeMillis());
            }
            
            if (model == null) {
                throw new IllegalStateException("ChatModel must be set");
            }
            
            if (strategy == null) {
                Class<?> contentType = Object.class;
                strategy = RoutingStrategy.basic(
                        "LLM-based content routing",
                        contentType,
                        Collections.emptySet()
                );
            }
            
            return new GraphLLMContentRouter<>(
                    nodeId,
                    model,
                    classificationPrompt,
                    includeConfidence,
                    includeAlternatives,
                    includeReasoning,
                    maxAlternatives,
                    strategy
            );
        }
    }
}