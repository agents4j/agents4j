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
 * @param <I> The input type
 * @param <O> The output type
 */
public class ExecutionResult<I, O> {
    
    public enum ResultType {
        COMPLETED,
        SUSPENDED,
        CONTINUE,
        GOTO,
        ERROR
    }
    
    private final ResultType type;
    private final O output;
    private final WorkflowState state;
    private final I nextInput;
    private final String targetNodeId;
    private final WorkflowExecutionException error;
    
    private ExecutionResult(ResultType type, O output, WorkflowState state, 
                           I nextInput, String targetNodeId, WorkflowExecutionException error) {
        this.type = Objects.requireNonNull(type);
        this.output = output;
        this.state = state;
        this.nextInput = nextInput;
        this.targetNodeId = targetNodeId;
        this.error = error;
    }
    
    // Factory methods
    public static <I, O> ExecutionResult<I, O> completed(O output, WorkflowState state) {
        return new ExecutionResult<>(ResultType.COMPLETED, output, state, null, null, null);
    }
    
    public static <I, O> ExecutionResult<I, O> suspended(WorkflowState state) {
        return new ExecutionResult<>(ResultType.SUSPENDED, null, state, null, null, null);
    }
    
    public static <I, O> ExecutionResult<I, O> continueWith(WorkflowState state, I nextInput) {
        return new ExecutionResult<>(ResultType.CONTINUE, null, state, nextInput, null, null);
    }
    
    public static <I, O> ExecutionResult<I, O> goTo(String targetNodeId, WorkflowState state, I nextInput) {
        return new ExecutionResult<>(ResultType.GOTO, null, state, nextInput, targetNodeId, null);
    }
    
    public static <I, O> ExecutionResult<I, O> failure(WorkflowExecutionException error) {
        return new ExecutionResult<>(ResultType.ERROR, null, null, null, null, error);
    }
    
    // Getters
    public ResultType getType() { 
        return type; 
    }
    
    public Optional<O> getOutput() { 
        return Optional.ofNullable(output); 
    }
    
    public WorkflowState getState() { 
        return state; 
    }
    
    public Optional<I> getNextInput() { 
        return Optional.ofNullable(nextInput); 
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
    public StatefulWorkflowResult<O> toWorkflowResult() {
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
                Objects.equals(nextInput, that.nextInput) &&
                Objects.equals(targetNodeId, that.targetNodeId) &&
                Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, output, state, nextInput, targetNodeId, error);
    }
}