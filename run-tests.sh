#!/bin/bash
# Bash script to run tests across all modules
# Works on Linux and Mac

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Functions
print_success() { echo -e "${GREEN}$1${NC}"; }
print_info() { echo -e "${CYAN}$1${NC}"; }
print_warning() { echo -e "${YELLOW}$1${NC}"; }
print_error() { echo -e "${RED}$1${NC}"; }

# Display help
show_help() {
    cat << EOF
${CYAN}Test Runner Script for Distributed File Scheduler System${NC}

USAGE:
    ./run-tests.sh [Target] [Options]

TARGETS:
    all             Run tests for all modules (default)
    scheduler       Run tests for scheduler module only
    worker          Run tests for worker module only
    common          Run tests for common module only
    unit            Run unit tests only (all modules)
    integration     Run integration tests only (all modules)
    coverage        Generate coverage reports for all modules

OPTIONS:
    --clean         Clean build artifacts before running tests
    --skip-integration  Skip integration tests
    --coverage      Generate coverage reports after tests
    --open-report   Open coverage reports in browser after generation
    --test-class    Run specific test class (e.g., "FileUploadControllerTest")
    --debug         Enable Maven debug output
    --help          Display this help message

EXAMPLES:
    # Run all tests
    ./run-tests.sh

    # Run scheduler tests only
    ./run-tests.sh scheduler

    # Run unit tests only
    ./run-tests.sh unit

    # Clean and run tests with coverage
    ./run-tests.sh all --clean --coverage

    # Run specific test class
    ./run-tests.sh --test-class FileUploadControllerTest

    # Run tests and open coverage report
    ./run-tests.sh coverage --open-report

    # Debug test execution
    ./run-tests.sh --debug

REQUIREMENTS:
    - Java 17 or higher
    - Maven 3.6+ (or use included Maven wrapper)
    - Docker (for integration tests)

EOF
    exit 0
}

# Parse arguments
TARGET="all"
CLEAN=false
SKIP_INTEGRATION=false
COVERAGE=false
OPEN_REPORT=false
TEST_CLASS=""
DEBUG=false

while [[ $# -gt 0 ]]; do
    case $1 in
        all|scheduler|worker|common|unit|integration|coverage)
            TARGET="$1"
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --skip-integration)
            SKIP_INTEGRATION=true
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        --open-report)
            OPEN_REPORT=true
            shift
            ;;
        --test-class)
            TEST_CLASS="$2"
            shift 2
            ;;
        --debug)
            DEBUG=true
            shift
            ;;
        --help|-h)
            show_help
            ;;
        *)
            print_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Check if Maven is available
check_maven() {
    if command -v mvn &> /dev/null; then
        return 0
    else
        return 1
    fi
}

# Get Maven command
get_maven_command() {
    local module=$1
    if check_maven; then
        echo "mvn"
    else
        echo "./$module/mvnw"
    fi
}

# Build Maven arguments
build_maven_args() {
    local goal=$1
    local module=$2
    local args=()
    
    if [ "$CLEAN" = true ]; then
        args+=("clean")
    fi
    
    args+=("$goal")
    
    if [ -n "$module" ]; then
        args+=("-f" "$module/pom.xml")
    fi
    
    if [ -n "$TEST_CLASS" ]; then
        args+=("-Dtest=$TEST_CLASS")
    fi
    
    if [ "$DEBUG" = true ]; then
        args+=("-X")
    fi
    
    echo "${args[@]}"
}

# Run Maven command
run_maven() {
    local module=$1
    local goal=$2
    
    local mvn_cmd=$(get_maven_command "$module")
    local mvn_args=$(build_maven_args "$goal" "$module")
    
    print_info "Running: $mvn_cmd $mvn_args"
    
    local start_time=$(date +%s)
    
    if $mvn_cmd $mvn_args; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        print_success "✓ Completed in ${duration} seconds"
        return 0
    else
        print_error "✗ Failed with exit code $?"
        return 1
    fi
}

# Open coverage report
open_coverage_report() {
    local module=$1
    local report_path="$module/target/site/jacoco/index.html"
    
    if [ -f "$report_path" ]; then
        print_info "Opening coverage report for $module..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            open "$report_path"
        else
            xdg-open "$report_path" 2>/dev/null || print_warning "Could not open browser. Please open $report_path manually."
        fi
    else
        print_warning "Coverage report not found: $report_path"
    fi
}

# Check Docker status
check_docker() {
    if command -v docker &> /dev/null && docker ps &> /dev/null; then
        return 0
    else
        return 1
    fi
}

# Make Maven wrappers executable
make_wrappers_executable() {
    chmod +x mvnw 2>/dev/null || true
    chmod +x */mvnw 2>/dev/null || true
}

# Main execution
print_info "=== Distributed File Scheduler Test Runner ==="
echo ""

# Make wrappers executable
make_wrappers_executable

# Check prerequisites
print_info "Checking prerequisites..."

if ! check_maven; then
    print_warning "Maven not found in PATH, will use Maven wrapper"
fi

# Check Docker for integration tests
if [ "$SKIP_INTEGRATION" = false ] && [[ "$TARGET" =~ ^(all|integration|scheduler|worker)$ ]]; then
    if ! check_docker; then
        print_warning "Docker is not running. Integration tests may fail."
        print_warning "Please start Docker and try again."
        read -p "Continue anyway? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        print_success "✓ Docker is running"
    fi
fi

echo ""

# Define modules
MODULES=("scheduler" "worker" "common")
SUCCESS=true

# Execute based on target
case $TARGET in
    all)
        print_info "Running tests for all modules..."
        echo ""
        
        for module in "${MODULES[@]}"; do
            print_info "--- Testing $module module ---"
            if [ "$SKIP_INTEGRATION" = true ]; then
                run_maven "$module" "test" || SUCCESS=false
            else
                run_maven "$module" "verify" || SUCCESS=false
            fi
            
            if [ "$COVERAGE" = true ]; then
                print_info "Generating coverage report for $module..."
                run_maven "$module" "jacoco:report" > /dev/null || true
            fi
            
            echo ""
        done
        ;;
        
    scheduler|worker|common)
        print_info "Running tests for $TARGET module..."
        if [ "$SKIP_INTEGRATION" = true ] || [ "$TARGET" = "common" ]; then
            run_maven "$TARGET" "test" || SUCCESS=false
        else
            run_maven "$TARGET" "verify" || SUCCESS=false
        fi
        
        if [ "$COVERAGE" = true ]; then
            print_info "Generating coverage report..."
            run_maven "$TARGET" "jacoco:report" > /dev/null || true
        fi
        ;;
        
    unit)
        print_info "Running unit tests for all modules..."
        echo ""
        
        for module in "${MODULES[@]}"; do
            print_info "--- Testing $module module (unit tests only) ---"
            run_maven "$module" "test" || SUCCESS=false
            echo ""
        done
        ;;
        
    integration)
        print_info "Running integration tests for all modules..."
        echo ""
        
        for module in "scheduler" "worker"; do
            print_info "--- Testing $module module (integration tests only) ---"
            run_maven "$module" "integration-test" || SUCCESS=false
            echo ""
        done
        ;;
        
    coverage)
        print_info "Generating coverage reports for all modules..."
        echo ""
        
        for module in "${MODULES[@]}"; do
            print_info "--- Generating coverage for $module ---"
            run_maven "$module" "test" > /dev/null || true
            run_maven "$module" "jacoco:report" > /dev/null || true
            print_success "✓ Coverage report generated for $module"
            echo ""
        done
        
        COVERAGE=true
        OPEN_REPORT=true
        ;;
esac

# Open coverage reports if requested
if [ "$OPEN_REPORT" = true ] && [ "$COVERAGE" = true ]; then
    echo ""
    print_info "Opening coverage reports..."
    
    case $TARGET in
        all|coverage)
            for module in "${MODULES[@]}"; do
                open_coverage_report "$module"
                sleep 0.5
            done
            ;;
        *)
            open_coverage_report "$TARGET"
            ;;
    esac
fi

# Summary
echo ""
print_info "=== Test Execution Summary ==="

if [ "$SUCCESS" = true ]; then
    print_success "✓ All tests passed successfully!"
    exit 0
else
    print_error "✗ Some tests failed. Check the output above for details."
    echo ""
    print_info "Test reports location:"
    print_info "  - scheduler/target/surefire-reports/"
    print_info "  - worker/target/surefire-reports/"
    print_info "  - common/target/surefire-reports/"
    exit 1
fi
