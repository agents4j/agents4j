package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.error.WorkflowError;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Commands specific to graph workflow navigation and execution.
 * Provides type-safe operations for controlling graph traversal and execution flow.
 */
public sealed interface GraphCommand<S>
    permits
        GraphCommand.Traverse,
        GraphCommand.Fork,
        GraphCommand.Join,
        GraphCommand.Suspend,
        GraphCommand.Complete,
        GraphCommand.Error {
    /**
     * Gets optional context updates associated with this command.
     *
     * @return Optional context updates
     */
    default Optional<WorkflowContext> getContextUpdates() {
        return Optional.empty();
    }

    /**
     * Gets optional state data updates associated with this command.
     *
     * @return Optional state data
     */
    default Optional<S> getStateData() {
        return Optional.empty();
    }

    /**
     * Checks if this command modifies the workflow state.
     *
     * @return true if the command modifies state
     */
    default boolean modifiesState() {
        return getContextUpdates().isPresent() || getStateData().isPresent();
    }

    /**
     * Command to traverse to a specific node in the graph.
     */
    record Traverse<S>(
        NodeId targetNode,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData,
        Optional<EdgeCondition> condition,
        String reason
    )
        implements GraphCommand<S> {
        public Traverse {
            Objects.requireNonNull(targetNode, "Target node cannot be null");
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );
            Objects.requireNonNull(
                condition,
                "Condition optional cannot be null"
            );
            reason = reason != null
                ? reason
                : "Traverse to " + targetNode.value();
        }

        public static <S> Traverse<S> to(NodeId nodeId) {
            return new Traverse<>(
                nodeId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Traverse<S> toWithContext(
            NodeId nodeId,
            WorkflowContext context
        ) {
            return new Traverse<>(
                nodeId,
                Optional.of(context),
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Traverse<S> toWithData(NodeId nodeId, S stateData) {
            return new Traverse<>(
                nodeId,
                Optional.empty(),
                Optional.of(stateData),
                Optional.empty(),
                null
            );
        }

        public static <S> Traverse<S> toWithCondition(
            NodeId nodeId,
            EdgeCondition condition
        ) {
            return new Traverse<>(
                nodeId,
                Optional.empty(),
                Optional.empty(),
                Optional.of(condition),
                null
            );
        }

        public static <S> Traverse<S> toWithUpdates(
            NodeId nodeId,
            WorkflowContext context,
            S stateData
        ) {
            return new Traverse<>(
                nodeId,
                Optional.of(context),
                Optional.of(stateData),
                Optional.empty(),
                null
            );
        }

        public static <S> Traverse<S> toWithReason(
            NodeId nodeId,
            String reason
        ) {
            return new Traverse<>(
                nodeId,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                reason
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }

    /**
     * Command to fork execution to multiple parallel nodes.
     */
    record Fork<S>(
        Set<NodeId> targetNodes,
        ForkStrategy strategy,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData,
        String reason
    )
        implements GraphCommand<S> {
        public Fork {
            Objects.requireNonNull(targetNodes, "Target nodes cannot be null");
            Objects.requireNonNull(strategy, "Fork strategy cannot be null");
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );

            if (targetNodes.isEmpty()) {
                throw new IllegalArgumentException(
                    "Target nodes cannot be empty"
                );
            }

            reason = reason != null
                ? reason
                : "Fork to " + targetNodes.size() + " nodes";

            // Make target nodes immutable
            targetNodes = Set.copyOf(targetNodes);
        }

        public enum ForkStrategy {
            PARALLEL, // Execute all branches in parallel
            SEQUENTIAL, // Execute branches one after another
            CONDITIONAL, // Execute based on conditions
        }

        public static <S> Fork<S> parallel(Set<NodeId> nodes) {
            return new Fork<>(
                nodes,
                ForkStrategy.PARALLEL,
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Fork<S> sequential(Set<NodeId> nodes) {
            return new Fork<>(
                nodes,
                ForkStrategy.SEQUENTIAL,
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Fork<S> conditional(Set<NodeId> nodes) {
            return new Fork<>(
                nodes,
                ForkStrategy.CONDITIONAL,
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Fork<S> parallelWithContext(
            Set<NodeId> nodes,
            WorkflowContext context
        ) {
            return new Fork<>(
                nodes,
                ForkStrategy.PARALLEL,
                Optional.of(context),
                Optional.empty(),
                null
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }

    /**
     * Command to join multiple parallel executions.
     */
    record Join<S>(
        NodeId joinNode,
        JoinStrategy strategy,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData,
        Optional<Duration> timeout,
        String reason
    )
        implements GraphCommand<S> {
        public Join {
            Objects.requireNonNull(joinNode, "Join node cannot be null");
            Objects.requireNonNull(strategy, "Join strategy cannot be null");
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );
            Objects.requireNonNull(timeout, "Timeout optional cannot be null");
            reason = reason != null ? reason : "Join at " + joinNode.value();
        }

        public enum JoinStrategy {
            WAIT_ALL, // Wait for all branches to complete
            WAIT_ANY, // Proceed when any branch completes
            WAIT_MAJORITY, // Proceed when majority complete
        }

        public static <S> Join<S> waitAll(NodeId joinNode) {
            return new Join<>(
                joinNode,
                JoinStrategy.WAIT_ALL,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Join<S> waitAny(NodeId joinNode) {
            return new Join<>(
                joinNode,
                JoinStrategy.WAIT_ANY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Join<S> waitMajority(NodeId joinNode) {
            return new Join<>(
                joinNode,
                JoinStrategy.WAIT_MAJORITY,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Join<S> waitAllWithTimeout(
            NodeId joinNode,
            Duration timeout
        ) {
            return new Join<>(
                joinNode,
                JoinStrategy.WAIT_ALL,
                Optional.empty(),
                Optional.empty(),
                Optional.of(timeout),
                null
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }

    /**
     * Command to suspend execution at the current node.
     */
    record Suspend<S>(
        String suspensionId,
        String reason,
        Optional<Duration> timeout,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData
    )
        implements GraphCommand<S> {
        public Suspend {
            Objects.requireNonNull(
                suspensionId,
                "Suspension ID cannot be null"
            );
            Objects.requireNonNull(reason, "Suspension reason cannot be null");
            Objects.requireNonNull(timeout, "Timeout optional cannot be null");
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );

            if (suspensionId.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Suspension ID cannot be empty"
                );
            }
            if (reason.trim().isEmpty()) {
                throw new IllegalArgumentException(
                    "Suspension reason cannot be empty"
                );
            }
        }

        public static <S> Suspend<S> withId(
            String suspensionId,
            String reason
        ) {
            return new Suspend<>(
                suspensionId,
                reason,
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
        }

        public static <S> Suspend<S> withTimeout(
            String suspensionId,
            String reason,
            Duration timeout
        ) {
            return new Suspend<>(
                suspensionId,
                reason,
                Optional.of(timeout),
                Optional.empty(),
                Optional.empty()
            );
        }

        public static <S> Suspend<S> withContext(
            String suspensionId,
            String reason,
            WorkflowContext context
        ) {
            return new Suspend<>(
                suspensionId,
                reason,
                Optional.empty(),
                Optional.of(context),
                Optional.empty()
            );
        }

        public static <S> Suspend<S> withData(
            String suspensionId,
            String reason,
            S stateData
        ) {
            return new Suspend<>(
                suspensionId,
                reason,
                Optional.empty(),
                Optional.empty(),
                Optional.of(stateData)
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }

    /**
     * Command to complete the workflow execution.
     */
    record Complete<S>(
        Object result,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData,
        String reason
    )
        implements GraphCommand<S> {
        public Complete {
            Objects.requireNonNull(result, "Result cannot be null");
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );
            reason = reason != null
                ? reason
                : "Workflow completed successfully";
        }

        public static <S> Complete<S> withResult(Object result) {
            return new Complete<>(
                result,
                Optional.empty(),
                Optional.empty(),
                null
            );
        }

        public static <S> Complete<S> withResultAndContext(
            Object result,
            WorkflowContext context
        ) {
            return new Complete<>(
                result,
                Optional.of(context),
                Optional.empty(),
                null
            );
        }

        public static <S> Complete<S> withResultAndData(
            Object result,
            S stateData
        ) {
            return new Complete<>(
                result,
                Optional.empty(),
                Optional.of(stateData),
                null
            );
        }

        public static <S> Complete<S> withAll(
            Object result,
            WorkflowContext context,
            S stateData,
            String reason
        ) {
            return new Complete<>(
                result,
                Optional.of(context),
                Optional.of(stateData),
                reason
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }

    /**
     * Command to indicate an error occurred during graph execution.
     */
    record Error<S>(
        WorkflowError error,
        Optional<NodeId> fallbackNode,
        Optional<WorkflowContext> contextUpdates,
        Optional<S> stateData,
        boolean isRecoverable
    )
        implements GraphCommand<S> {
        public Error {
            Objects.requireNonNull(error, "Error cannot be null");
            Objects.requireNonNull(
                fallbackNode,
                "Fallback node optional cannot be null"
            );
            Objects.requireNonNull(
                contextUpdates,
                "Context updates optional cannot be null"
            );
            Objects.requireNonNull(
                stateData,
                "State data optional cannot be null"
            );
        }

        public static <S> Error<S> of(WorkflowError error) {
            return new Error<>(
                error,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                error.isRecoverable()
            );
        }

        public static <S> Error<S> withFallback(
            WorkflowError error,
            NodeId fallbackNode
        ) {
            return new Error<>(
                error,
                Optional.of(fallbackNode),
                Optional.empty(),
                Optional.empty(),
                true
            );
        }

        public static <S> Error<S> recoverable(WorkflowError error) {
            return new Error<>(
                error,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true
            );
        }

        public static <S> Error<S> fatal(WorkflowError error) {
            return new Error<>(
                error,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false
            );
        }

        public static <S> Error<S> withContext(
            WorkflowError error,
            WorkflowContext context
        ) {
            return new Error<>(
                error,
                Optional.empty(),
                Optional.of(context),
                Optional.empty(),
                error.isRecoverable()
            );
        }

        @Override
        public Optional<WorkflowContext> getContextUpdates() {
            return contextUpdates;
        }

        @Override
        public Optional<S> getStateData() {
            return stateData;
        }
    }
}
