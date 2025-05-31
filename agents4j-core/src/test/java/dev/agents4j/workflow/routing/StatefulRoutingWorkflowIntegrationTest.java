package dev.agents4j.workflow.routing;

import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.routing.ContentRouter;
import dev.agents4j.api.routing.Route;
import dev.agents4j.api.routing.RoutingDecision;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the refactored StatefulRoutingWorkflow.
 * Tests the complete workflow execution using the StatefulWorkflow interface.
 */
class StatefulRoutingWorkflowIntegrationTest {

    private ContentRouter<String> mockRouter;
    private Route<String, String> mockRoute1;
    private Route<String, String> mockRoute2;
    private Route<String, String> mockFallbackRoute;
    private RoutingWorkflow<String, String> workflow;

    @BeforeEach
    void setUp() {
        // Setup mock router
        mockRouter = Mockito.mock(ContentRouter.class);
        when(mockRouter.getRouterName()).thenReturn("TestRouter");
        when(mockRouter.getRouterConfiguration()).thenReturn(Map.of("type", "test"));

        // Setup mock routes
        mockRoute1 = createMockRoute("route1", "Route 1", 1.0, 5);
        mockRoute2 = createMockRoute("route2", "Route 2", 0.8, 3);
        mockFallbackRoute = createMockRoute("fallback", "Fallback Route", 0.0, 1);

        // Create workflow using the StatefulWorkflow interface
        workflow = RoutingWorkflow.<String, String>builder()
            .name("StatefulRoutingTest")
            .router(mockRouter)
            .addRoute(mockRoute1)
            .addRoute(mockRoute2)
            .fallbackRoute(mockFallbackRoute)
            .confidenceThreshold(0.7)
            .enableFallbackOnLowConfidence(true)
            .enableRouteAnalytics(true)
            .build();
    }

    @Test
    void testStatefulWorkflowStart() throws WorkflowExecutionException {
        // Setup routing decision
        RoutingDecision decision = createMockRoutingDecision("route1", 0.9);
        when(mockRouter.route(eq("test input"), any(Set.class), any(Map.class)))
            .thenReturn(decision);

        StatefulWorkflowResult<String> result = workflow.start("test input");

        assertNotNull(result);
        assertTrue(result.isCompleted());
        assertFalse(result.isSuspended());
        assertFalse(result.isError());

        String output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertEquals("route1-result", output);

        // Verify state
        WorkflowState finalState = result.getState();
        assertNotNull(finalState);
        assertTrue(finalState.getWorkflowId().startsWith("StatefulRoutingTest-"));
        assertTrue(finalState.get("execution_successful", false));
        assertEquals("route1", finalState.get("executed_route_id").orElse(null));

        // Verify router was called
        verify(mockRouter, times(1)).route(eq("test input"), any(Set.class), any(Map.class));
    }

    @Test
    void testStatefulWorkflowWithContext() throws WorkflowExecutionException {
        // Setup routing decision
        RoutingDecision decision = createMockRoutingDecision("route2", 0.85);
        when(mockRouter.route(eq("context input"), any(Set.class), any(Map.class)))
            .thenReturn(decision);

        Map<String, Object> context = new HashMap<>();
        context.put("user_id", "test_user");
        context.put("session_id", "test_session");

        StatefulWorkflowResult<String> result = workflow.start("context input", context);

        assertTrue(result.isCompleted());
        
        // Verify context was populated with workflow information
        assertEquals("StatefulRoutingTest", context.get("workflow_name"));
        assertEquals("routing", context.get("workflow_type"));
        assertEquals("TestRouter", context.get("router_name"));

        // Verify original context is preserved
        assertEquals("test_user", context.get("user_id"));
        assertEquals("test_session", context.get("session_id"));
    }

    @Test
    void testStatefulWorkflowAsync() throws Exception {
        // Setup routing decision
        RoutingDecision decision = createMockRoutingDecision("route1", 0.95);
        when(mockRouter.route(eq("async input"), any(Set.class), any(Map.class)))
            .thenReturn(decision);

        CompletableFuture<StatefulWorkflowResult<String>> future = workflow.startAsync("async input");
        StatefulWorkflowResult<String> result = future.get();

        assertTrue(result.isCompleted());
        String output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertEquals("route1-result", output);

        verify(mockRouter, times(1)).route(eq("async input"), any(Set.class), any(Map.class));
    }

    @Test
    void testWorkflowStructure() {
        // Verify the workflow has proper stateful structure
        assertDoesNotThrow(() -> workflow.validate());

        // Verify nodes exist
        assertFalse(workflow.getNodes().isEmpty());
        assertEquals(2, workflow.getNodes().size());

        // Verify specific nodes
        assertTrue(workflow.getNode("content-router").isPresent());
        assertTrue(workflow.getNode("route-executor").isPresent());

        // Verify routes exist
        assertFalse(workflow.getRoutes().isEmpty());
        assertEquals(1, workflow.getRoutes().size());

        // Verify entry points
        assertFalse(workflow.getEntryPoints().isEmpty());
        assertEquals(1, workflow.getEntryPoints().size());
        assertEquals("content-router", workflow.getEntryPoints().get(0).getNodeId());

        // Verify routes from router node
        List<dev.agents4j.api.workflow.WorkflowRoute<String>> routesFromRouter = 
            workflow.getRoutesFrom("content-router");
        assertEquals(1, routesFromRouter.size());
        assertEquals("route-executor", routesFromRouter.get(0).getToNodeId());
    }

    @Test
    void testLowConfidenceFallback() throws WorkflowExecutionException {
        // Setup low confidence routing decision
        RoutingDecision decision = createMockRoutingDecision("route1", 0.5); // Below threshold of 0.7
        when(mockRouter.route(eq("low confidence input"), any(Set.class), any(Map.class)))
            .thenReturn(decision);

        StatefulWorkflowResult<String> result = workflow.start("low confidence input");

        assertTrue(result.isCompleted());
        String output = result.getOutput().orElse(null);
        assertNotNull(output);
        assertEquals("fallback-result", output); // Should use fallback route

        // Verify fallback was used
        WorkflowState finalState = result.getState();
        assertEquals("fallback", finalState.get("executed_route_id").orElse(null));
    }

    @Test
    void testErrorHandling() throws WorkflowExecutionException {
        // Setup router to throw exception
        when(mockRouter.route(eq("error input"), any(Set.class), any(Map.class)))
            .thenThrow(new RuntimeException("Router error"));

        StatefulWorkflowResult<String> result = workflow.start("error input");

        assertTrue(result.isError());
        assertFalse(result.isCompleted());
        assertFalse(result.isSuspended());
        assertTrue(result.getErrorMessage().isPresent());
        assertFalse(result.getOutput().isPresent());

        // Verify error state
        WorkflowState errorState = result.getState();
        assertNotNull(errorState);
        assertFalse(errorState.get("execution_successful", true));
    }

    @Test
    void testWorkflowMetadata() throws WorkflowExecutionException {
        // Setup routing decision
        RoutingDecision decision = createMockRoutingDecision("route2", 0.8);
        when(mockRouter.route(eq("metadata input"), any(Set.class), any(Map.class)))
            .thenReturn(decision);

        StatefulWorkflowResult<String> result = workflow.start("metadata input");

        assertTrue(result.isCompleted());

        // Check metadata
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.containsKey("execution_time"));
        assertTrue((Long) metadata.get("execution_time") >= 0);
    }

    @Test
    void testWorkflowName() {
        assertEquals("StatefulRoutingTest", workflow.getName());
    }

    @Test
    void testAvailableRoutes() {
        Map<String, Route<String, String>> availableRoutes = workflow.getAvailableRoutes();
        assertNotNull(availableRoutes);
        assertEquals(2, availableRoutes.size());
        assertTrue(availableRoutes.containsKey("route1"));
        assertTrue(availableRoutes.containsKey("route2"));
    }

    @Test
    void testGetRouter() {
        ContentRouter<String> router = workflow.getRouter();
        assertNotNull(router);
        assertEquals("TestRouter", router.getRouterName());
    }

    @Test
    void testGetFallbackRoute() {
        Route<String, String> fallback = workflow.getFallbackRoute();
        assertNotNull(fallback);
        assertEquals("fallback", fallback.getId());
    }

    @Test
    void testGetConfidenceThreshold() {
        assertEquals(0.7, workflow.getConfidenceThreshold(), 0.001);
    }

    private Route<String, String> createMockRoute(String id, String description, double confidenceThreshold, int priority) {
        Route<String, String> route = Mockito.mock(Route.class);
        when(route.getId()).thenReturn(id);
        when(route.getDescription()).thenReturn(description);
        when(route.getConfidenceThreshold()).thenReturn(confidenceThreshold);
        when(route.getPriority()).thenReturn(priority);
        when(route.getTags()).thenReturn(List.of("test"));
        when(route.getMetadata()).thenReturn(Map.of("test", "value"));
        when(route.getFallbackRoute()).thenReturn(null);
        
        // Mock strategy and execution
        dev.agents4j.api.strategy.WorkflowExecutionStrategy<String, String> mockStrategy = 
            Mockito.mock(dev.agents4j.api.strategy.WorkflowExecutionStrategy.class);
        when(mockStrategy.getStrategyName()).thenReturn("test-strategy");
        try {
            when(mockStrategy.execute(any(), eq("test input"), any()))
                .thenReturn(id + "-result");
            when(mockStrategy.execute(any(), eq("context input"), any()))
                .thenReturn(id + "-result");
            when(mockStrategy.execute(any(), eq("async input"), any()))
                .thenReturn(id + "-result");
            when(mockStrategy.execute(any(), eq("low confidence input"), any()))
                .thenReturn(id + "-result");
            when(mockStrategy.execute(any(), eq("metadata input"), any()))
                .thenReturn(id + "-result");
        } catch (WorkflowExecutionException e) {
            throw new RuntimeException(e);
        }
        
        when(route.getStrategy()).thenReturn(mockStrategy);
        when(route.getNodes()).thenReturn(List.of());
        
        return route;
    }

    private RoutingDecision createMockRoutingDecision(String selectedRoute, double confidence) {
        RoutingDecision decision = Mockito.mock(RoutingDecision.class);
        when(decision.getSelectedRoute()).thenReturn(selectedRoute);
        when(decision.getConfidence()).thenReturn(confidence);
        when(decision.getReasoning()).thenReturn("Test routing decision");
        when(decision.getAlternatives()).thenReturn(List.of());
        return decision;
    }
}