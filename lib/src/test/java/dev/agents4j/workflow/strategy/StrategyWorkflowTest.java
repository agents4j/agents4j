package dev.agents4j.workflow.strategy;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.strategy.WorkflowExecutionStrategy;
import dev.agents4j.workflow.StrategyWorkflow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class StrategyWorkflowTest {

    private TestAgentNode node1;
    private TestAgentNode node2;
    private TestAgentNode node3;
    private TestExecutionStrategy strategy;

    @BeforeEach
    void setUp() {
        node1 = new TestAgentNode("Node1", "processed by node1");
        node2 = new TestAgentNode("Node2", "processed by node2");
        node3 = new TestAgentNode("Node3", "processed by node3");
        strategy = new TestExecutionStrategy();
    }

    @Test
    @DisplayName("Should create workflow with valid parameters")
    void shouldCreateWorkflowWithValidParameters() {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .addNode(node2)
            .build();

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals(strategy, workflow.getStrategy());
        assertEquals(2, workflow.getNodes().size());
    }

    @Test
    @DisplayName("Should throw exception when creating workflow without strategy")
    void shouldThrowExceptionWhenCreatingWorkflowWithoutStrategy() {
        assertThrows(IllegalStateException.class, () -> {
            StrategyWorkflow.<String, String>builder()
                .name("TestWorkflow")
                .addNode(node1)
                .build();
        });
    }

    @Test
    @DisplayName("Should throw exception when creating workflow without nodes")
    void shouldThrowExceptionWhenCreatingWorkflowWithoutNodes() {
        assertThrows(IllegalStateException.class, () -> {
            StrategyWorkflow.<String, String>builder()
                .name("TestWorkflow")
                .strategy(strategy)
                .build();
        });
    }

    @Test
    @DisplayName("Should execute workflow successfully")
    void shouldExecuteWorkflowSuccessfully() throws WorkflowExecutionException {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .addNode(node2)
            .build();

        String result = workflow.execute("test input");

        assertEquals("test strategy result", result);
        assertTrue(strategy.wasExecuted());
    }

    @Test
    @DisplayName("Should execute workflow with context")
    void shouldExecuteWorkflowWithContext() throws WorkflowExecutionException {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .defaultContext("defaultKey", "defaultValue")
            .build();

        Map<String, Object> context = new HashMap<>();
        context.put("testKey", "testValue");

        String result = workflow.execute("test input", context);

        assertEquals("test strategy result", result);
        assertTrue(strategy.wasExecuted());
        
        // Verify context was merged
        Map<String, Object> executionContext = strategy.getLastExecutionContext();
        assertEquals("defaultValue", executionContext.get("defaultKey"));
        assertEquals("testValue", executionContext.get("testKey"));
        assertEquals("TestWorkflow", executionContext.get("workflow_name"));
        assertEquals("strategy", executionContext.get("workflow_type"));
    }

    @Test
    @DisplayName("Should execute workflow asynchronously")
    void shouldExecuteWorkflowAsynchronously() throws ExecutionException, InterruptedException {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .build();

        CompletableFuture<String> future = workflow.executeAsync("test input");
        String result = future.get();

        assertEquals("test strategy result", result);
        assertTrue(strategy.wasExecuted());
    }

    @Test
    @DisplayName("Should handle strategy execution failure")
    void shouldHandleStrategyExecutionFailure() {
        TestExecutionStrategy failingStrategy = new TestExecutionStrategy(true);
        
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(failingStrategy)
            .addNode(node1)
            .build();

        assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("test input");
        });
    }

    @Test
    @DisplayName("Should validate strategy can execute nodes")
    void shouldValidateStrategyCanExecuteNodes() {
        TestExecutionStrategy restrictiveStrategy = new TestExecutionStrategy(false, false);
        
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(restrictiveStrategy)
            .addNode(node1)
            .build();

        assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("test input");
        });
    }

    @Test
    @DisplayName("Should return correct configuration")
    void shouldReturnCorrectConfiguration() {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .addNode(node2)
            .defaultContext("configKey", "configValue")
            .build();

        Map<String, Object> config = workflow.getConfiguration();

        assertEquals("strategy", config.get("workflowType"));
        assertEquals("TestStrategy", config.get("strategyName"));
        assertEquals(2, config.get("nodeCount"));
        assertArrayEquals(new String[]{"Node1", "Node2"}, (String[]) config.get("nodes"));
        assertNotNull(config.get("strategyConfiguration"));
        assertNotNull(config.get("defaultContext"));
    }

    @Test
    @DisplayName("Should get execution characteristics")
    void shouldGetExecutionCharacteristics() {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.<String, String>builder()
            .name("TestWorkflow")
            .strategy(strategy)
            .addNode(node1)
            .build();

        Map<String, Object> characteristics = workflow.getExecutionCharacteristics(Map.of("testParam", "testValue"));

        assertEquals(1, characteristics.get("nodeCount"));
        assertEquals("TestStrategy", characteristics.get("strategyType"));
    }

    @Test
    @DisplayName("Should create workflow using static create method")
    void shouldCreateWorkflowUsingStaticCreateMethod() {
        StrategyWorkflow<String, String> workflow = StrategyWorkflow.create(
            "TestWorkflow", 
            strategy, 
            node1, 
            node2
        );

        assertNotNull(workflow);
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals(2, workflow.getNodes().size());
    }

    // Test helper classes

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

    private static class TestExecutionStrategy implements WorkflowExecutionStrategy<String, String> {
        private boolean executed = false;
        private final boolean shouldFail;
        private final boolean canExecute;
        private Map<String, Object> lastExecutionContext;

        public TestExecutionStrategy() {
            this(false, true);
        }

        public TestExecutionStrategy(boolean shouldFail) {
            this(shouldFail, true);
        }

        public TestExecutionStrategy(boolean shouldFail, boolean canExecute) {
            this.shouldFail = shouldFail;
            this.canExecute = canExecute;
        }

        @Override
        public String execute(List<AgentNode<?, ?>> nodes, String input, Map<String, Object> context)
                throws WorkflowExecutionException {
            executed = true;
            lastExecutionContext = new HashMap<>(context);
            
            if (shouldFail) {
                throw new WorkflowExecutionException("TestStrategy", "Simulated failure");
            }
            
            return "test strategy result";
        }

        @Override
        public String getStrategyName() {
            return "TestStrategy";
        }

        @Override
        public boolean canExecute(List<AgentNode<?, ?>> nodes, Map<String, Object> context) {
            return canExecute;
        }

        @Override
        public Map<String, Object> getExecutionCharacteristics(List<AgentNode<?, ?>> nodes, Map<String, Object> context) {
            Map<String, Object> characteristics = new HashMap<>();
            characteristics.put("nodeCount", nodes.size());
            characteristics.put("strategyType", getStrategyName());
            return characteristics;
        }

        public boolean wasExecuted() {
            return executed;
        }

        public Map<String, Object> getLastExecutionContext() {
            return lastExecutionContext;
        }
    }
}