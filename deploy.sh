#!/bin/bash

# Production Deployment Script for ATS Application
# This script handles the deployment of the ATS application to production

set -e  # Exit on any error

# Configuration
APP_NAME="ats-application"
DOCKER_IMAGE="ats-app"
DOCKER_TAG="latest"
CONTAINER_NAME="ats-container"
LOG_DIR="./logs"
BACKUP_DIR="./backups"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# Check if required tools are installed
check_dependencies() {
    log "Checking dependencies..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed. Please install Docker first."
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        error "Docker Compose is not installed. Please install Docker Compose first."
    fi
    
    log "All dependencies are available."
}

# Create necessary directories
setup_directories() {
    log "Setting up directories..."
    
    mkdir -p "$LOG_DIR"
    mkdir -p "$BACKUP_DIR"
    mkdir -p "./ssl"
    
    log "Directories created successfully."
}

# Check if API key is configured
check_api_key() {
    log "Checking API key configuration..."
    
    if [ -z "$OPENAI_API_KEY" ] && [ ! -f "./api-key.txt" ]; then
        error "OpenAI API key is not configured. Please set OPENAI_API_KEY environment variable or create api-key.txt file."
    fi
    
    log "API key configuration verified."
}

# Build the application
build_application() {
    log "Building application..."
    
    # Build with Maven
    mvn clean package -DskipTests
    
    if [ $? -ne 0 ]; then
        error "Maven build failed."
    fi
    
    log "Application built successfully."
}

# Build Docker image
build_docker_image() {
    log "Building Docker image..."
    
    docker build -t "$DOCKER_IMAGE:$DOCKER_TAG" .
    
    if [ $? -ne 0 ]; then
        error "Docker build failed."
    fi
    
    log "Docker image built successfully."
}

# Stop existing containers
stop_existing_containers() {
    log "Stopping existing containers..."
    
    if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
        docker stop "$CONTAINER_NAME"
        log "Existing container stopped."
    fi
    
    if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
        docker rm "$CONTAINER_NAME"
        log "Existing container removed."
    fi
}

# Deploy with Docker Compose
deploy_with_compose() {
    log "Deploying with Docker Compose..."
    
    # Stop existing services
    docker-compose down
    
    # Start services
    docker-compose up -d
    
    if [ $? -ne 0 ]; then
        error "Docker Compose deployment failed."
    fi
    
    log "Application deployed successfully with Docker Compose."
}

# Deploy with Docker run
deploy_with_docker() {
    log "Deploying with Docker run..."
    
    docker run -d \
        --name "$CONTAINER_NAME" \
        --restart unless-stopped \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=production \
        -e OPENAI_API_KEY="$OPENAI_API_KEY" \
        -v "$(pwd)/logs:/app/logs" \
        -v "$(pwd)/api-key.txt:/app/api-key.txt:ro" \
        "$DOCKER_IMAGE:$DOCKER_TAG"
    
    if [ $? -ne 0 ]; then
        error "Docker deployment failed."
    fi
    
    log "Application deployed successfully with Docker."
}

# Health check
health_check() {
    log "Performing health check..."
    
    # Wait for application to start
    sleep 30
    
    # Check if application is responding
    for i in {1..10}; do
        if curl -f http://localhost:8080/api/health/liveness > /dev/null 2>&1; then
            log "Health check passed. Application is running."
            return 0
        fi
        warn "Health check attempt $i failed. Retrying in 10 seconds..."
        sleep 10
    done
    
    error "Health check failed. Application is not responding."
}

# Show deployment status
show_status() {
    log "Deployment Status:"
    echo "=================="
    
    if command -v docker-compose &> /dev/null && [ -f "docker-compose.yml" ]; then
        docker-compose ps
    else
        docker ps -f name="$CONTAINER_NAME"
    fi
    
    echo ""
    log "Application URLs:"
    echo "  - Health Check: http://localhost:8080/api/health/status"
    echo "  - API Documentation: http://localhost:8080/api"
    echo "  - Metrics: http://localhost:8080/actuator/metrics"
    echo ""
    log "Logs can be found in: $LOG_DIR"
}

# Cleanup function
cleanup() {
    log "Cleaning up..."
    # Add any cleanup tasks here
}

# Main deployment function
main() {
    log "Starting ATS Application deployment..."
    
    # Set trap for cleanup on exit
    trap cleanup EXIT
    
    check_dependencies
    setup_directories
    check_api_key
    build_application
    build_docker_image
    stop_existing_containers
    
    # Choose deployment method
    if [ -f "docker-compose.yml" ]; then
        deploy_with_compose
    else
        deploy_with_docker
    fi
    
    health_check
    show_status
    
    log "Deployment completed successfully!"
}

# Handle command line arguments
case "${1:-}" in
    "build")
        check_dependencies
        build_application
        build_docker_image
        ;;
    "deploy")
        main
        ;;
    "stop")
        log "Stopping application..."
        if [ -f "docker-compose.yml" ]; then
            docker-compose down
        else
            docker stop "$CONTAINER_NAME" || true
            docker rm "$CONTAINER_NAME" || true
        fi
        log "Application stopped."
        ;;
    "restart")
        log "Restarting application..."
        if [ -f "docker-compose.yml" ]; then
            docker-compose restart
        else
            docker restart "$CONTAINER_NAME"
        fi
        log "Application restarted."
        ;;
    "logs")
        if [ -f "docker-compose.yml" ]; then
            docker-compose logs -f
        else
            docker logs -f "$CONTAINER_NAME"
        fi
        ;;
    "status")
        show_status
        ;;
    *)
        echo "Usage: $0 {build|deploy|stop|restart|logs|status}"
        echo ""
        echo "Commands:"
        echo "  build   - Build the application and Docker image"
        echo "  deploy  - Deploy the application (default)"
        echo "  stop    - Stop the application"
        echo "  restart - Restart the application"
        echo "  logs    - Show application logs"
        echo "  status  - Show deployment status"
        exit 1
        ;;
esac
