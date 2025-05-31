package dev.agents4j.api;

import dev.agents4j.api.workflow.WorkflowCommand;
import dev.agents4j.api.workflow.WorkflowState;
import dev.agents4j.api.WorkflowNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleApiTest {

    @Test
    public void testWorkflowStateGeneric() {
        // Test with String state data
        WorkflowState<String> stringState = WorkflowState.create("test-workflow", "initial-data");
        assertEquals("test-workflow", stringState.getWorkflowId());
        assertEquals("initial-data", stringState.getData());
        assertTrue(stringState.getContext().isEmpty());
        
        // Test with context updates
        Map<String, Object> contextUpdates = new HashMap<>();
        contextUpdates.put("step", 1);
        contextUpdates.put("user", "test");
        
        WorkflowState<String> updatedState = stringState.withContextUpdates(contextUpdates);
        assertEquals("initial-data", updatedState.getData());
        assertEquals(1, updatedState.getContextValue("step").orElse(0));
        assertEquals("test", updatedState.getContextValue("user").orElse(""));
    }

    @Test
    public void testWorkflowNode() {
        // Create a simple test node
        WorkflowNode<String> testNode = new WorkflowNode<String>() {
            @Override
            public WorkflowCommand<String> process(WorkflowState<String> state) {
                String data = state.getData();
                if (data == null || data.isEmpty()) {
                    return WorkflowCommand.<String>error("No data provided").build();
                }
                
                return WorkflowCommand.<String>continueWith()
                        .updateState("processed", true)
                        .updateState("result", data.toUpperCase())
                        .build();
            }

            @Override
            public String getNodeId() {
                return "test-node";
            }

            @Override
            public String getName() {
                return "Test Node";
            }
        };

        // Test processing with data
        WorkflowState<String> state = WorkflowState.create("test-workflow", "hello world");
        WorkflowCommand<String> command = testNode.process(state);
        
        assertEquals(WorkflowCommand.CommandType.CONTINUE, command.getType());
        assertEquals(Boolean.TRUE, command.getStateUpdates().get("processed"));
        assertEquals("HELLO WORLD", command.getStateUpdates().get("result"));
        
        // Test error case
        WorkflowState<String> emptyState = WorkflowState.create("test-workflow", "");
        WorkflowCommand<String> errorCommand = testNode.process(emptyState);
        
        assertEquals(WorkflowCommand.CommandType.ERROR, errorCommand.getType());
        assertTrue(errorCommand.getErrorMessage().isPresent());
    }

    @Test
    public void testWorkflowCommand() {
        // Test continue command
        WorkflowCommand<String> continueCommand = WorkflowCommand.<String>continueWith()
                .updateState("key1", "value1")
                .updateState("key2", 42)
                .addMetadata("timestamp", System.currentTimeMillis())
                .build();
        
        assertEquals(WorkflowCommand.CommandType.CONTINUE, continueCommand.getType());
        assertEquals("value1", continueCommand.getStateUpdates().get("key1"));
        assertEquals(42, continueCommand.getStateUpdates().get("key2"));
        assertFalse(continueCommand.getMetadata().isEmpty());
        
        // Test complete command
        WorkflowCommand<String> completeCommand = WorkflowCommand.<String>complete()
                .updateState("final", true)
                .build();
        
        assertEquals(WorkflowCommand.CommandType.COMPLETE, completeCommand.getType());
        assertEquals(Boolean.TRUE, completeCommand.getStateUpdates().get("final"));
        
        // Test goto command
        WorkflowCommand<String> gotoCommand = WorkflowCommand.<String>goTo("target-node")
                .updateState("reason", "branching")
                .build();
        
        assertEquals(WorkflowCommand.CommandType.GOTO, gotoCommand.getType());
        assertEquals("target-node", gotoCommand.getTargetNodeId().orElse(""));
        assertEquals("branching", gotoCommand.getStateUpdates().get("reason"));
    }
}