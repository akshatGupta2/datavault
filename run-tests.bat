@echo off
REM Batch script to run tests across all modules
REM Works on Windows CMD

setlocal enabledelayedexpansion

REM Parse arguments
set TARGET=all
set CLEAN=false
set SKIP_INTEGRATION=false
set COVERAGE=false
set TEST_CLASS=
set DEBUG=false

:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="all" set TARGET=all& shift & goto parse_args
if /i "%~1"=="scheduler" set TARGET=scheduler& shift & goto parse_args
if /i "%~1"=="worker" set TARGET=worker& shift & goto parse_args
if /i "%~1"=="common" set TARGET=common& shift & goto parse_args
if /i "%~1"=="unit" set TARGET=unit& shift & goto parse_args
if /i "%~1"=="integration" set TARGET=integration& shift & goto parse_args
if /i "%~1"=="coverage" set TARGET=coverage& shift & goto parse_args
if /i "%~1"=="--clean" set CLEAN=true& shift & goto parse_args
if /i "%~1"=="--skip-integration" set SKIP_INTEGRATION=true& shift & goto parse_args
if /i "%~1"=="--coverage" set COVERAGE=true& shift & goto parse_args
if /i "%~1"=="--test-class" set TEST_CLASS=%~2& shift & shift & goto parse_args
if /i "%~1"=="--debug" set DEBUG=true& shift & goto parse_args
if /i "%~1"=="--help" goto show_help
if /i "%~1"=="-h" goto show_help
echo Unknown option: %~1
echo Use --help for usage information
exit /b 1
:end_parse

REM Main execution
echo === Distributed File Scheduler Test Runner ===
echo.

REM Check if Maven is available
where mvn >nul 2>&1
if %errorlevel% equ 0 (
    set MVN_CMD=mvn
    echo Using system Maven
) else (
    echo Maven not found in PATH, will use Maven wrapper
)

REM Check Docker
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    if "%SKIP_INTEGRATION%"=="false" (
        if "%TARGET%"=="all" goto warn_docker
        if "%TARGET%"=="integration" goto warn_docker
        if "%TARGET%"=="scheduler" goto warn_docker
        if "%TARGET%"=="worker" goto warn_docker
    )
    goto skip_docker_check
    
    :warn_docker
    echo WARNING: Docker is not running. Integration tests may fail.
    echo Please start Docker Desktop and try again.
    set /p CONTINUE="Continue anyway? (y/N) "
    if /i not "!CONTINUE!"=="y" exit /b 1
    
    :skip_docker_check
) else (
    echo Docker is running
)

echo.

REM Execute based on target
if "%TARGET%"=="all" goto run_all
if "%TARGET%"=="scheduler" goto run_scheduler
if "%TARGET%"=="worker" goto run_worker
if "%TARGET%"=="common" goto run_common
if "%TARGET%"=="unit" goto run_unit
if "%TARGET%"=="integration" goto run_integration
if "%TARGET%"=="coverage" goto run_coverage

:run_all
echo Running tests for all modules...
echo.
call :test_module scheduler
call :test_module worker
call :test_module common
goto summary

:run_scheduler
echo Running tests for scheduler module...
call :test_module scheduler
goto summary

:run_worker
echo Running tests for worker module...
call :test_module worker
goto summary

:run_common
echo Running tests for common module...
call :test_module common
goto summary

:run_unit
echo Running unit tests for all modules...
echo.
call :test_module_unit scheduler
call :test_module_unit worker
call :test_module_unit common
goto summary

:run_integration
echo Running integration tests...
echo.
call :test_module_integration scheduler
call :test_module_integration worker
goto summary

:run_coverage
echo Generating coverage reports for all modules...
echo.
call :generate_coverage scheduler
call :generate_coverage worker
call :generate_coverage common
echo.
echo Opening coverage reports...
start scheduler\target\site\jacoco\index.html
timeout /t 1 /nobreak >nul
start worker\target\site\jacoco\index.html
timeout /t 1 /nobreak >nul
start common\target\site\jacoco\index.html
goto summary

REM Functions
:test_module
set MODULE=%~1
echo --- Testing %MODULE% module ---

if defined MVN_CMD (
    set CMD=mvn
) else (
    set CMD=%MODULE%\mvnw.cmd
)

if "%CLEAN%"=="true" set GOAL=clean test
if "%CLEAN%"=="false" (
    if "%SKIP_INTEGRATION%"=="true" (
        set GOAL=test
    ) else (
        set GOAL=verify
    )
)

if defined TEST_CLASS (
    set EXTRA_ARGS=-Dtest=%TEST_CLASS%
) else (
    set EXTRA_ARGS=
)

if "%DEBUG%"=="true" set EXTRA_ARGS=%EXTRA_ARGS% -X

echo Running: !CMD! %GOAL% -f %MODULE%\pom.xml %EXTRA_ARGS%
call !CMD! %GOAL% -f %MODULE%\pom.xml %EXTRA_ARGS%

if %errorlevel% neq 0 (
    echo FAILED: %MODULE% tests failed
    set TEST_FAILED=true
) else (
    echo SUCCESS: %MODULE% tests passed
)

if "%COVERAGE%"=="true" (
    echo Generating coverage report for %MODULE%...
    call !CMD! jacoco:report -f %MODULE%\pom.xml >nul 2>&1
)

echo.
goto :eof

:test_module_unit
set MODULE=%~1
echo --- Testing %MODULE% module (unit tests only) ---

if defined MVN_CMD (
    set CMD=mvn
) else (
    set CMD=%MODULE%\mvnw.cmd
)

if "%CLEAN%"=="true" (
    set GOAL=clean test
) else (
    set GOAL=test
)

if defined TEST_CLASS (
    set EXTRA_ARGS=-Dtest=%TEST_CLASS%
) else (
    set EXTRA_ARGS=
)

if "%DEBUG%"=="true" set EXTRA_ARGS=%EXTRA_ARGS% -X

echo Running: !CMD! %GOAL% -f %MODULE%\pom.xml %EXTRA_ARGS%
call !CMD! %GOAL% -f %MODULE%\pom.xml %EXTRA_ARGS%

if %errorlevel% neq 0 (
    echo FAILED: %MODULE% tests failed
    set TEST_FAILED=true
) else (
    echo SUCCESS: %MODULE% tests passed
)

echo.
goto :eof

:test_module_integration
set MODULE=%~1
echo --- Testing %MODULE% module (integration tests only) ---

if defined MVN_CMD (
    set CMD=mvn
) else (
    set CMD=%MODULE%\mvnw.cmd
)

echo Running: !CMD! integration-test -f %MODULE%\pom.xml
call !CMD! integration-test -f %MODULE%\pom.xml

if %errorlevel% neq 0 (
    echo FAILED: %MODULE% integration tests failed
    set TEST_FAILED=true
) else (
    echo SUCCESS: %MODULE% integration tests passed
)

echo.
goto :eof

:generate_coverage
set MODULE=%~1
echo --- Generating coverage for %MODULE% ---

if defined MVN_CMD (
    set CMD=mvn
) else (
    set CMD=%MODULE%\mvnw.cmd
)

call !CMD! test -f %MODULE%\pom.xml >nul 2>&1
call !CMD! jacoco:report -f %MODULE%\pom.xml >nul 2>&1
echo Coverage report generated for %MODULE%
goto :eof

:summary
echo.
echo === Test Execution Summary ===
if defined TEST_FAILED (
    echo FAILED: Some tests failed. Check the output above for details.
    echo.
    echo Test reports location:
    echo   - scheduler\target\surefire-reports\
    echo   - worker\target\surefire-reports\
    echo   - common\target\surefire-reports\
    exit /b 1
) else (
    echo SUCCESS: All tests passed successfully!
    exit /b 0
)

:show_help
echo Test Runner Script for Distributed File Scheduler System
echo.
echo USAGE:
echo     run-tests.bat [Target] [Options]
echo.
echo TARGETS:
echo     all             Run tests for all modules (default)
echo     scheduler       Run tests for scheduler module only
echo     worker          Run tests for worker module only
echo     common          Run tests for common module only
echo     unit            Run unit tests only (all modules)
echo     integration     Run integration tests only (all modules)
echo     coverage        Generate coverage reports for all modules
echo.
echo OPTIONS:
echo     --clean         Clean build artifacts before running tests
echo     --skip-integration  Skip integration tests
echo     --coverage      Generate coverage reports after tests
echo     --test-class    Run specific test class
echo     --debug         Enable Maven debug output
echo     --help          Display this help message
echo.
echo EXAMPLES:
echo     run-tests.bat
echo     run-tests.bat scheduler
echo     run-tests.bat unit
echo     run-tests.bat all --clean --coverage
echo     run-tests.bat --test-class FileUploadControllerTest
echo     run-tests.bat coverage
echo.
echo REQUIREMENTS:
echo     - Java 17 or higher
echo     - Maven 3.6+ (or use included Maven wrapper)
echo     - Docker Desktop (for integration tests)
echo.
exit /b 0
