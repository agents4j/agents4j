package dev.agents4j.api.routing;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the result of a content routing decision.
 * 
 * <p>This class encapsulates all information about how content was classified
 * and routed, including the selected route, confidence metrics, reasoning,
 * and alternative options that were considered.</p>
 * 
 * <p><b>Key Information:</b></p>
 * <ul>
 * <li><b>Selected Route</b>: The chosen route identifier</li>
 * <li><b>Confidence</b>: Numerical confidence in the routing decision</li>
 * <li><b>Reasoning</b>: Human-readable explanation of the decision</li>
 * <li><b>Alternatives</b>: Other routes that were considered</li>
 * <li><b>Metadata</b>: Additional classification and routing information</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RoutingDecision decision = RoutingDecision.builder()
 *     .selectedRoute("technical-support")
 *     .confidence(0.85)
 *     .reasoning("Contains technical error codes and system information")
 *     .addAlternative("billing", 0.23)
 *     .addMetadata("urgency", "high")
 *     .build();
 * }</pre>
 */
public class RoutingDecision {

    private final String selectedRoute;
    private final double confidence;
    private final String reasoning;
    private final List<RouteCandidate> alternatives;
    private final Map<String, Object> metadata;
    private final long processingTimeMs;

    /**
     * Creates a new RoutingDecision with the specified parameters.
     *
     * @param selectedRoute The chosen route identifier
     * @param confidence The confidence score (0.0 to 1.0)
     * @param reasoning Human-readable explanation of the decision
     * @param alternatives List of alternative routes considered
     * @param metadata Additional classification and routing information
     * @param processingTimeMs Time taken to make the routing decision
     */
    private RoutingDecision(String selectedRoute, double confidence, String reasoning,
                           List<RouteCandidate> alternatives, Map<String, Object> metadata,
                           long processingTimeMs) {
        this.selectedRoute = Objects.requireNonNull(selectedRoute, "Selected route cannot be null");
        this.confidence = validateConfidence(confidence);
        this.reasoning = reasoning != null ? reasoning : "No reasoning provided";
        this.alternatives = Collections.unmodifiableList(alternatives != null ? alternatives : Collections.emptyList());
        this.metadata = Collections.unmodifiableMap(metadata != null ? metadata : Collections.emptyMap());
        this.processingTimeMs = Math.max(0, processingTimeMs);
    }

    /**
     * Gets the selected route identifier.
     *
     * @return The chosen route identifier
     */
    public String getSelectedRoute() {
        return selectedRoute;
    }

    /**
     * Gets the confidence score for the routing decision.
     *
     * @return Confidence score between 0.0 and 1.0
     */
    public double getConfidence() {
        return confidence;
    }

    /**
     * Gets the reasoning behind the routing decision.
     *
     * @return Human-readable explanation of the decision
     */
    public String getReasoning() {
        return reasoning;
    }

    /**
     * Gets the list of alternative routes that were considered.
     *
     * @return Unmodifiable list of alternative routes with their scores
     */
    public List<RouteCandidate> getAlternatives() {
        return alternatives;
    }

    /**
     * Gets the metadata associated with this routing decision.
     *
     * @return Unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets a specific metadata value.
     *
     * @param key The metadata key
     * @param <T> The expected type of the value
     * @return The metadata value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key) {
        return (T) metadata.get(key);
    }

    /**
     * Gets a specific metadata value with a default.
     *
     * @param key The metadata key
     * @param defaultValue The default value if key is not found
     * @param <T> The expected type of the value
     * @return The metadata value, or default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        return (T) metadata.getOrDefault(key, defaultValue);
    }

    /**
     * Gets the time taken to make this routing decision.
     *
     * @return Processing time in milliseconds
     */
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Checks if the confidence meets the specified threshold.
     *
     * @param threshold The confidence threshold (0.0 to 1.0)
     * @return true if confidence is greater than or equal to threshold
     */
    public boolean meetsThreshold(double threshold) {
        return confidence >= validateConfidence(threshold);
    }

    /**
     * Gets the best alternative route if it exists.
     *
     * @return The highest-scoring alternative route, or null if none exist
     */
    public RouteCandidate getBestAlternative() {
        return alternatives.stream()
            .max((a, b) -> Double.compare(a.getScore(), b.getScore()))
            .orElse(null);
    }

    /**
     * Validates that a confidence value is between 0.0 and 1.0.
     */
    private static double validateConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0, got: " + confidence);
        }
        return confidence;
    }

    /**
     * Creates a simple routing decision with just a route and confidence.
     *
     * @param selectedRoute The chosen route identifier
     * @param confidence The confidence score
     * @return A new RoutingDecision instance
     */
    public static RoutingDecision simple(String selectedRoute, double confidence) {
        return builder()
            .selectedRoute(selectedRoute)
            .confidence(confidence)
            .build();
    }

    /**
     * Creates a routing decision with route, confidence, and reasoning.
     *
     * @param selectedRoute The chosen route identifier
     * @param confidence The confidence score
     * @param reasoning The reasoning behind the decision
     * @return A new RoutingDecision instance
     */
    public static RoutingDecision withReasoning(String selectedRoute, double confidence, String reasoning) {
        return builder()
            .selectedRoute(selectedRoute)
            .confidence(confidence)
            .reasoning(reasoning)
            .build();
    }

    /**
     * Creates a new Builder for constructing RoutingDecision instances.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating RoutingDecision instances.
     */
    public static class Builder {
        private String selectedRoute;
        private double confidence = 0.0;
        private String reasoning;
        private List<RouteCandidate> alternatives = new java.util.ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private long processingTimeMs = 0;

        /**
         * Sets the selected route identifier.
         *
         * @param selectedRoute The chosen route identifier
         * @return This builder instance
         */
        public Builder selectedRoute(String selectedRoute) {
            this.selectedRoute = selectedRoute;
            return this;
        }

        /**
         * Sets the confidence score for the routing decision.
         *
         * @param confidence The confidence score (0.0 to 1.0)
         * @return This builder instance
         */
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }

        /**
         * Sets the reasoning behind the routing decision.
         *
         * @param reasoning Human-readable explanation
         * @return This builder instance
         */
        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        /**
         * Adds an alternative route that was considered.
         *
         * @param routeId The alternative route identifier
         * @param score The score for this alternative
         * @return This builder instance
         */
        public Builder addAlternative(String routeId, double score) {
            this.alternatives.add(new RouteCandidate(routeId, score));
            return this;
        }

        /**
         * Adds an alternative route candidate.
         *
         * @param candidate The route candidate to add
         * @return This builder instance
         */
        public Builder addAlternative(RouteCandidate candidate) {
            this.alternatives.add(candidate);
            return this;
        }

        /**
         * Sets all alternative routes.
         *
         * @param alternatives List of alternative route candidates
         * @return This builder instance
         */
        public Builder alternatives(List<RouteCandidate> alternatives) {
            this.alternatives = new java.util.ArrayList<>(alternatives);
            return this;
        }

        /**
         * Adds a metadata entry.
         *
         * @param key The metadata key
         * @param value The metadata value
         * @return This builder instance
         */
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Sets all metadata.
         *
         * @param metadata Map of metadata entries
         * @return This builder instance
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        /**
         * Sets the processing time for this decision.
         *
         * @param processingTimeMs Processing time in milliseconds
         * @return This builder instance
         */
        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        /**
         * Builds the RoutingDecision instance.
         *
         * @return A new RoutingDecision instance
         * @throws IllegalStateException if required fields are not set
         */
        public RoutingDecision build() {
            if (selectedRoute == null) {
                throw new IllegalStateException("Selected route must be set");
            }
            return new RoutingDecision(selectedRoute, confidence, reasoning, alternatives, metadata, processingTimeMs);
        }
    }

    @Override
    public String toString() {
        return String.format("RoutingDecision{route='%s', confidence=%.2f, reasoning='%s', alternatives=%d, processingTime=%dms}",
            selectedRoute, confidence, reasoning, alternatives.size(), processingTimeMs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingDecision that = (RoutingDecision) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               processingTimeMs == that.processingTimeMs &&
               Objects.equals(selectedRoute, that.selectedRoute) &&
               Objects.equals(reasoning, that.reasoning) &&
               Objects.equals(alternatives, that.alternatives) &&
               Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedRoute, confidence, reasoning, alternatives, metadata, processingTimeMs);
    }
}