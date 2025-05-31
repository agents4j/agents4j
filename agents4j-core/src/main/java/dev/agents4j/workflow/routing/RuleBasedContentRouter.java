package dev.agents4j.workflow.routing;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.RouteCandidate;
import dev.agents4j.api.routing.RoutingDecision;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Rule-based content router that uses predefined patterns and rules for classification.
 * 
 * <p>This router provides fast, deterministic routing based on configurable rules
 * such as regex patterns, keyword matching, and custom predicates. It's ideal for
 * scenarios where routing logic is well-defined and doesn't require the complexity
 * of LLM-based classification.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Fast, deterministic routing decisions</li>
 * <li>Multiple rule types: regex, keywords, custom predicates</li>
 * <li>Rule priority and fallback mechanisms</li>
 * <li>No external API dependencies</li>
 * <li>Configurable confidence scoring</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RuleBasedContentRouter<String> router = RuleBasedContentRouter.<String>builder()
 *     .addRegexRule("technical", Pattern.compile("(error|bug|crash|exception)", Pattern.CASE_INSENSITIVE), 0.9)
 *     .addKeywordRule("billing", List.of("payment", "invoice", "charge", "refund"), 0.8)
 *     .addPredicateRule("urgent", (input, ctx) -> input.contains("URGENT"), 1.0)
 *     .defaultRoute("general")
 *     .build();
 * }</pre>
 *
 * @param <I> The input type to be routed (typically String)
 */
public class RuleBasedContentRouter<I> implements ContentRouter<I> {

    private static final String ROUTER_NAME = "Rule-Based";

    /**
     * Represents a routing rule with its associated logic and confidence.
     */
    public static class RoutingRule<I> {
        private final String routeId;
        private final BiPredicate<I, Map<String, Object>> predicate;
        private final double confidence;
        private final int priority;
        private final String description;

        public RoutingRule(String routeId, BiPredicate<I, Map<String, Object>> predicate, 
                          double confidence, int priority, String description) {
            this.routeId = Objects.requireNonNull(routeId, "Route ID cannot be null");
            this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
            this.confidence = validateConfidence(confidence);
            this.priority = priority;
            this.description = description != null ? description : "";
        }

        public String getRouteId() { return routeId; }
        public BiPredicate<I, Map<String, Object>> getPredicate() { return predicate; }
        public double getConfidence() { return confidence; }
        public int getPriority() { return priority; }
        public String getDescription() { return description; }

        public boolean matches(I input, Map<String, Object> context) {
            try {
                return predicate.test(input, context);
            } catch (Exception e) {
                return false;
            }
        }

        private static double validateConfidence(double confidence) {
            if (confidence < 0.0 || confidence > 1.0) {
                throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0, got: " + confidence);
            }
            return confidence;
        }
    }

    private final List<RoutingRule<I>> rules;
    private final String defaultRoute;
    private final double defaultConfidence;
    private final boolean enableMultipleMatches;
    private final int maxAlternatives;

    /**
     * Creates a new RuleBasedContentRouter with the specified configuration.
     */
    private RuleBasedContentRouter(List<RoutingRule<I>> rules, String defaultRoute, 
                                  double defaultConfidence, boolean enableMultipleMatches,
                                  int maxAlternatives) {
        this.rules = new ArrayList<>(rules);
        this.defaultRoute = defaultRoute;
        this.defaultConfidence = defaultConfidence;
        this.enableMultipleMatches = enableMultipleMatches;
        this.maxAlternatives = maxAlternatives;

        // Sort rules by priority (higher priority first)
        this.rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
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
            List<RouteMatch> matches = findMatches(input, availableRoutes, context);
            
            RoutingDecision.Builder builder = RoutingDecision.builder();
            
            if (!matches.isEmpty()) {
                // Use the highest priority/confidence match
                RouteMatch bestMatch = matches.get(0);
                builder.selectedRoute(bestMatch.routeId)
                       .confidence(bestMatch.confidence)
                       .reasoning(buildReasoning(bestMatch, matches));
                
                // Add alternatives if enabled
                if (enableMultipleMatches && matches.size() > 1) {
                    List<RouteCandidate> alternatives = new ArrayList<>();
                    for (int i = 1; i < Math.min(matches.size(), maxAlternatives + 1); i++) {
                        RouteMatch match = matches.get(i);
                        alternatives.add(new RouteCandidate(match.routeId, match.confidence));
                    }
                    builder.alternatives(alternatives);
                }
            } else {
                // No rules matched, use default route
                if (defaultRoute != null && availableRoutes.contains(defaultRoute)) {
                    builder.selectedRoute(defaultRoute)
                           .confidence(defaultConfidence)
                           .reasoning("No specific rules matched, using default route");
                } else {
                    // Pick the first available route as last resort
                    String fallbackRoute = availableRoutes.iterator().next();
                    builder.selectedRoute(fallbackRoute)
                           .confidence(0.1)
                           .reasoning("No rules matched and no valid default route, using fallback");
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            builder.processingTimeMs(processingTime)
                   .addMetadata("router_type", "rule_based")
                   .addMetadata("rules_evaluated", rules.size())
                   .addMetadata("matches_found", matches.size());
            
            RoutingDecision decision = builder.build();
            
            // Store routing metadata in context
            context.put("routing_matches", matches);
            context.put("routing_time_ms", processingTime);
            context.put("rules_evaluated", rules.size());
            
            return decision;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            context.put("routing_error", e.getMessage());
            context.put("routing_time_ms", processingTime);
            
            throw new WorkflowExecutionException(
                "RuleBasedContentRouter", 
                "Failed to route content using rules: " + e.getMessage(), 
                e
            );
        }
    }

    /**
     * Finds all matching rules for the input.
     */
    private List<RouteMatch> findMatches(I input, Set<String> availableRoutes, Map<String, Object> context) {
        List<RouteMatch> matches = new ArrayList<>();
        
        for (RoutingRule<I> rule : rules) {
            if (availableRoutes.contains(rule.getRouteId()) && rule.matches(input, context)) {
                matches.add(new RouteMatch(rule.getRouteId(), rule.getConfidence(), 
                                         rule.getPriority(), rule.getDescription()));
                
                if (!enableMultipleMatches) {
                    break; // Stop at first match if multiple matches are disabled
                }
            }
        }
        
        // Sort by priority first, then by confidence
        matches.sort((m1, m2) -> {
            int priorityCompare = Integer.compare(m2.priority, m1.priority);
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Double.compare(m2.confidence, m1.confidence);
        });
        
        return matches;
    }

    /**
     * Builds reasoning text for the routing decision.
     */
    private String buildReasoning(RouteMatch bestMatch, List<RouteMatch> allMatches) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Matched rule for route '").append(bestMatch.routeId).append("'");
        
        if (!bestMatch.description.isEmpty()) {
            reasoning.append(": ").append(bestMatch.description);
        }
        
        if (allMatches.size() > 1) {
            reasoning.append(". Alternative matches found for: ");
            for (int i = 1; i < allMatches.size(); i++) {
                if (i > 1) reasoning.append(", ");
                reasoning.append(allMatches.get(i).routeId);
            }
        }
        
        return reasoning.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<RoutingDecision> routeAsync(I input, Set<String> availableRoutes, 
                                                        Map<String, Object> context) {
        // Rule-based routing is fast, so async doesn't add much value, but we support it
        return CompletableFuture.supplyAsync(() -> {
            try {
                return route(input, availableRoutes, context);
            } catch (WorkflowExecutionException e) {
                throw new RuntimeException("Async rule-based routing failed", e);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getRouterConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("routerName", ROUTER_NAME);
        config.put("routerType", "rule_based");
        config.put("ruleCount", rules.size());
        config.put("defaultRoute", defaultRoute);
        config.put("defaultConfidence", defaultConfidence);
        config.put("enableMultipleMatches", enableMultipleMatches);
        config.put("maxAlternatives", maxAlternatives);
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
        characteristics.put("routerType", "rule_based");
        characteristics.put("routeCount", availableRoutes.size());
        characteristics.put("ruleCount", rules.size());
        characteristics.put("expectedLatency", "very-low");
        characteristics.put("accuracy", "medium-high");
        characteristics.put("costPerRequest", "very-low");
        characteristics.put("deterministic", true);
        return characteristics;
    }

    /**
     * Helper class to represent a route match.
     */
    private static class RouteMatch {
        final String routeId;
        final double confidence;
        final int priority;
        final String description;

        RouteMatch(String routeId, double confidence, int priority, String description) {
            this.routeId = routeId;
            this.confidence = confidence;
            this.priority = priority;
            this.description = description;
        }
    }

    /**
     * Builder for creating RuleBasedContentRouter instances.
     *
     * @param <I> The input type for the router
     */
    public static class Builder<I> {
        private final List<RoutingRule<I>> rules = new ArrayList<>();
        private String defaultRoute;
        private double defaultConfidence = 0.5;
        private boolean enableMultipleMatches = false;
        private int maxAlternatives = 3;

        /**
         * Adds a regex-based routing rule.
         *
         * @param routeId The route identifier
         * @param pattern The regex pattern to match
         * @param confidence The confidence score for this rule
         * @return This builder instance
         */
        public Builder<I> addRegexRule(String routeId, Pattern pattern, double confidence) {
            return addRegexRule(routeId, pattern, confidence, 0, "Regex pattern: " + pattern.pattern());
        }

        /**
         * Adds a regex-based routing rule with priority and description.
         *
         * @param routeId The route identifier
         * @param pattern The regex pattern to match
         * @param confidence The confidence score for this rule
         * @param priority The rule priority (higher = more important)
         * @param description Human-readable description of the rule
         * @return This builder instance
         */
        public Builder<I> addRegexRule(String routeId, Pattern pattern, double confidence, 
                                      int priority, String description) {
            BiPredicate<I, Map<String, Object>> predicate = (input, ctx) -> 
                pattern.matcher(input.toString()).find();
            
            rules.add(new RoutingRule<>(routeId, predicate, confidence, priority, description));
            return this;
        }

        /**
         * Adds a keyword-based routing rule.
         *
         * @param routeId The route identifier
         * @param keywords List of keywords to match
         * @param confidence The confidence score for this rule
         * @return This builder instance
         */
        public Builder<I> addKeywordRule(String routeId, List<String> keywords, double confidence) {
            return addKeywordRule(routeId, keywords, confidence, 0, 
                                "Keywords: " + String.join(", ", keywords));
        }

        /**
         * Adds a keyword-based routing rule with priority and description.
         *
         * @param routeId The route identifier
         * @param keywords List of keywords to match
         * @param confidence The confidence score for this rule
         * @param priority The rule priority
         * @param description Human-readable description of the rule
         * @return This builder instance
         */
        public Builder<I> addKeywordRule(String routeId, List<String> keywords, double confidence,
                                        int priority, String description) {
            BiPredicate<I, Map<String, Object>> predicate = (input, ctx) -> {
                String text = input.toString().toLowerCase();
                return keywords.stream().anyMatch(keyword -> text.contains(keyword.toLowerCase()));
            };
            
            rules.add(new RoutingRule<>(routeId, predicate, confidence, priority, description));
            return this;
        }

        /**
         * Adds a custom predicate-based routing rule.
         *
         * @param routeId The route identifier
         * @param predicate The custom predicate logic
         * @param confidence The confidence score for this rule
         * @return This builder instance
         */
        public Builder<I> addPredicateRule(String routeId, BiPredicate<I, Map<String, Object>> predicate, 
                                          double confidence) {
            return addPredicateRule(routeId, predicate, confidence, 0, "Custom predicate");
        }

        /**
         * Adds a custom predicate-based routing rule with priority and description.
         *
         * @param routeId The route identifier
         * @param predicate The custom predicate logic
         * @param confidence The confidence score for this rule
         * @param priority The rule priority
         * @param description Human-readable description of the rule
         * @return This builder instance
         */
        public Builder<I> addPredicateRule(String routeId, BiPredicate<I, Map<String, Object>> predicate, 
                                          double confidence, int priority, String description) {
            rules.add(new RoutingRule<>(routeId, predicate, confidence, priority, description));
            return this;
        }

        /**
         * Sets the default route to use when no rules match.
         *
         * @param defaultRoute The default route identifier
         * @return This builder instance
         */
        public Builder<I> defaultRoute(String defaultRoute) {
            this.defaultRoute = defaultRoute;
            return this;
        }

        /**
         * Sets the confidence score for the default route.
         *
         * @param confidence The default confidence score
         * @return This builder instance
         */
        public Builder<I> defaultConfidence(double confidence) {
            this.defaultConfidence = confidence;
            return this;
        }

        /**
         * Enables or disables multiple matches.
         *
         * @param enable Whether to allow multiple rule matches
         * @return This builder instance
         */
        public Builder<I> enableMultipleMatches(boolean enable) {
            this.enableMultipleMatches = enable;
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
         * Builds the RuleBasedContentRouter instance.
         *
         * @return A new RuleBasedContentRouter instance
         */
        public RuleBasedContentRouter<I> build() {
            return new RuleBasedContentRouter<>(rules, defaultRoute, defaultConfidence, 
                                               enableMultipleMatches, maxAlternatives);
        }
    }

    /**
     * Creates a new Builder for constructing RuleBasedContentRouter instances.
     *
     * @param <I> The input type for the router
     * @return A new Builder instance
     */
    public static <I> Builder<I> builder() {
        return new Builder<>();
    }

    /**
     * Creates a simple rule-based router with keyword rules.
     *
     * @param <I> The input type for the router
     * @param keywordRules Map of route IDs to their keyword lists
     * @return A new RuleBasedContentRouter instance
     */
    public static <I> RuleBasedContentRouter<I> createWithKeywords(Map<String, List<String>> keywordRules) {
        Builder<I> builder = builder();
        
        for (Map.Entry<String, List<String>> entry : keywordRules.entrySet()) {
            builder.addKeywordRule(entry.getKey(), entry.getValue(), 0.8);
        }
        
        return builder.build();
    }
}