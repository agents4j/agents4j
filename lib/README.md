# Agents4J Library Module

The core library providing agent workflow capabilities using LangChain4J.

## Overview

Agents4J is a framework for executing AI Agent Workflows using LangChain4J implementations. It provides a flexible, extensible way to create agent-based systems with support for various LLM providers through LangChain4J.

## Key Components

### Interfaces

- **AgentNode**: The core interface representing a node in a workflow that can process inputs and produce outputs.
- **AgentWorkflow**: Interface for workflows that coordinate the execution of agent nodes.
- **LangChain4JAgentNode**: Extension of AgentNode that integrates with LangChain4J's ChatModel and ChatMemory.

### Implementations

- **ChainWorkflow**: A workflow that chains multiple agent nodes together where the output of one becomes the input of the next.
- **BaseLangChain4JAgentNode**: Base implementation of LangChain4JAgentNode that handles common functionality.
- **StringLangChain4JAgentNode**: Simple implementation that processes string inputs and produces string outputs.

### Factories

- **AgentWorkflowFactory**: Factory methods for creating various types of agent workflows.

## Usage Examples

### Creating a Simple Chain Workflow

```java
// Create a chain workflow with two agent nodes
ChainWorkflow<String, String> workflow = ChainWorkflow.<String>builder()
    .name("MyWorkflow")
    .firstNode(myFirstAgent) // Takes a String input, returns a String output
    .node(mySecondAgent)     // Takes a String input, returns a String output
    .build();

// Execute the workflow
String result = workflow.execute("Initial input");
```

### Using the Factory to Create a Workflow

```java
// Create a model using LangChain4J (example with OpenAI)
ChatModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-3.5-turbo")
    .build();

// Create a workflow with multiple agents, each with their own system prompts
ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflow(
    "ResearchWorkflow",
    model,
    "You are a research assistant. Your job is to find relevant information about the topic.",
    "You are a summarizer. Your job is to summarize the information provided."
);

// Execute the workflow
String result = workflow.execute("Tell me about artificial intelligence");
```

### Using Chat Memory

```java
// Create a memory for the agent
ChatMemory memory = MessageWindowChatMemory.builder()
    .maxMessages(10)
    .build();

// Create a workflow with memory
ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflowWithMemory(
    "ConversationalWorkflow",
    model,
    memory,
    "You are a helpful assistant that can remember previous messages"
);

// Execute the workflow multiple times with the same memory
String response1 = workflow.execute("My name is John");
String response2 = workflow.execute("What's my name?"); // The agent should remember "John"
```

### Creating a Custom Agent Node

```java
public class MyCustomAgentNode extends BaseLangChain4JAgentNode<CustomInput, CustomOutput> {
    
    public MyCustomAgentNode(String name, ChatModel model, ChatMemory memory, String systemPrompt) {
        super(name, model, memory, systemPrompt);
    }

    @Override
    protected UserMessage createUserMessage(CustomInput input, Map<String, Object> context) {
        // Convert your custom input to a UserMessage
        return UserMessage.from(input.getText());
    }

    @Override
    protected CustomOutput convertToOutput(AiMessage aiMessage, Map<String, Object> context) {
        // Convert the AI response to your custom output
        return new CustomOutput(aiMessage.text());
    }
}
```

## Integration with LangChain4J

This library is designed to work seamlessly with LangChain4J components:

- **ChatModel**: Provides the AI capabilities through various LLM providers
- **ChatMemory**: Maintains conversation history for stateful interactions
- **Message System**: Uses LangChain4J's message types (UserMessage, AiMessage, etc.)

## Dependencies

The library requires:
- Java 17 or higher
- LangChain4J 1.0.1 or higher

The project uses LangChain4J's Bill of Materials (BOM) for consistent dependency management:

```gradle
// LangChain4J BOM for consistent version management
api platform('dev.langchain4j:langchain4j-bom:1.0.1')

// LangChain4J dependencies - versions managed by BOM
api 'dev.langchain4j:langchain4j'
api 'dev.langchain4j:langchain4j-core'
```

## Contributing

Please see the main project README for contribution guidelines.
