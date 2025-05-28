![Agents4J Logo](./docs/logo.png)

# Agents4J

A Java library for building AI agent-based workflows using LangChain4J.

## Overview

Agents4J provides a framework for executing AI Agent Workflows with LangChain4J implementations. It's designed to make it easy for any Java application to create flexible agent workflows by leveraging the power of LangChain4J and Large Language Models.

### Key Features

- **Chain Workflow** - Create workflows where multiple agents pass information sequentially
- **Flexible Architecture** - Easily create custom agent implementations for specific use cases
- **LangChain4J Integration** - Seamlessly works with LangChain4J's chat models and memory systems
- **Typed Input/Output** - Support for both simple string-based and complex structured data
- **Context Sharing** - Agents can share context and state throughout the workflow
- **Asynchronous Execution** - Built-in support for non-blocking workflow execution
- **Extensible Design** - Create custom agent node types for specialized tasks

## Project Structure

The project is organized into the following modules:

- **lib** - The core library providing the agent workflow functionality
- **quarkus-integration** - A sample Quarkus application that demonstrates how to use the library and serves as an integration test harness

## Getting Started

### Prerequisites

- JDK 17 or later
- Gradle 7.4 or later
- A LangChain4J-compatible LLM provider (OpenAI, Anthropic, etc.)

### Building the Project

To build the entire project:

```bash
./gradlew build
```

### Running Tests

To run all tests:

```bash
./gradlew test
```

### Quick Start - CLI Examples

The project includes a Quarkus-based CLI application that demonstrates all the Agents4J features:

1. **Set up your environment:**
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   ```

2. **Run examples using the convenient CLI script:**
   ```bash
   # Show available commands
   ./quarkus-integration/agents4j-cli.sh help
   
   # Interactive three why questions example
   ./quarkus-integration/agents4j-cli.sh interactive-why
   
   # Chain workflow examples
   ./quarkus-integration/agents4j-cli.sh chain-workflow
   
   # Parallelization examples
   ./quarkus-integration/agents4j-cli.sh parallelization
   
   # Strategy pattern examples
   ./quarkus-integration/agents4j-cli.sh strategy-pattern
   
   # Routing pattern examples
   ./quarkus-integration/agents4j-cli.sh routing-pattern
   ```

3. **Or run directly with Java:**
   ```bash
   # Build first
   ./gradlew :quarkus-integration:build
   
   # Run examples
   java -jar quarkus-integration/build/quarkus-app/quarkus-run.jar --help
   java -jar quarkus-integration/build/quarkus-app/quarkus-run.jar interactive-why -q "Why is the sky blue?"
   ```

The CLI provides comprehensive examples of:
- **Chain Workflow**: Sequential agent processing
- **Parallelization Workflow**: Concurrent processing for performance
- **Strategy Pattern**: Pluggable execution strategies
- **Routing Pattern**: Intelligent content classification and routing

### Running the Integration Application

To run the Quarkus integration application in dev mode:

```bash
./gradlew quarkus-integration:quarkusDev
```

This will start the application on port 8181.

## Usage Examples

### Simple Chain Workflow

```java
// Create a model using LangChain4J
ChatModel model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-3.5-turbo")
    .build();

// Create a workflow with two agent nodes
ChainWorkflow<String, String> workflow = AgentWorkflowFactory.createStringChainWorkflow(
    "ResearchWorkflow",
    model,
    "You are a research assistant. Find information about the topic.",
    "You are a summarizer. Create a concise summary of the information."
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

### Advanced Usage with Custom Input/Output

```java
// Create a complex agent node
ComplexLangChain4JAgentNode analysisNode = ComplexLangChain4JAgentNode.builder()
    .name("AnalysisNode")
    .model(model)
    .systemPromptTemplate("You are an analyst specialized in {domain}.")
    .userPromptTemplate("Analyze the following: {content}")
    .defaultParameter("domain", "technology")
    .build();

// Build a workflow with the complex node
ChainWorkflow<AgentInput, AgentOutput> workflow = ChainWorkflow.<AgentInput, AgentOutput>builder()
    .name("AdvancedWorkflow")
    .firstNode(analysisNode)
    .build();

// Create a structured input
AgentInput input = AgentInput.builder("Analyze the impact of AI on healthcare")
    .withMetadata("source", "user_query")
    .withParameter("domain", "healthcare")
    .build();

// Execute the workflow
AgentOutput output = workflow.execute(input);
```

## Module Documentation

### Core Library (lib)

The core library provides the fundamental functionality for building AI agent workflows. It includes:

- API interfaces for agent nodes and workflows
- Implementation of the Chain Workflow pattern
- LangChain4J integrations for various LLM providers
- Utility classes and builders for easier workflow creation

See the [lib README](lib/README.md) for more details on the available APIs.

### Quarkus Integration (quarkus-integration)

This module demonstrates how to use the Agents4J library in a Quarkus application.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
