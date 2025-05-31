package dev.agents4j.api.routing;

import java.util.Objects;

/**
 * Represents a candidate route with its associated score.
 * 
 * <p>This class is used to represent alternative routes that were considered
 * during the routing decision process, along with their confidence scores.
 * It's typically used in the alternatives list of a RoutingDecision.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * RouteCandidate candidate = new RouteCandidate("billing-support", 0.72);
 * RouteCandidate highConfidence = RouteCandidate.of("technical-support", 0.95);
 * }</pre>
 */
public class RouteCandidate {

    private final String routeId;
    private final double score;

    /**
     * Creates a new RouteCandidate with the specified route ID and score.
     *
     * @param routeId The route identifier
     * @param score The confidence/relevance score for this route (0.0 to 1.0)
     * @throws IllegalArgumentException if routeId is null or score is invalid
     */
    public RouteCandidate(String routeId, double score) {
        this.routeId = Objects.requireNonNull(routeId, "Route ID cannot be null");
        this.score = validateScore(score);
    }

    /**
     * Gets the route identifier.
     *
     * @return The route identifier
     */
    public String getRouteId() {
        return routeId;
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
     * @param routeId The route identifier
     * @param score The confidence/relevance score for this route
     * @return A new RouteCandidate instance
     */
    public static RouteCandidate of(String routeId, double score) {
        return new RouteCandidate(routeId, score);
    }

    /**
     * Creates a RouteCandidate with maximum confidence.
     *
     * @param routeId The route identifier
     * @return A new RouteCandidate with score 1.0
     */
    public static RouteCandidate highConfidence(String routeId) {
        return new RouteCandidate(routeId, 1.0);
    }

    /**
     * Creates a RouteCandidate with medium confidence.
     *
     * @param routeId The route identifier
     * @return A new RouteCandidate with score 0.5
     */
    public static RouteCandidate mediumConfidence(String routeId) {
        return new RouteCandidate(routeId, 0.5);
    }

    /**
     * Creates a RouteCandidate with low confidence.
     *
     * @param routeId The route identifier
     * @return A new RouteCandidate with score 0.25
     */
    public static RouteCandidate lowConfidence(String routeId) {
        return new RouteCandidate(routeId, 0.25);
    }

    @Override
    public String toString() {
        return String.format("RouteCandidate{routeId='%s', score=%.3f}", routeId, score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteCandidate that = (RouteCandidate) o;
        return Double.compare(that.score, score) == 0 && Objects.equals(routeId, that.routeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId, score);
    }
}