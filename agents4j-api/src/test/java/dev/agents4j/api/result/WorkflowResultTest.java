package dev.agents4j.api.result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class WorkflowResultTest {

    @Nested
    @DisplayName("Creation Tests")
    class CreationTests {

        @Test
        @DisplayName("Should create success result")
        void shouldCreateSuccessResult() {
            var result = WorkflowResult.success("test value");
            
            assertTrue(result.isSuccess());
            assertFalse(result.isFailure());
            assertFalse(result.isSuspended());
            assertEquals(Optional.of("test value"), result.getValue());
            assertEquals(Optional.empty(), result.getError());
        }

        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.failure(error);
            
            assertFalse(result.isSuccess());
            assertTrue(result.isFailure());
            assertFalse(result.isSuspended());
            assertEquals(Optional.empty(), result.getValue());
            assertEquals(Optional.of(error), result.getError());
        }

        @Test
        @DisplayName("Should create suspended result")
        void shouldCreateSuspendedResult() {
            var result = WorkflowResult.suspended("id-123", "state", "waiting for user");
            
            assertFalse(result.isSuccess());
            assertFalse(result.isFailure());
            assertTrue(result.isSuspended());
            assertEquals(Optional.empty(), result.getValue());
            assertEquals(Optional.empty(), result.getError());
            
            var suspension = result.getSuspension().orElseThrow();
            assertEquals("id-123", suspension.suspensionId());
            assertEquals("state", suspension.suspensionState());
            assertEquals("waiting for user", suspension.reason());
        }

        @Test
        @DisplayName("Should throw exception for null value in success")
        void shouldThrowExceptionForNullValueInSuccess() {
            assertThrows(NullPointerException.class, () -> 
                WorkflowResult.success(null));
        }

        @Test
        @DisplayName("Should throw exception for null error in failure")
        void shouldThrowExceptionForNullErrorInFailure() {
            assertThrows(NullPointerException.class, () -> 
                WorkflowResult.failure(null));
        }
    }

    @Nested
    @DisplayName("Map Operations Tests")
    class MapOperationsTests {

        @Test
        @DisplayName("Should map success value")
        void shouldMapSuccessValue() {
            var result = WorkflowResult.<String, WorkflowError>success("hello")
                .map(String::toUpperCase);
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("HELLO"), result.getValue());
        }

        @Test
        @DisplayName("Should not map failure value")
        void shouldNotMapFailureValue() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .map(String::toUpperCase);
            
            assertTrue(result.isFailure());
            assertEquals(Optional.of(error), result.getError());
        }

        @Test
        @DisplayName("Should not map suspended value")
        void shouldNotMapSuspendedValue() {
            var result = WorkflowResult.<String, WorkflowError>suspended("id", "state", "reason")
                .map(String::toUpperCase);
            
            assertTrue(result.isSuspended());
        }

        @Test
        @DisplayName("Should throw exception for null mapper")
        void shouldThrowExceptionForNullMapper() {
            var result = WorkflowResult.success("test");
            
            assertThrows(NullPointerException.class, () -> 
                result.map(null));
        }
    }

    @Nested
    @DisplayName("FlatMap Operations Tests")
    class FlatMapOperationsTests {

        @Test
        @DisplayName("Should flatMap success value")
        void shouldFlatMapSuccessValue() {
            var result = WorkflowResult.<String, WorkflowError>success("42")
                .flatMap(s -> {
                    try {
                        int value = Integer.parseInt(s);
                        return WorkflowResult.success(value);
                    } catch (NumberFormatException e) {
                        return WorkflowResult.failure(ValidationError.invalidFormat("number", s, "integer"));
                    }
                });
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of(42), result.getValue());
        }

        @Test
        @DisplayName("Should flatMap success to failure")
        void shouldFlatMapSuccessToFailure() {
            var result = WorkflowResult.<String, WorkflowError>success("not-a-number")
                .flatMap(s -> {
                    try {
                        int value = Integer.parseInt(s);
                        return WorkflowResult.success(value);
                    } catch (NumberFormatException e) {
                        return WorkflowResult.failure(ValidationError.invalidFormat("number", s, "integer"));
                    }
                });
            
            assertTrue(result.isFailure());
            var error = result.getError().orElseThrow();
            assertEquals("INVALID_FORMAT", error.code());
        }

        @Test
        @DisplayName("Should not flatMap failure value")
        void shouldNotFlatMapFailureValue() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .flatMap(s -> WorkflowResult.success(s.length()));
            
            assertTrue(result.isFailure());
            assertEquals(Optional.of(error), result.getError());
        }
    }

    @Nested
    @DisplayName("Error Mapping Tests")
    class ErrorMappingTests {

        @Test
        @DisplayName("Should map error type")
        void shouldMapErrorType() {
            var originalError = ValidationError.required("field");
            var result = WorkflowResult.<String, ValidationError>failure(originalError)
                .mapError(e -> ExecutionError.of("MAPPED_ERROR", e.message(), "node-1"));
            
            assertTrue(result.isFailure());
            var mappedError = result.getError().orElseThrow();
            assertEquals("MAPPED_ERROR", mappedError.code());
            assertEquals(originalError.message(), mappedError.message());
        }

        @Test
        @DisplayName("Should not map error for success")
        void shouldNotMapErrorForSuccess() {
            var result = WorkflowResult.<String, WorkflowError>success("test")
                .mapError(e -> ExecutionError.of("SHOULD_NOT_MAP", e.message(), "node"));
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("test"), result.getValue());
        }
    }

    @Nested
    @DisplayName("Recovery Operations Tests")
    class RecoveryOperationsTests {

        @Test
        @DisplayName("Should recover from failure")
        void shouldRecoverFromFailure() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .recover(e -> "default value");
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("default value"), result.getValue());
        }

        @Test
        @DisplayName("Should not recover from success")
        void shouldNotRecoverFromSuccess() {
            var result = WorkflowResult.<String, WorkflowError>success("original")
                .recover(e -> "recovery");
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("original"), result.getValue());
        }

        @Test
        @DisplayName("Should recover with WorkflowResult")
        void shouldRecoverWithWorkflowResult() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .recoverWith(e -> WorkflowResult.success("recovered"));
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("recovered"), result.getValue());
        }

        @Test
        @DisplayName("Should recover failure with another failure")
        void shouldRecoverFailureWithAnotherFailure() {
            var originalError = ValidationError.required("field");
            var recoveryError = ExecutionError.of("RECOVERY_FAILED", "Recovery failed", "node");
            
            var result = WorkflowResult.<String, WorkflowError>failure(originalError)
                .recoverWith(e -> WorkflowResult.failure(recoveryError));
            
            assertTrue(result.isFailure());
            assertEquals(Optional.of(recoveryError), result.getError());
        }
    }

    @Nested
    @DisplayName("Filter Operations Tests")
    class FilterOperationsTests {

        @Test
        @DisplayName("Should pass filter when predicate is true")
        void shouldPassFilterWhenPredicateIsTrue() {
            var result = WorkflowResult.<String, WorkflowError>success("hello")
                .filter(s -> s.length() > 3, () -> ValidationError.invalidFormat("text", "hello", "short"));
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of("hello"), result.getValue());
        }

        @Test
        @DisplayName("Should fail filter when predicate is false")
        void shouldFailFilterWhenPredicateIsFalse() {
            var result = WorkflowResult.<String, WorkflowError>success("hi")
                .filter(s -> s.length() > 3, () -> ValidationError.invalidFormat("text", "hi", "long"));
            
            assertTrue(result.isFailure());
            var error = result.getError().orElseThrow();
            assertEquals("INVALID_FORMAT", error.code());
        }

        @Test
        @DisplayName("Should not filter failure")
        void shouldNotFilterFailure() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .filter(s -> true, () -> dev.agents4j.api.result.ValidationError.invalidFormat("text", "value", "any"));
            
            assertTrue(result.isFailure());
            assertEquals(Optional.of(error), result.getError());
        }
    }

    @Nested
    @DisplayName("Side Effect Operations Tests")
    class SideEffectOperationsTests {

        @Test
        @DisplayName("Should execute action on success")
        void shouldExecuteActionOnSuccess() {
            var executed = new AtomicBoolean(false);
            var capturedValue = new AtomicReference<String>();
            
            var result = WorkflowResult.<String, WorkflowError>success("test")
                .onSuccess(value -> {
                    executed.set(true);
                    capturedValue.set(value);
                });
            
            assertTrue(executed.get());
            assertEquals("test", capturedValue.get());
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Should not execute success action on failure")
        void shouldNotExecuteSuccessActionOnFailure() {
            var executed = new AtomicBoolean(false);
            var error = ValidationError.required("field");
            
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .onSuccess(value -> executed.set(true));
            
            assertFalse(executed.get());
            assertTrue(result.isFailure());
        }

        @Test
        @DisplayName("Should execute action on failure")
        void shouldExecuteActionOnFailure() {
            var executed = new AtomicBoolean(false);
            var capturedError = new AtomicReference<WorkflowError>();
            var error = ValidationError.required("field");
            
            var result = WorkflowResult.<String, WorkflowError>failure(error)
                .onFailure(e -> {
                    executed.set(true);
                    capturedError.set(e);
                });
            
            assertTrue(executed.get());
            assertEquals(error, capturedError.get());
            assertTrue(result.isFailure());
        }

        @Test
        @DisplayName("Should execute action on suspension")
        void shouldExecuteActionOnSuspension() {
            var executed = new AtomicBoolean(false);
            var capturedSuspension = new AtomicReference<WorkflowResult.Suspended<String, WorkflowError>>();
            
            var result = WorkflowResult.<String, WorkflowError>suspended("id", "state", "reason")
                .onSuspension(suspension -> {
                    executed.set(true);
                    capturedSuspension.set(suspension);
                });
            
            assertTrue(executed.get());
            assertEquals("id", capturedSuspension.get().suspensionId());
            assertTrue(result.isSuspended());
        }
    }

    @Nested
    @DisplayName("Value Extraction Tests")
    class ValueExtractionTests {

        @Test
        @DisplayName("Should get value or throw for success")
        void shouldGetValueOrThrowForSuccess() {
            var result = WorkflowResult.<String, WorkflowError>success("test");
            
            assertEquals("test", result.getOrThrow());
        }

        @Test
        @DisplayName("Should throw exception for failure in getOrThrow")
        void shouldThrowExceptionForFailureInGetOrThrow() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error);
            
            var exception = assertThrows(WorkflowResult.WorkflowExecutionException.class, 
                result::getOrThrow);
            
            assertTrue(exception.getMessage().contains("Operation failed"));
            assertEquals(Optional.of(error), exception.getWorkflowError());
        }

        @Test
        @DisplayName("Should throw exception for suspension in getOrThrow")
        void shouldThrowExceptionForSuspensionInGetOrThrow() {
            var result = WorkflowResult.<String, WorkflowError>suspended("id", "state", "waiting");
            
            var exception = assertThrows(WorkflowResult.WorkflowExecutionException.class, 
                result::getOrThrow);
            
            assertTrue(exception.getMessage().contains("Operation suspended"));
        }

        @Test
        @DisplayName("Should get value or else for success")
        void shouldGetValueOrElseForSuccess() {
            var result = WorkflowResult.<String, WorkflowError>success("test");
            
            assertEquals("test", result.getOrElse("default"));
        }

        @Test
        @DisplayName("Should get default for failure")
        void shouldGetDefaultForFailure() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error);
            
            assertEquals("default", result.getOrElse("default"));
        }

        @Test
        @DisplayName("Should get computed default for failure")
        void shouldGetComputedDefaultForFailure() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error);
            
            assertEquals("computed", result.getOrElse(() -> "computed"));
        }
    }

    @Nested
    @DisplayName("Combination Operations Tests")
    class CombinationOperationsTests {

        @Test
        @DisplayName("Should combine two successes")
        void shouldCombineTwoSuccesses() {
            var result1 = WorkflowResult.<String, WorkflowError>success("hello");
            var result2 = WorkflowResult.<String, WorkflowError>success(" world");
            
            var combined = result1.combine(result2, s1 -> s2 -> s1 + s2);
            
            assertTrue(combined.isSuccess());
            assertEquals(Optional.of("hello world"), combined.getValue());
        }

        @Test
        @DisplayName("Should fail combination if first fails")
        void shouldFailCombinationIfFirstFails() {
            var error = ValidationError.required("field");
            var result1 = WorkflowResult.<String, WorkflowError>failure(error);
            var result2 = WorkflowResult.<String, WorkflowError>success("world");
            
            var combined = result1.combine(result2, s1 -> s2 -> s1 + s2);
            
            assertTrue(combined.isFailure());
            assertEquals(Optional.of(error), combined.getError());
        }

        @Test
        @DisplayName("Should fail combination if second fails")
        void shouldFailCombinationIfSecondFails() {
            var error = ValidationError.required("field");
            var result1 = WorkflowResult.<String, WorkflowError>success("hello");
            var result2 = WorkflowResult.<String, WorkflowError>failure(error);
            
            var combined = result1.combine(result2, s1 -> s2 -> s1 + s2);
            
            assertTrue(combined.isFailure());
            assertEquals(Optional.of(error), combined.getError());
        }
    }

    @Nested
    @DisplayName("Pattern Matching Tests")
    class PatternMatchingTests {

        @Test
        @DisplayName("Should handle success in conditional logic")
        void shouldHandleSuccessInConditionalLogic() {
            var result = WorkflowResult.<String, WorkflowError>success("test");
            
            String outcome;
            if (result instanceof WorkflowResult.Success) {
                WorkflowResult.Success<String, WorkflowError> success = (WorkflowResult.Success<String, WorkflowError>) result;
                outcome = "Got: " + success.value();
            } else if (result instanceof WorkflowResult.Failure) {
                WorkflowResult.Failure<String, WorkflowError> failure = (WorkflowResult.Failure<String, WorkflowError>) result;
                outcome = "Error: " + failure.error().code();
            } else if (result instanceof WorkflowResult.Suspended) {
                WorkflowResult.Suspended<String, WorkflowError> suspended = (WorkflowResult.Suspended<String, WorkflowError>) result;
                outcome = "Suspended: " + suspended.reason();
            } else {
                outcome = "Unknown";
            }
            
            assertEquals("Got: test", outcome);
        }

        @Test
        @DisplayName("Should handle failure in conditional logic")
        void shouldHandleFailureInConditionalLogic() {
            var error = ValidationError.required("field");
            var result = WorkflowResult.<String, WorkflowError>failure(error);
            
            String outcome;
            if (result instanceof WorkflowResult.Success) {
                WorkflowResult.Success<String, WorkflowError> success = (WorkflowResult.Success<String, WorkflowError>) result;
                outcome = "Got: " + success.value();
            } else if (result instanceof WorkflowResult.Failure) {
                WorkflowResult.Failure<String, WorkflowError> failure = (WorkflowResult.Failure<String, WorkflowError>) result;
                outcome = "Error: " + failure.error().code();
            } else if (result instanceof WorkflowResult.Suspended) {
                WorkflowResult.Suspended<String, WorkflowError> suspended = (WorkflowResult.Suspended<String, WorkflowError>) result;
                outcome = "Suspended: " + suspended.reason();
            } else {
                outcome = "Unknown";
            }
            
            assertEquals("Error: FIELD_REQUIRED", outcome);
        }

        @Test
        @DisplayName("Should handle suspension in conditional logic")
        void shouldHandleSuspensionInConditionalLogic() {
            var result = WorkflowResult.<String, WorkflowError>suspended("id", "state", "waiting");
            
            String outcome;
            if (result instanceof WorkflowResult.Success) {
                WorkflowResult.Success<String, WorkflowError> success = (WorkflowResult.Success<String, WorkflowError>) result;
                outcome = "Got: " + success.value();
            } else if (result instanceof WorkflowResult.Failure) {
                WorkflowResult.Failure<String, WorkflowError> failure = (WorkflowResult.Failure<String, WorkflowError>) result;
                outcome = "Error: " + failure.error().code();
            } else if (result instanceof WorkflowResult.Suspended) {
                WorkflowResult.Suspended<String, WorkflowError> suspended = (WorkflowResult.Suspended<String, WorkflowError>) result;
                outcome = "Suspended: " + suspended.reason();
            } else {
                outcome = "Unknown";
            }
            
            assertEquals("Suspended: waiting", outcome);
        }
    }

    @Nested
    @DisplayName("Chaining Operations Tests")
    class ChainingOperationsTests {

        @Test
        @DisplayName("Should chain multiple operations successfully")
        void shouldChainMultipleOperationsSuccessfully() {
            var result = WorkflowResult.<String, WorkflowError>success("  hello world  ")
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> s.length() > 5, () -> ValidationError.invalidFormat("text", "", "long"))
                .flatMap(s -> WorkflowResult.success(s.split(" ").length));
            
            assertTrue(result.isSuccess());
            assertEquals(Optional.of(2), result.getValue());
        }

        @Test
        @DisplayName("Should short-circuit on first failure")
        void shouldShortCircuitOnFirstFailure() {
            var executed = new AtomicBoolean(false);
            
            var result = WorkflowResult.<String, WorkflowError>success("hi")
                .filter(s -> s.length() > 5, () -> ValidationError.invalidFormat("text", "hi", "long"))
                .map(s -> {
                    executed.set(true);
                    return s.toUpperCase();
                });
            
            assertTrue(result.isFailure());
            assertFalse(executed.get()); // Should not execute map after filter failure
        }
    }
}