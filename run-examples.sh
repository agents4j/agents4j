#!/bin/bash

# Agents4J Examples Runner
# This script provides an easy way to run the Agents4J examples

set -e

echo "=== Agents4J Examples Runner ==="
echo ""

# Check if we're in the right directory
if [ ! -f "settings.gradle" ]; then
    echo "Error: Please run this script from the agents4j project root directory"
    exit 1
fi

# Check for API key
DEV_PROPERTIES="quarkus-integration/src/main/resources/dev.properties"
if [ ! -f "$DEV_PROPERTIES" ]; then
    echo "Error: dev.properties file not found at $DEV_PROPERTIES"
    echo "Please create this file with your OpenAI API key:"
    echo "OPENAI_API_KEY=your_api_key_here"
    exit 1
fi

# Check if API key is set
if ! grep -q "OPENAI_API_KEY=" "$DEV_PROPERTIES"; then
    echo "Error: OPENAI_API_KEY not found in $DEV_PROPERTIES"
    echo "Please add your OpenAI API key to the file:"
    echo "OPENAI_API_KEY=your_api_key_here"
    exit 1
fi

echo "Building project..."
./gradlew build

echo ""
echo "Starting Agents4J Examples..."
echo "You will be prompted to choose between:"
echo "1. ChainWorkflow - Interactive 'Why' questions example"
echo "2. ParallelizationWorkflow - Automated parallel processing demos"
echo ""

# Run the quarkus application
./gradlew quarkus-integration:quarkusDev