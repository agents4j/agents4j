package dev.agents4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.agents4j.api.exception.ErrorCode;
import dev.agents4j.api.exception.ValidationException;
import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExceptionSystemTest {

    @Test
    void testErrorCodeProperties() {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        assertEquals("AGT-1001", errorCode.getCode());
        assertEquals("Invalid input provided", errorCode.getDescription());
        assertTrue(errorCode.toString().contains("AGT-1001"));
        assertTrue(errorCode.toString().contains("Invalid input provided"));
    }

    @Test
    void testWorkflowExecutionExceptionWithMessage() {
        String workflowName = "TestWorkflow";
        String message = "Execution failed";

        WorkflowExecutionException exception = new WorkflowExecutionException(
            workflowName,
            message
        );

        assertEquals(
            ErrorCode.WORKFLOW_EXECUTION_FAILED,
            exception.getErrorCode()
        );
        assertEquals(workflowName, exception.getWorkflowName());
        assertTrue(exception.getMessage().contains(message));
        assertTrue(
            exception
                .getMessage()
                .contains(ErrorCode.WORKFLOW_EXECUTION_FAILED.getCode())
        );
        assertEquals(workflowName, exception.getContext().get("workflowName"));
    }

    @Test
    void testWorkflowExecutionExceptionWithCause() {
        String workflowName = "TestWorkflow";
        String message = "Execution failed";
        Throwable cause = new RuntimeException("Root cause");

        WorkflowExecutionException exception = new WorkflowExecutionException(
            workflowName,
            message,
            cause
        );

        assertEquals(
            ErrorCode.WORKFLOW_EXECUTION_FAILED,
            exception.getErrorCode()
        );
        assertEquals(workflowName, exception.getWorkflowName());
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains(message));
    }

    @Test
    void testWorkflowExecutionExceptionWithContext() {
        String workflowName = "TestWorkflow";
        String message = "Execution failed";
        Map<String, Object> context = new HashMap<>();
        context.put("nodeIndex", 2);
        context.put("totalNodes", 5);

        WorkflowExecutionException exception = new WorkflowExecutionException(
            workflowName,
            message,
            context
        );

        assertEquals(
            ErrorCode.WORKFLOW_EXECUTION_FAILED,
            exception.getErrorCode()
        );
        assertEquals(workflowName, exception.getWorkflowName());
        assertEquals(2, exception.getContext().get("nodeIndex"));
        assertEquals(5, exception.getContext().get("totalNodes"));
        assertEquals(workflowName, exception.getContext().get("workflowName"));
    }

    @Test
    void testValidationException() {
        String message = "Validation failed";
        List<String> validationErrors = Arrays.asList(
            "Field 'name' cannot be null",
            "Field 'age' must be positive"
        );

        ValidationException exception = new ValidationException(
            message,
            validationErrors
        );

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals(validationErrors, exception.getValidationErrors());
        assertTrue(exception.getMessage().contains(message));
        assertEquals(
            validationErrors,
            exception.getContext().get("validationErrors")
        );
    }

    @Test
    void testValidationExceptionWithContext() {
        String message = "Validation failed";
        List<String> validationErrors = Arrays.asList("Invalid input");
        Map<String, Object> context = new HashMap<>();
        context.put("source", "UserInput");

        ValidationException exception = new ValidationException(
            message,
            validationErrors,
            context
        );

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals(validationErrors, exception.getValidationErrors());
        assertEquals("UserInput", exception.getContext().get("source"));
        assertEquals(
            validationErrors,
            exception.getContext().get("validationErrors")
        );
    }

    @Test
    void testAgentExceptionContextModification() {
        WorkflowExecutionException exception = new WorkflowExecutionException(
            "TestWorkflow",
            "Test message"
        );

        exception.addContext("customKey", "customValue");
        exception.addContext("retryCount", 3);

        assertEquals("customValue", exception.getContext().get("customKey"));
        assertEquals(3, exception.getContext().get("retryCount"));
    }

    @Test
    void testExceptionContextImmutability() {
        WorkflowExecutionException exception = new WorkflowExecutionException(
            "TestWorkflow",
            "Test message"
        );
        Map<String, Object> context = exception.getContext();

        assertThrows(UnsupportedOperationException.class, () -> {
            context.put("newKey", "newValue");
        });
    }

    @Test
    void testErrorCodeCategories() {
        // Test validation error codes are in 1000 range
        assertTrue(ErrorCode.INVALID_INPUT.getCode().startsWith("AGT-1"));
        assertTrue(
            ErrorCode.INVALID_CONFIGURATION.getCode().startsWith("AGT-1")
        );
        assertTrue(
            ErrorCode.MISSING_REQUIRED_PARAMETER.getCode().startsWith("AGT-1")
        );

        // Test workflow error codes are in 2000 range
        assertTrue(
            ErrorCode.WORKFLOW_EXECUTION_FAILED.getCode().startsWith("AGT-2")
        );
        assertTrue(ErrorCode.WORKFLOW_TIMEOUT.getCode().startsWith("AGT-2"));
        assertTrue(
            ErrorCode.WORKFLOW_INTERRUPTED.getCode().startsWith("AGT-2")
        );

        // Test agent error codes are in 3000 range
        assertTrue(
            ErrorCode.AGENT_PROCESSING_FAILED.getCode().startsWith("AGT-3")
        );
        assertTrue(
            ErrorCode.AGENT_INITIALIZATION_FAILED.getCode().startsWith("AGT-3")
        );
        assertTrue(ErrorCode.AGENT_NOT_FOUND.getCode().startsWith("AGT-3"));

        // Test provider error codes are in 4000 range
        assertTrue(ErrorCode.PROVIDER_ERROR.getCode().startsWith("AGT-4"));
        assertTrue(ErrorCode.PROVIDER_RATE_LIMIT.getCode().startsWith("AGT-4"));
        assertTrue(
            ErrorCode.PROVIDER_AUTHENTICATION_FAILED.getCode()
                .startsWith("AGT-4")
        );

        // Test system error codes are in 5000 range
        assertTrue(ErrorCode.INTERNAL_ERROR.getCode().startsWith("AGT-5"));
        assertTrue(ErrorCode.RESOURCE_EXHAUSTED.getCode().startsWith("AGT-5"));
        assertTrue(ErrorCode.CONFIGURATION_ERROR.getCode().startsWith("AGT-5"));
    }

    @Test
    void testExceptionMessageFormatting() {
        String message = "Custom error message";
        WorkflowExecutionException exception = new WorkflowExecutionException(
            "TestWorkflow",
            message
        );

        String formattedMessage = exception.getMessage();
        assertTrue(formattedMessage.startsWith("[AGT-2001]"));
        assertTrue(formattedMessage.contains(message));
    }
}
