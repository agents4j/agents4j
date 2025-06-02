package dev.agents4j.api.result;

import dev.agents4j.api.context.*;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Sealed interface representing the result of a workflow operation.
 * Provides functional error handling with type safety and monadic operations.
 *
 * @param <T> The type of the success value
 * @param <E> The type of the error, must extend WorkflowError
 */
public sealed interface WorkflowResult<T, E extends WorkflowError>
    permits
        WorkflowResult.Success,
        WorkflowResult.Failure,
        WorkflowResult.Suspended {
    /**
     * Represents a successful workflow operation.
     *
     * @param <T> The type of the success value
     * @param <E> The type of the error
     */
    record Success<T, E extends WorkflowError>(
        T value,
        WorkflowContext finalContext
    )
        implements WorkflowResult<T, E> {
        public Success {
            Objects.requireNonNull(value, "Success value cannot be null");
            Objects.requireNonNull(
                finalContext,
                "Final context cannot be null"
            );
        }
    }

    /**
     * Represents a failed workflow operation.
     *
     * @param <T> The type of the success value
     * @param <E> The type of the error
     */
    record Failure<T, E extends WorkflowError>(
        E error,
        T value,
        WorkflowContext finalContext
    )
        implements WorkflowResult<T, E> {
        public Failure {
            Objects.requireNonNull(error, "Error cannot be null");
            // finalContext can be null for failures that occur before workflow state is established
        }
    }

    /**
     * Represents a suspended workflow operation that can be resumed later.
     *
     * @param <T> The type of the success value
     * @param <E> The type of the error
     * @deprecated Use {@link dev.agents4j.api.enhanced.WorkflowSuspension} for type-safe suspension handling.
     * This record will be removed in version 2.0.0. The {@code suspensionState} field returns an untyped Object,
     * which requires unsafe casting and can cause ClassCastException at runtime.
     *
     * Migration path:
     * 1. Use {@link dev.agents4j.api.enhanced.EnhancedGraphWorkflow#extractTypedSuspension(WorkflowResult)}
     * 2. Or upgrade to {@link dev.agents4j.api.enhanced.EnhancedGraphWorkflow} interface
     *
     * @see dev.agents4j.api.enhanced.WorkflowSuspension
     * @see dev.agents4j.api.enhanced.EnhancedGraphWorkflow
     */
    @Deprecated(since = "0.3.0", forRemoval = true)
    record Suspended<T, E extends WorkflowError>(
        String suspensionId,
        Object suspensionState,
        String reason
    )
        implements WorkflowResult<T, E> {
        public Suspended {
            Objects.requireNonNull(
                suspensionId,
                "Suspension ID cannot be null"
            );
            Objects.requireNonNull(reason, "Suspension reason cannot be null");
        }
    }

    /**
     * Creates a successful result.
     *
     * @param value The success value
     * @param <T> The type of the success value
     * @param <E> The type of the error
     * @return A Success result
     */
    static <T, E extends WorkflowError> WorkflowResult<T, E> success(T value) {
        return new Success<>(value, WorkflowContext.empty());
    }

    /**
     * Creates a successful result with final context.
     *
     * @param value The success value
     * @param finalContext The final workflow context
     * @param <T> The type of the success value
     * @param <E> The type of the error
     * @return A Success result
     */
    static <T, E extends WorkflowError> WorkflowResult<T, E> success(
        T value,
        WorkflowContext finalContext
    ) {
        return new Success<>(value, finalContext);
    }

    /**
     * Creates a failed result.
     *
     * @param error The error
     * @param <T> The type of the success value
     * @param <E> The type of the error
     * @return A Failure result
     */
    static <T, E extends WorkflowError> WorkflowResult<T, E> failure(E error) {
        return new Failure<>(error, null, null);
    }

    static <T, E extends WorkflowError> WorkflowResult<T, E> failure(
        E error,
        T value
    ) {
        return new Failure<>(error, value, null);
    }

    static <T, E extends WorkflowError> WorkflowResult<T, E> failure(
        E error,
        T value,
        WorkflowContext finalContext
    ) {
        return new Failure<>(error, value, finalContext);
    }

    /**
     * Creates a suspended result.
     *
     * @param suspensionId The suspension identifier
     * @param suspensionState The suspended state
     * @param reason The suspension reason
     * @param <T> The type of the success value
     * @param <E> The type of the error
     * @return A Suspended result
     * @deprecated Use {@link dev.agents4j.api.enhanced.EnhancedGraphWorkflow#createSuspension(Object, String)}
     * to create type-safe suspensions. This method creates an untyped suspension that requires unsafe casting.
     * Will be removed in version 2.0.0.
     *
     * Migration example:
     * <pre>{@code
     * // Old way (unsafe):
     * WorkflowResult<T, E> result = WorkflowResult.suspended("id", state, "reason");
     *
     * // New way (type-safe):
     * EnhancedGraphWorkflow<T, O> workflow = ...;
     * WorkflowSuspension<GraphWorkflowState<T>> suspension =
     *     workflow.createSuspension(state, "reason");
     * }</pre>
     */
    @Deprecated(since = "0.3.0", forRemoval = true)
    static <T, E extends WorkflowError> WorkflowResult<T, E> suspended(
        String suspensionId,
        Object suspensionState,
        String reason
    ) {
        return new Suspended<>(suspensionId, suspensionState, reason);
    }

    /**
     * Checks if this result represents a success.
     *
     * @return true if this is a Success
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if this result represents a failure.
     *
     * @return true if this is a Failure
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Checks if this result represents a suspension.
     *
     * @return true if this is a Suspended
     */
    default boolean isSuspended() {
        return this instanceof Suspended;
    }

    /**
     * Gets the success value if present.
     *
     * @return Optional containing the success value
     */
    default Optional<T> getValue() {
        if (this instanceof Success) {
            return Optional.of(((Success<T, E>) this).value());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the final workflow context if present.
     *
     * @return Optional containing the final context
     */
    default Optional<WorkflowContext> getFinalContext() {
        if (this instanceof Success) {
            return Optional.of(((Success<T, E>) this).finalContext());
        } else if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            return Optional.ofNullable(failure.finalContext());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the error if present.
     *
     * @return Optional containing the error
     */
    default Optional<E> getError() {
        if (this instanceof Failure) {
            return Optional.of(((Failure<T, E>) this).error());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the suspension details if suspended.
     *
     * @return Optional containing the Suspended record
     */
    default Optional<Suspended<T, E>> getSuspension() {
        if (this instanceof Suspended) {
            return Optional.of((Suspended<T, E>) this);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Maps the success value to a new type.
     *
     * @param mapper The mapping function
     * @param <U> The new type
     * @return A new WorkflowResult with the mapped value
     */
    default <U> WorkflowResult<U, E> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return WorkflowResult.success(
                mapper.apply(success.value()),
                success.finalContext()
            );
        } else if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            return WorkflowResult.failure(
                failure.error(),
                null,
                failure.finalContext()
            );
        } else if (this instanceof Suspended) {
            Suspended<T, E> suspended = (Suspended<T, E>) this;
            return WorkflowResult.suspended(
                suspended.suspensionId(),
                suspended.suspensionState(),
                suspended.reason()
            );
        } else {
            throw new IllegalStateException("Unknown WorkflowResult type");
        }
    }

    /**
     * FlatMaps the success value to a new WorkflowResult.
     *
     * @param mapper The mapping function that returns a WorkflowResult
     * @param <U> The new type
     * @return The result of applying the mapper
     */
    default <U> WorkflowResult<U, E> flatMap(
        Function<T, WorkflowResult<U, E>> mapper
    ) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return mapper.apply(success.value());
        } else if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            return WorkflowResult.failure(
                failure.error(),
                null,
                failure.finalContext()
            );
        } else if (this instanceof Suspended) {
            Suspended<T, E> suspended = (Suspended<T, E>) this;
            return WorkflowResult.suspended(
                suspended.suspensionId(),
                suspended.suspensionState(),
                suspended.reason()
            );
        } else {
            throw new IllegalStateException("Unknown WorkflowResult type");
        }
    }

    /**
     * Maps the error to a new type.
     *
     * @param mapper The error mapping function
     * @param <F> The new error type
     * @return A new WorkflowResult with the mapped error
     */
    default <F extends WorkflowError> WorkflowResult<T, F> mapError(
        Function<E, F> mapper
    ) {
        Objects.requireNonNull(mapper, "Error mapper function cannot be null");
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return WorkflowResult.success(
                success.value(),
                success.finalContext()
            );
        } else if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            return WorkflowResult.failure(
                mapper.apply(failure.error()),
                failure.value(),
                failure.finalContext()
            );
        } else if (this instanceof Suspended) {
            Suspended<T, E> suspended = (Suspended<T, E>) this;
            return WorkflowResult.suspended(
                suspended.suspensionId(),
                suspended.suspensionState(),
                suspended.reason()
            );
        } else {
            throw new IllegalStateException("Unknown WorkflowResult type");
        }
    }

    /**
     * Recovers from a failure using a recovery function.
     *
     * @param recovery The recovery function
     * @return A new WorkflowResult with recovered value or original result
     */
    default WorkflowResult<T, E> recover(Function<E, T> recovery) {
        Objects.requireNonNull(recovery, "Recovery function cannot be null");
        if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            WorkflowContext context = failure.finalContext() != null
                ? failure.finalContext()
                : WorkflowContext.empty();
            return WorkflowResult.success(
                recovery.apply(failure.error()),
                context
            );
        } else {
            return this;
        }
    }

    /**
     * Recovers from a failure using a recovery function that returns a WorkflowResult.
     *
     * @param recovery The recovery function
     * @return The recovered result or original result
     */
    default WorkflowResult<T, E> recoverWith(
        Function<E, WorkflowResult<T, E>> recovery
    ) {
        Objects.requireNonNull(recovery, "Recovery function cannot be null");
        if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            return recovery.apply(failure.error());
        } else {
            return this;
        }
    }

    /**
     * Filters the success value with a predicate.
     *
     * @param predicate The predicate to test the value
     * @param errorSupplier Supplier for error if predicate fails
     * @return The original result if predicate passes or not applicable, otherwise a failure
     */
    default WorkflowResult<T, E> filter(
        Predicate<T> predicate,
        Supplier<E> errorSupplier
    ) {
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        Objects.requireNonNull(errorSupplier, "Error supplier cannot be null");
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return predicate.test(success.value())
                ? this
                : WorkflowResult.failure(
                    errorSupplier.get(),
                    null,
                    success.finalContext()
                );
        } else {
            return this;
        }
    }

    /**
     * Performs an action on the success value if present.
     *
     * @param action The action to perform
     * @return This result for chaining
     */
    default WorkflowResult<T, E> onSuccess(Consumer<T> action) {
        Objects.requireNonNull(action, "Action cannot be null");
        if (this instanceof Success<T, E> success) {
            action.accept(success.value());
        }
        return this;
    }

    /**
     * Performs an action on the error if present.
     *
     * @param action The action to perform
     * @return This result for chaining
     */
    default WorkflowResult<T, E> onFailure(Consumer<E> action) {
        Objects.requireNonNull(action, "Action cannot be null");
        if (this instanceof Failure<T, E> failure) {
            action.accept(failure.error());
        }
        return this;
    }

    /**
     * Performs an action on the suspension if present.
     *
     * @param action The action to perform
     * @return This result for chaining
     */
    default WorkflowResult<T, E> onSuspension(
        Consumer<Suspended<T, E>> action
    ) {
        Objects.requireNonNull(action, "Action cannot be null");
        if (this instanceof Suspended<T, E> suspended) {
            action.accept(suspended);
        }
        return this;
    }

    /**
     * Gets the value or throws an exception.
     *
     * @return The success value
     * @throws WorkflowExecutionException if this is a failure or suspension
     */
    default T getOrThrow() {
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return success.value();
        } else if (this instanceof Failure) {
            Failure<T, E> failure = (Failure<T, E>) this;
            throw new WorkflowExecutionException(
                "Operation failed: " + failure.error().message(),
                failure.error()
            );
        } else if (this instanceof Suspended) {
            Suspended<T, E> suspended = (Suspended<T, E>) this;
            throw new WorkflowExecutionException(
                "Operation suspended: " + suspended.reason()
            );
        } else {
            throw new IllegalStateException("Unknown WorkflowResult type");
        }
    }

    /**
     * Gets the value or a default.
     *
     * @param defaultValue The default value
     * @return The success value or default
     */
    default T getOrElse(T defaultValue) {
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return success.value();
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the value or computes one from a supplier.
     *
     * @param supplier The supplier for the default value
     * @return The success value or computed default
     */
    default T getOrElse(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "Supplier cannot be null");
        if (this instanceof Success) {
            Success<T, E> success = (Success<T, E>) this;
            return success.value();
        } else {
            return supplier.get();
        }
    }

    /**
     * Combines two results using a binary function.
     *
     * @param other The other result
     * @param combiner The combining function
     * @param <U> The type of the other result's value
     * @param <V> The type of the combined result
     * @return The combined result
     */
    default <U, V> WorkflowResult<V, E> combine(
        WorkflowResult<U, E> other,
        Function<T, Function<U, V>> combiner
    ) {
        Objects.requireNonNull(other, "Other result cannot be null");
        Objects.requireNonNull(combiner, "Combiner function cannot be null");

        return this.flatMap(thisValue ->
                other.map(otherValue ->
                    combiner.apply(thisValue).apply(otherValue)
                )
            );
    }

    /**
     * Exception thrown when getting value from a failed or suspended result.
     */
    class WorkflowExecutionException extends RuntimeException {

        private final WorkflowError workflowError;

        public WorkflowExecutionException(String message) {
            super(message);
            this.workflowError = null;
        }

        public WorkflowExecutionException(String message, WorkflowError error) {
            super(message);
            this.workflowError = error;
        }

        public Optional<WorkflowError> getWorkflowError() {
            return Optional.ofNullable(workflowError);
        }
    }
}
