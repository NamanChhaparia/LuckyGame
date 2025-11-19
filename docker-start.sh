#!/bin/bash

# Luck & Reward Engine - Docker Quick Start Script
# This script makes it easy to run the application with Docker

set -e  # Exit on error

echo "🐳 Luck & Reward Engine - Docker Quick Start"
echo "=============================================="
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed!"
    echo ""
    echo "Please install Docker Desktop:"
    echo "  macOS: https://www.docker.com/products/docker-desktop/"
    echo "  Windows: https://www.docker.com/products/docker-desktop/"
    echo "  Linux: curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh"
    echo ""
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker is not running!"
    echo ""
    echo "Please start Docker Desktop and try again."
    echo "  macOS: Open Docker from Applications"
    echo "  Windows: Open Docker Desktop from Start Menu"
    echo ""
    exit 1
fi

echo "✅ Docker is installed and running"
echo ""

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null; then
    echo "⚠️  docker-compose not found, using 'docker compose' instead"
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# Parse arguments
MODE="${1:-dev}"

case "$MODE" in
    dev|development)
        echo "🚀 Starting in DEVELOPMENT mode..."
        echo "   - Using H2 in-memory database"
        echo "   - Sample data will be loaded"
        echo ""
        $COMPOSE_CMD up --build
        ;;
    
    prod|production)
        echo "🚀 Starting in PRODUCTION mode..."
        echo "   - Using PostgreSQL database"
        echo "   - Data will persist in volume"
        echo ""
        $COMPOSE_CMD -f docker-compose-prod.yml up --build
        ;;
    
    stop)
        echo "🛑 Stopping application..."
        $COMPOSE_CMD down
        echo "✅ Application stopped"
        ;;
    
    restart)
        echo "🔄 Restarting application..."
        $COMPOSE_CMD restart
        echo "✅ Application restarted"
        ;;
    
    logs)
        echo "📋 Showing application logs..."
        echo "   Press Ctrl+C to exit"
        echo ""
        $COMPOSE_CMD logs -f
        ;;
    
    clean)
        echo "🧹 Cleaning up Docker resources..."
        $COMPOSE_CMD down -v
        docker system prune -f
        echo "✅ Cleanup complete"
        ;;
    
    status)
        echo "📊 Container Status:"
        docker ps --filter "name=luck"
        echo ""
        echo "💾 Images:"
        docker images | grep luck
        ;;
    
    help|--help|-h)
        echo "Usage: ./docker-start.sh [command]"
        echo ""
        echo "Commands:"
        echo "  dev          Start in development mode (default)"
        echo "  prod         Start in production mode with PostgreSQL"
        echo "  stop         Stop the application"
        echo "  restart      Restart the application"
        echo "  logs         View application logs"
        echo "  clean        Stop and remove all containers/volumes"
        echo "  status       Show container and image status"
        echo "  help         Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./docker-start.sh              # Start in dev mode"
        echo "  ./docker-start.sh dev          # Start in dev mode"
        echo "  ./docker-start.sh prod         # Start in production mode"
        echo "  ./docker-start.sh logs         # View logs"
        echo "  ./docker-start.sh stop         # Stop application"
        echo ""
        echo "After starting, access:"
        echo "  Application:  http://localhost:8080"
        echo "  H2 Console:   http://localhost:8080/h2-console"
        echo "  API Docs:     See API_TESTING_GUIDE.md"
        ;;
    
    *)
        echo "❌ Unknown command: $MODE"
        echo ""
        echo "Run './docker-start.sh help' for usage information"
        exit 1
        ;;
esac

