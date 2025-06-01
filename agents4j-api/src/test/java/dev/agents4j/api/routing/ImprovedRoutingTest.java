package dev.agents4j.api.routing;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.context.ContextKey;
import dev.agents4j.api.context.WorkflowContext;
import dev.agents4j.api.graph.EdgeCondition;
import dev.agents4j.api.graph.GraphCommand;
import dev.agents4j.api.graph.GraphWorkflowState;
import dev.agents4j.api.graph.NodeId;
import dev.agents4j.api.graph.WorkflowId;
import dev.agents4j.api.result.WorkflowResult;
import dev.agents4j.api.result.error.WorkflowError;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the improved routing interfaces that integrate
 * with graph workflow architecture.
 */
class ImprovedRoutingTest {

    private static final ContextKey<Double> CONFIDENCE_KEY = ContextKey.of(
        "routing.confidence",
        Double.class
    );
    private static final ContextKey<String> URGENCY_KEY = ContextKey.stringKey(
        "urgency"
    );
    private static final ContextKey<String> CATEGORY_KEY = ContextKey.stringKey(
        "category"
    );

    private WorkflowId workflowId;
    private WorkflowContext testContext;
    private Set<NodeId> availableRoutes;

    @BeforeEach
    void setUp() {
        workflowId = WorkflowId.generate();
        testContext = WorkflowContext.empty()
            .with(URGENCY_KEY, "high")
            .with(CATEGORY_KEY, "technical");

        availableRoutes = Set.of(
            NodeId.of("technical-support"),
            NodeId.of("billing-support"),
            NodeId.of("general-inquiry")
        );
    }

    @Test
    @DisplayName("Should create RoutingDecision with NodeId integration")
    void shouldCreateRoutingDecisionWithNodeIdIntegration() {
        var targetRoute = NodeId.of("technical-support");
        var decision = RoutingDecision.builder()
            .selectedRoute(targetRoute)
            .confidence(0.85)
            .reasoning("High technical content detected")
            .addAlternative("billing-support", 0.23)
            .addContextUpdate(CONFIDENCE_KEY, 0.85)
            .processingTimeMs(150)
            .build();

        assertEquals(targetRoute, decision.getSelectedRoute());
        assertEquals("technical-support", decision.getSelectedRouteId());
        assertEquals(0.85, decision.getConfidence());
        assertEquals(
            "High technical content detected",
            decision.getReasoning()
        );
        assertTrue(decision.meetsThreshold(0.8));
        assertFalse(decision.meetsThreshold(0.9));
        assertEquals(150, decision.getProcessingTimeMs());

        var contextUpdates = decision.getContextUpdates();
        assertEquals(0.85, contextUpdates.get(CONFIDENCE_KEY).orElse(0.0));
    }

    @Test
    @DisplayName("Should create RoutingDecision with edge conditions")
    void shouldCreateRoutingDecisionWithEdgeConditions() {
        var condition = EdgeCondition.whenContextGreaterThan(
            CONFIDENCE_KEY,
            0.7
        );
        var decision = RoutingDecision.builder()
            .selectedRoute("technical-support")
            .confidence(0.85)
            .withEdgeCondition(condition)
            .addFallbackRoute(NodeId.of("general-inquiry"))
            .build();

        assertTrue(decision.getEdgeCondition().isPresent());
        assertEquals(condition, decision.getEdgeCondition().get());
        assertTrue(decision.hasFallbackRoutes());
        assertEquals(
            NodeId.of("general-inquiry"),
            decision.getPrimaryFallbackRoute().get()
        );
    }

    @Test
    @DisplayName("Should create RouteCandidate with NodeId support")
    void shouldCreateRouteCandidateWithNodeIdSupport() {
        // Test string constructor
        var candidate1 = new RouteCandidate("billing-support", 0.72);
        assertEquals("billing-support", candidate1.getRouteId());
        assertEquals(NodeId.of("billing-support"), candidate1.getRouteNodeId());
        assertEquals(0.72, candidate1.getScore());

        // Test NodeId constructor
        var nodeId = NodeId.of("technical-support");
        var candidate2 = new RouteCandidate(nodeId, 0.95);
        assertEquals("technical-support", candidate2.getRouteId());
        assertEquals(nodeId, candidate2.getRouteNodeId());
        assertEquals(0.95, candidate2.getScore());

        // Test factory methods
        var highConfidence = RouteCandidate.highConfidence(nodeId);
        assertEquals(1.0, highConfidence.getScore());
        assertEquals(nodeId, highConfidence.getRouteNodeId());

        var mediumConfidence = RouteCandidate.mediumConfidence(
            "general-inquiry"
        );
        assertEquals(0.5, mediumConfidence.getScore());
        assertEquals("general-inquiry", mediumConfidence.getRouteId());

        assertTrue(candidate2.meetsThreshold(0.9));
        assertFalse(candidate1.meetsThreshold(0.8));
        assertTrue(candidate2.compareScore(candidate1) > 0);
    }

    @Test
    @DisplayName("Should create RoutingStrategy with configuration")
    void shouldCreateRoutingStrategyWithConfiguration() {
        var strategy = RoutingStrategy.basic(
            "Customer support routing based on content analysis",
            String.class,
            availableRoutes
        );

        assertEquals(
            "Customer support routing based on content analysis",
            strategy.getDescription()
        );
        assertTrue(strategy.supportsContentType(String.class));
        assertFalse(strategy.supportsContentType(Integer.class));
        assertEquals(availableRoutes, strategy.getDefaultRoutes());
        assertEquals(0.6, strategy.getMinimumConfidence());
        assertFalse(strategy.getFallbackNode().isPresent());

        var criteria = strategy.getRoutingCriteria();
        assertEquals(0.6, criteria.get("confidence_threshold"));
        assertEquals(false, criteria.get("fallback_enabled"));
        assertEquals(3, criteria.get("default_route_count"));
    }

    @Test
    @DisplayName("Should create RoutingStrategy with fallback configuration")
    void shouldCreateRoutingStrategyWithFallbackConfiguration() {
        var fallbackNode = NodeId.of("default-support");
        var strategy = RoutingStrategy.withFallback(
            "Advanced routing with fallback",
            String.class,
            availableRoutes,
            fallbackNode,
            0.75
        );

        assertEquals(
            "Advanced routing with fallback",
            strategy.getDescription()
        );
        assertTrue(strategy.supportsContentType(String.class));
        assertEquals(availableRoutes, strategy.getDefaultRoutes());
        assertEquals(fallbackNode, strategy.getFallbackNode().get());
        assertEquals(0.75, strategy.getMinimumConfidence());

        var criteria = strategy.getRoutingCriteria();
        assertEquals(0.75, criteria.get("confidence_threshold"));
        assertEquals(true, criteria.get("fallback_enabled"));
    }

    @Test
    @DisplayName("Should create RoutingPerformance with metrics")
    void shouldCreateRoutingPerformanceWithMetrics() {
        var performance = RoutingPerformance.builder()
            .routerName("ml-content-classifier")
            .contentType("CustomerMessage")
            .routeCount(5)
            .estimatedProcessingTimeMs(250)
            .actualProcessingTimeMs(230)
            .confidenceScore(0.92)
            .throughputPerSecond(4.2)
            .contentComplexity(7)
            .addResourceMetric("cpu_usage", 0.45)
            .addResourceMetric("memory_mb", 128)
            .addQualityMetric("accuracy", 0.94)
            .addQualityMetric("precision", 0.89)
            .build();

        assertEquals("ml-content-classifier", performance.getRouterName());
        assertEquals("CustomerMessage", performance.getContentType());
        assertEquals(5, performance.getRouteCount());
        assertEquals(250, performance.getEstimatedProcessingTimeMs());
        assertEquals(230, performance.getActualProcessingTimeMs());
        assertEquals(0.92, performance.getConfidenceScore());
        assertEquals(4.2, performance.getThroughputPerSecond());
        assertEquals(7, performance.getContentComplexity());

        // Test time estimation accuracy
        assertEquals(0.92, performance.getTimeEstimationAccuracy(), 0.01);
        assertFalse(performance.exceededEstimate());
        assertEquals(-20, performance.getProcessingTimeVariance());

        // Test operations per minute calculation
        assertEquals(240.0, performance.getEstimatedOperationsPerMinute(), 0.1);

        // Test resource and quality metrics
        assertEquals(0.45, performance.<Double>getResourceMetric("cpu_usage"));
        assertEquals(128, performance.<Integer>getResourceMetric("memory_mb"));
        assertEquals(0.94, performance.<Double>getQualityMetric("accuracy"));
        assertEquals(0.89, performance.<Double>getQualityMetric("precision"));

        // Test performance summary
        String summary = performance.getPerformanceSummary();
        assertTrue(summary.contains("ml-content-classifier"));
        assertTrue(summary.contains("230ms"));
        assertTrue(summary.contains("0.92"));
    }

    @Test
    @DisplayName("Should implement ContentRouter interface")
    void shouldImplementContentRouterInterface() {
        var router = new TestContentRouter();
        var state = GraphWorkflowState.create(
            workflowId,
            "test message",
            NodeId.of("start"),
            testContext
        );

        assertEquals("test-content-router", router.getRouterName());
        assertEquals("Test Content Router", router.getName());
        assertTrue(router.getDescription().contains("test router"));
        assertEquals(NodeId.of("test-content-router"), router.getNodeId());

        // Test routing capability validation
        assertTrue(
            router.canRoute("test message", availableRoutes, testContext)
        );
        assertFalse(router.canRoute(null, availableRoutes, testContext));
        assertFalse(router.canRoute("test", Set.of(), testContext));

        // Test minimum confidence threshold
        assertEquals(0.7, router.getMinimumConfidenceThreshold());

        // Test processing time estimation
        assertEquals(
            100,
            router.estimateProcessingTime("test", availableRoutes)
        );

        // Test routing performance
        var performance = router.getRoutingPerformance(
            "test",
            availableRoutes,
            testContext
        );
        assertEquals("test-content-router", performance.getRouterName());
        assertEquals("String", performance.getContentType());
        assertEquals(3, performance.getRouteCount());
    }

    @Test
    @DisplayName("Should process workflow with routing integration")
    void shouldProcessWorkflowWithRoutingIntegration() {
        var router = new TestContentRouter();
        var context = testContext.with(
            TestContentRouter.ROUTES_KEY,
            (Object) Set.of("technical-support", "billing-support")
        );
        var state = GraphWorkflowState.create(
            workflowId,
            "technical issue with billing",
            NodeId.of("router"),
            context
        );

        var result = router.process(state);

        assertTrue(result.isSuccess());
        var command = result.getValue().get();
        assertInstanceOf(GraphCommand.Traverse.class, command);

        var traverseCommand = (GraphCommand.Traverse<String>) command;
        assertEquals(
            NodeId.of("technical-support"),
            traverseCommand.targetNode()
        );
        assertTrue(traverseCommand.getContextUpdates().isPresent());

        var updatedContext = traverseCommand.getContextUpdates().get();
        assertTrue(
            updatedContext
                .get(router.getRoutingStrategy().getConfidenceContextKey())
                .isPresent()
        );
        assertTrue(
            updatedContext
                .get(router.getRoutingStrategy().getReasoningContextKey())
                .isPresent()
        );
    }

    @Test
    @DisplayName("Should handle routing failures with fallback")
    void shouldHandleRoutingFailuresWithFallback() {
        var router = new TestContentRouter();
        var context = testContext.with(
            TestContentRouter.ROUTES_KEY,
            (Object) Set.of("unknown-route")
        );
        var state = GraphWorkflowState.create(
            workflowId,
            "unclear message",
            NodeId.of("router"),
            context
        );

        var result = router.process(state);

        assertTrue(result.isSuccess());
        var command = result.getValue().get();

        // Should either route to fallback or suspend for manual review
        assertTrue(
            command instanceof GraphCommand.Traverse ||
            command instanceof GraphCommand.Suspend
        );

        if (command instanceof GraphCommand.Suspend<String> suspendCommand) {
            assertTrue(suspendCommand.reason().contains("Confidence"));
        }
    }

    @Test
    @DisplayName("Should support asynchronous routing")
    void shouldSupportAsynchronousRouting() throws Exception {
        var router = new TestContentRouter();

        var future = router.analyzeContentAsync(
            "technical issue with billing",
            availableRoutes,
            testContext
        );
        assertNotNull(future);

        var decision = future.get();
        assertNotNull(decision);
        assertEquals(
            NodeId.of("technical-support"),
            decision.getSelectedRoute()
        );
        assertTrue(decision.getConfidence() > 0);
    }

    @Test
    @DisplayName("Should validate routing decisions")
    void shouldValidateRoutingDecisions() {
        var strategy = RoutingStrategy.basic(
            "Test strategy",
            String.class,
            availableRoutes
        );

        var validDecision = RoutingDecision.simple(
            NodeId.of("technical-support"),
            0.8
        );
        assertTrue(strategy.validateRoutingDecision(validDecision));

        var lowConfidenceDecision = RoutingDecision.simple(
            NodeId.of("technical-support"),
            0.3
        );
        assertFalse(strategy.validateRoutingDecision(lowConfidenceDecision));

        var invalidRouteDecision = RoutingDecision.simple(
            NodeId.of("unknown-route"),
            0.9
        );
        assertFalse(strategy.validateRoutingDecision(invalidRouteDecision));
    }

    /**
     * Test implementation of ContentRouter for testing purposes.
     */
    private static class TestContentRouter implements ContentRouter<String> {

        public static final ContextKey<Object> ROUTES_KEY = ContextKey.of(
            "available.routes",
            Object.class
        );

        private final RoutingStrategy strategy = RoutingStrategy.withFallback(
            "Test routing strategy",
            String.class,
            Set.of(
                NodeId.of("technical-support"),
                NodeId.of("billing-support"),
                NodeId.of("general-inquiry")
            ),
            NodeId.of("general-inquiry"),
            0.7
        );

        @Override
        public RoutingDecision analyzeContent(
            String content,
            Set<NodeId> availableRoutes,
            WorkflowContext context
        ) {
            // Simple routing logic for testing
            if (content.toLowerCase().contains("technical")) {
                return RoutingDecision.builder()
                    .selectedRoute(NodeId.of("technical-support"))
                    .confidence(0.85)
                    .reasoning("Technical keywords detected")
                    .processingTimeMs(50)
                    .build();
            } else if (content.toLowerCase().contains("billing")) {
                return RoutingDecision.builder()
                    .selectedRoute(NodeId.of("billing-support"))
                    .confidence(0.80)
                    .reasoning("Billing keywords detected")
                    .processingTimeMs(45)
                    .build();
            } else {
                return RoutingDecision.builder()
                    .selectedRoute(NodeId.of("general-inquiry"))
                    .confidence(0.65)
                    .reasoning("General content classification")
                    .processingTimeMs(30)
                    .build();
            }
        }

        @Override
        public RoutingStrategy getRoutingStrategy() {
            return strategy;
        }

        @Override
        public double getMinimumConfidenceThreshold() {
            return 0.7;
        }

        @Override
        public String getRouterName() {
            return "test-content-router";
        }

        @Override
        public NodeId getNodeId() {
            return NodeId.of("test-content-router");
        }

        @Override
        public String getName() {
            return "Test Content Router";
        }

        @Override
        public String getDescription() {
            return "A test router for unit testing routing functionality";
        }

        @Override
        public long estimateProcessingTime(
            String content,
            Set<NodeId> availableRoutes
        ) {
            return 100; // Simple fixed estimate for testing
        }
    }
}
