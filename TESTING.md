# Testing Guide

Comprehensive testing guide for the Distributed File Scheduler System.

## Quick Start

### Run All Tests

**Windows (PowerShell):**
```powershell
.\run-tests.ps1
```

**Windows (CMD):**
```cmd
run-tests.bat
```

**Linux/Mac:**
```bash
chmod +x run-tests.sh  # First time only
./run-tests.sh
```

### Run Tests for Specific Module

**Windows (PowerShell):**
```powershell
.\run-tests.ps1 scheduler
.\run-tests.ps1 worker
.\run-tests.ps1 common
```

**Linux/Mac:**
```bash
./run-tests.sh scheduler
./run-tests.sh worker
./run-tests.sh common
```

### Generate Coverage Reports

**Windows (PowerShell):**
```powershell
.\run-tests.ps1 coverage -OpenReport
```

**Linux/Mac:**
```bash
./run-tests.sh coverage --open-report
```

## Helper Scripts

We provide three helper scripts for different platforms:

- **`run-tests.ps1`** - PowerShell script (Windows, Linux, Mac with PowerShell Core)
- **`run-tests.sh`** - Bash script (Linux, Mac)
- **`run-tests.bat`** - Batch script (Windows CMD)

### Features

- ✅ Run tests for all modules or specific modules
- ✅ Run unit tests only or integration tests only
- ✅ Generate and open coverage reports
- ✅ Clean build artifacts before testing
- ✅ Run specific test classes
- ✅ Debug mode support
- ✅ Automatic Docker status check
- ✅ Color-coded output
- ✅ Execution time tracking

### Usage Examples

**Run unit tests only:**
```powershell
# PowerShell
.\run-tests.ps1 unit

# Bash
./run-tests.sh unit

# CMD
run-tests.bat unit
```

**Clean and run tests:**
```powershell
# PowerShell
.\run-tests.ps1 -Clean

# Bash
./run-tests.sh --clean

# CMD
run-tests.bat --clean
```

**Run specific test class:**
```powershell
# PowerShell
.\run-tests.ps1 -TestClass FileUploadControllerTest

# Bash
./run-tests.sh --test-class FileUploadControllerTest

# CMD
run-tests.bat --test-class FileUploadControllerTest
```

**Get help:**
```powershell
# PowerShell
.\run-tests.ps1 -Help

# Bash
./run-tests.sh --help

# CMD
run-tests.bat --help
```

## Manual Maven Commands

If you prefer to run Maven commands directly:

### Run All Tests

```bash
# All platforms
mvn verify
```

### Run Unit Tests Only

```bash
# All platforms
mvn test
```

### Generate Coverage Report

```bash
# All platforms
mvn test jacoco:report
```

### Run Tests for Specific Module

**Windows:**
```powershell
mvn test -f scheduler/pom.xml
mvn test -f worker/pom.xml
mvn test -f common/pom.xml
```

**Linux/Mac:**
```bash
mvn test -f scheduler/pom.xml
mvn test -f worker/pom.xml
mvn test -f common/pom.xml
```

### Using Maven Wrapper

**Windows:**
```powershell
.\scheduler\mvnw.cmd test -f scheduler/pom.xml
.\worker\mvnw.cmd test -f worker/pom.xml
.\common\mvnw.cmd test -f common/pom.xml
```

**Linux/Mac:**
```bash
./scheduler/mvnw test -f scheduler/pom.xml
./worker/mvnw test -f worker/pom.xml
./common/mvnw test -f common/pom.xml
```

## Test Structure

### Modules

- **scheduler** - Scheduler service tests (REST API, gRPC, database)
- **worker** - Worker service tests (message consumption, file storage)
- **common** - Common module tests (protobuf, gRPC stubs)

### Test Types

- **Unit Tests** - Test individual components in isolation
- **Integration Tests** - Test component interactions with real infrastructure
- **Configuration Tests** - Verify Spring Boot configuration

### Test Organization

```
module/
├── src/
│   ├── main/java/
│   └── test/java/
│       ├── controller/      # Controller tests
│       ├── service/         # Service tests
│       ├── repository/      # Repository tests
│       ├── config/          # Configuration tests
│       ├── integration/     # Integration tests
│       └── util/            # Test utilities
```

## Coverage Reports

### View Coverage Reports

**Windows:**
```powershell
# PowerShell
Start-Process scheduler\target\site\jacoco\index.html
Start-Process worker\target\site\jacoco\index.html
Start-Process common\target\site\jacoco\index.html

# CMD
start scheduler\target\site\jacoco\index.html
start worker\target\site\jacoco\index.html
start common\target\site\jacoco\index.html
```

**Linux/Mac:**
```bash
# Mac
open scheduler/target/site/jacoco/index.html
open worker/target/site/jacoco/index.html
open common/target/site/jacoco/index.html

# Linux
xdg-open scheduler/target/site/jacoco/index.html
xdg-open worker/target/site/jacoco/index.html
xdg-open common/target/site/jacoco/index.html
```

### Coverage Targets

- **Minimum**: 70% line coverage
- **Current Status**:
  - Scheduler: 62% (some tests failing)
  - Worker: 45% (some tests failing)
  - Common: 33% (mostly generated code)

## Prerequisites

### Required

- **Java 17 or higher**
- **Maven 3.6+** (or use included Maven wrapper)
- **Docker Desktop** (for integration tests)

### Check Prerequisites

**Java:**
```bash
java -version
```

**Maven:**
```bash
mvn -version
```

**Docker:**
```bash
docker ps
```

## Test Configuration

### Maven Surefire Plugin

- **Parallel Execution**: Tests run in parallel by class (4 threads per core)
- **Test Separation**: Unit tests run before integration tests
- **Execution Order**: Alphabetical (predictable)
- **Fork Mode**: Single forked JVM with reuse

### JaCoCo Coverage

- **Minimum Coverage**: 70% line coverage enforced in verify phase
- **Reports**: HTML reports generated in `target/site/jacoco/`
- **Exclusions**: Generated gRPC code excluded from coverage

### Test Cleanup

- **Mocks**: Automatically reset between tests
- **File Resources**: Cleaned up in @AfterEach methods
- **System Properties**: Cleared after tests
- **Database**: Records deleted in integration tests
- **Testcontainers**: Automatically stopped after test class

## Troubleshooting

### Maven not found

**Solution: Use Maven wrapper**
```powershell
# Windows
.\scheduler\mvnw.cmd test

# Linux/Mac
./scheduler/mvnw test
```

### Docker not running

**Solution: Start Docker Desktop**
```bash
# Check status
docker ps

# If error, start Docker Desktop
```

### Permission denied (Linux/Mac)

**Solution: Make wrapper executable**
```bash
chmod +x mvnw
chmod +x */mvnw
chmod +x run-tests.sh
```

### Out of memory

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

### Tests failing

**Check test reports:**
```
scheduler/target/surefire-reports/
worker/target/surefire-reports/
common/target/surefire-reports/
```

**Enable debug output:**
```bash
mvn test -X
```

## Performance

### Expected Execution Times

- **Unit tests only**: ~1 minute
- **All tests (with integration)**: ~4 minutes
- **Parallel execution**: 40-50% faster than sequential

### Optimization

- Tests run in parallel by class
- Single forked JVM with reuse
- Integration tests run sequentially (avoid resource conflicts)
- Testcontainers reused within test class

## CI/CD Integration

### GitHub Actions

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run tests
        run: mvn verify
      - name: Generate coverage
        run: mvn jacoco:report
```

### Azure DevOps

```yaml
trigger:
  - main
pool:
  vmImage: 'windows-latest'
steps:
  - task: Maven@3
    inputs:
      mavenPomFile: 'pom.xml'
      goals: 'verify'
      publishJUnitResults: true
```

## Documentation

### Comprehensive Guides

- **[TESTING-QUICKSTART.md](.kiro/specs/spring-boot-testing-suite/TESTING-QUICKSTART.md)** - Quick start guide
- **[test-execution-summary.md](.kiro/specs/spring-boot-testing-suite/test-execution-summary.md)** - Complete configuration details
- **[test-cleanup-guide.md](.kiro/specs/spring-boot-testing-suite/test-cleanup-guide.md)** - Resource cleanup patterns
- **[design.md](.kiro/specs/spring-boot-testing-suite/design.md)** - Testing strategy and architecture
- **[requirements.md](.kiro/specs/spring-boot-testing-suite/requirements.md)** - Testing requirements

### External Resources

- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Testcontainers](https://www.testcontainers.org/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

## Best Practices

1. ✅ Run unit tests frequently during development
2. ✅ Run integration tests before committing
3. ✅ Check coverage reports to identify gaps
4. ✅ Clean build artifacts when switching branches
5. ✅ Ensure Docker Desktop is running for integration tests
6. ✅ Use helper scripts for consistent test execution
7. ✅ Review test output for warnings and errors
8. ✅ Keep tests fast and focused
9. ✅ Clean up resources in @AfterEach methods
10. ✅ Use descriptive test names

## Support

For issues or questions:

1. Check test output in `target/surefire-reports/`
2. Review coverage reports in `target/site/jacoco/`
3. Enable debug output with `-X` flag
4. Check Docker Desktop status for integration tests
5. Review comprehensive documentation in `.kiro/specs/spring-boot-testing-suite/`

---

**Last Updated**: November 2025  
**Maven Version**: 3.9+  
**Java Version**: 17+  
**Spring Boot Version**: 3.4.4
