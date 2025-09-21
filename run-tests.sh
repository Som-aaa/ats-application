#!/bin/bash

# Test Runner Script for ATS Application
# This script runs all tests and generates reports

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
TEST_REPORTS_DIR="./test-reports"
COVERAGE_REPORTS_DIR="./coverage-reports"
LOG_DIR="./logs"

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

info() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] INFO: $1${NC}"
}

# Create necessary directories
setup_directories() {
    log "Setting up test directories..."
    
    mkdir -p "$TEST_REPORTS_DIR"
    mkdir -p "$COVERAGE_REPORTS_DIR"
    mkdir -p "$LOG_DIR"
    
    log "Test directories created successfully."
}

# Check if Maven is available
check_maven() {
    log "Checking Maven availability..."
    
    if ! command -v mvn &> /dev/null; then
        error "Maven is not installed. Please install Maven first."
    fi
    
    log "Maven is available."
}

# Run unit tests
run_unit_tests() {
    log "Running unit tests..."
    
    mvn test \
        -Dspring.profiles.active=test \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportsDirectory="$TEST_REPORTS_DIR/unit" \
        -Dmaven.test.failure.ignore=true
    
    if [ $? -eq 0 ]; then
        log "Unit tests completed successfully."
    else
        warn "Some unit tests failed. Check reports for details."
    fi
}

# Run integration tests
run_integration_tests() {
    log "Running integration tests..."
    
    mvn test \
        -Dtest="*IntegrationTest" \
        -Dspring.profiles.active=test \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportsDirectory="$TEST_REPORTS_DIR/integration" \
        -Dmaven.test.failure.ignore=true
    
    if [ $? -eq 0 ]; then
        log "Integration tests completed successfully."
    else
        warn "Some integration tests failed. Check reports for details."
    fi
}

# Run performance tests
run_performance_tests() {
    log "Running performance tests..."
    
    mvn test \
        -Dtest="*PerformanceTest" \
        -Dspring.profiles.active=test \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportsDirectory="$TEST_REPORTS_DIR/performance" \
        -Dmaven.test.failure.ignore=true
    
    if [ $? -eq 0 ]; then
        log "Performance tests completed successfully."
    else
        warn "Some performance tests failed. Check reports for details."
    fi
}

# Generate test coverage report
generate_coverage_report() {
    log "Generating test coverage report..."
    
    # Check if JaCoCo plugin is available
    if mvn help:describe -Dplugin=org.jacoco:jacoco-maven-plugin &> /dev/null; then
        mvn jacoco:report \
            -Djacoco.reportDirectory="$COVERAGE_REPORTS_DIR" \
            -Djacoco.outputDirectory="$COVERAGE_REPORTS_DIR"
        
        if [ $? -eq 0 ]; then
            log "Coverage report generated successfully."
        else
            warn "Coverage report generation failed."
        fi
    else
        warn "JaCoCo plugin not available. Skipping coverage report."
    fi
}

# Run all tests
run_all_tests() {
    log "Running all tests..."
    
    mvn clean test \
        -Dspring.profiles.active=test \
        -Dmaven.test.failure.ignore=true \
        -Dsurefire.reportsDirectory="$TEST_REPORTS_DIR/all" \
        -Dmaven.test.failure.ignore=true
    
    if [ $? -eq 0 ]; then
        log "All tests completed successfully."
    else
        warn "Some tests failed. Check reports for details."
    fi
}

# Generate test summary
generate_test_summary() {
    log "Generating test summary..."
    
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # Count test results from Surefire reports
    if [ -d "$TEST_REPORTS_DIR" ]; then
        for report_file in "$TEST_REPORTS_DIR"/*/TEST-*.xml; do
            if [ -f "$report_file" ]; then
                local tests=$(grep -o 'tests="[0-9]*"' "$report_file" | grep -o '[0-9]*' | head -1)
                local failures=$(grep -o 'failures="[0-9]*"' "$report_file" | grep -o '[0-9]*' | head -1)
                local errors=$(grep -o 'errors="[0-9]*"' "$report_file" | grep -o '[0-9]*' | head -1)
                
                if [ -n "$tests" ]; then
                    total_tests=$((total_tests + tests))
                    failed_tests=$((failed_tests + failures + errors))
                fi
            fi
        done
        
        passed_tests=$((total_tests - failed_tests))
    fi
    
    # Generate summary report
    cat > "$TEST_REPORTS_DIR/test-summary.txt" << EOF
ATS Application Test Summary
============================
Generated: $(date)
Total Tests: $total_tests
Passed: $passed_tests
Failed: $failed_tests
Success Rate: $((passed_tests * 100 / total_tests))%

Test Reports Location: $TEST_REPORTS_DIR
Coverage Reports Location: $COVERAGE_REPORTS_DIR

Detailed Reports:
- Unit Tests: $TEST_REPORTS_DIR/unit/
- Integration Tests: $TEST_REPORTS_DIR/integration/
- Performance Tests: $TEST_REPORTS_DIR/performance/
EOF

    log "Test summary generated: $TEST_REPORTS_DIR/test-summary.txt"
    
    # Display summary
    echo ""
    info "Test Summary:"
    echo "============="
    echo "Total Tests: $total_tests"
    echo "Passed: $passed_tests"
    echo "Failed: $failed_tests"
    echo "Success Rate: $((passed_tests * 100 / total_tests))%"
    echo ""
}

# Clean up old test reports
cleanup_old_reports() {
    log "Cleaning up old test reports..."
    
    if [ -d "$TEST_REPORTS_DIR" ]; then
        find "$TEST_REPORTS_DIR" -name "*.xml" -mtime +7 -delete
        find "$TEST_REPORTS_DIR" -name "*.txt" -mtime +7 -delete
    fi
    
    if [ -d "$COVERAGE_REPORTS_DIR" ]; then
        find "$COVERAGE_REPORTS_DIR" -name "*.html" -mtime +7 -delete
    fi
    
    log "Old test reports cleaned up."
}

# Main function
main() {
    log "Starting ATS Application test suite..."
    
    setup_directories
    check_maven
    cleanup_old_reports
    
    case "${1:-all}" in
        "unit")
            run_unit_tests
            ;;
        "integration")
            run_integration_tests
            ;;
        "performance")
            run_performance_tests
            ;;
        "coverage")
            generate_coverage_report
            ;;
        "all")
            run_all_tests
            generate_coverage_report
            ;;
        *)
            echo "Usage: $0 {unit|integration|performance|coverage|all}"
            echo ""
            echo "Commands:"
            echo "  unit        - Run unit tests only"
            echo "  integration - Run integration tests only"
            echo "  performance - Run performance tests only"
            echo "  coverage    - Generate coverage report only"
            echo "  all         - Run all tests and generate reports (default)"
            exit 1
            ;;
    esac
    
    generate_test_summary
    
    log "Test suite completed!"
}

# Handle command line arguments
main "$@"
