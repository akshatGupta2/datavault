# PowerShell script to run tests across all modules
# Works on Windows, Linux, and Mac (PowerShell Core)

param(
    [Parameter(Position=0)]
    [ValidateSet("all", "scheduler", "worker", "common", "unit", "integration", "coverage")]
    [string]$Target = "all",
    
    [Parameter()]
    [switch]$Clean,
    
    [Parameter()]
    [switch]$SkipIntegration,
    
    [Parameter()]
    [switch]$Coverage,
    
    [Parameter()]
    [switch]$OpenReport,
    
    [Parameter()]
    [string]$TestClass,
    
    [Parameter()]
    [switch]$Debug,
    
    [Parameter()]
    [switch]$Help
)

# Display help
if ($Help) {
    Write-Host @"
Test Runner Script for Distributed File Scheduler System

USAGE:
    .\run-tests.ps1 [Target] [Options]

TARGETS:
    all             Run tests for all modules (default)
    scheduler       Run tests for scheduler module only
    worker          Run tests for worker module only
    common          Run tests for common module only
    unit            Run unit tests only (all modules)
    integration     Run integration tests only (all modules)
    coverage        Generate coverage reports for all modules

OPTIONS:
    -Clean          Clean build artifacts before running tests
    -SkipIntegration Skip integration tests
    -Coverage       Generate coverage reports after tests
    -OpenReport     Open coverage reports in browser after generation
    -TestClass      Run specific test class (e.g., "FileUploadControllerTest")
    -Debug          Enable Maven debug output
    -Help           Display this help message

EXAMPLES:
    # Run all tests
    .\run-tests.ps1

    # Run scheduler tests only
    .\run-tests.ps1 scheduler

    # Run unit tests only
    .\run-tests.ps1 unit

    # Clean and run tests with coverage
    .\run-tests.ps1 -Clean -Coverage

    # Run specific test class
    .\run-tests.ps1 -TestClass FileUploadControllerTest

    # Run tests and open coverage report
    .\run-tests.ps1 coverage -OpenReport

    # Debug test execution
    .\run-tests.ps1 -Debug

REQUIREMENTS:
    - Java 17 or higher
    - Maven 3.6+ (or use included Maven wrapper)
    - Docker Desktop (for integration tests)

"@ -ForegroundColor Cyan
    exit 0
}

# Color functions
function Write-Success { param($Message) Write-Host $Message -ForegroundColor Green }
function Write-Info { param($Message) Write-Host $Message -ForegroundColor Cyan }
function Write-Warning { param($Message) Write-Host $Message -ForegroundColor Yellow }
function Write-Error { param($Message) Write-Host $Message -ForegroundColor Red }

# Check if Maven is available
function Test-Maven {
    try {
        $null = mvn -version 2>&1
        return $true
    } catch {
        return $false
    }
}

# Get Maven command (prefer system Maven, fallback to wrapper)
function Get-MavenCommand {
    param([string]$Module)
    
    if (Test-Maven) {
        return "mvn"
    } else {
        if ($IsWindows -or $env:OS -eq "Windows_NT") {
            return ".\$Module\mvnw.cmd"
        } else {
            return "./$Module/mvnw"
        }
    }
}

# Build Maven command arguments
function Get-MavenArgs {
    param(
        [string]$Goal,
        [string]$Module,
        [switch]$CleanFirst,
        [string]$TestPattern
    )
    
    $args = @()
    
    if ($CleanFirst) {
        $args += "clean"
    }
    
    $args += $Goal
    
    if ($Module) {
        $args += "-f"
        $args += "$Module/pom.xml"
    }
    
    if ($TestPattern) {
        $args += "-Dtest=$TestPattern"
    }
    
    if ($Debug) {
        $args += "-X"
    }
    
    return $args
}

# Run Maven command
function Invoke-Maven {
    param(
        [string]$Module,
        [string]$Goal,
        [string]$TestPattern = ""
    )
    
    $mvnCmd = Get-MavenCommand -Module $Module
    $mvnArgs = Get-MavenArgs -Goal $Goal -Module $Module -CleanFirst:$Clean -TestPattern $TestPattern
    
    Write-Info "Running: $mvnCmd $($mvnArgs -join ' ')"
    
    $startTime = Get-Date
    
    if ($mvnCmd -like "*mvnw*") {
        & $mvnCmd @mvnArgs
    } else {
        & mvn @mvnArgs
    }
    
    $exitCode = $LASTEXITCODE
    $duration = (Get-Date) - $startTime
    
    if ($exitCode -eq 0) {
        Write-Success "✓ Completed in $($duration.TotalSeconds.ToString('F2')) seconds"
        return $true
    } else {
        Write-Error "✗ Failed with exit code $exitCode"
        return $false
    }
}

# Open coverage report
function Open-CoverageReport {
    param([string]$Module)
    
    $reportPath = "$Module\target\site\jacoco\index.html"
    
    if (Test-Path $reportPath) {
        Write-Info "Opening coverage report for $Module..."
        if ($IsWindows -or $env:OS -eq "Windows_NT") {
            Start-Process $reportPath
        } elseif ($IsMacOS) {
            & open $reportPath
        } else {
            & xdg-open $reportPath
        }
    } else {
        Write-Warning "Coverage report not found: $reportPath"
    }
}

# Check Docker status
function Test-Docker {
    try {
        $null = docker ps 2>&1
        return $true
    } catch {
        return $false
    }
}

# Main execution
Write-Info "=== Distributed File Scheduler Test Runner ==="
Write-Info ""

# Check prerequisites
Write-Info "Checking prerequisites..."

if (-not (Test-Maven)) {
    Write-Warning "Maven not found in PATH, will use Maven wrapper"
}

# Check Docker for integration tests
if (-not $SkipIntegration -and $Target -in @("all", "integration", "scheduler", "worker")) {
    if (-not (Test-Docker)) {
        Write-Warning "Docker is not running. Integration tests may fail."
        Write-Warning "Please start Docker Desktop and try again."
        $response = Read-Host "Continue anyway? (y/N)"
        if ($response -ne "y" -and $response -ne "Y") {
            exit 1
        }
    } else {
        Write-Success "✓ Docker is running"
    }
}

Write-Info ""

# Define modules
$modules = @("scheduler", "worker", "common")
$success = $true

# Execute based on target
switch ($Target) {
    "all" {
        Write-Info "Running tests for all modules..."
        Write-Info ""
        
        foreach ($module in $modules) {
            Write-Info "--- Testing $module module ---"
            $goal = if ($SkipIntegration) { "test" } else { "verify" }
            $result = Invoke-Maven -Module $module -Goal $goal -TestPattern $TestClass
            
            if ($Coverage) {
                Write-Info "Generating coverage report for $module..."
                Invoke-Maven -Module $module -Goal "jacoco:report" | Out-Null
            }
            
            Write-Info ""
            $success = $success -and $result
        }
    }
    
    "scheduler" {
        Write-Info "Running tests for scheduler module..."
        $goal = if ($SkipIntegration) { "test" } else { "verify" }
        $success = Invoke-Maven -Module "scheduler" -Goal $goal -TestPattern $TestClass
        
        if ($Coverage) {
            Write-Info "Generating coverage report..."
            Invoke-Maven -Module "scheduler" -Goal "jacoco:report" | Out-Null
        }
    }
    
    "worker" {
        Write-Info "Running tests for worker module..."
        $goal = if ($SkipIntegration) { "test" } else { "verify" }
        $success = Invoke-Maven -Module "worker" -Goal $goal -TestPattern $TestClass
        
        if ($Coverage) {
            Write-Info "Generating coverage report..."
            Invoke-Maven -Module "worker" -Goal "jacoco:report" | Out-Null
        }
    }
    
    "common" {
        Write-Info "Running tests for common module..."
        $success = Invoke-Maven -Module "common" -Goal "test" -TestPattern $TestClass
        
        if ($Coverage) {
            Write-Info "Generating coverage report..."
            Invoke-Maven -Module "common" -Goal "jacoco:report" | Out-Null
        }
    }
    
    "unit" {
        Write-Info "Running unit tests for all modules..."
        Write-Info ""
        
        foreach ($module in $modules) {
            Write-Info "--- Testing $module module (unit tests only) ---"
            $result = Invoke-Maven -Module $module -Goal "test" -TestPattern $TestClass
            Write-Info ""
            $success = $success -and $result
        }
    }
    
    "integration" {
        Write-Info "Running integration tests for all modules..."
        Write-Info ""
        
        foreach ($module in @("scheduler", "worker")) {
            Write-Info "--- Testing $module module (integration tests only) ---"
            $result = Invoke-Maven -Module $module -Goal "integration-test"
            Write-Info ""
            $success = $success -and $result
        }
    }
    
    "coverage" {
        Write-Info "Generating coverage reports for all modules..."
        Write-Info ""
        
        foreach ($module in $modules) {
            Write-Info "--- Generating coverage for $module ---"
            Invoke-Maven -Module $module -Goal "test" | Out-Null
            Invoke-Maven -Module $module -Goal "jacoco:report" | Out-Null
            Write-Success "✓ Coverage report generated for $module"
            Write-Info ""
        }
        
        $Coverage = $true
        $OpenReport = $true
    }
}

# Open coverage reports if requested
if ($OpenReport -and $Coverage) {
    Write-Info ""
    Write-Info "Opening coverage reports..."
    
    switch ($Target) {
        "all" {
            foreach ($module in $modules) {
                Open-CoverageReport -Module $module
                Start-Sleep -Milliseconds 500
            }
        }
        "coverage" {
            foreach ($module in $modules) {
                Open-CoverageReport -Module $module
                Start-Sleep -Milliseconds 500
            }
        }
        default {
            Open-CoverageReport -Module $Target
        }
    }
}

# Summary
Write-Info ""
Write-Info "=== Test Execution Summary ==="

if ($success) {
    Write-Success "✓ All tests passed successfully!"
    exit 0
} else {
    Write-Error "✗ Some tests failed. Check the output above for details."
    Write-Info ""
    Write-Info "Test reports location:"
    Write-Info "  - scheduler/target/surefire-reports/"
    Write-Info "  - worker/target/surefire-reports/"
    Write-Info "  - common/target/surefire-reports/"
    exit 1
}
