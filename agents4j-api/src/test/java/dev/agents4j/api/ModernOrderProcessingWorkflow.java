package dev.agents4j.api;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Complete example implementation of ModernStatefulWorkflow that demonstrates
 * how to build workflows with the new type-safe, functional architecture.
 *
 * This eliminates all bridge adapters and uses modern components throughout.
 */
public class ModernOrderProcessingWorkflow
    implements StatefulWorkflow<OrderData, OrderResult> {

    // Type-safe context keys
    private static final ContextKey<String> ORDER_ID = ContextKey.stringKey(
        "order.id"
    );
    private static final ContextKey<String> CUSTOMER_ID = ContextKey.stringKey(
        "customer.id"
    );
    private static final ContextKey<Double> TOTAL_AMOUNT = ContextKey.of(
        "total.amount",
        Double.class
    );
    private static final ContextKey<OrderStatus> STATUS = ContextKey.of(
        "status",
        OrderStatus.class
    );
    private static final ContextKey<Boolean> REQUIRES_APPROVAL =
        ContextKey.booleanKey("requires.approval");
    private static final ContextKey<Instant> CREATED_AT = ContextKey.of(
        "created.at",
        Instant.class
    );
    private static final ContextKey<String> PAYMENT_ID = ContextKey.stringKey(
        "payment.id"
    );
    private static final ContextKey<Integer> RETRY_COUNT = ContextKey.intKey(
        "retry.count"
    );

    @Override
    public String getWorkflowId() {
        return "order-processing-workflow";
    }

    @Override
    public String getName() {
        return "Order Processing Workflow";
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public Class<OrderData> getSupportedStateType() {
        return OrderData.class;
    }

    @Override
    public Class<OrderResult> getSupportedOutputType() {
        return OrderResult.class;
    }

    @Override
    public WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > start(OrderData initialStateData, WorkflowContext initialContext) {
        // Validate input
        var validationResult = validate(initialStateData);
        if (validationResult.isFailure()) {
            return WorkflowResult.failure(validationResult.getError().get());
        }
        if (validationResult.isSuspended()) {
            return WorkflowResult.suspended(
                validationResult.getSuspension().get().suspensionId(),
                initialStateData,
                validationResult.getSuspension().get().reason()
            );
        }

        // Create execution context
        String executionId = UUID.randomUUID().toString();
        var context = initialContext
            .with(ORDER_ID, initialStateData.orderId())
            .with(CUSTOMER_ID, initialStateData.customerId())
            .with(TOTAL_AMOUNT, initialStateData.totalAmount())
            .with(STATUS, OrderStatus.PENDING)
            .with(CREATED_AT, Instant.now())
            .with(RETRY_COUNT, 0);

        // Start processing
        return processWorkflow(initialStateData, context, executionId);
    }

    @Override
    public WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > resume(WorkflowExecution<OrderData, OrderResult> execution) {
        return resume(execution, WorkflowContext.empty());
    }

    @Override
    public WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > resume(
        WorkflowExecution<OrderData, OrderResult> execution,
        WorkflowContext contextUpdates
    ) {
        if (!execution.isSuspended()) {
            return WorkflowResult.suspended(
                "invalid-resume",
                execution.getStateData(),
                "Can only resume suspended executions"
            );
        }

        var mergedContext = execution.getContext().merge(contextUpdates);
        return processWorkflow(
            execution.getStateData(),
            mergedContext,
            execution.getExecutionId()
        );
    }

    @Override
    public WorkflowResult<OrderData, WorkflowError> validate(
        OrderData stateData
    ) {
        if (stateData == null) {
            return WorkflowResult.suspended(
                "validation-failed",
                null,
                "Order data is required"
            );
        }

        // Validate order ID
        if (
            stateData.orderId() == null || stateData.orderId().trim().isEmpty()
        ) {
            return WorkflowResult.suspended(
                "validation-failed",
                stateData,
                "Order ID is required"
            );
        }

        // Validate customer ID
        if (
            stateData.customerId() == null ||
            stateData.customerId().trim().isEmpty()
        ) {
            return WorkflowResult.suspended(
                "validation-failed",
                stateData,
                "Customer ID is required"
            );
        }

        // Validate items
        if (stateData.items() == null || stateData.items().isEmpty()) {
            return WorkflowResult.suspended(
                "validation-failed",
                stateData,
                "Order items are required"
            );
        }

        // Validate total amount
        if (stateData.totalAmount() == null || stateData.totalAmount() <= 0) {
            return WorkflowResult.suspended(
                "validation-failed",
                stateData,
                "Total amount must be positive"
            );
        }

        // Validate individual items
        for (int i = 0; i < stateData.items().size(); i++) {
            var item = stateData.items().get(i);
            if (item.quantity() <= 0) {
                return WorkflowResult.suspended(
                    "validation-failed",
                    stateData,
                    "Item quantity must be positive for item " + i
                );
            }
            if (item.price() <= 0) {
                return WorkflowResult.suspended(
                    "validation-failed",
                    stateData,
                    "Item price must be positive for item " + i
                );
            }
        }

        return WorkflowResult.success(stateData);
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > processWorkflow(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        var currentStatus = context.get(STATUS).orElse(OrderStatus.PENDING);

        return switch (currentStatus) {
            case PENDING -> processValidation(stateData, context, executionId);
            case VALIDATED -> processPayment(stateData, context, executionId);
            case PAYMENT_PROCESSING -> checkPaymentStatus(
                stateData,
                context,
                executionId
            );
            case PAYMENT_COMPLETED -> processApproval(
                stateData,
                context,
                executionId
            );
            case APPROVED -> processCompletion(stateData, context, executionId);
            case COMPLETED -> WorkflowResult.success(
                WorkflowExecution.completed(
                    stateData,
                    context,
                    executionId,
                    new OrderResult(
                        stateData.orderId(),
                        "Order completed successfully",
                        stateData.totalAmount()
                    )
                )
            );
            case FAILED -> WorkflowResult.suspended(
                "order-failed",
                stateData,
                "Order processing failed"
            );
        };
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > processValidation(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        // Perform business validation
        var businessValidation = performBusinessValidation(stateData, context);
        if (businessValidation.isFailure()) {
            var failedData = stateData.withStatus(OrderStatus.FAILED);
            var failedContext = context.with(STATUS, OrderStatus.FAILED);
            return WorkflowResult.success(
                WorkflowExecution.failed(
                    failedData,
                    failedContext,
                    executionId,
                    businessValidation.getError().get()
                )
            );
        }

        // Validation successful - move to next step
        var validatedData = stateData.withStatus(OrderStatus.VALIDATED);
        var updatedContext = context
            .with(STATUS, OrderStatus.VALIDATED)
            .with(REQUIRES_APPROVAL, stateData.totalAmount() > 1000.0);

        return processPayment(validatedData, updatedContext, executionId);
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > processPayment(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        // Start payment processing
        var paymentResult = initiatePayment(stateData, context);
        if (paymentResult.isFailure()) {
            // Check if we should retry
            int retryCount = context.getOrDefault(RETRY_COUNT, 0);
            if (retryCount < 3) {
                // Increment retry count and suspend for retry
                var retryContext = context.with(RETRY_COUNT, retryCount + 1);
                return WorkflowResult.success(
                    WorkflowExecution.suspendedWithTimeout(
                        stateData,
                        retryContext,
                        executionId,
                        "payment-retry-" + (retryCount + 1),
                        "Payment failed, will retry",
                        java.time.Duration.ofMinutes(5)
                    )
                );
            } else {
                // Max retries exceeded
                var failedData = stateData.withStatus(OrderStatus.FAILED);
                var failedContext = context.with(STATUS, OrderStatus.FAILED);
                return WorkflowResult.success(
                    WorkflowExecution.failed(
                        failedData,
                        failedContext,
                        executionId,
                        paymentResult.getError().get()
                    )
                );
            }
        }

        if (paymentResult.isSuspended()) {
            // Payment initiation was suspended, propagate the suspension
            var suspension = paymentResult.getSuspension().get();
            return WorkflowResult.success(
                WorkflowExecution.suspended(
                    stateData,
                    context,
                    executionId,
                    suspension.suspensionId(),
                    suspension.reason()
                )
            );
        }

        // Payment initiated successfully
        var paymentId = paymentResult.getValue().get();
        var processingData = stateData.withStatus(
            OrderStatus.PAYMENT_PROCESSING
        );
        var processingContext = context
            .with(STATUS, OrderStatus.PAYMENT_PROCESSING)
            .with(PAYMENT_ID, paymentId);

        // Suspend while payment is processing
        return WorkflowResult.success(
            WorkflowExecution.suspendedWithTimeout(
                processingData,
                processingContext,
                executionId,
                "payment-processing-" + paymentId,
                "Waiting for payment to complete",
                java.time.Duration.ofMinutes(30)
            )
        );
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > checkPaymentStatus(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        var paymentId = context.get(PAYMENT_ID).orElse("");
        var paymentStatus = checkPaymentCompletion(paymentId);

        if (paymentStatus.isFailure()) {
            var failedData = stateData.withStatus(OrderStatus.FAILED);
            var failedContext = context.with(STATUS, OrderStatus.FAILED);
            return WorkflowResult.success(
                WorkflowExecution.failed(
                    failedData,
                    failedContext,
                    executionId,
                    paymentStatus.getError().get()
                )
            );
        }

        if (!paymentStatus.getValue().get()) {
            // Payment still processing - remain suspended
            return WorkflowResult.success(
                WorkflowExecution.suspendedWithTimeout(
                    stateData,
                    context,
                    executionId,
                    "payment-processing-" + paymentId,
                    "Payment still processing",
                    java.time.Duration.ofMinutes(10)
                )
            );
        }

        // Payment completed
        var completedData = stateData.withStatus(OrderStatus.PAYMENT_COMPLETED);
        var completedContext = context.with(
            STATUS,
            OrderStatus.PAYMENT_COMPLETED
        );

        return processApproval(completedData, completedContext, executionId);
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > processApproval(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        boolean requiresApproval = context.getOrDefault(
            REQUIRES_APPROVAL,
            false
        );

        if (requiresApproval) {
            // Suspend for manual approval
            return WorkflowResult.success(
                WorkflowExecution.suspended(
                    stateData,
                    context,
                    executionId,
                    "approval-" + stateData.orderId(),
                    "Order requires manual approval due to high value"
                )
            );
        } else {
            // Auto-approve
            var approvedData = stateData.withStatus(OrderStatus.APPROVED);
            var approvedContext = context.with(STATUS, OrderStatus.APPROVED);

            return processCompletion(
                approvedData,
                approvedContext,
                executionId
            );
        }
    }

    private WorkflowResult<
        WorkflowExecution<OrderData, OrderResult>,
        WorkflowError
    > processCompletion(
        OrderData stateData,
        WorkflowContext context,
        String executionId
    ) {
        // Finalize the order
        var completionResult = finalizeOrder(stateData, context);
        if (completionResult.isFailure()) {
            var failedData = stateData.withStatus(OrderStatus.FAILED);
            var failedContext = context.with(STATUS, OrderStatus.FAILED);
            return WorkflowResult.success(
                WorkflowExecution.failed(
                    failedData,
                    failedContext,
                    executionId,
                    completionResult.getError().get()
                )
            );
        }

        // Order completed successfully
        var completedData = stateData.withStatus(OrderStatus.COMPLETED);
        var completedContext = context.with(STATUS, OrderStatus.COMPLETED);
        var orderResult = new OrderResult(
            stateData.orderId(),
            "Order completed successfully",
            stateData.totalAmount()
        );

        return WorkflowResult.success(
            WorkflowExecution.completed(
                completedData,
                completedContext,
                executionId,
                orderResult
            )
        );
    }

    // Business logic methods (would typically be injected services)

    private WorkflowResult<Boolean, WorkflowError> performBusinessValidation(
        OrderData orderData,
        WorkflowContext context
    ) {
        // Simulate business validation
        if (orderData.customerId().startsWith("BLOCKED")) {
            return WorkflowResult.suspended(
                "validation-failed",
                orderData,
                "Customer is blocked"
            );
        }

        // Check inventory
        for (var item : orderData.items()) {
            if (item.productId().startsWith("OUT_OF_STOCK")) {
                return WorkflowResult.suspended(
                    "validation-failed",
                    orderData,
                    "Product " + item.productId() + " is out of stock"
                );
            }
        }

        return WorkflowResult.success(true);
    }

    private WorkflowResult<String, WorkflowError> initiatePayment(
        OrderData orderData,
        WorkflowContext context
    ) {
        // Simulate payment initiation
        if (orderData.totalAmount() > 10000.0) {
            return WorkflowResult.suspended(
                "payment-failed",
                orderData,
                "Payment amount exceeds limit"
            );
        }

        // Simulate 10% failure rate
        if (Math.random() < 0.1) {
            return WorkflowResult.suspended(
                "payment-failed",
                orderData,
                "Payment service temporarily unavailable"
            );
        }

        return WorkflowResult.success(
            "PAY-" + UUID.randomUUID().toString().substring(0, 8)
        );
    }

    private WorkflowResult<Boolean, WorkflowError> checkPaymentCompletion(
        String paymentId
    ) {
        // Simulate payment status check
        if (paymentId.isEmpty()) {
            return WorkflowResult.suspended(
                "payment-check-failed",
                null,
                "Payment ID is missing"
            );
        }

        // Simulate 80% completion rate
        boolean isCompleted = Math.random() < 0.8;
        return WorkflowResult.success(isCompleted);
    }

    private WorkflowResult<Boolean, WorkflowError> finalizeOrder(
        OrderData orderData,
        WorkflowContext context
    ) {
        // Simulate order finalization
        if (orderData.items().size() > 10) {
            return WorkflowResult.suspended(
                "fulfillment-failed",
                orderData,
                "Too many items for single shipment"
            );
        }

        return WorkflowResult.success(true);
    }
}

// Supporting data classes

record OrderData(
    String orderId,
    String customerId,
    List<OrderItem> items,
    OrderStatus status,
    Double totalAmount
) {
    public OrderData withStatus(OrderStatus newStatus) {
        return new OrderData(
            orderId,
            customerId,
            items,
            newStatus,
            totalAmount
        );
    }
}

record OrderItem(String productId, int quantity, double price) {}

record OrderResult(String orderId, String message, Double totalAmount) {}

enum OrderStatus {
    PENDING,
    VALIDATED,
    PAYMENT_PROCESSING,
    PAYMENT_COMPLETED,
    APPROVED,
    COMPLETED,
    FAILED,
}

// WorkflowExecution class to represent workflow execution state
record WorkflowExecution<S, O>(
    String executionId,
    S stateData,
    WorkflowContext context,
    Instant timestamp,
    ExecutionStatus status,
    O result,
    WorkflowError error
) {
    public boolean isSuspended() {
        return status == ExecutionStatus.SUSPENDED;
    }

    public boolean isCompleted() {
        return status == ExecutionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == ExecutionStatus.FAILED;
    }

    public String getExecutionId() {
        return executionId;
    }

    public S getStateData() {
        return stateData;
    }

    public WorkflowContext getContext() {
        return context;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public static <S, O> WorkflowExecution<S, O> suspended(
        S stateData,
        WorkflowContext context,
        String executionId,
        String reason
    ) {
        return new WorkflowExecution<>(
            executionId,
            stateData,
            context,
            Instant.now(),
            ExecutionStatus.SUSPENDED,
            null,
            null
        );
    }

    public static <S, O> WorkflowExecution<S, O> suspendedWithTimeout(
        S stateData,
        WorkflowContext context,
        String executionId,
        String suspensionId,
        String reason,
        java.time.Duration timeout
    ) {
        return new WorkflowExecution<>(
            executionId,
            stateData,
            context,
            Instant.now(),
            ExecutionStatus.SUSPENDED,
            null,
            null
        );
    }

    public static <S, O> WorkflowExecution<S, O> completed(
        S stateData,
        WorkflowContext context,
        String executionId,
        O result
    ) {
        return new WorkflowExecution<>(
            executionId,
            stateData,
            context,
            Instant.now(),
            ExecutionStatus.COMPLETED,
            result,
            null
        );
    }

    public static <S, O> WorkflowExecution<S, O> failed(
        S stateData,
        WorkflowContext context,
        String executionId,
        WorkflowError error
    ) {
        return new WorkflowExecution<>(
            executionId,
            stateData,
            context,
            Instant.now(),
            ExecutionStatus.FAILED,
            null,
            error
        );
    }
}

enum ExecutionStatus {
    SUSPENDED,
    COMPLETED,
    FAILED,
}
