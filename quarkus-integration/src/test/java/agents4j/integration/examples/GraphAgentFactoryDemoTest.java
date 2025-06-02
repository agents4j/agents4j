package agents4j.integration.examples;

import static org.junit.jupiter.api.Assertions.*;

import dev.agents4j.api.GraphWorkflow;
import dev.agents4j.api.graph.GraphWorkflowNode;
import dev.agents4j.langchain4j.workflow.GraphAgentFactory;
import dev.agents4j.workflow.GraphWorkflowFactory;
import dev.langchain4j.model.chat.ChatModel;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GraphAgentFactoryDemoTest {

    @Inject
    ChatModel chatModel;

    @BeforeEach
    void setUp() {
        assertNotNull(chatModel, "ChatModel should be injected for testing");
    }

    @Test
    @DisplayName("Demo: Create a simple 2-node sequence workflow")
    public void demoSimpleTwoNodeSequence() {
        System.out.println("\n=== DEMO: Simple 2-Node Sequence ===");

        // Create a simple 2-node workflow
        GraphWorkflow<String, String> workflow = GraphAgentFactory.createLLMSequence(
            "simple-analysis",
            String.class,
            GraphAgentFactory.llmNode(
                "analyzer",
                chatModel,
                "Analyze the input text and identify key themes"
            ),
            GraphAgentFactory.llmNode(
                "summarizer", 
                chatModel,
                "Create a concise summary based on the analysis"
            )
        );

        assertNotNull(workflow);
        assertEquals("simple-analysis", workflow.getName());
        System.out.println("✓ Created 2-node sequence: analyzer → summarizer");
    }

    @Test
    @DisplayName("Demo: Create a complex multi-node workflow with custom extractors")
    public void demoComplexMultiNodeWorkflow() {
        System.out.println("\n=== DEMO: Complex Multi-Node Workflow ===");

        // Create a complex document processing workflow
        GraphWorkflow<DocumentRequest, String> workflow = GraphAgentFactory.createLLMSequence(
            "document-processor",
            DocumentRequest.class,
            GraphAgentFactory.llmNode(
                "content-validator",
                chatModel,
                "Validate that the document content is appropriate and complete",
                state -> "Document to validate: " + state.data().content()
            ),
            GraphAgentFactory.llmNode(
                "content-analyzer",
                chatModel,
                "Analyze the document for key topics, sentiment, and structure",
                state -> "Document content: " + state.data().content() + 
                        "\nDocument type: " + state.data().type()
            ),
            GraphAgentFactory.llmNode(
                "metadata-extractor",
                chatModel,
                "Extract important metadata and keywords from the analyzed content",
                state -> "Previous analysis complete. Extract metadata from: " + state.data().content()
            ),
            GraphAgentFactory.llmNode(
                "final-formatter",
                chatModel,
                "Format the analysis and metadata into a professional report",
                state -> "Document: " + state.data().content() + 
                        "\nTarget format: " + state.data().format()
            )
        );

        assertNotNull(workflow);
        assertEquals("document-processor", workflow.getName());
        System.out.println("✓ Created 4-node sequence: validator → analyzer → extractor → formatter");
    }

    @Test
    @DisplayName("Demo: Create workflow from List of node specifications")
    public void demoWorkflowFromList() {
        System.out.println("\n=== DEMO: Workflow from List ===");

        // Create workflow directly with varargs (simpler than List for demo)
        GraphWorkflow<String, String> workflow = GraphAgentFactory.createLLMSequence(
            "content-pipeline",
            String.class,
            GraphAgentFactory.llmNode(
                "input-cleaner",
                chatModel,
                "Clean and normalize the input text"
            ),
            GraphAgentFactory.llmNode(
                "content-enhancer",
                chatModel,
                "Enhance the content with additional context and details"
            ),
            GraphAgentFactory.llmNode(
                "quality-checker",
                chatModel,
                "Check the quality and completeness of the enhanced content"
            ),
            GraphAgentFactory.llmNode(
                "output-formatter",
                chatModel,
                "Format the final output according to specified standards"
            )
        );

        assertNotNull(workflow);
        assertEquals("content-pipeline", workflow.getName());
        System.out.println("✓ Created workflow from varargs: 4 processing steps");
    }

    @Test
    @DisplayName("Demo: Enhanced GraphWorkflowFactory with arbitrary nodes")
    public void demoEnhancedWorkflowFactory() {
        System.out.println("\n=== DEMO: Enhanced GraphWorkflowFactory ===");

        // Create individual nodes
        GraphWorkflowNode<String> step1 = GraphAgentFactory.createSequenceLLMNode(
            "preprocessing", 
            chatModel, 
            "Preprocess the input data",
            "analysis"
        );

        GraphWorkflowNode<String> step2 = GraphAgentFactory.createSequenceLLMNode(
            "analysis",
            chatModel,
            "Perform detailed analysis",
            "postprocessing"
        );

        GraphWorkflowNode<String> step3 = GraphAgentFactory.createCompletingLLMNode(
            "postprocessing",
            chatModel,
            "Post-process and finalize results"
        );

        // Create workflow using enhanced factory
        GraphWorkflow<String, String> workflow = GraphWorkflowFactory.createSequence(
            "custom-workflow",
            String.class,
            step1,
            step2,
            step3
        );

        assertNotNull(workflow);
        assertEquals("custom-workflow", workflow.getName());
        System.out.println("✓ Created custom workflow: preprocessing → analysis → postprocessing");
    }

    @Test
    @DisplayName("Demo: Compare old vs new approach")
    public void demoOldVsNewApproach() {
        System.out.println("\n=== DEMO: Old vs New Approach ===");

        // OLD APPROACH (limited to 2 nodes, deprecated method)
        System.out.println("OLD APPROACH:");
        GraphWorkflowNode<String> oldNode1 = GraphAgentFactory.createLLMNode(
            "old-step-1",
            chatModel,
            "First step in old approach"
        );
        GraphWorkflowNode<String> oldNode2 = GraphAgentFactory.createLLMNode(
            "old-step-2", 
            chatModel,
            "Second step in old approach"
        );

        @SuppressWarnings("deprecation")
        GraphWorkflow<String, String> oldWorkflow = GraphWorkflowFactory.createSequence(
            "old-workflow",
            oldNode1,
            oldNode2
        );

        assertNotNull(oldWorkflow);
        System.out.println("✓ Old approach: Limited to 2 nodes, uses deprecated method");

        // NEW APPROACH (unlimited nodes, type-safe, proper node routing)
        System.out.println("NEW APPROACH:");
        GraphWorkflow<String, String> newWorkflow = GraphAgentFactory.createLLMSequence(
            "new-workflow",
            String.class,
            GraphAgentFactory.llmNode("step-1", chatModel, "First step with proper routing"),
            GraphAgentFactory.llmNode("step-2", chatModel, "Second step with proper routing"),
            GraphAgentFactory.llmNode("step-3", chatModel, "Third step - unlimited nodes!"),
            GraphAgentFactory.llmNode("step-4", chatModel, "Fourth step - type-safe!"),
            GraphAgentFactory.llmNode("step-5", chatModel, "Fifth step - no 'Node not found' errors!")
        );

        assertNotNull(newWorkflow);
        System.out.println("✓ New approach: Unlimited nodes, type-safe, proper routing, no errors!");
    }

    @Test
    @DisplayName("Demo: Workflow with different input types")
    public void demoDifferentInputTypes() {
        System.out.println("\n=== DEMO: Different Input Types ===");

        // Workflow for processing email requests
        GraphWorkflow<EmailRequest, String> emailWorkflow = GraphAgentFactory.createLLMSequence(
            "email-processor",
            EmailRequest.class,
            GraphAgentFactory.llmNode(
                "email-classifier",
                chatModel,
                "Classify the email type and urgency",
                state -> "From: " + state.data().sender() + 
                        "\nSubject: " + state.data().subject() + 
                        "\nBody: " + state.data().body()
            ),
            GraphAgentFactory.llmNode(
                "response-generator",
                chatModel,
                "Generate an appropriate response based on the classification",
                state -> "Email from " + state.data().sender() + 
                        " about: " + state.data().subject()
            )
        );

        // Workflow for processing data analysis requests
        GraphWorkflow<DataRequest, String> dataWorkflow = GraphAgentFactory.createLLMSequence(
            "data-analyzer",
            DataRequest.class,
            GraphAgentFactory.llmNode(
                "data-validator",
                chatModel,
                "Validate the data format and completeness",
                state -> "Dataset: " + state.data().dataset() + 
                        "\nQuery: " + state.data().query()
            ),
            GraphAgentFactory.llmNode(
                "analysis-engine",
                chatModel,
                "Perform statistical analysis on the validated data",
                state -> "Analyzing: " + state.data().dataset() + 
                        " with parameters: " + state.data().parameters()
            ),
            GraphAgentFactory.llmNode(
                "insight-generator",
                chatModel,
                "Generate business insights from the analysis results",
                state -> "Data analysis complete for: " + state.data().dataset()
            )
        );

        assertNotNull(emailWorkflow);
        assertNotNull(dataWorkflow);
        System.out.println("✓ Created type-safe workflows for EmailRequest and DataRequest");
    }

    @Test
    @DisplayName("Demo: Direct node creation patterns")
    public void demoNodeCreationPatterns() {
        System.out.println("\n=== DEMO: Direct Node Creation Patterns ===");

        // Create workflow with direct node creation
        GraphWorkflow<String, String> workflow = GraphAgentFactory.createLLMSequence(
            "direct-creation-workflow",
            String.class,
            GraphAgentFactory.llmNode("validator", chatModel, "Validate input"),
            GraphAgentFactory.llmNode("processor", chatModel, "Process data", 
                state -> "Processing: " + state.data().toString()),
            GraphAgentFactory.llmNode("formatter", chatModel, "Format output")
        );

        assertNotNull(workflow);
        assertEquals("direct-creation-workflow", workflow.getName());
        System.out.println("✓ Created workflow using direct node creation pattern");
    }

    @Test
    @DisplayName("Demo: Error handling and validation")
    public void demoErrorHandling() {
        System.out.println("\n=== DEMO: Error Handling and Validation ===");

        // Test error handling for empty sequences
        assertThrows(IllegalArgumentException.class, () -> {
            GraphAgentFactory.createLLMSequence("empty", String.class);
        });
        System.out.println("✓ Properly handles empty sequence error");

        assertThrows(IllegalArgumentException.class, () -> {
            GraphWorkflowFactory.createSequence("empty", String.class);
        });
        System.out.println("✓ Properly handles empty workflow error");

        // Test that workflows are properly configured
        GraphWorkflow<String, String> validWorkflow = GraphAgentFactory.createLLMSequence(
            "valid-workflow",
            String.class,
            GraphAgentFactory.llmNode("test", chatModel, "Test node")
        );

        assertNotNull(validWorkflow);
        assertEquals("valid-workflow", validWorkflow.getName());
        assertEquals("1.0.0", validWorkflow.getVersion());
        System.out.println("✓ Valid workflows are properly configured");
    }

    // Test record types for demonstrations
    public record DocumentRequest(String content, String type, String format) {}
    public record EmailRequest(String sender, String subject, String body) {}
    public record DataRequest(String dataset, String query, String parameters) {}
}