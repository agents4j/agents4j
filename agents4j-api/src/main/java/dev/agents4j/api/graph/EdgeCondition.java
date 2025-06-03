package dev.agents4j.api.graph;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Represents conditions for edge traversal in graph workflows.
 * Provides type-safe conditional logic for determining when edges can be traversed.
 */
public sealed interface EdgeCondition
    permits
        EdgeCondition.Always,
        EdgeCondition.Never,
        EdgeCondition.Conditional,
        EdgeCondition.ContextBased,
        EdgeCondition.And,
        EdgeCondition.Or,
        EdgeCondition.Not {
    /**
     * Evaluates whether this edge condition is satisfied for the given state.
     *
     * @param state The current workflow state
     * @return true if the condition is satisfied and edge can be traversed
     */
    boolean evaluate(GraphWorkflowState<?> state);

    /**
     * Gets a human-readable description of this condition.
     *
     * @return Description of the condition
     */
    String getDescription();

    /**
     * Condition that always allows traversal.
     */
    record Always() implements EdgeCondition {
        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            return true;
        }

        @Override
        public String getDescription() {
            return "Always";
        }
    }

    /**
     * Condition that never allows traversal.
     */
    record Never() implements EdgeCondition {
        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            return false;
        }

        @Override
        public String getDescription() {
            return "Never";
        }
    }

    /**
     * Condition based on a predicate function.
     */
    record Conditional(
        Predicate<GraphWorkflowState<?>> predicate,
        String description
    )
        implements EdgeCondition {
        /**
         * Creates a new conditional edge condition.
         * 
         * @param predicate the predicate to evaluate
         * @param description description of the condition
         * @throws NullPointerException if predicate or description is null
         */
        public Conditional {
            Objects.requireNonNull(predicate, "Predicate cannot be null");
            Objects.requireNonNull(description, "Description cannot be null");
        }

        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            Objects.requireNonNull(state, "State cannot be null");
            return predicate.test(state);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    /**
     * Condition based on context values.
     */
    record ContextBased<T>(
        dev.agents4j.api.context.ContextKey<T> contextKey,
        Predicate<T> valuePredicate,
        String description
    )
        implements EdgeCondition {
        /**
         * Creates a new context-based edge condition.
         * 
         * @param contextKey the context key to evaluate
         * @param valuePredicate the predicate to apply to the context value
         * @param description description of the condition
         * @throws NullPointerException if any parameter is null
         */
        public ContextBased {
            Objects.requireNonNull(contextKey, "Context key cannot be null");
            Objects.requireNonNull(
                valuePredicate,
                "Value predicate cannot be null"
            );
            Objects.requireNonNull(description, "Description cannot be null");
        }

        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            Objects.requireNonNull(state, "State cannot be null");
            return state
                .getContext(contextKey)
                .map(valuePredicate::test)
                .orElse(false);
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    /**
     * Logical AND of two conditions.
     */
    record And(EdgeCondition left, EdgeCondition right)
        implements EdgeCondition {
        /**
         * Creates a new AND edge condition.
         * 
         * @param left the left condition
         * @param right the right condition
         * @throws NullPointerException if either condition is null
         */
        public And {
            Objects.requireNonNull(left, "Left condition cannot be null");
            Objects.requireNonNull(right, "Right condition cannot be null");
        }

        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            return left.evaluate(state) && right.evaluate(state);
        }

        @Override
        public String getDescription() {
            return (
                "(" +
                left.getDescription() +
                " AND " +
                right.getDescription() +
                ")"
            );
        }
    }

    /**
     * Logical OR of two conditions.
     */
    record Or(EdgeCondition left, EdgeCondition right)
        implements EdgeCondition {
        /**
         * Creates a new OR edge condition.
         * 
         * @param left the left condition
         * @param right the right condition
         * @throws NullPointerException if either condition is null
         */
        public Or {
            Objects.requireNonNull(left, "Left condition cannot be null");
            Objects.requireNonNull(right, "Right condition cannot be null");
        }

        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            return left.evaluate(state) || right.evaluate(state);
        }

        @Override
        public String getDescription() {
            return (
                "(" +
                left.getDescription() +
                " OR " +
                right.getDescription() +
                ")"
            );
        }
    }

    /**
     * Logical NOT of a condition.
     */
    record Not(EdgeCondition condition) implements EdgeCondition {
        /**
         * Creates a new NOT edge condition.
         * 
         * @param condition the condition to negate
         * @throws NullPointerException if condition is null
         */
        public Not {
            Objects.requireNonNull(condition, "Condition cannot be null");
        }

        @Override
        public boolean evaluate(GraphWorkflowState<?> state) {
            return !condition.evaluate(state);
        }

        @Override
        public String getDescription() {
            return "NOT(" + condition.getDescription() + ")";
        }
    }

    // Factory methods for common conditions

    /**
     * Creates a condition that always allows traversal.
     *
     * @return An Always condition
     */
    static EdgeCondition always() {
        return new Always();
    }

    /**
     * Creates a condition that never allows traversal.
     *
     * @return A Never condition
     */
    static EdgeCondition never() {
        return new Never();
    }

    /**
     * Creates a condition based on a predicate.
     *
     * @param predicate The predicate to evaluate
     * @param description Human-readable description
     * @return A Conditional edge condition
     */
    static EdgeCondition when(
        Predicate<GraphWorkflowState<?>> predicate,
        String description
    ) {
        return new Conditional(predicate, description);
    }

    /**
     * Creates a condition based on a context value.
     *
     * @param contextKey The context key to check
     * @param valuePredicate The predicate to apply to the context value
     * @param description Human-readable description
     * @param <T> The type of the context value
     * @return A ContextBased edge condition
     */
    static <T> EdgeCondition whenContext(
        dev.agents4j.api.context.ContextKey<T> contextKey,
        Predicate<T> valuePredicate,
        String description
    ) {
        return new ContextBased<>(contextKey, valuePredicate, description);
    }

    /**
     * Creates a condition that checks if a context value equals a specific value.
     *
     * @param contextKey The context key to check
     * @param expectedValue The expected value
     * @param <T> The type of the context value
     * @return A ContextBased edge condition
     */
    static <T> EdgeCondition whenContextEquals(
        dev.agents4j.api.context.ContextKey<T> contextKey,
        T expectedValue
    ) {
        return new ContextBased<>(
            contextKey,
            value -> Objects.equals(value, expectedValue),
            contextKey.name() + " equals " + expectedValue
        );
    }

    /**
     * Creates a condition that checks if a numeric context value is greater than a threshold.
     *
     * @param contextKey The context key to check
     * @param threshold The threshold value
     * @return A ContextBased edge condition
     */
    static EdgeCondition whenContextGreaterThan(
        dev.agents4j.api.context.ContextKey<? extends Number> contextKey,
        Number threshold
    ) {
        return new ContextBased<>(
            contextKey,
            value -> value.doubleValue() > threshold.doubleValue(),
            contextKey.name() + " > " + threshold
        );
    }

    /**
     * Creates a condition that checks if a numeric context value is less than a threshold.
     *
     * @param contextKey The context key to check
     * @param threshold The threshold value
     * @return A ContextBased edge condition
     */
    static EdgeCondition whenContextLessThan(
        dev.agents4j.api.context.ContextKey<? extends Number> contextKey,
        Number threshold
    ) {
        return new ContextBased<>(
            contextKey,
            value -> value.doubleValue() < threshold.doubleValue(),
            contextKey.name() + " < " + threshold
        );
    }

    /**
     * Creates a condition that checks if the workflow has visited a specific node.
     *
     * @param nodeId The node ID to check
     * @return A Conditional edge condition
     */
    static EdgeCondition whenVisited(NodeId nodeId) {
        return new Conditional(
            state -> state.hasVisited(nodeId),
            "has visited " + nodeId.value()
        );
    }

    /**
     * Creates a condition that checks if the workflow has NOT visited a specific node.
     *
     * @param nodeId The node ID to check
     * @return A Conditional edge condition
     */
    static EdgeCondition whenNotVisited(NodeId nodeId) {
        return new Conditional(
            state -> !state.hasVisited(nodeId),
            "has not visited " + nodeId.value()
        );
    }

    /**
     * Creates a condition that checks if the workflow depth is within a range.
     *
     * @param maxDepth The maximum allowed depth
     * @return A Conditional edge condition
     */
    static EdgeCondition whenDepthLessThan(int maxDepth) {
        return new Conditional(
            state -> state.getDepth() < maxDepth,
            "depth < " + maxDepth
        );
    }

    // Combinators

    /**
     * Combines this condition with another using logical AND.
     *
     * @param other The other condition
     * @return A new And condition
     */
    default EdgeCondition and(EdgeCondition other) {
        return new And(this, other);
    }

    /**
     * Combines this condition with another using logical OR.
     *
     * @param other The other condition
     * @return A new Or condition
     */
    default EdgeCondition or(EdgeCondition other) {
        return new Or(this, other);
    }

    /**
     * Negates this condition.
     *
     * @return A new Not condition
     */
    default EdgeCondition not() {
        return new Not(this);
    }
}
