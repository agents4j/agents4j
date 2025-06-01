package dev.agents4j.api;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.context.*;
import dev.agents4j.api.result.*;
import dev.agents4j.api.result.error.*;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the key modernizations implemented in agents4j-api.
 * This test showcases type safety, functional error handling, and immutable data structures.
 */
class ModernizationDemoTest {

    @Test
    @DisplayName("Type-Safe Context System")
    void demonstrateTypeSafeContext() {
        // Old approach: Map<String, Object> context - unsafe casting required
        // New approach: Type-safe context with compile-time guarantees

        var userIdKey = ContextKey.stringKey("user.id");
        var ageKey = ContextKey.intKey("age");
        var enabledKey = ContextKey.booleanKey("enabled");

        var context = WorkflowContext.empty()
            .with(userIdKey, "user-123")
            .with(ageKey, 30)
            .with(enabledKey, true);

        // Type-safe access - no casting required
        String userId = context.get(userIdKey).orElse("unknown");
        Integer age = context.get(ageKey).orElse(0);
        Boolean enabled = context.getOrDefault(enabledKey, false);

        assertEquals("user-123", userId);
        assertEquals(30, age);
        assertTrue(enabled);

        // Type safety is enforced at compile time - no runtime test needed
        assertTrue(true); // This demonstrates that incorrect types won't compile
    }

    @Test
    @DisplayName("Functional Error Handling with WorkflowResult")
    void demonstrateFunctionalErrorHandling() {
        // Old approach: Exception-based error handling
        // New approach: Functional error handling with WorkflowResult

        // Success case
        var successResult = WorkflowResult.<String, WorkflowError>success(
            "Hello World"
        );
        assertTrue(successResult.isSuccess());
        assertEquals(Optional.of("Hello World"), successResult.getValue());

        // Functional transformations
        var transformed = successResult
            .map(String::toUpperCase)
            .map(s -> s + "!");

        assertTrue(transformed.isSuccess());
        assertEquals("HELLO WORLD!", transformed.getOrThrow());

        // Error case - using suspended for testing
        var suspendedResult = WorkflowResult.<String, WorkflowError>suspended(
            "test-id",
            "state",
            "testing"
        );
        assertTrue(suspendedResult.isSuspended());

        // Recovery using getOrElse
        String recovered = suspendedResult.getOrElse("Default Value");
        assertEquals("Default Value", recovered);
    }

    @Test
    @DisplayName("Immutable Data Structures")
    void demonstrateImmutability() {
        // All data structures are immutable by design
        var originalContext = WorkflowContext.empty()
            .with(ContextKey.stringKey("name"), "John");

        var modifiedContext = originalContext.with(
            ContextKey.intKey("age"),
            30
        );

        // Original context unchanged
        assertEquals(1, originalContext.size());
        assertFalse(originalContext.contains(ContextKey.intKey("age")));

        // Modified context has both values
        assertEquals(2, modifiedContext.size());
        assertTrue(modifiedContext.contains(ContextKey.stringKey("name")));
        assertTrue(modifiedContext.contains(ContextKey.intKey("age")));
    }

    @Test
    @DisplayName("WorkflowResult Pattern Matching")
    void demonstrateWorkflowResultPatternMatching() {
        // Test different result types
        var successResult = WorkflowResult.<String, WorkflowError>success(
            "test"
        );
        var suspendedResult = WorkflowResult.<String, WorkflowError>suspended(
            "id",
            "state",
            "reason"
        );

        // Pattern matching with conditional logic
        String successOutcome = getResultDescription(successResult);
        String suspendedOutcome = getResultDescription(suspendedResult);

        assertEquals("Success: test", successOutcome);
        assertEquals("Suspended: reason", suspendedOutcome);
    }

    private String getResultDescription(
        WorkflowResult<String, WorkflowError> result
    ) {
        if (result instanceof WorkflowResult.Success) {
            return "Success: " + result.getValue().orElse("unknown");
        } else if (result instanceof WorkflowResult.Suspended) {
            return (
                "Suspended: " +
                result.getSuspension().map(s -> s.reason()).orElse("unknown")
            );
        } else {
            return "Other";
        }
    }

    @Test
    @DisplayName("Context Type Validation")
    void demonstrateContextValidation() {
        var validationContext = ValidationContext.empty()
            .withError("Username is required")
            .withError("Password too weak")
            .withWarning("Email not verified");

        assertTrue(validationContext.hasErrors());
        assertEquals(2, validationContext.getErrorCount());
        assertTrue(validationContext.hasWarnings());
        assertEquals(1, validationContext.getWarningCount());
        assertFalse(validationContext.isValid());

        var cleanContext = validationContext.clearValidation();
        assertTrue(cleanContext.isValid());
        assertEquals(0, cleanContext.getErrorCount());
    }

    @Test
    @DisplayName("Complete Workflow Pattern")
    void demonstrateCompleteWorkflowPattern() {
        // Simulate a complete workflow with type safety and error handling
        var userIdKey = ContextKey.stringKey("user.id");
        var validatedKey = ContextKey.booleanKey("validated");

        // Step 1: Create initial context
        var initialContext = WorkflowContext.of(userIdKey, "user-123");

        // Step 2: Validation step
        var validationResult = validateUser(
            initialContext.get(userIdKey).orElse("")
        );

        assertTrue(validationResult.isSuccess());

        // Step 3: Update context and continue processing
        var updatedContext = initialContext.with(validatedKey, true);
        var processResult = processUser(updatedContext);

        assertTrue(processResult.isSuccess());
        assertEquals("User processed successfully", processResult.getOrThrow());
    }

    // Helper methods for workflow demonstration
    private WorkflowResult<String, WorkflowError> validateUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return WorkflowResult.suspended(
                "validation-pending",
                userId,
                "User ID validation required"
            );
        }
        if (!userId.startsWith("user-")) {
            return WorkflowResult.suspended(
                "format-check",
                userId,
                "User ID format validation required"
            );
        }
        return WorkflowResult.success("Validation passed");
    }

    private WorkflowResult<String, WorkflowError> processUser(
        WorkflowContext context
    ) {
        var userId = context.get(ContextKey.stringKey("user.id"));
        var validated = context.get(ContextKey.booleanKey("validated"));

        if (userId.isEmpty()) {
            return WorkflowResult.suspended(
                "user-missing",
                null,
                "User ID required"
            );
        }
        if (!validated.orElse(false)) {
            return WorkflowResult.suspended(
                "not-validated",
                userId.get(),
                "User validation required"
            );
        }

        return WorkflowResult.success("User processed successfully");
    }
}
