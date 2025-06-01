package dev.agents4j.api.routing;

import dev.agents4j.api.graph.NodeId;
import java.util.Objects;

/**
 * Represents a candidate route with its associated score, integrated with graph workflow navigation.
 * 
 * <p>This class is used to represent alternative routes that were considered
 * during the routing decision process, along with their confidence scores.
 * It supports both string identifiers and NodeId objects for graph workflow integration.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RouteCandidate candidate = new RouteCandidate("billing-support", 0.72);
 * RouteCandidate nodeCandidate = new RouteCandidate(NodeId.of("technical-support"), 0.95);
 * RouteCandidate highConfidence = RouteCandidate.of("technical-support", 0.95);
 * }</pre>
 */
public class RouteCandidate {

    private final NodeId routeNodeId;
    private final double score;

    /**
     * Creates a new RouteCandidate with the specified route ID string and score.
     *
     * @param routeId The route identifier string
     * @param score The confidence/relevance score for this route (0.0 to 1.0)
     * @throws IllegalArgumentException if routeId is null or score is invalid
     */
    public RouteCandidate(String routeId, double score) {
        Objects.requireNonNull(routeId, "Route ID cannot be null");
        this.routeNodeId = NodeId.of(routeId);
        this.score = validateScore(score);
    }

    /**
     * Creates a new RouteCandidate with the specified NodeId and score.
     *
     * @param routeNodeId The route node identifier
     * @param score The confidence/relevance score for this route (0.0 to 1.0)
     * @throws IllegalArgumentException if routeNodeId is null or score is invalid
     */
    public RouteCandidate(NodeId routeNodeId, double score) {
        this.routeNodeId = Objects.requireNonNull(routeNodeId, "Route node ID cannot be null");
        this.score = validateScore(score);
    }

    /**
     * Gets the route identifier as a string.
     *
     * @return The route identifier string
     */
    public String getRouteId() {
        return routeNodeId.value();
    }

    /**
     * Gets the route as a NodeId for graph workflow integration.
     *
     * @return The route node identifier
     */
    public NodeId getRouteNodeId() {
        return routeNodeId;
    }

    /**
     * Gets the confidence/relevance score for this route.
     *
     * @return The score between 0.0 and 1.0
     */
    public double getScore() {
        return score;
    }

    /**
     * Checks if this candidate's score meets the specified threshold.
     *
     * @param threshold The minimum score threshold (0.0 to 1.0)
     * @return true if this candidate's score is greater than or equal to the threshold
     */
    public boolean meetsThreshold(double threshold) {
        return score >= validateScore(threshold);
    }

    /**
     * Compares this candidate to another based on score.
     *
     * @param other The other candidate to compare to
     * @return negative if this score is lower, positive if higher, 0 if equal
     */
    public int compareScore(RouteCandidate other) {
        return Double.compare(this.score, other.score);
    }

    /**
     * Validates that a score value is between 0.0 and 1.0.
     */
    private static double validateScore(double score) {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Score must be between 0.0 and 1.0, got: " + score);
        }
        return score;
    }

    /**
     * Creates a new RouteCandidate with the specified route ID and score.
     * This is a convenience factory method.
     *
     * @param routeId The route identifier string
     * @param score The confidence/relevance score for this route
     * @return A new RouteCandidate instance
     */
    public static RouteCandidate of(String routeId, double score) {
        return new RouteCandidate(routeId, score);
    }

    /**
     * Creates a new RouteCandidate with the specified NodeId and score.
     * This is a convenience factory method for graph workflow integration.
     *
     * @param routeNodeId The route node identifier
     * @param score The confidence/relevance score for this route
     * @return A new RouteCandidate instance
     */
    public static RouteCandidate of(NodeId routeNodeId, double score) {
        return new RouteCandidate(routeNodeId, score);
    }

    /**
     * Creates a RouteCandidate with maximum confidence.
     *
     * @param routeId The route identifier string
     * @return A new RouteCandidate with score 1.0
     */
    public static RouteCandidate highConfidence(String routeId) {
        return new RouteCandidate(routeId, 1.0);
    }

    /**
     * Creates a RouteCandidate with maximum confidence using NodeId.
     *
     * @param routeNodeId The route node identifier
     * @return A new RouteCandidate with score 1.0
     */
    public static RouteCandidate highConfidence(NodeId routeNodeId) {
        return new RouteCandidate(routeNodeId, 1.0);
    }

    /**
     * Creates a RouteCandidate with medium confidence.
     *
     * @param routeId The route identifier string
     * @return A new RouteCandidate with score 0.5
     */
    public static RouteCandidate mediumConfidence(String routeId) {
        return new RouteCandidate(routeId, 0.5);
    }

    /**
     * Creates a RouteCandidate with medium confidence using NodeId.
     *
     * @param routeNodeId The route node identifier
     * @return A new RouteCandidate with score 0.5
     */
    public static RouteCandidate mediumConfidence(NodeId routeNodeId) {
        return new RouteCandidate(routeNodeId, 0.5);
    }

    /**
     * Creates a RouteCandidate with low confidence.
     *
     * @param routeId The route identifier string
     * @return A new RouteCandidate with score 0.25
     */
    public static RouteCandidate lowConfidence(String routeId) {
        return new RouteCandidate(routeId, 0.25);
    }

    /**
     * Creates a RouteCandidate with low confidence using NodeId.
     *
     * @param routeNodeId The route node identifier
     * @return A new RouteCandidate with score 0.25
     */
    public static RouteCandidate lowConfidence(NodeId routeNodeId) {
        return new RouteCandidate(routeNodeId, 0.25);
    }

    @Override
    public String toString() {
        return String.format("RouteCandidate{routeId='%s', score=%.3f}", routeNodeId.value(), score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteCandidate that = (RouteCandidate) o;
        return Double.compare(that.score, score) == 0 && Objects.equals(routeNodeId, that.routeNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeNodeId, score);
    }
}