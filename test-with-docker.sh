#!/bin/bash

# Script to build and test the project using Docker (no local JDK needed)

set -e

echo "🧪 Testing Luck & Reward Engine with Docker"
echo "============================================"
echo ""

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker is not running!"
    echo ""
    echo "Please start Docker Desktop and try again."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Parse arguments
MODE="${1:-all}"

case "$MODE" in
    compile)
        echo "📦 Compiling code with Maven + JDK 8 in Docker..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn clean compile
        ;;
    
    test)
        echo "🧪 Running unit tests with Maven + JDK 8 in Docker..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn test
        ;;
    
    package)
        echo "📦 Building JAR with Maven + JDK 8 in Docker..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn clean package
        ;;
    
    all)
        echo "📦 Full build: compile, test, and package..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn clean package
        ;;
    
    skip-tests)
        echo "📦 Building JAR (skipping tests)..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn clean package -DskipTests
        ;;
    
    verify)
        echo "🔍 Verifying project with Maven + JDK 8 in Docker..."
        docker run --rm \
          -v "$(pwd)":/app \
          -w /app \
          maven:3.6-jdk-8 \
          mvn verify
        ;;
    
    help|--help|-h)
        echo "Usage: ./test-with-docker.sh [command]"
        echo ""
        echo "Commands:"
        echo "  compile      Compile source code only"
        echo "  test         Run unit tests"
        echo "  package      Build JAR file"
        echo "  all          Compile, test, and package (default)"
        echo "  skip-tests   Build JAR without running tests"
        echo "  verify       Full verification"
        echo "  help         Show this help"
        echo ""
        echo "Examples:"
        echo "  ./test-with-docker.sh              # Full build"
        echo "  ./test-with-docker.sh compile      # Just compile"
        echo "  ./test-with-docker.sh test         # Run tests"
        echo "  ./test-with-docker.sh skip-tests   # Fast build"
        echo ""
        echo "Note: Uses Maven 3.6 + JDK 8 from Docker"
        echo "      No local JDK installation needed!"
        ;;
    
    *)
        echo "❌ Unknown command: $MODE"
        echo ""
        echo "Run './test-with-docker.sh help' for usage"
        exit 1
        ;;
esac

echo ""
echo "✅ Done!"

