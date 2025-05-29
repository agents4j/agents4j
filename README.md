![Agents4J Logo](./docs/logo.png)

# Agents4J

A Java library for building AI agent-based workflows using LangChain4J.

## Overview

Agents4J provides a framework for executing AI Agent Workflows with LangChain4J implementations. It's designed to make it easy for any Java application to create flexible agent workflows by leveraging the power of LangChain4J and Large Language Models.

### Key Features

- **Chain Workflow** - Create workflows where multiple agents pass information sequentially
- **Parallelization Workflow** - Execute multiple operations concurrently for improved throughput
- **Strategy Pattern** - Pluggable execution strategies (sequential, parallel, conditional, batch)
- **Routing Pattern** - Intelligent content classification and routing
- **Orchestrator-Workers Pattern** - Complex task decomposition with specialized workers
- **REST API** - Complete HTTP API for all workflow patterns
- **LangChain4J Integration** - Seamlessly works with LangChain4J's chat models and memory systems
- **Typed Input/Output** - Support for both simple string-based and complex structured data
- **Context Sharing** - Agents can share context and state throughout the workflow
- **Asynchronous Execution** - Built-in support for non-blocking workflow execution
- **Extensible Design** - Create custom agent node types for specialized tasks

## Project Structure

The project is organized into the following modules:

- **lib** - The core library providing the agent workflow functionality
- **quarkus-integration** - A Quarkus REST API service that provides HTTP endpoints for all workflow patterns

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


This module demonstrates how to use the Agents4J library in a Quarkus application.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
