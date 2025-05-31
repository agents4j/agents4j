package dev.agents4j.api;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.exception.WorkflowExecutionException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test to verify Workflow interface works correctly after removing redundant interfaces.
 */
public class WorkflowTest {

    private TestWorkflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new TestWorkflow();
    }

    @Test
    void testExecuteWithInput() throws WorkflowExecutionException {
        String result = workflow.execute("test input");
        assertEquals("processed: test input", result);
    }

    @Test
    void testExecuteWithInputAndContext() throws WorkflowExecutionException {
        Map<String, Object> context = new HashMap<>();
        context.put("prefix", "custom");

        String result = workflow.execute("test input", context);
        assertEquals("custom: test input", result);
    }

    @Test
    void testExecuteAsync() throws Exception {
        CompletableFuture<String> future = workflow.executeAsync("async test");
        String result = future.get();
        assertEquals("processed: async test", result);
    }

    @Test
    void testExecuteAsyncWithContext() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("prefix", "async");

        CompletableFuture<String> future = workflow.executeAsync(
            "async test",
            context
        );
        String result = future.get();
        assertEquals("async: async test", result);
    }

    @Test
    void testGetName() {
        assertEquals("TestWorkflow", workflow.getName());
    }

    @Test
    void testGetConfiguration() {
        Map<String, Object> config = workflow.getConfiguration();
        assertNotNull(config);
        assertEquals("test", config.get("type"));
    }

    @Test
    void testGetConfigurationProperty() {
        String type = workflow.getConfigurationProperty("type", "default");
        assertEquals("test", type);

        String missing = workflow.getConfigurationProperty(
            "missing",
            "default"
        );
        assertEquals("default", missing);
    }

    /**
     * Simple test implementation of Workflow to verify the interface works.
     */
    private static class TestWorkflow implements Workflow<String, String> {

        private final Map<String, Object> configuration = new HashMap<>();

        public TestWorkflow() {
            configuration.put("type", "test");
        }

        @Override
        public String execute(String input) throws WorkflowExecutionException {
            return execute(input, new HashMap<>());
        }

        @Override
        public String execute(String input, Map<String, Object> context)
            throws WorkflowExecutionException {
            String prefix = (String) context.getOrDefault(
                "prefix",
                "processed"
            );
            return prefix + ": " + input;
        }

        @Override
        public CompletableFuture<String> executeAsync(String input) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execute(input);
                } catch (WorkflowExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public CompletableFuture<String> executeAsync(
            String input,
            Map<String, Object> context
        ) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return execute(input, context);
                } catch (WorkflowExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public String getName() {
            return "TestWorkflow";
        }

        @Override
        public Map<String, Object> getConfiguration() {
            return new HashMap<>(configuration);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getConfigurationProperty(String key, T defaultValue) {
            return (T) configuration.getOrDefault(key, defaultValue);
        }
    }
}
