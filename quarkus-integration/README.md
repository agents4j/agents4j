# Agents4J Quarkus Integration - Examples CLI

This module demonstrates how to use the Agents4J library within a Quarkus application. It includes interactive CLI examples showcasing different workflow types:

1. **ChainWorkflow** - Interactive "Why" questions example
2. **ChainWorkflow** - Comprehensive sequential processing examples
3. **ParallelizationWorkflow** - Concurrent processing for batch operations

## Overview

This integration provides three main examples:

### ChainWorkflow Example - "Why Chain CLI"
The "Why Chain CLI" is designed to help users explore topics more deeply by automatically generating and answering a series of progressively deeper "why" questions. This approach, inspired by the "5 Whys" technique often used in root cause analysis, can help uncover fundamental insights about any topic.

### ChainWorkflow Examples - Comprehensive Demonstrations
The comprehensive ChainWorkflow examples demonstrate sequential processing capabilities including:
- Simple string-based workflows using factory methods
- Memory-enabled conversational workflows
- Manual workflow construction with builder pattern
- Advanced workflows with complex agent nodes
- Configuration and context usage patterns

### ParallelizationWorkflow Examples
The ParallelizationWorkflow examples demonstrate concurrent processing capabilities including:
- Batch translation to multiple languages
- Document analysis sectioning
- Asynchronous content generation
- Sentiment analysis of multiple texts
- Performance comparisons between parallel and sequential processing

## How It Works

### ChainWorkflow Example
1. The application asks you for an initial question
2. It creates a ChainWorkflow with 4 agent nodes:
   - First node: Analyzes your question and asks the first "why" question
   - Second node: Answers the first "why" and asks a second, deeper "why"
   - Third node: Answers the second "why" and asks a third, even deeper "why"
   - Final node: Answers the third "why" and provides a comprehensive summary

### ChainWorkflow Examples (Option 2)
1. The application runs a series of demonstrations automatically
2. Each example shows different aspects of sequential processing:
   - **Simple String Workflow**: Basic factory method usage
   - **Memory Workflow**: Conversation history and context
   - **Manual Construction**: Builder pattern and custom nodes
   - **Advanced Workflow**: Complex nodes with structured output
   - **Configured Workflow**: Context and metadata usage

### ParallelizationWorkflow Examples (Option 3)
1. The application runs a series of demonstrations automatically
2. Each example shows different aspects of parallel processing:
   - **Simple Parallel Query**: Sentiment analysis of multiple texts
   - **Batch Translation**: Concurrent translation to different languages
   - **Document Analysis**: Parallel analysis of document sections
   - **Asynchronous Processing**: Non-blocking concurrent operations
   - **Custom Workflow**: Builder pattern with performance measurement

## Running the Application

### Prerequisites

- Java 17 or higher
- Gradle
- An OpenAI API key (stored in `src/main/resources/dev.properties`)

### Build and Run

```bash
# Navigate to the project root
cd agents4j

# Build the project
./gradlew build

# Run the application
./gradlew quarkus-integration:quarkusDev
```

### Usage

1. When the application starts, choose which example to run:
   - Option 1: ChainWorkflow - Three Why Questions
   - Option 2: ChainWorkflow - Comprehensive Examples
   - Option 3: ParallelizationWorkflow - Concurrent Processing Examples

#### For ChainWorkflow (Option 1):
1. Enter your initial question when prompted
2. The application will process your question through the chain of "why" questions
3. The final result will include answers to all three "why" questions and a comprehensive summary

#### For ChainWorkflow Examples (Option 2):
1. The application will automatically run through 5 different examples
2. Each example demonstrates different sequential processing patterns
3. You'll see various workflow construction and configuration techniques

#### For ParallelizationWorkflow (Option 3):
1. The application will automatically run through 5 different examples
2. Each example demonstrates different parallel processing patterns
3. You'll see real-time output showing concurrent operations in action

## Configuration

The OpenAI API key is configured in `src/main/resources/dev.properties`:

```properties
OPENAI_API_KEY=your_api_key_here
```

This properties file is automatically loaded by Quarkus when running in development mode.

## Examples

### ChainWorkflow Example
```
Choose an example to run:
1. ChainWorkflow - Three Why Questions
2. ChainWorkflow - Comprehensive Examples
3. ParallelizationWorkflow - Concurrent Processing Examples
Enter your choice (1, 2, or 3): 1

Enter your initial question:
Why do leaves change color in the fall?

Processing your question through 3 levels of 'why'...

--- Final Result ---
[Comprehensive answer that explores the chemical processes, evolutionary advantages, 
and ecological significance of leaf color changes in autumn]
```

### ParallelizationWorkflow Example
```
Choose an example to run:
1. ChainWorkflow - Three Why Questions
2. ChainWorkflow - Comprehensive Examples
3. ParallelizationWorkflow - Concurrent Processing Examples
Enter your choice (1, 2, or 3): 2

=== ChainWorkflow Examples ===

1. Simple String Workflow
-------------------------
Input: Tell me about artificial intelligence
Result: [AI explanation and summary]
...

2. Workflow with Memory
-----------------------
First interaction:
Input: My name is John Doe
Response: [Greeting response]

Second interaction (testing memory):
Input: What's my name?
Response: [Response showing memory of previous interaction]
...

[Additional examples continue...]
```

### ParallelizationWorkflow Example
```
Choose an example to run:
1. ChainWorkflow - Three Why Questions
2. ChainWorkflow - Comprehensive Examples  
3. ParallelizationWorkflow - Concurrent Processing Examples
Enter your choice (1, 2, or 3): 3

=== ParallelizationWorkflow Examples ===

1. Simple Parallel Query
------------------------
Sentiment Analysis Results:
Text: The weather is beautiful today
Sentiment: POSITIVE
...

2. Batch Translation
-------------------
Translation Results:
Request: Hello, how are you? -> French
Translation: Bonjour, comment allez-vous ?
...

[Additional examples continue...]
```

## Key Features Demonstrated

- **Concurrent Processing**: Multiple LLM calls executed in parallel
- **Batch Operations**: Efficient processing of multiple similar tasks
- **Asynchronous Execution**: Non-blocking operations with CompletableFuture
- **Performance Optimization**: Worker thread configuration and rate limiting
- **Error Handling**: Robust exception handling for parallel operations
- **Builder Pattern**: Flexible workflow construction
- **Context Management**: Execution metadata and debugging support