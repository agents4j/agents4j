package agents4j.integration.examples;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.langchain4j.workflow.GraphAgentFactory;
import dev.agents4j.workflow.GraphWorkflowFactory;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GraphAgentFactoryTest {

    @Inject
    ChatModel chatModel;

    @BeforeEach
    void setUp() {
        assertNotNull(chatModel, "ChatModel should be injected for testing");
    }

    @Test
    @DisplayName(
        "Verify GraphAgentFactory can create sequence with proper node IDs"
    )
    public void testSequenceCreationWithNodeIds() {
        System.out.println("=== Testing Enhanced Sequence Creation ===");

        // Test that GraphAgentFactory can create nodes with explicit next node IDs
        GraphWorkflowNode<String> firstNode =
            GraphAgentFactory.createSequenceLLMNode(
                "step-1",
                chatModel,
                "Process the input text",
                "step-2"
            );

        GraphWorkflowNode<String> secondNode =
            GraphAgentFactory.createSequenceLLMNode(
                "step-2",
                chatModel,
                "Refine the processed text",
                "step-3"
            );

        GraphWorkflowNode<String> thirdNode = GraphAgentFactory.createLLMNode(
            "step-3",
            chatModel,
            "Finalize the output"
        );

        // Verify node IDs
        assertEquals("step-1", firstNode.getNodeId().value());
        assertEquals("step-2", secondNode.getNodeId().value());
        assertEquals("step-3", thirdNode.getNodeId().value());

        // Create workflow using enhanced GraphWorkflowFactory
        GraphWorkflow<String, String> workflow =
            GraphWorkflowFactory.createSequence(
                "multi-step-workflow",
                String.class,
                firstNode,
                secondNode,
                thirdNode
            );

        assertNotNull(workflow);
        assertEquals("multi-step-workflow", workflow.getName());
        assertEquals("1.0.0", workflow.getVersion());

        System.out.println("✓ Successfully created 3-node sequence workflow");
    }

    @Test
    @DisplayName(
        "Verify GraphAgentFactory can create LLM sequences using builder pattern"
    )
    public void testLLMSequenceBuilderPattern() {
        System.out.println("=== Testing LLM Sequence Builder Pattern ===");

        // Create a sequence using the new LLM sequence builder
        GraphWorkflow<String, String> workflow =
            GraphAgentFactory.createLLMSequence(
                "builder-sequence",
                String.class,
                GraphAgentFactory.llmNode(
                    "analyzer",
                    chatModel,
                    "Analyze the input text for sentiment and topics"
                ),
                GraphAgentFactory.llmNode(
                    "summarizer",
                    chatModel,
                    "Create a concise summary of the analyzed content"
                ),
                GraphAgentFactory.llmNode(
                    "formatter",
                    chatModel,
                    "Format the summary into a professional report"
                )
            );

        assertNotNull(workflow);
        assertEquals("builder-sequence", workflow.getName());
        assertEquals("1.0.0", workflow.getVersion());

        System.out.println(
            "✓ Successfully created sequence using builder pattern"
        );
    }

    @Test
    @DisplayName(
        "Verify GraphAgentFactory can create sequences with custom message extractors"
    )
    public void testSequenceWithCustomExtractors() {
        System.out.println(
            "=== Testing Sequence with Custom Message Extractors ==="
        );

        // Create a sequence with custom message extractors
        GraphWorkflow<TestRequest, String> workflow =
            GraphAgentFactory.createLLMSequence(
                "custom-extractor-sequence",
                TestRequest.class,
                GraphAgentFactory.llmNode(
                    "input-processor",
                    chatModel,
                    "Process the user input",
                    state -> "User says: " + state.data().message()
                ),
                GraphAgentFactory.llmNode(
                    "context-adder",
                    chatModel,
                    "Add contextual information to the response",
                    state ->
                        "Previous processing complete. Context: " +
                        state.data().context()
                )
            );

        assertNotNull(workflow);
        assertEquals("custom-extractor-sequence", workflow.getName());

        System.out.println(
            "✓ Successfully created sequence with custom extractors"
        );
    }

    @Test
    @DisplayName(
        "Verify GraphAgentFactory can create sequences from List of specs"
    )
    public void testSequenceFromList() {
        System.out.println("=== Testing Sequence Creation from List ===");

        // Create specs as a list
        List<GraphAgentFactory.LLMNodeSpec<String>> specs = Arrays.asList(
            GraphAgentFactory.llmNode(
                "validator",
                chatModel,
                "Validate the input data"
            ),
            GraphAgentFactory.llmNode(
                "processor",
                chatModel,
                "Process the validated data"
            ),
            GraphAgentFactory.llmNode(
                "output-formatter",
                chatModel,
                "Format the processed data for output"
            )
        );

        GraphWorkflow<String, String> workflow =
            GraphAgentFactory.createLLMSequence(
                "list-based-sequence",
                String.class,
                specs
            );

        assertNotNull(workflow);
        assertEquals("list-based-sequence", workflow.getName());

        System.out.println(
            "✓ Successfully created sequence from List of specs"
        );
    }

    @Test
    @DisplayName("Verify enhanced factory maintains backward compatibility")
    public void testBackwardCompatibility() {
        System.out.println("=== Testing Backward Compatibility ===");

        // Test that original methods still work
        GraphWorkflowNode<String> node1 = GraphAgentFactory.createLLMNode(
            "legacy-node-1",
            chatModel,
            "Legacy node creation"
        );

        GraphWorkflowNode<String> node2 = GraphAgentFactory.createLLMNode(
            "legacy-node-2",
            chatModel,
            "Another legacy node",
            state -> state.data()
        );

        assertNotNull(node1);
        assertNotNull(node2);
        assertEquals("legacy-node-1", node1.getNodeId().value());
        assertEquals("legacy-node-2", node2.getNodeId().value());

        // Test that legacy sequence creation still works
        GraphWorkflow<String, String> legacyWorkflow =
            GraphWorkflowFactory.createSequence(
                "legacy-workflow",
                String.class,
                node1,
                node2
            );

        assertNotNull(legacyWorkflow);
        assertEquals("legacy-workflow", legacyWorkflow.getName());

        System.out.println("✓ Backward compatibility maintained");
    }

    @Test
    @DisplayName("Test sequence workflow error handling")
    public void testSequenceErrorHandling() {
        System.out.println("=== Testing Sequence Error Handling ===");

        // Test error handling for empty sequences
        assertThrows(IllegalArgumentException.class, () -> {
            GraphAgentFactory.createLLMSequence("empty-sequence", String.class);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            GraphWorkflowFactory.createSequence("empty-workflow", String.class);
        });

        System.out.println(
            "✓ Error handling works correctly for invalid inputs"
        );
    }

    @Test
    @DisplayName("Verify node specifications are properly configured")
    public void testNodeSpecifications() {
        System.out.println("=== Testing Node Specifications ===");

        // Test simple node spec
        var simpleSpec = GraphAgentFactory.llmNode(
            "simple-node",
            chatModel,
            "Simple processing"
        );

        assertEquals("simple-node", simpleSpec.nodeId());
        assertEquals(chatModel, simpleSpec.model());
        assertEquals("Simple processing", simpleSpec.systemPrompt());
        assertNull(simpleSpec.userMessageExtractor());

        // Test node spec with extractor
        var extractorSpec = GraphAgentFactory.llmNode(
            "extractor-node",
            chatModel,
            "Processing with extractor",
            state -> "Extracted: " + state.data().toString()
        );

        assertEquals("extractor-node", extractorSpec.nodeId());
        assertEquals(chatModel, extractorSpec.model());
        assertEquals("Processing with extractor", extractorSpec.systemPrompt());
        assertNotNull(extractorSpec.userMessageExtractor());

        System.out.println("✓ Node specifications configured correctly");
    }

    @Test
    @DisplayName(
        "Integration test - verify enhanced workflow doesn't break existing API"
    )
    public void testIntegrationWithExistingAPI() {
        System.out.println("=== Testing Integration with Existing API ===");

        // Test that the enhanced factory works with the existing SnarkyResponseResource pattern
        String requestBody =
            """
            {
                "question": "Test the enhanced GraphAgentFactory integration"
            }
            """;

        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/api/snarky")
            .then()
            .extract()
            .response();

        System.out.println("Response Status: " + response.getStatusCode());
        System.out.println("Response Body: " + response.asString());

        // Verify that we don't get the old "Node not found: next" error
        String responseBody = response.asString();
        assertFalse(
            responseBody.contains("Node not found: next"),
            "Enhanced factory should not cause 'Node not found: next' errors"
        );

        // We expect either 500 (API key error) or 200 (success)
        int statusCode = response.getStatusCode();
        assertTrue(
            statusCode == 500 || statusCode == 200,
            "Should get proper workflow execution, not structural errors. Got: " +
            statusCode
        );

        if (statusCode == 500) {
            assertTrue(
                responseBody.contains("API key") ||
                responseBody.contains("Authentication"),
                "500 errors should be API-related, not workflow structure related"
            );
        }

        System.out.println(
            "✓ Enhanced factory integrates properly with existing API"
        );
    }

    /**
     * Test record for custom extractor testing
     */
    public record TestRequest(String message, String context) {}
}
