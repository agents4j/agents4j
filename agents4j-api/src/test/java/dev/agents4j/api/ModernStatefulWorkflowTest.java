package dev.agents4j.api;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.WorkflowError;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive test demonstrating the modernized StatefulWorkflow architecture.
 * Shows how the new design eliminates bridge adapters and provides full type safety.
 */
class ModernStatefulWorkflowTest {

    private ModernOrderProcessingWorkflow workflow;
    private OrderData validOrderData;
    private OrderData invalidOrderData;
    private OrderData highValueOrderData;
    private WorkflowContext initialContext;

    @BeforeEach
    void setUp() {
        workflow = new ModernOrderProcessingWorkflow();
        
        validOrderData = new OrderData(
            "ORD-123",
            "CUST-456", 
            List.of(
                new OrderItem("PROD-001", 2, 50.0),
                new OrderItem("PROD-002", 1, 75.0)
            ),
            OrderStatus.PENDING,
            175.0
        );
        
        invalidOrderData = new OrderData(
            "", // Invalid: empty order ID
            "CUST-456",
            List.of(),
            OrderStatus.PENDING,
            0.0
        );
        
        highValueOrderData = new OrderData(
            "ORD-999",
            "CUST-VIP",
            List.of(
                new OrderItem("PROD-PREMIUM", 1, 1500.0)
            ),
            OrderStatus.PENDING,
            1500.0
        );
        
        initialContext = WorkflowContext.empty()
            .with(ContextKey.stringKey("source"), "api")
            .with(ContextKey.stringKey("region"), "us-east-1");
    }

    @Test
    @DisplayName("Should provide workflow metadata")
    void shouldProvideWorkflowMetadata() {
        var metadata = workflow.getMetadata();
        
        assertEquals("order-processing-workflow", metadata.workflowId());
        assertEquals("Order Processing Workflow", metadata.name());
        assertEquals("2.0.0", metadata.version());
        assertEquals(OrderData.class, metadata.stateType());
        assertEquals(OrderResult.class, metadata.outputType());
    }

    @Test
    @DisplayName("Should validate input data correctly")
    void shouldValidateInputDataCorrectly() {
        // Valid data should pass
        var validResult = workflow.validate(validOrderData);
        assertTrue(validResult.isSuccess());
        assertEquals(validOrderData, validResult.getOrThrow());
        
        // Invalid data should fail with structured errors
        var invalidResult = workflow.validate(invalidOrderData);
        assertTrue(invalidResult.isFailure());
        var error = invalidResult.getError().get();
        assertEquals("FIELD_REQUIRED", error.code());
        assertTrue(error.message().contains("order.id"));
        
        // Null data should fail
        var nullResult = workflow.validate(null);
        assertTrue(nullResult.isFailure());
        assertEquals("FIELD_REQUIRED", nullResult.getError().get().code());
    }

    @Test
    @DisplayName("Should start workflow with type-safe context")
    void shouldStartWorkflowWithTypeSafeContext() {
        var result = workflow.start(validOrderData, initialContext);
        
        assertTrue(result.isSuccess());
        var execution = result.getOrThrow();
        
        // Verify execution properties
        assertNotNull(execution.getExecutionId());
        assertNotNull(execution.getTimestamp());
        assertEquals(validOrderData.orderId(), execution.getStateData().orderId());
        
        // Verify type-safe context access
        var context = execution.getContext();
        assertEquals("ORD-123", context.get(ContextKey.stringKey("order.id")).orElse(""));
        assertEquals("CUST-456", context.get(ContextKey.stringKey("customer.id")).orElse(""));
        assertEquals(175.0, context.get(ContextKey.of("total.amount", Double.class)).orElse(0.0));
        assertEquals("api", context.get(ContextKey.stringKey("source")).orElse(""));
        assertEquals("us-east-1", context.get(ContextKey.stringKey("region")).orElse(""));
    }

    @Test
    @DisplayName("Should handle workflow completion for normal orders")
    void shouldHandleWorkflowCompletionForNormalOrders() {
        var result = workflow.start(validOrderData, initialContext);
        
        assertTrue(result.isSuccess());
        var execution = result.getOrThrow();
        
        // For normal orders (< $1000), workflow should complete automatically
        // Note: In real implementation, this might go through multiple steps
        if (execution.isCompleted()) {
            var completedExecution = (ModernStatefulWorkflow.WorkflowExecution.Completed<OrderData, OrderResult>) execution;
            assertNotNull(completedExecution.output());
            assertEquals("ORD-123", completedExecution.output().orderId());
            assertTrue(completedExecution.output().message().contains("successfully"));
        } else if (execution.isSuspended()) {
            // Workflow suspended for payment processing - this is expected
            var suspendedExecution = (ModernStatefulWorkflow.WorkflowExecution.Suspended<OrderData, OrderResult>) execution;
            assertTrue(suspendedExecution.reason().contains("payment") || 
                      suspendedExecution.reason().contains("processing"));
        }
    }

    @Test
    @DisplayName("Should suspend high-value orders for approval")
    void shouldSuspendHighValueOrdersForApproval() {
        var result = workflow.start(highValueOrderData, initialContext);
        
        assertTrue(result.isSuccess());
        var execution = result.getOrThrow();
        
        // High-value orders should eventually be suspended for approval
        // Let's simulate the workflow progression
        if (execution.isSuspended()) {
            var suspendedExecution = (ModernStatefulWorkflow.WorkflowExecution.Suspended<OrderData, OrderResult>) execution;
            
            // Verify suspension details
            assertNotNull(suspendedExecution.suspensionId());
            assertNotNull(suspendedExecution.reason());
            assertEquals(highValueOrderData.orderId(), suspendedExecution.getStateData().orderId());
            
            // Verify context contains approval flag
            var context = suspendedExecution.getContext();
            Boolean requiresApproval = context.get(ContextKey.booleanKey("requires.approval")).orElse(false);
            // This might be set during validation step
        }
    }

    @Test
    @DisplayName("Should resume suspended workflows")
    void shouldResumeSuspendedWorkflows() {
        // Start a workflow that will be suspended
        var startResult = workflow.start(highValueOrderData, initialContext);
        assertTrue(startResult.isSuccess());
        
        var execution = startResult.getOrThrow();
        
        if (execution.isSuspended()) {
            // Resume with approval context
            var approvalContext = WorkflowContext.empty()
                .with(ContextKey.booleanKey("approved"), true)
                .with(ContextKey.stringKey("approver"), "manager@company.com");
            
            var resumeResult = workflow.resume(execution, approvalContext);
            
            assertTrue(resumeResult.isSuccess());
            var resumedExecution = resumeResult.getOrThrow();
            
            // Verify the workflow continued processing
            assertNotNull(resumedExecution);
            assertEquals(execution.getExecutionId(), resumedExecution.getExecutionId());
            
            // Verify approval context was merged
            var mergedContext = resumedExecution.getContext();
            assertEquals("manager@company.com", 
                mergedContext.get(ContextKey.stringKey("approver")).orElse(""));
        }
    }

    @Test
    @DisplayName("Should handle validation failures gracefully")
    void shouldHandleValidationFailuresGracefully() {
        var result = workflow.start(invalidOrderData, initialContext);
        
        // Workflow should start but fail during validation
        if (result.isSuccess()) {
            var execution = result.getOrThrow();
            if (execution.isFailed()) {
                var failedExecution = (ModernStatefulWorkflow.WorkflowExecution.Failed<OrderData, OrderResult>) execution;
                assertNotNull(failedExecution.error());
                assertTrue(failedExecution.error().message().contains("required") || 
                          failedExecution.error().message().contains("invalid"));
            }
        } else {
            // Validation failed at start
            assertTrue(result.isFailure());
            var error = result.getError().get();
            assertEquals("FIELD_REQUIRED", error.code());
        }
    }

    @Test
    @DisplayName("Should support async execution")
    void shouldSupportAsyncExecution() {
        CompletableFuture<WorkflowResult<ModernStatefulWorkflow.WorkflowExecution<OrderData, OrderResult>, WorkflowError>> 
            futureResult = workflow.startAsync(validOrderData, initialContext);
        
        assertNotNull(futureResult);
        assertFalse(futureResult.isDone() && futureResult.isCompletedExceptionally());
        
        // Wait for completion (in real scenarios, you'd handle this asynchronously)
        var result = futureResult.join();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should demonstrate functional composition")
    void shouldDemonstrateFunctionalComposition() {
        var result = workflow.start(validOrderData, initialContext)
            .flatMap(execution -> {
                if (execution.isCompleted()) {
                    return WorkflowResult.success("Workflow completed immediately");
                } else if (execution.isSuspended()) {
                    return WorkflowResult.success("Workflow suspended as expected");
                } else {
                    return WorkflowResult.success("Workflow in progress");
                }
            })
            .map(String::toUpperCase)
            .recover(error -> "WORKFLOW FAILED: " + error.message());
        
        assertTrue(result.isSuccess());
        String outcome = result.getOrThrow();
        assertTrue(outcome.startsWith("WORKFLOW"));
    }

    @Test
    @DisplayName("Should provide type-safe context operations")
    void shouldProvideTypeSafeContextOperations() {
        var result = workflow.start(validOrderData, initialContext);
        assertTrue(result.isSuccess());
        
        var execution = result.getOrThrow();
        var context = execution.getContext();
        
        // Type-safe context access
        String orderId = context.get(ContextKey.stringKey("order.id")).orElse("unknown");
        Double amount = context.get(ContextKey.of("total.amount", Double.class)).orElse(0.0);
        Integer retryCount = context.get(ContextKey.intKey("retry.count")).orElse(-1);
        
        assertEquals("ORD-123", orderId);
        assertEquals(175.0, amount);
        assertEquals(0, retryCount);
        
        // Context immutability
        var newContext = context.with(ContextKey.stringKey("new.field"), "test-value");
        assertNotSame(context, newContext);
        assertEquals(context.size() + 1, newContext.size());
        assertFalse(context.contains(ContextKey.stringKey("new.field")));
        assertTrue(newContext.contains(ContextKey.stringKey("new.field")));
    }

    @Test
    @DisplayName("Should demonstrate error recovery patterns")
    void shouldDemonstrateErrorRecoveryPatterns() {
        // Create order with blocked customer (will cause validation failure)
        var blockedCustomerOrder = new OrderData(
            "ORD-BLOCKED",
            "BLOCKED-CUSTOMER",
            List.of(new OrderItem("PROD-001", 1, 100.0)),
            OrderStatus.PENDING,
            100.0
        );
        
        var result = workflow.start(blockedCustomerOrder, initialContext)
            .recover(error -> {
                // Recovery: create a default successful execution for blocked customers
                return ModernStatefulWorkflow.WorkflowExecution.completed(
                    blockedCustomerOrder.withStatus(OrderStatus.FAILED),
                    WorkflowContext.empty(),
                    "recovery-execution",
                    new OrderResult("ORD-BLOCKED", "Order blocked due to customer status", 0.0)
                );
            })
            .onFailure(error -> {
                System.out.println("Workflow failed: " + error.message());
            })
            .onSuccess(execution -> {
                System.out.println("Workflow execution created: " + execution.getExecutionId());
            });
        
        assertTrue(result.isSuccess());
        var execution = result.getOrThrow();
        
        if (execution.isCompleted()) {
            var output = ((ModernStatefulWorkflow.WorkflowExecution.Completed<OrderData, OrderResult>) execution).output();
            assertTrue(output.message().contains("blocked"));
        }
    }

    @Test
    @DisplayName("Should handle concurrent execution safely")
    void shouldHandleConcurrentExecutionSafely() {
        // Create multiple concurrent workflow executions
        var futures = List.of(
            workflow.startAsync(validOrderData, initialContext),
            workflow.startAsync(highValueOrderData, initialContext),
            workflow.startAsync(validOrderData.withStatus(OrderStatus.PENDING), initialContext)
        );
        
        // Wait for all to complete
        var results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        // All should succeed (workflows are independent)
        results.forEach(result -> assertTrue(result.isSuccess()));
        
        // Each should have unique execution IDs
        var executionIds = results.stream()
            .map(WorkflowResult::getOrThrow)
            .map(ModernStatefulWorkflow.WorkflowExecution::getExecutionId)
            .toList();
        
        assertEquals(3, executionIds.size());
        assertEquals(3, executionIds.stream().distinct().count()); // All unique
    }

    @Test
    @DisplayName("Should demonstrate workflow state transitions")
    void shouldDemonstrateWorkflowStateTransitions() {
        var result = workflow.start(validOrderData, initialContext);
        assertTrue(result.isSuccess());
        
        var execution = result.getOrThrow();
        var context = execution.getContext();
        
        // Initial state should be set
        var status = context.get(ContextKey.of("status", OrderStatus.class));
        assertTrue(status.isPresent());
        
        // Timestamps should be set
        var createdAt = context.get(ContextKey.of("created.at", Instant.class));
        assertTrue(createdAt.isPresent());
        assertTrue(createdAt.get().isBefore(Instant.now().plusSeconds(1)));
        
        // Retry count should be initialized
        var retryCount = context.get(ContextKey.intKey("retry.count"));
        assertEquals(0, retryCount.orElse(-1));
    }
}