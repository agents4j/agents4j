package dev.agents4j.workflow.api;

import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.exception.WorkflowExecutionException;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of a workflow execution step.
 * Provides better error handling and result composition.
 *
 * @param <S> The state type
 * @param <O> The output type
 */
public class ExecutionResult<S, O> {
    
    public enum ResultType {
        COMPLETED,
        SUSPENDED,
        CONTINUE,
        GOTO,
        ERROR
    }
    
    private final ResultType type;
    private final O output;
    private final WorkflowState<S> state;
    private final S nextStateData;
    private final String targetNodeId;
    private final WorkflowExecutionException error;
    
    private ExecutionResult(ResultType type, O output, WorkflowState<S> state, 
                           S nextStateData, String targetNodeId, WorkflowExecutionException error) {
        this.type = Objects.requireNonNull(type);
        this.output = output;
        this.state = state;
        this.nextStateData = nextStateData;
        this.targetNodeId = targetNodeId;
        this.error = error;
    }
    
    // Factory methods
    public static <S, O> ExecutionResult<S, O> completed(O output, WorkflowState<S> state) {
        return new ExecutionResult<>(ResultType.COMPLETED, output, state, null, null, null);
    }
    
    public static <S, O> ExecutionResult<S, O> suspended(WorkflowState<S> state) {
        return new ExecutionResult<>(ResultType.SUSPENDED, null, state, null, null, null);
    }
    
    public static <S, O> ExecutionResult<S, O> continueWith(WorkflowState<S> state, S nextStateData) {
        return new ExecutionResult<>(ResultType.CONTINUE, null, state, nextStateData, null, null);
    }
    
    public static <S, O> ExecutionResult<S, O> goTo(String targetNodeId, WorkflowState<S> state, S nextStateData) {
        return new ExecutionResult<>(ResultType.GOTO, null, state, nextStateData, targetNodeId, null);
    }
    
    public static <S, O> ExecutionResult<S, O> failure(WorkflowExecutionException error) {
        return new ExecutionResult<>(ResultType.ERROR, null, null, null, null, null);
    }
    
    // Getters
    public ResultType getType() { 
        return type; 
    }
    
    public Optional<O> getOutput() { 
        return Optional.ofNullable(output); 
    }
    
    public WorkflowState<S> getState() { 
        return state; 
    }
    
    public Optional<S> getNextStateData() { 
        return Optional.ofNullable(nextStateData); 
    }
    
    public Optional<String> getTargetNodeId() { 
        return Optional.ofNullable(targetNodeId); 
    }
    
    public Optional<WorkflowExecutionException> getError() { 
        return Optional.ofNullable(error); 
    }
    
    public boolean isSuccess() { 
        return type != ResultType.ERROR; 
    }
    
    public boolean isFailure() { 
        return type == ResultType.ERROR; 
    }
    
    /**
     * Converts this ExecutionResult to a StatefulWorkflowResult.
     *
     * @return A StatefulWorkflowResult representation of this execution result
     * @throws IllegalStateException if the result type cannot be converted
     */
    public StatefulWorkflowResult<S, O> toWorkflowResult() {
        switch (type) {
            case COMPLETED:
                return StatefulWorkflowResult.completed(output, state);
            case SUSPENDED:
                return StatefulWorkflowResult.suspended(state);
            case ERROR:
                return StatefulWorkflowResult.error(error.getMessage(), state);
            default:
                throw new IllegalStateException("Cannot convert " + type + " to StatefulWorkflowResult");
        }
    }
    
    @Override
    public String toString() {
        return String.format("ExecutionResult{type=%s, hasOutput=%s, hasError=%s}", 
                type, output != null, error != null);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResult<?, ?> that = (ExecutionResult<?, ?>) o;
        return type == that.type &&
                Objects.equals(output, that.output) &&
                Objects.equals(state, that.state) &&
                Objects.equals(nextStateData, that.nextStateData) &&
                Objects.equals(targetNodeId, that.targetNodeId) &&
                Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, output, state, nextStateData, targetNodeId, error);
    }
}