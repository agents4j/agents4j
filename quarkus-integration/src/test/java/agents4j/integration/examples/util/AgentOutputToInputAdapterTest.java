package agents4j.integration.examples.util;

import dev.agents4j.model.AgentInput;
import dev.agents4j.model.AgentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class AgentOutputToInputAdapterTest {

    private AgentOutputToInputAdapter adapter;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        adapter = new AgentOutputToInputAdapter("TestAdapter");
        context = new HashMap<>();
    }

    @Test
    void testGetName() {
        assertEquals("TestAdapter", adapter.getName());
    }

    @Test
    void testGetNameWithDefaultConstructor() {
        AgentOutputToInputAdapter defaultAdapter = new AgentOutputToInputAdapter();
        assertEquals("AgentOutputToInputAdapter", defaultAdapter.getName());
    }

    @Test
    void testProcessBasicConversion() {
        // Given
        AgentOutput output = AgentOutput.builder("Test content")
            .successful(true)
            .build();

        // When
        AgentInput result = adapter.process(output, context);

        // Then
        assertNotNull(result);
        assertEquals("Test content", result.getContent());
        assertTrue(result.getMetadata().isEmpty());
    }

    @Test
    void testProcessWithMetadata() {
        // Given
        AgentOutput output = AgentOutput.builder("Test content with metadata")
            .successful(true)
            .withMetadata("source", "test")
            .withMetadata("timestamp", 12345L)
            .build();

        // When
        AgentInput result = adapter.process(output, context);

        // Then
        assertNotNull(result);
        assertEquals("Test content with metadata", result.getContent());
        assertEquals("test", result.getMetadata().get("source"));
        assertEquals(12345L, result.getMetadata().get("timestamp"));
        assertEquals(2, result.getMetadata().size());
    }

    @Test
    void testProcessWithNullInput() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> adapter.process(null, context)
        );
        assertEquals("Input cannot be null", exception.getMessage());
    }

    @Test
    void testProcessAsync() throws ExecutionException, InterruptedException {
        // Given
        AgentOutput output = AgentOutput.builder("Async test content")
            .successful(true)
            .withMetadata("async", true)
            .build();

        // When
        CompletableFuture<AgentInput> future = adapter.processAsync(output, context);
        AgentInput result = future.get();

        // Then
        assertNotNull(result);
        assertEquals("Async test content", result.getContent());
        assertEquals(true, result.getMetadata().get("async"));
    }

    @Test
    void testCreateWithName() {
        AgentOutputToInputAdapter namedAdapter = AgentOutputToInputAdapter.create("CustomName");
        assertEquals("CustomName", namedAdapter.getName());
    }

    @Test
    void testCreateWithDefaultName() {
        AgentOutputToInputAdapter defaultAdapter = AgentOutputToInputAdapter.create();
        assertEquals("AgentOutputToInputAdapter", defaultAdapter.getName());
    }

    @Test
    void testProcessPreservesFailedStatus() {
        // Given
        AgentOutput output = AgentOutput.builder("Failed content")
            .successful(false)
            .build();

        // When
        AgentInput result = adapter.process(output, context);

        // Then
        assertNotNull(result);
        assertEquals("Failed content", result.getContent());
        // Note: AgentInput doesn't have a success flag, so we just ensure content is preserved
    }

    @Test
    void testProcessWithEmptyContent() {
        // Given
        AgentOutput output = AgentOutput.builder("")
            .successful(true)
            .build();

        // When
        AgentInput result = adapter.process(output, context);

        // Then
        assertNotNull(result);
        assertEquals("", result.getContent());
    }

    @Test
    void testProcessWithComplexMetadata() {
        // Given
        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("string_value", "test");
        complexMetadata.put("integer_value", 42);
        complexMetadata.put("boolean_value", true);
        complexMetadata.put("nested_map", Map.of("key", "value"));

        AgentOutput output = AgentOutput.builder("Complex metadata test")
            .successful(true)
            .withAllMetadata(complexMetadata)
            .build();

        // When
        AgentInput result = adapter.process(output, context);

        // Then
        assertNotNull(result);
        assertEquals("Complex metadata test", result.getContent());
        assertEquals("test", result.getMetadata().get("string_value"));
        assertEquals(42, result.getMetadata().get("integer_value"));
        assertEquals(true, result.getMetadata().get("boolean_value"));
        assertNotNull(result.getMetadata().get("nested_map"));
        assertEquals(4, result.getMetadata().size());
    }
}