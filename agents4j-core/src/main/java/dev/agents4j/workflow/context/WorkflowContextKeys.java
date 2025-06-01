package dev.agents4j.workflow.context;

import dev.agents4j.api.context.ContextKey;
import java.time.Instant;

/**
 * Context keys used by the workflow implementation.
 * These keys are used to store and retrieve values from the workflow context.
 */
public class WorkflowContextKeys {

    /**
     * The workflow ID.
     */
    public static final ContextKey<String> WORKFLOW_ID =
        ContextKey.of("workflow.id", String.class);

    /**
     * The workflow name.
     */
    public static final ContextKey<String> WORKFLOW_NAME =
        ContextKey.of("workflow.name", String.class);

    /**
     * The time when the workflow was started.
     */
    public static final ContextKey<Instant> WORKFLOW_START_TIME =
        ContextKey.of("workflow.startTime", Instant.class);

    /**
     * The time when the workflow was last resumed.
     */
    public static final ContextKey<Instant> WORKFLOW_RESUMED_TIME =
        ContextKey.of("workflow.resumedTime", Instant.class);

    /**
     * The number of times the workflow has been resumed.
     */
    public static final ContextKey<Integer> WORKFLOW_RESUMED_COUNT =
        ContextKey.of("workflow.resumedCount", Integer.class);

    /**
     * The ID of the last traversed edge.
     */
    public static final ContextKey<String> LAST_EDGE_ID =
        ContextKey.of("workflow.lastEdgeId", String.class);

    /**
     * The time when the last edge was traversed.
     */
    public static final ContextKey<Instant> LAST_EDGE_TIME =
        ContextKey.of("workflow.lastEdgeTime", Instant.class);

    // Private constructor to prevent instantiation
    private WorkflowContextKeys() {}
}