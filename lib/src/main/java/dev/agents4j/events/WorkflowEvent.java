/*
 * Agents4J Library - A framework for AI Agent Workflows using LangChain4J
 */
package dev.agents4j.events;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all workflow events.
 */
public abstract class WorkflowEvent {
    private final String eventId;
    private final String workflowName;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    protected WorkflowEvent(String workflowName, Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID().toString();
        this.workflowName = workflowName;
        this.timestamp = Instant.now();
        this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public String getEventId() {
        return eventId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public abstract String getEventType();
}

/**
 * Event fired when a workflow starts execution.
 */
class WorkflowStartedEvent extends WorkflowEvent {
    private final Object input;

    public WorkflowStartedEvent(String workflowName, Object input, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_STARTED";
    }
}

/**
 * Event fired when a workflow completes successfully.
 */
class WorkflowCompletedEvent extends WorkflowEvent {
    private final Object output;
    private final long executionTimeMs;

    public WorkflowCompletedEvent(String workflowName, Object output, long executionTimeMs, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.output = output;
        this.executionTimeMs = executionTimeMs;
    }

    public Object getOutput() {
        return output;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_COMPLETED";
    }
}

/**
 * Event fired when a workflow fails.
 */
class WorkflowFailedEvent extends WorkflowEvent {
    private final Throwable error;
    private final long executionTimeMs;

    public WorkflowFailedEvent(String workflowName, Throwable error, long executionTimeMs, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public Throwable getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_FAILED";
    }
}

/**
 * Event fired when a workflow is cancelled.
 */
class WorkflowCancelledEvent extends WorkflowEvent {
    private final String reason;
    private final long executionTimeMs;

    public WorkflowCancelledEvent(String workflowName, String reason, long executionTimeMs, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.reason = reason;
        this.executionTimeMs = executionTimeMs;
    }

    public String getReason() {
        return reason;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String getEventType() {
        return "WORKFLOW_CANCELLED";
    }
}

/**
 * Event fired when a node starts executing.
 */
class NodeStartingEvent extends WorkflowEvent {
    private final String nodeName;
    private final Object input;

    public NodeStartingEvent(String workflowName, String nodeName, Object input, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.nodeName = nodeName;
        this.input = input;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Object getInput() {
        return input;
    }

    @Override
    public String getEventType() {
        return "NODE_STARTING";
    }
}

/**
 * Event fired when a node completes execution.
 */
class NodeExecutionEvent extends WorkflowEvent {
    private final String nodeName;
    private final Object input;
    private final Object output;
    private final long executionTimeMs;

    public NodeExecutionEvent(String workflowName, String nodeName, Object input, Object output, long executionTimeMs, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.nodeName = nodeName;
        this.input = input;
        this.output = output;
        this.executionTimeMs = executionTimeMs;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Object getInput() {
        return input;
    }

    public Object getOutput() {
        return output;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String getEventType() {
        return "NODE_EXECUTED";
    }
}

/**
 * Event fired when a node fails.
 */
class NodeFailedEvent extends WorkflowEvent {
    private final String nodeName;
    private final Object input;
    private final Throwable error;
    private final long executionTimeMs;

    public NodeFailedEvent(String workflowName, String nodeName, Object input, Throwable error, long executionTimeMs, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.nodeName = nodeName;
        this.input = input;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Object getInput() {
        return input;
    }

    public Throwable getError() {
        return error;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public String getEventType() {
        return "NODE_FAILED";
    }
}

/**
 * Event fired when workflow context is updated.
 */
class ContextUpdatedEvent extends WorkflowEvent {
    private final String key;
    private final Object oldValue;
    private final Object newValue;

    public ContextUpdatedEvent(String workflowName, String key, Object oldValue, Object newValue, Map<String, Object> metadata) {
        super(workflowName, metadata);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getKey() {
        return key;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public String getEventType() {
        return "CONTEXT_UPDATED";
    }
}