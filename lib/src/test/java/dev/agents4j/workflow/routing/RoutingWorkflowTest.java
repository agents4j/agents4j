package dev.agents4j.workflow.routing;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.routing.RoutingDecision;
import dev.agents4j.workflow.strategy.StrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class RoutingWorkflowTest {

    private TestContentRouter router;
    private Route<String, String> route1;
    private Route<String, String> route2;
    private Route<String, String> fallbackRoute;

    @BeforeEach
    void setUp() {
        router = new TestContentRouter();
        
        route1 = Route.<String, String>builder()
            .id("route1")
            .description("Test route 1")
            .addNode(new TestAgentNode("Node1", "Result from route1"))
            .strategy(StrategyFactory.sequential())
            .build();
            
        route2 = Route.<String, String>builder()
            .id("route2")
            .description("Test route 2")
            .addNode(new TestAgentNode("Node2", "Result from route2"))
            .strategy(StrategyFactory.sequential())
            .build();
            
        fallbackRoute = Route.<String, String>builder()
            .id("fallback")
            .description("Fallback route")
            .addNode(new TestAgentNode("FallbackNode", "Fallback result"))
            .strategy(StrategyFactory.sequential())
            .build();
    }

    @Test
    @DisplayName("Should create routing workflow with valid configuration")
    void shouldCreateRoutingWorkflowWithValidConfiguration() {
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .build();

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals(router, workflow.getRouter());
        assertEquals(2, workflow.getRoutes().size());
        assertTrue(workflow.getRoutes().containsKey("route1"));
        assertTrue(workflow.getRoutes().containsKey("route2"));
    }

    @Test
    @DisplayName("Should throw exception when creating workflow without router")
    void shouldThrowExceptionWhenCreatingWorkflowWithoutRouter() {
        assertThrows(IllegalStateException.class, () -> {
            RoutingWorkflow.<String, String>builder()
                .name("TestWorkflow")
                .addRoute(route1)
                .build();
        });
    }

    @Test
    @DisplayName("Should throw exception when creating workflow without routes")
    void shouldThrowExceptionWhenCreatingWorkflowWithoutRoutes() {
        assertThrows(IllegalStateException.class, () -> {
            RoutingWorkflow.<String, String>builder()
                .name("TestWorkflow")
                .router(router)
                .build();
        });
    }

    @Test
    @DisplayName("Should execute workflow and route to correct path")
    void shouldExecuteWorkflowAndRouteToCorrectPath() throws WorkflowExecutionException {
        router.setRouteDecision("route1", 0.8);
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .build();

        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("test input", context);

        assertEquals("Result from route1", result);
        
        // Check routing decision in context
        RoutingDecision decision = (RoutingDecision) context.get("routing_decision");
        assertNotNull(decision);
        assertEquals("route1", decision.getSelectedRoute());
        assertEquals(0.8, decision.getConfidence());
    }

    @Test
    @DisplayName("Should use fallback route when confidence is below threshold")
    void shouldUseFallbackRouteWhenConfidenceIsBelowThreshold() throws WorkflowExecutionException {
        router.setRouteDecision("route1", 0.4); // Low confidence
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .fallbackRoute(fallbackRoute)
            .confidenceThreshold(0.7)
            .enableFallbackOnLowConfidence(true)
            .build();

        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("test input", context);

        assertEquals("Fallback result", result);
        assertTrue(context.containsKey("low_confidence_fallback"));
        assertEquals("route1", context.get("original_route"));
    }

    @Test
    @DisplayName("Should execute workflow asynchronously")
    void shouldExecuteWorkflowAsynchronously() throws ExecutionException, InterruptedException {
        router.setRouteDecision("route1", 0.9);
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .build();

        CompletableFuture<String> future = workflow.executeAsync("test input");
        String result = future.get();

        assertEquals("Result from route1", result);
    }

    @Test
    @DisplayName("Should handle router failure gracefully")
    void shouldHandleRouterFailureGracefully() {
        router.setShouldFail(true);
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .build();

        assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("test input");
        });
    }

    @Test
    @DisplayName("Should handle route execution failure with fallback")
    void shouldHandleRouteExecutionFailureWithFallback() throws WorkflowExecutionException {
        router.setRouteDecision("route1", 0.9);
        
        // Create a route that will fail
        Route<String, String> failingRoute = Route.<String, String>builder()
            .id("route1")
            .description("Failing route")
            .addNode(new FailingAgentNode())
            .strategy(StrategyFactory.sequential())
            .build();
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(failingRoute)
            .addRoute(route2)
            .fallbackRoute(fallbackRoute)
            .build();

        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("test input", context);

        assertEquals("Fallback result", result);
        assertTrue(context.containsKey("using_workflow_fallback"));
    }

    @Test
    @DisplayName("Should return correct configuration")
    void shouldReturnCorrectConfiguration() {
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .fallbackRoute(fallbackRoute)
            .confidenceThreshold(0.8)
            .build();

        Map<String, Object> config = workflow.getConfiguration();

        assertEquals("routing", config.get("workflowType"));
        assertEquals("TestRouter", config.get("routerName"));
        assertEquals(2, config.get("routeCount"));
        assertEquals(0.8, config.get("confidenceThreshold"));
        assertTrue((Boolean) config.get("hasFallbackRoute"));
        assertNotNull(config.get("routes"));
        assertNotNull(config.get("routerConfiguration"));
    }

    @Test
    @DisplayName("Should handle missing route gracefully")
    void shouldHandleMissingRouteGracefully() {
        router.setRouteDecision("nonexistent", 0.9);
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .build();

        WorkflowExecutionException exception = assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("test input");
        });
        
        assertTrue(exception.getMessage().contains("not found in available routes"));
    }

    @Test
    @DisplayName("Should include routing analytics when enabled")
    void shouldIncludeRoutingAnalyticsWhenEnabled() throws WorkflowExecutionException {
        router.setRouteDecision("route1", 0.9);
        
        RoutingWorkflow<String, String> workflow = RoutingWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .router(router)
            .addRoute(route1)
            .addRoute(route2)
            .enableRouteAnalytics(true)
            .build();

        Map<String, Object> context = new HashMap<>();
        workflow.execute("test input", context);

        assertTrue(context.containsKey("routing_analytics"));
        @SuppressWarnings("unchecked")
        Map<String, Object> analytics = (Map<String, Object>) context.get("routing_analytics");
        assertEquals("TestWorkflow", analytics.get("workflow_name"));
        assertEquals("route1", analytics.get("executed_route_id"));
        assertEquals(0.9, analytics.get("routing_confidence"));
        assertTrue(analytics.containsKey("total_execution_time_ms"));
    }

    // Test helper classes

    private static class TestContentRouter implements ContentRouter<String> {
        private String selectedRoute = "route1";
        private double confidence = 0.8;
        private boolean shouldFail = false;

        public void setRouteDecision(String route, double confidence) {
            this.selectedRoute = route;
            this.confidence = confidence;
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        @Override
        public RoutingDecision route(String input, Set<String> availableRoutes, Map<String, Object> context) 
                throws WorkflowExecutionException {
            if (shouldFail) {
                throw new WorkflowExecutionException("TestRouter", "Simulated router failure");
            }
            
            return RoutingDecision.builder()
                .selectedRoute(selectedRoute)
                .confidence(confidence)
                .reasoning("Test routing decision")
                .build();
        }

        @Override
        public String getRouterName() {
            return "TestRouter";
        }
    }

    private static class TestAgentNode implements AgentNode<String, String> {
        private final String name;
        private final String result;

        public TestAgentNode(String name, String result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String process(String input, Map<String, Object> context) {
            return result;
        }
    }

    private static class FailingAgentNode implements AgentNode<String, String> {
        @Override
        public String getName() {
            return "FailingNode";
        }

        @Override
        public String process(String input, Map<String, Object> context) {
            throw new RuntimeException("Simulated node failure");
        }
    }
}