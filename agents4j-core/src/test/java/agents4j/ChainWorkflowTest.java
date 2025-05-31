package agents4j;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.agents4j.api.AgentNode;
import dev.agents4j.api.exception.WorkflowExecutionException;
import dev.agents4j.workflow.AgentWorkflowFactory;
import dev.agents4j.workflow.ChainWorkflow;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChainWorkflowTest {

    @Test
    void testChainWorkflowExecution() throws WorkflowExecutionException {
        // Create mock agent nodes
        @SuppressWarnings("unchecked")
        AgentNode<String, String> firstNode = Mockito.mock(AgentNode.class);
        when(firstNode.getName()).thenReturn("FirstNode");
        when(firstNode.process(eq("input"), any())).thenReturn("firstOutput");

        @SuppressWarnings("unchecked")
        AgentNode<String, String> secondNode = Mockito.mock(AgentNode.class);
        when(secondNode.getName()).thenReturn("SecondNode");
        when(secondNode.process(eq("firstOutput"), any())).thenReturn(
            "secondOutput"
        );

        @SuppressWarnings("unchecked")
        AgentNode<String, Integer> thirdNode = Mockito.mock(AgentNode.class);
        when(thirdNode.getName()).thenReturn("ThirdNode");
        when(thirdNode.process(eq("secondOutput"), any())).thenReturn(42);

        // Build the chain workflow
        ChainWorkflow<String, Integer> workflow = ChainWorkflow.<
                String,
                Integer
            >builder()
            .name("TestWorkflow")
            .firstNode(firstNode)
            .node(secondNode)
            .node(thirdNode)
            .build();

        // Execute the workflow
        Integer result = workflow.execute("input");

        // Verify the result
        assertEquals(42, result);
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals(3, workflow.getNodes().size());
    }

    @Test
    void testChainWorkflowWithContext() throws WorkflowExecutionException {
        // Create mock agent nodes
        @SuppressWarnings("unchecked")
        AgentNode<String, String> firstNode = Mockito.mock(AgentNode.class);
        when(firstNode.getName()).thenReturn("FirstNode");
        when(firstNode.process(eq("input"), any())).thenAnswer(invocation -> {
            Map<String, Object> context = invocation.getArgument(1);
            context.put("firstNodeProcessed", true);
            return "firstOutput";
        });

        @SuppressWarnings("unchecked")
        AgentNode<String, String> secondNode = Mockito.mock(AgentNode.class);
        when(secondNode.getName()).thenReturn("SecondNode");
        when(secondNode.process(eq("firstOutput"), any())).thenAnswer(
            invocation -> {
                Map<String, Object> context = invocation.getArgument(1);
                assertTrue((Boolean) context.get("firstNodeProcessed"));
                context.put("secondNodeProcessed", true);
                return "finalOutput";
            }
        );

        // Build the chain workflow
        ChainWorkflow<String, String> workflow = ChainWorkflow.<
                String,
                String
            >builder()
            .name("TestWorkflow")
            .firstNode(firstNode)
            .node(secondNode)
            .build();

        // Setup context and execute
        Map<String, Object> context = new HashMap<>();
        String result = workflow.execute("input", context);

        // Verify the result and context
        assertEquals("finalOutput", result);
        assertTrue((Boolean) context.get("firstNodeProcessed"));
        assertTrue((Boolean) context.get("secondNodeProcessed"));
        assertEquals("firstOutput", context.get("result_FirstNode"));
        assertEquals("finalOutput", context.get("result_SecondNode"));
    }

    @Test
    void testChainWorkflowFactoryMethods() {
        // Mock the ChatModel
        ChatModel mockModel = Mockito.mock(ChatModel.class);

        // Create a workflow using the factory
        ChainWorkflow<String, String> workflow =
            AgentWorkflowFactory.createStringChainWorkflow(
                "TestWorkflow",
                mockModel,
                "You are a helpful assistant",
                "You are a summarizer"
            );

        // Verify the workflow structure
        assertEquals("TestWorkflow", workflow.getName());
        assertEquals(2, workflow.getNodes().size());
    }

    @Test
    void testChainWorkflowFactoryWithMemory() {
        // Mock the ChatModel
        ChatModel mockModel = Mockito.mock(ChatModel.class);

        // Create a memory
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(10)
            .build();

        // Create a workflow using the factory
        ChainWorkflow<String, String> workflow =
            AgentWorkflowFactory.createStringChainWorkflowWithMemory(
                "TestWorkflowWithMemory",
                mockModel,
                memory,
                "You are a helpful assistant",
                "You are a summarizer"
            );

        // Verify the workflow structure
        assertEquals("TestWorkflowWithMemory", workflow.getName());
        assertEquals(2, workflow.getNodes().size());
    }
}
