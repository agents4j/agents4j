package dev.agents4j.workflow.routing;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.RouteCandidate;
import dev.agents4j.api.routing.RoutingDecision;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM-based content router that uses language models for intelligent classification.
 * 
 * <p>This router leverages the reasoning capabilities of large language models
 * to analyze input content and make intelligent routing decisions. It supports
 * custom classification prompts, confidence scoring, and detailed reasoning
 * for routing decisions.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Intelligent content analysis using LLMs</li>
 * <li>Customizable classification prompts</li>
 * <li>Confidence scoring and reasoning</li>
 * <li>Support for multiple route alternatives</li>
 * <li>Structured response parsing</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * LLMContentRouter<String> router = LLMContentRouter.<String>builder()
 *     .model(chatModel)
 *     .classificationPrompt("Analyze this support ticket and classify it...")
 *     .includeConfidenceScoring(true)
 *     .includeAlternatives(true)
 *     .build();
 * 
 * RoutingDecision decision = router.route(inputText, availableRoutes, context);
 * }</pre>
 *
 * @param <I> The input type to be routed (typically String)
 */
public class LLMContentRouter<I> implements ContentRouter<I> {

    private static final String ROUTER_NAME = "LLM-Based";
    
    // Configuration keys
    public static final String CLASSIFICATION_PROMPT = "classificationPrompt";
    public static final String INCLUDE_CONFIDENCE = "includeConfidence";
    public static final String INCLUDE_ALTERNATIVES = "includeAlternatives";
    public static final String INCLUDE_REASONING = "includeReasoning";
    public static final String MAX_ALTERNATIVES = "maxAlternatives";
    public static final String TEMPERATURE = "temperature";
    
    // Default values
    private static final String DEFAULT_CLASSIFICATION_PROMPT = 
        "Analyze the following input and classify it into one of the available categories.";
    private static final boolean DEFAULT_INCLUDE_CONFIDENCE = true;
    private static final boolean DEFAULT_INCLUDE_ALTERNATIVES = false;
    private static final boolean DEFAULT_INCLUDE_REASONING = true;
    private static final int DEFAULT_MAX_ALTERNATIVES = 3;
    
    // Response parsing patterns
    private static final Pattern ROUTE_PATTERN = Pattern.compile("\"route\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CONFIDENCE_PATTERN = Pattern.compile("\"confidence\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern REASONING_PATTERN = Pattern.compile("\"reasoning\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ALTERNATIVES_PATTERN = Pattern.compile("\"alternatives\"\\s*:\\s*\\[(.*?)\\]");
    private static final Pattern ALTERNATIVE_ITEM_PATTERN = Pattern.compile("\\{\\s*\"route\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"score\"\\s*:\\s*([0-9]*\\.?[0-9]+)\\s*\\}");

    private final ChatModel model;
    private final String classificationPrompt;
    private final boolean includeConfidence;
    private final boolean includeAlternatives;
    private final boolean includeReasoning;
    private final int maxAlternatives;

    /**
     * Creates a new LLMContentRouter with the specified configuration.
     */
    private LLMContentRouter(ChatModel model, String classificationPrompt,
                           boolean includeConfidence, boolean includeAlternatives,
                           boolean includeReasoning, int maxAlternatives) {
        this.model = Objects.requireNonNull(model, "ChatModel cannot be null");
        this.classificationPrompt = classificationPrompt != null ? classificationPrompt : DEFAULT_CLASSIFICATION_PROMPT;
        this.includeConfidence = includeConfidence;
        this.includeAlternatives = includeAlternatives;
        this.includeReasoning = includeReasoning;
        this.maxAlternatives = Math.max(1, maxAlternatives);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRouterName() {
        return ROUTER_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RoutingDecision route(I input, Set<String> availableRoutes, Map<String, Object> context) 
            throws WorkflowExecutionException {
        
        Objects.requireNonNull(input, "Input cannot be null");
        Objects.requireNonNull(availableRoutes, "Available routes cannot be null");
        Objects.requireNonNull(context, "Context cannot be null");
        
        if (availableRoutes.isEmpty()) {
            throw new IllegalArgumentException("Available routes cannot be empty");
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // Build the classification prompt
            String fullPrompt = buildClassificationPrompt(input, availableRoutes, context);
            
            // Query the LLM
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(SystemMessage.from(classificationPrompt));
            messages.add(UserMessage.from(fullPrompt));
            
            AiMessage response = model.chat(messages).aiMessage();
            String responseText = response.text();
            
            // Parse the response
            RoutingDecision decision = parseResponse(responseText, availableRoutes, startTime);
            
            // Store routing metadata in context
            context.put("routing_prompt", fullPrompt);
            context.put("llm_response", responseText);
            context.put("routing_time_ms", decision.getProcessingTimeMs());
            
            return decision;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            context.put("routing_error", e.getMessage());
            context.put("routing_time_ms", processingTime);
            
            throw new WorkflowExecutionException(
                "LLMContentRouter", 
                "Failed to route content using LLM: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<RoutingDecision> routeAsync(I input, Set<String> availableRoutes, 
                                                        Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return route(input, availableRoutes, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async LLM routing failed", e);
            }
        });
    }

    /**
     * Builds the complete classification prompt for the LLM.
     */
    private String buildClassificationPrompt(I input, Set<String> availableRoutes, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Available routes: ").append(availableRoutes).append("\n\n");
        prompt.append("Input to classify:\n").append(input.toString()).append("\n\n");
        
        prompt.append("Please respond with a JSON object containing:\n");
        prompt.append("- \"route\": The selected route from the available options\n");
        
        if (includeConfidence) {
            prompt.append("- \"confidence\": A confidence score between 0.0 and 1.0\n");
        }
        
        if (includeReasoning) {
            prompt.append("- \"reasoning\": Brief explanation of your classification decision\n");
        }
        
        if (includeAlternatives) {
            prompt.append("- \"alternatives\": Array of up to ").append(maxAlternatives)
                  .append(" alternative routes with scores (format: [{\"route\": \"name\", \"score\": 0.0}])\n");
        }
        
        prompt.append("\nExample response format:\n");
        prompt.append("{\n");
        prompt.append("  \"route\": \"selected_route\",\n");
        
        if (includeConfidence) {
            prompt.append("  \"confidence\": 0.85,\n");
        }
        
        if (includeReasoning) {
            prompt.append("  \"reasoning\": \"Classification explanation\",\n");
        }
        
        if (includeAlternatives) {
            prompt.append("  \"alternatives\": [{\"route\": \"alternative\", \"score\": 0.23}]\n");
        } else {
            // Remove trailing comma if alternatives not included
            if (includeReasoning) {
                prompt.setLength(prompt.length() - 2); // Remove ",\n"
                prompt.append("\n");
            }
        }
        
        prompt.append("}");
        
        return prompt.toString();
    }

    /**
     * Parses the LLM response to extract routing decision information.
     */
    private RoutingDecision parseResponse(String response, Set<String> availableRoutes, long startTime) 
            throws WorkflowExecutionException {
        
        try {
            RoutingDecision.Builder builder = RoutingDecision.builder();
            
            // Parse selected route
            Matcher routeMatcher = ROUTE_PATTERN.matcher(response);
            if (routeMatcher.find()) {
                String selectedRoute = routeMatcher.group(1);
                if (!availableRoutes.contains(selectedRoute)) {
                    throw new WorkflowExecutionException(
                        "LLMContentRouter", 
                        "LLM selected invalid route: " + selectedRoute + 
                        ". Available routes: " + availableRoutes
                    );
                }
                builder.selectedRoute(selectedRoute);
            } else {
                throw new WorkflowExecutionException(
                    "LLMContentRouter", 
                    "Could not parse route from LLM response: " + response
                );
            }
            
            // Parse confidence if included
            if (includeConfidence) {
                Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(response);
                if (confidenceMatcher.find()) {
                    double confidence = Double.parseDouble(confidenceMatcher.group(1));
                    builder.confidence(Math.min(1.0, Math.max(0.0, confidence))); // Clamp to valid range
                } else {
                    builder.confidence(0.5); // Default confidence if not found
                }
            } else {
                builder.confidence(1.0); // Max confidence when not measuring
            }
            
            // Parse reasoning if included
            if (includeReasoning) {
                Matcher reasoningMatcher = REASONING_PATTERN.matcher(response);
                if (reasoningMatcher.find()) {
                    builder.reasoning(reasoningMatcher.group(1));
                }
            }
            
            // Parse alternatives if included
            if (includeAlternatives) {
                Matcher alternativesMatcher = ALTERNATIVES_PATTERN.matcher(response);
                if (alternativesMatcher.find()) {
                    String alternativesJson = alternativesMatcher.group(1);
                    List<RouteCandidate> alternatives = parseAlternatives(alternativesJson);
                    builder.alternatives(alternatives);
                }
            }
            
            // Set processing time and metadata
            long processingTime = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(processingTime);
            builder.addMetadata("router_type", "llm");
            builder.addMetadata("model_response", response);
            
            return builder.build();
            
        } catch (Exception e) {
            throw new WorkflowExecutionException(
                "LLMContentRouter", 
                "Failed to parse LLM response: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Parses alternative routes from the JSON alternatives array.
     */
    private List<RouteCandidate> parseAlternatives(String alternativesJson) {
        List<RouteCandidate> alternatives = new ArrayList<>();
        
        Matcher matcher = ALTERNATIVE_ITEM_PATTERN.matcher(alternativesJson);
        while (matcher.find() && alternatives.size() < maxAlternatives) {
            String route = matcher.group(1);
            double score = Double.parseDouble(matcher.group(2));
            score = Math.min(1.0, Math.max(0.0, score)); // Clamp to valid range
            alternatives.add(new RouteCandidate(route, score));
        }
        
        return alternatives;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getRouterConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("routerName", ROUTER_NAME);
        config.put("routerType", "llm");
        config.put("includeConfidence", includeConfidence);
        config.put("includeAlternatives", includeAlternatives);
        config.put("includeReasoning", includeReasoning);
        config.put("maxAlternatives", maxAlternatives);
        config.put("classificationPrompt", classificationPrompt);
        return config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getRoutingCharacteristics(I input, Set<String> availableRoutes, 
                                                         Map<String, Object> context) {
        Map<String, Object> characteristics = new HashMap<>();
        characteristics.put("routerName", getRouterName());
        characteristics.put("routerType", "llm");
        characteristics.put("routeCount", availableRoutes.size());
        characteristics.put("inputLength", input.toString().length());
        characteristics.put("expectedLatency", "medium-high");
        characteristics.put("accuracy", "high");
        characteristics.put("costPerRequest", "medium");
        return characteristics;
    }

    /**
     * Builder for creating LLMContentRouter instances.
     *
     * @param <I> The input type for the router
     */
    public static class Builder<I> {
        private ChatModel model;
        private String classificationPrompt = DEFAULT_CLASSIFICATION_PROMPT;
        private boolean includeConfidence = DEFAULT_INCLUDE_CONFIDENCE;
        private boolean includeAlternatives = DEFAULT_INCLUDE_ALTERNATIVES;
        private boolean includeReasoning = DEFAULT_INCLUDE_REASONING;
        private int maxAlternatives = DEFAULT_MAX_ALTERNATIVES;

        /**
         * Sets the ChatModel to use for classification.
         *
         * @param model The ChatModel instance
         * @return This builder instance
         */
        public Builder<I> model(ChatModel model) {
            this.model = model;
            return this;
        }

        /**
         * Sets the classification prompt template.
         *
         * @param prompt The system prompt for classification
         * @return This builder instance
         */
        public Builder<I> classificationPrompt(String prompt) {
            this.classificationPrompt = prompt;
            return this;
        }

        /**
         * Sets whether to include confidence scoring.
         *
         * @param include Whether to include confidence scores
         * @return This builder instance
         */
        public Builder<I> includeConfidenceScoring(boolean include) {
            this.includeConfidence = include;
            return this;
        }

        /**
         * Sets whether to include alternative routes.
         *
         * @param include Whether to include alternative routes
         * @return This builder instance
         */
        public Builder<I> includeAlternatives(boolean include) {
            this.includeAlternatives = include;
            return this;
        }

        /**
         * Sets whether to include reasoning explanations.
         *
         * @param include Whether to include reasoning
         * @return This builder instance
         */
        public Builder<I> includeReasoning(boolean include) {
            this.includeReasoning = include;
            return this;
        }

        /**
         * Sets the maximum number of alternative routes to include.
         *
         * @param max Maximum number of alternatives
         * @return This builder instance
         */
        public Builder<I> maxAlternatives(int max) {
            this.maxAlternatives = max;
            return this;
        }

        /**
         * Builds the LLMContentRouter instance.
         *
         * @return A new LLMContentRouter instance
         * @throws IllegalStateException if required fields are not set
         */
        public LLMContentRouter<I> build() {
            if (model == null) {
                throw new IllegalStateException("ChatModel must be set");
            }
            
            return new LLMContentRouter<>(model, classificationPrompt, includeConfidence, 
                                        includeAlternatives, includeReasoning, maxAlternatives);
        }
    }

    /**
     * Creates a new Builder for constructing LLMContentRouter instances.
     *
     * @param <I> The input type for the router
     * @return A new Builder instance
     */
    public static <I> Builder<I> builder() {
        return new Builder<>();
    }

    /**
     * Creates a simple LLMContentRouter with default configuration.
     *
     * @param <I> The input type for the router
     * @param model The ChatModel to use
     * @return A new LLMContentRouter instance
     */
    public static <I> LLMContentRouter<I> create(ChatModel model) {
        return LLMContentRouter.<I>builder()
            .model(model)
            .build();
    }

    /**
     * Creates an LLMContentRouter with a custom classification prompt.
     *
     * @param <I> The input type for the router
     * @param model The ChatModel to use
     * @param classificationPrompt The system prompt for classification
     * @return A new LLMContentRouter instance
     */
    public static <I> LLMContentRouter<I> create(ChatModel model, String classificationPrompt) {
        return LLMContentRouter.<I>builder()
            .model(model)
            .classificationPrompt(classificationPrompt)
            .build();
    }
}