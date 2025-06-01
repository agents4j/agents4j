package dev.agents4j.api.graph;

import dev.agents4j.api.context.WorkflowContext;
import java.util.Optional;

/**
 * Commands specific to graph workflow navigation and execution.
 * Provides type-safe operations for controlling graph traversal and execution flow.
 */
public sealed interface GraphCommand<S>
    permits
        GraphCommandTraverse,
        GraphCommandFork,
        GraphCommandJoin,
        GraphCommandSuspend,
        GraphCommandComplete,
        GraphCommandError {
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
     * @return Optional state data updates
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
        return getStateData().isPresent();
    }
}