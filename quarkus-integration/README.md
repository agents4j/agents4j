# Agents4J Quarkus Integration - Why Chain CLI

This module demonstrates how to use the Agents4J library within a Quarkus application. It implements a simple CLI application that uses a ChainWorkflow to perform 3 consecutive "why" questions given an initial question as input.

## Overview

The "Why Chain CLI" is designed to help users explore topics more deeply by automatically generating and answering a series of progressively deeper "why" questions. This approach, inspired by the "5 Whys" technique often used in root cause analysis, can help uncover fundamental insights about any topic.

## How It Works

1. The application asks you for an initial question
2. It creates a ChainWorkflow with 4 agent nodes:
   - First node: Analyzes your question and asks the first "why" question
   - Second node: Answers the first "why" and asks a second, deeper "why"
   - Third node: Answers the second "why" and asks a third, even deeper "why"
   - Final node: Answers the third "why" and provides a comprehensive summary

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

1. When prompted, enter your initial question
2. The application will process your question through the chain of "why" questions
3. The final result will include answers to all three "why" questions and a comprehensive summary

## Configuration

The OpenAI API key is configured in `src/main/resources/dev.properties`:

```properties
OPENAI_API_KEY=your_api_key_here
```

This properties file is automatically loaded by Quarkus when running in development mode.

## Example

```
Enter your initial question:
Why do leaves change color in the fall?

Processing your question through 3 levels of 'why'...

--- Final Result ---
[Comprehensive answer that explores the chemical processes, evolutionary advantages, 
and ecological significance of leaf color changes in autumn]
```