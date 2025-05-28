#!/bin/bash

# Agents4J CLI Runner Script
# This script provides a convenient way to run the Agents4J CLI examples

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default values
JAR_PATH="$SCRIPT_DIR/build/quarkus-app/quarkus-run.jar"
BUILD_IF_MISSING=true

# Help function
show_help() {
    echo -e "${BLUE}Agents4J CLI Runner${NC}"
    echo ""
    echo "Usage: $0 [options] [command] [command-options]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  --no-build          Don't build if JAR is missing"
    echo "  --jar-path PATH     Use custom JAR path"
    echo ""
    echo "Commands:"
    echo "  help                Show CLI help"
    echo "  interactive-why     Interactive three why questions example"
    echo "  chain-workflow      Chain workflow examples"
    echo "  parallelization     Parallelization workflow examples"
    echo "  strategy-pattern    Strategy pattern examples"
    echo "  routing-pattern     Routing pattern examples"
    echo ""
    echo "Examples:"
    echo "  $0 help"
    echo "  $0 interactive-why"
    echo "  $0 interactive-why -q \"Why is the sky blue?\""
    echo "  $0 chain-workflow"
    echo ""
    echo "Environment Variables:"
    echo "  OPENAI_API_KEY      Your OpenAI API key (required)"
    echo ""
}

# Check if API key is set
check_api_key() {
    if [ -z "$OPENAI_API_KEY" ]; then
        echo -e "${RED}Error: OPENAI_API_KEY environment variable is not set${NC}" >&2
        echo -e "${YELLOW}Please set your OpenAI API key:${NC}" >&2
        echo -e "${YELLOW}  export OPENAI_API_KEY=your-api-key-here${NC}" >&2
        exit 1
    fi
}

# Build the application if needed
build_if_needed() {
    if [ ! -f "$JAR_PATH" ]; then
        if [ "$BUILD_IF_MISSING" = true ]; then
            echo -e "${YELLOW}JAR not found, building application...${NC}"
            cd "$PROJECT_ROOT"
            ./gradlew :quarkus-integration:build -x test
            echo -e "${GREEN}Build completed successfully${NC}"
        else
            echo -e "${RED}Error: JAR not found at $JAR_PATH${NC}" >&2
            echo -e "${YELLOW}Run with --build or build manually with: ./gradlew :quarkus-integration:build${NC}" >&2
            exit 1
        fi
    fi
}

# Parse command line arguments
POSITIONAL_ARGS=()
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        --no-build)
            BUILD_IF_MISSING=false
            shift
            ;;
        --jar-path)
            JAR_PATH="$2"
            shift 2
            ;;
        -*|--*)
            # Unknown option, pass to Java application
            POSITIONAL_ARGS+=("$1")
            shift
            ;;
        *)
            # Positional argument, pass to Java application
            POSITIONAL_ARGS+=("$1")
            shift
            ;;
    esac
done

# Restore positional parameters
set -- "${POSITIONAL_ARGS[@]}"

# Check for API key (except for help commands)
if [[ $# -eq 0 || "$1" == "help" || "$1" == "--help" ]]; then
    # For help commands, don't require API key
    :
else
    check_api_key
fi

# Build if needed
build_if_needed

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}Error: JAR file not found at $JAR_PATH${NC}" >&2
    exit 1
fi

# Run the application
echo -e "${BLUE}Running Agents4J CLI...${NC}"
echo ""

if [[ $# -eq 0 ]]; then
    # No arguments, show main help
    java -jar "$JAR_PATH"
elif [[ "$1" == "help" ]]; then
    # Show CLI help
    java -jar "$JAR_PATH" --help
else
    # Pass all arguments to the Java application
    java -jar "$JAR_PATH" "$@"
fi