# Testing Quick Start Guide

Quick reference for running tests on different platforms.

## Prerequisites

- Java 17 or higher
- Maven 3.6+ (or use included Maven wrapper)
- Docker Desktop (for integration tests)

## Quick Start

### Using Helper Scripts (Recommended)

We provide cross-platform helper scripts to make testing easier:

**Windows (PowerShell):**
```powershell
# Run all tests
.\run-tests.ps1

# Run specific module
.\run-tests.ps1 scheduler

# Run with coverage report
.\run-tests.ps1 coverage -OpenReport

# Run unit tests only
.\run-tests.ps1 unit

# Get help
.\run-tests.ps1 -Help
```

**Windows (CMD):**
```cmd
REM Run all tests
run-tests.bat

REM Run specific module
run-tests.bat scheduler

REM Run with coverage
run-tests.bat coverage

REM Get help
run-tests.bat --help
```

**Linux/Mac (Bash):**
```bash
# Make script executable (first time only)
chmod +x run-tests.sh

# Run all tests
./run-tests.sh

# Run specific module
./run-tests.sh scheduler

# Run with coverage report
./run-tests.sh coverage --open-report

# Run unit tests only
./run-tests.sh unit

# Get help
./run-tests.sh --help
```

### Manual Commands

If you prefer to run Maven commands directly:

#### Windows (PowerShell)

```powershell
# Run all unit tests
mvn test

# Run all tests including integration tests
mvn verify

# Generate coverage report
mvn test jacoco:report

# View coverage report
Start-Process scheduler\target\site\jacoco\index.html
```

### Windows (CMD)

```cmd
# Run all unit tests
mvn test

# Run all tests including integration tests
mvn verify

# Generate coverage report
mvn test jacoco:report

# View coverage report
start scheduler\target\site\jacoco\index.html
```

### Linux/Mac (Bash)

```bash
# Run all unit tests
mvn test

# Run all tests including integration tests
mvn verify

# Generate coverage report
mvn test jacoco:report

# View coverage report
open scheduler/target/site/jacoco/index.html  # Mac
xdg-open scheduler/target/site/jacoco/index.html  # Linux
```

## Using Maven Wrapper (No Maven Installation Required)

### Windows (PowerShell)

```powershell
# Scheduler module
.\scheduler\mvnw.cmd test -f scheduler/pom.xml

# Worker module
.\worker\mvnw.cmd test -f worker/pom.xml

# Common module
.\common\mvnw.cmd test -f common/pom.xml
```

### Linux/Mac (Bash)

```bash
# Make wrapper executable (first time only)
chmod +x */mvnw

# Scheduler module
./scheduler/mvnw test -f scheduler/pom.xml

# Worker module
./worker/mvnw test -f worker/pom.xml

# Common module
./common/mvnw test -f common/pom.xml
```

## Test a Specific Module

### Windows

```powershell
# PowerShell
mvn test -f scheduler/pom.xml
mvn test -f worker/pom.xml
mvn test -f common/pom.xml

# CMD
mvn test -f scheduler\pom.xml
mvn test -f worker\pom.xml
mvn test -f common\pom.xml
```

### Linux/Mac

```bash
mvn test -f scheduler/pom.xml
mvn test -f worker/pom.xml
mvn test -f common/pom.xml
```

## Run a Single Test Class

### All Platforms

```bash
# Run specific test class
mvn test -Dtest=FileUploadControllerTest

# Run specific test method
mvn test -Dtest=FileUploadControllerTest#testUploadFile_Success

# Run in specific module
mvn test -Dtest=FileUploadControllerTest -f scheduler/pom.xml
```

## View Test Results

### Test Reports Location

```
scheduler/target/surefire-reports/
worker/target/surefire-reports/
common/target/surefire-reports/
```

### Coverage Reports Location

```
scheduler/target/site/jacoco/index.html
worker/target/site/jacoco/index.html
common/target/site/jacoco/index.html
```

## Common Issues

### Windows: Maven not found

**Solution 1: Use Maven wrapper**
```powershell
.\mvnw.cmd test
```

**Solution 2: Add Maven to PATH**
```powershell
$env:PATH += ";C:\path\to\maven\bin"
```

### Windows: Docker not running

**Solution: Start Docker Desktop**
```powershell
# Check Docker status
docker ps

# If error, start Docker Desktop from Start Menu
```

### Linux/Mac: Permission denied on mvnw

**Solution: Make wrapper executable**
```bash
chmod +x mvnw
chmod +x */mvnw
```

### All Platforms: Out of memory

**Solution: Increase Maven memory**

**Windows PowerShell:**
```powershell
$env:MAVEN_OPTS = "-Xmx1024m"
mvn test
```

**Windows CMD:**
```cmd
set MAVEN_OPTS=-Xmx1024m
mvn test
```

**Linux/Mac:**
```bash
export MAVEN_OPTS="-Xmx1024m"
mvn test
```

## Test Execution Time

- **Unit tests only**: ~1 minute
- **All tests (with integration)**: ~4 minutes
- **Parallel execution**: Enabled by default (40-50% faster)

## Coverage Targets

- **Minimum**: 70% line coverage
- **Current Status**:
  - Scheduler: 62% (some tests failing)
  - Worker: 45% (some tests failing)
  - Common: 33% (mostly generated code)

## Next Steps

1. Fix failing tests to improve coverage
2. Run tests before committing code
3. Check coverage reports to identify gaps
4. Review test output for any warnings

## Full Documentation

For complete documentation, see:
- `test-execution-summary.md` - Comprehensive configuration guide
- `test-cleanup-guide.md` - Resource cleanup patterns
- `design.md` - Testing strategy and architecture
- `requirements.md` - Testing requirements

## Quick Commands Reference

### Helper Scripts

| Task | PowerShell | Bash | CMD |
|------|-----------|------|-----|
| All tests | `.\run-tests.ps1` | `./run-tests.sh` | `run-tests.bat` |
| Specific module | `.\run-tests.ps1 scheduler` | `./run-tests.sh scheduler` | `run-tests.bat scheduler` |
| Unit tests only | `.\run-tests.ps1 unit` | `./run-tests.sh unit` | `run-tests.bat unit` |
| Coverage report | `.\run-tests.ps1 coverage -OpenReport` | `./run-tests.sh coverage --open-report` | `run-tests.bat coverage` |
| Clean & test | `.\run-tests.ps1 -Clean` | `./run-tests.sh --clean` | `run-tests.bat --clean` |
| Single test | `.\run-tests.ps1 -TestClass ClassName` | `./run-tests.sh --test-class ClassName` | `run-tests.bat --test-class ClassName` |
| Help | `.\run-tests.ps1 -Help` | `./run-tests.sh --help` | `run-tests.bat --help` |

### Maven Commands

| Task | Command |
|------|---------|
| Unit tests only | `mvn test` |
| All tests | `mvn verify` |
| Coverage report | `mvn jacoco:report` |
| Clean & test | `mvn clean test` |
| Skip tests | `mvn install -DskipTests` |
| Debug mode | `mvn test -X` |
| Single test | `mvn test -Dtest=ClassName` |

## Platform-Specific Notes

### Windows
- Use `\` for paths in CMD, `/` works in PowerShell and Maven
- Use `mvnw.cmd` instead of `mvnw`
- Requires Docker Desktop for integration tests
- Use `Start-Process` or `Invoke-Item` to open HTML reports

### Linux/Mac
- Use `/` for paths
- Make `mvnw` executable with `chmod +x`
- Requires Docker daemon running
- Use `open` (Mac) or `xdg-open` (Linux) for HTML reports

## Support

If you encounter issues:
1. Check the error message in console output
2. Review `target/surefire-reports/` for detailed test results
3. Enable debug output with `-X` flag
4. Ensure Docker Desktop is running (for integration tests)
5. Try cleaning build artifacts: `mvn clean`

---

**Last Updated**: November 2025
**Maven Version**: 3.9+
**Java Version**: 17+
**Spring Boot Version**: 3.4.4
