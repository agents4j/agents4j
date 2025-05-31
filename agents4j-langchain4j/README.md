# Agents4J LangChain4J Integration

This module provides seamless integration between the Agents4J framework and LangChain4J, allowing you to easily create agent nodes powered by LangChain4J's language models and AI services. This module contains both the LangChain4J-specific interfaces and their implementations.

## Features

- **LangChain4J Interfaces**: Specialized interfaces for LangChain4J integration (`LangChain4JAgentNode`)
- **Easy Integration**: Simple factory methods to create agent nodes from LangChain4J AI services
- **Memory Support**: Built-in support for LangChain4J chat memory for conversation history
- **Builder Pattern**: Fluent API for configuring agent nodes with various options
- **Type Safety**: Full generic type support for input/output types
- **Async Support**: Asynchronous processing capabilities inherited from Agents4J framework
- **Base Implementations**: Abstract base classes for common LangChain4J integration patterns

## Installation

Add the dependency to your `build.gradle`:

```gradle
dependencies {
    implementation 'dev.agents4j:agents4j-langchain4j:1.0.0'
}
```

## Core Interfaces

### `LangChain4JAgentNode<I, O>`
Specialized interface for LangChain4J-powered agents, extending the base `AgentNode` interface.

```java
public interface LangChain4JAgentNode<I, O> extends AgentNode<I, O> {
    ChatModel getModel();
    Optional<ChatMemory> getMemory();
}
```

## Quick Start

### Basic Usage

```java
import dev.agents4j.langchain4j.LangChain4JAgentIntegration;
import dev.langchain4j.model.openai.OpenAiChatModel;

// Define your AI service interface
interface ChatAgent {
    String chat(String message);
}

// Create a chat model
ChatModel chatModel = OpenAiChatModel.builder()
    .apiKey("your-api-key")
    .modelName("gpt-3.5-turbo")
    .build();

// Create an agent node
AgentNode<String, String> agentNode = LangChain4JAgentIntegration.createAgentNode(
    ChatAgent.class,
    chatModel
);

// Use the agent node in your workflow
Map<String, Object> context = new HashMap<>();
String response = agentNode.process("Hello, how are you?", context);
```

### With Memory

```java
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

// Create chat memory
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

// Create agent node with memory
AgentNode<String, String> agentNode = LangChain4JAgentIntegration.createAgentNode(
    ChatAgent.class,
    chatModel,
    memory
);
```

### Builder Pattern

```java
AgentNode<String, String> agentNode = LangChain4JAgentIntegration
    .builder(ChatAgent.class)
    .withChatModel(chatModel)
    .withDefaultMemory()  // Uses MessageWindowChatMemory with 10 messages
    .build();
```

### Custom Memory Configuration

```java
ChatMemory customMemory = MessageWindowChatMemory.withMaxMessages(20);

AgentNode<String, String> agentNode = LangChain4JAgentIntegration
    .builder(ChatAgent.class)
    .withChatModel(chatModel)
    .withMemory(customMemory)
    .build();
```

## AI Service Interface Requirements

Your AI service interfaces must follow these requirements:

1. Have at least one method that takes a `String` parameter
2. The method should return a type that can be converted to `String`
3. Preferred method names: `chat`, `ask`, `process`, or `generate`

Example interfaces:

```java
interface ChatAgent {
    String chat(String message);
}

interface QuestionAnsweringAgent {
    String ask(String question);
}

interface TextProcessor {
    String process(String input);
}
```

## Integration with Agents4J Workflows

The created agent nodes implement the `LangChain4JAgentNode` interface and can be used in any Agents4J workflow:

```java
// In a chain workflow
ChainWorkflow<String, String> workflow = AgentWorkflowFactory
    .createChainWorkflow("chat-workflow")
    .addNode(agentNode)
    .build();

// In a routing workflow
RoutingWorkflow<String, String> routingWorkflow = AgentWorkflowFactory
    .createRoutingWorkflow("intelligent-routing")
    .addRoute(WorkflowRoute.builder("chat-route")
        .addNode(agentNode)
        .build())
    .build();
```

## Advanced Usage

### Accessing LangChain4J Components

The adapter provides access to underlying LangChain4J components:

```java
LangChain4JAgentNodeAdapter<ChatAgent> adapter = 
    (LangChain4JAgentNodeAdapter<ChatAgent>) agentNode;

// Access the chat model
ChatModel model = adapter.getModel();

// Access the memory (if configured)
Optional<ChatMemory> memory = adapter.getMemory();

// Access the AI service instance
ChatAgent aiService = adapter.getAiService();
```

### Custom Error Handling

The integration wraps LangChain4J exceptions in runtime exceptions. You can handle these in your workflow:

```java
try {
    String result = agentNode.process(input, context);
} catch (RuntimeException e) {
    // Handle LangChain4J processing errors
    logger.error("Agent processing failed", e);
}
```

## Supported LangChain4J Features

- ✅ Chat Models (OpenAI, Azure OpenAI, Anthropic, etc.)
- ✅ Chat Memory (Message Window, Token Window)
- ✅ AI Services with method-level annotations
- ⚠️ Tools and Function Calling (limited support)
- ❌ Embedding Models (not yet supported)
- ❌ Document Processing (not yet supported)

## Requirements

- Java 17 or higher
- Agents4J API 1.0.0 or higher
- Agents4J Core 1.0.0 or higher (for implementations)
- LangChain4J 1.0.0 or higher

## License

This project is licensed under the MIT License - see the main project LICENSE file for details.