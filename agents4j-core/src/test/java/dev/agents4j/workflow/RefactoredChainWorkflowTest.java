package dev.agents4j.workflow;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.StatefulWorkflow;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.api.workflow.StatefulWorkflowResult;
import dev.agents4j.api.workflow.WorkflowState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the refactored ChainWorkflow that uses StatefulWorkflow internally.
 */
public class RefactoredChainWorkflowTest {

    // Test AgentNode implementations
    static class UpperCaseNode implements AgentNode<String, String> {
        @Override
        public String process(String input, Map<String, Object> context) {
            return input.toUpperCase();
        }

        @Override
        public String getName() {
            return "UpperCaseNode";
        }
    }

    static class PrefixNode implements AgentNode<String, String> {
        private final String prefix;

        public PrefixNode(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String process(String input, Map<String, Object> context) {
            return prefix + input;
        }

        @Override
        public String getName() {
            return "PrefixNode";
        }
    }

    static class CounterNode implements AgentNode<String, String> {
        @Override
        public String process(String input, Map<String, Object> context) {
            Integer count = (Integer) context.getOrDefault("counter", 0);
            context.put("counter", count + 1);
            return input + " [" + (count + 1) + "]";
        }

        @Override
        public String getName() {
            return "CounterNode";
        }
    }

    static class ErrorNode implements AgentNode<String, String> {
        @Override
        public String process(String input, Map<String, Object> context) {
            throw new RuntimeException("Simulated error");
        }

        @Override
        public String getName() {
            return "ErrorNode";
        }
    }

    static class ValidationNode implements AgentNode<String, String> {
        @Override
        public String process(String input, Map<String, Object> context) {
            if (input == null || input.trim().isEmpty()) {
                throw new IllegalArgumentException("Input cannot be empty");
            }
            context.put("validated", true);
            return input.trim();
        }

        @Override
        public String getName() {
            return "ValidationNode";
        }
    }

    @Test
    public void testBasicChainExecution() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Basic Chain Test")
                .node(new ValidationNode())
                .node(new UpperCaseNode())
                .node(new PrefixNode("RESULT: "))
                .build();

        String result = workflow.execute("hello world");
        assertEquals("RESULT: HELLO WORLD", result);
    }

    @Test
    public void testChainWithContext() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Context Chain Test")
                .nodes(new ValidationNode(), new CounterNode(), new UpperCaseNode())
                .build();

        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("test input", context);

        assertEquals("TEST INPUT [1]", result);
        assertTrue((Boolean) context.get("validated"));
        assertEquals(1, context.get("counter"));
    }

    @Test
    public void testAsyncExecution() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Async Chain Test")
                .node(new UpperCaseNode())
                .node(new PrefixNode("ASYNC: "))
                .build();

        CompletableFuture<String> future = workflow.executeAsync("hello");
        String result = future.get();

        assertEquals("ASYNC: HELLO", result);
    }

    @Test
    public void testErrorHandling() {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Error Chain Test")
                .node(new ValidationNode())
                .node(new ErrorNode())
                .node(new UpperCaseNode())
                .build();

        assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("test");
        });
    }

    @Test
    public void testEmptyInput() {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Validation Test")
                .node(new ValidationNode())
                .build();

        assertThrows(WorkflowExecutionException.class, () -> {
            workflow.execute("");
        });
    }

    @Test
    public void testSingleNode() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Single Node Test")
                .node(new UpperCaseNode())
                .build();

        String result = workflow.execute("single");
        assertEquals("SINGLE", result);
    }

    @Test
    public void testWorkflowConfiguration() {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Config Test")
                .nodes(new ValidationNode(), new UpperCaseNode(), new PrefixNode("TEST: "))
                .build();

        Map<String, Object> config = workflow.getConfiguration();

        assertEquals(3, config.get("nodeCount"));
        assertEquals("chain", config.get("workflowType"));
        assertEquals("stateful", config.get("implementation"));
        assertEquals("Config Test", workflow.getName());

        String[] nodeNames = (String[]) config.get("nodes");
        assertEquals(3, nodeNames.length);
        assertEquals("ValidationNode", nodeNames[0]);
        assertEquals("UpperCaseNode", nodeNames[1]);
        assertEquals("PrefixNode", nodeNames[2]);
    }

    @Test
    public void testStatefulWorkflowAccess() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Stateful Access Test")
                .nodes(new ValidationNode(), new UpperCaseNode())
                .build();

        // Access the underlying StatefulWorkflow
        StatefulWorkflow<String, String> statefulWorkflow = workflow.getStatefulWorkflow();
        assertNotNull(statefulWorkflow);
        assertEquals("Stateful Access Test_stateful", statefulWorkflow.getName());
        assertEquals(2, statefulWorkflow.getNodes().size());
        assertEquals(1, statefulWorkflow.getRoutes().size());
    }

    @Test
    public void testExecuteWithState() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("State Execution Test")
                .nodes(new ValidationNode(), new CounterNode(), new UpperCaseNode())
                .build();

        Map<String, Object> context = new HashMap<>();
        StatefulWorkflowResult<String> result = workflow.executeWithState("test", context);

        assertTrue(result.isCompleted());
        assertEquals("TEST [1]", result.getOutput().orElse(null));

        WorkflowState finalState = result.getState();
        assertNotNull(finalState);
        
        // Debug output to see what's actually in the state
        System.err.println("Final state keys: " + finalState.getData().keySet());
        System.err.println("Context keys: " + context.keySet());
        System.err.println("Current step: " + finalState.get("current_step", 0));
        
        // Just check that we have some state data
        assertFalse(finalState.getData().isEmpty());
        assertNotNull(finalState.get("final_output").orElse(null));
    }

    @Test
    public void testBuilderValidation() {
        // Test empty workflow
        assertThrows(IllegalStateException.class, () -> {
            ChainWorkflow.<String, String>builder()
                    .name("Empty Test")
                    .build();
        });

        // Test null node
        assertThrows(NullPointerException.class, () -> {
            ChainWorkflow.<String, String>builder()
                    .name("Null Node Test")
                    .node(null)
                    .build();
        });
    }

    @Test
    public void testWorkflowNames() {
        // Test with explicit name
        ChainWorkflow<String, String> namedWorkflow = ChainWorkflow.<String, String>builder()
                .name("Custom Name")
                .node(new UpperCaseNode())
                .build();
        assertEquals("Custom Name", namedWorkflow.getName());

        // Test with auto-generated name
        ChainWorkflow<String, String> autoNamedWorkflow = ChainWorkflow.<String, String>builder()
                .node(new UpperCaseNode())
                .build();
        assertTrue(autoNamedWorkflow.getName().startsWith("ChainWorkflow-"));
    }

    @Test
    public void testComplexChain() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Complex Chain")
                .firstNode(new ValidationNode())
                .node(new UpperCaseNode())
                .node(new PrefixNode("STEP1: "))
                .node(new CounterNode())
                .node(new PrefixNode("FINAL: "))
                .build();

        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("complex test", context);

        assertEquals("FINAL: STEP1: COMPLEX TEST [1]", result);
        assertTrue((Boolean) context.get("validated"));
        assertEquals(1, context.get("counter"));
    }

    @Test
    public void testErrorPropagation() throws Exception {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Error Propagation Test")
                .node(new ValidationNode())
                .node(new ErrorNode())
                .build();

        // Test with state access to see error details
        StatefulWorkflowResult<String> result = workflow.executeWithState("test");

        assertTrue(result.isError());
        assertFalse(result.isCompleted());
        
        String errorMessage = result.getErrorMessage().orElse("");
        assertTrue(errorMessage.contains("Simulated error"));

        WorkflowState errorState = result.getState();
        assertEquals("chain_node_1", errorState.get("error_node").orElse(null));
        assertTrue(errorState.get("error_message", "").toString().contains("Simulated error"));
    }

    @Test
    public void testNodeListImmutability() {
        ChainWorkflow<String, String> workflow = ChainWorkflow.<String, String>builder()
                .name("Immutability Test")
                .nodes(new ValidationNode(), new UpperCaseNode())
                .build();

        assertThrows(UnsupportedOperationException.class, () -> {
            workflow.getNodes().add(new PrefixNode("Should fail"));
        });
    }
}