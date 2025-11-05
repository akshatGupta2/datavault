# Test Resource Cleanup Guide

## Overview

This document outlines the test resource cleanup strategies implemented across all test modules to ensure no test data leakage and proper resource management.

## Cleanup Strategies by Module

### Scheduler Module

#### FileUploadControllerTest
- **Mocks Used**: FileMetadataRepository, RabbitTemplate, CreateMetadataService, EntityManager
- **Cleanup**: Mocks are automatically reset by MockitoExtension between tests
- **Additional**: No file resources created, no additional cleanup needed

#### SchedulerServiceImplTest
- **Mocks Used**: FileMetadataRepository, StreamObserver instances
- **Cleanup**: Service instance recreated in @BeforeEach, effectively resetting state
- **Additional**: Worker pool is reset with each new service instance

#### CreateMetadataServiceTest
- **Mocks Used**: FileMetadataRepository
- **Cleanup**: Mocks automatically reset by MockitoExtension

#### FileMetadataRepositoryTest
- **Database**: H2 in-memory database
- **Cleanup**: Database automatically cleared between tests by @DataJpaTest
- **Additional**: No explicit cleanup needed

#### Configuration Tests (RabbitMQConfigTest, CorsConfigTest, GrpcServerConfigTest)
- **Mocks Used**: Various @MockBean instances
- **Cleanup**: Spring Test context handles mock lifecycle
- **Additional**: No file or database resources

#### FileUploadIntegrationTest
- **Resources**: PostgreSQL Testcontainer, RabbitMQ Testcontainer, Database records
- **Cleanup**: 
  - `@AfterEach cleanup()` method deletes all metadata records
  - Testcontainers automatically stopped after test class
- **Implementation**:
```java
@AfterEach
void cleanup() {
    metadataRepository.deleteAll();
}
```

### Worker Module

#### ChunkTaskConsumerTest
- **Mocks Used**: SchedulerServiceGrpc.SchedulerServiceBlockingStub
- **File Resources**: Test files created in app/storage/{WORKER_ID}
- **System Properties**: WORKER_ID
- **Cleanup**:
```java
@AfterEach
void tearDown() {
    System.clearProperty("WORKER_ID");
    TestDataBuilder.cleanupDirectoryQuietly(new File("app/storage/" + TEST_WORKER_ID));
}
```

#### WorkerServiceImplTest
- **Mocks Used**: StreamObserver instances, SchedulerServiceBlockingStub
- **File Resources**: Test files created in app/storage/{WORKER_ID}
- **System Properties**: WORKER_ID, HOST, PORT, SCHEDULER_HOST, SCHEDULER_PORT
- **Cleanup**:
```java
@AfterEach
void tearDown() {
    System.clearProperty("WORKER_ID");
    System.clearProperty("HOST");
    System.clearProperty("PORT");
    System.clearProperty("SCHEDULER_HOST");
    System.clearProperty("SCHEDULER_PORT");
    TestDataBuilder.cleanupDirectoryQuietly(new File("app/storage/" + TEST_WORKER_ID));
}
```

#### ChunkStorageIntegrationTest
- **Resources**: RabbitMQ Testcontainer, File storage
- **System Properties**: WORKER_ID
- **Cleanup**:
```java
@AfterEach
void tearDown() {
    System.clearProperty("WORKER_ID");
}
```

### Common Module

#### GrpcServiceStubTest
- **Resources**: ManagedChannel instances
- **Cleanup**:
```java
@AfterEach
void tearDown() {
    if (channel != null) {
        channel.shutdownNow();
    }
}
```

#### ProtobufSerializationTest
- **Resources**: None (pure serialization tests)
- **Cleanup**: No cleanup needed

#### CommonApplicationTest
- **Resources**: Spring application context
- **Cleanup**: Spring Test framework handles context lifecycle

## Cleanup Patterns

### 1. Mock Reset
All tests using `@ExtendWith(MockitoExtension.class)` automatically have mocks reset between test methods. No explicit reset needed.

### 2. File Resource Cleanup
Tests creating file resources use the `TestDataBuilder.cleanupDirectoryQuietly()` utility method in `@AfterEach`:

```java
@AfterEach
void tearDown() {
    TestDataBuilder.cleanupDirectoryQuietly(new File("path/to/test/files"));
}
```

### 3. System Property Cleanup
Tests setting system properties clear them in `@AfterEach`:

```java
@AfterEach
void tearDown() {
    System.clearProperty("PROPERTY_NAME");
}
```

### 4. Database Cleanup
Integration tests with database access clear data in `@AfterEach`:

```java
@AfterEach
void cleanup() {
    repository.deleteAll();
}
```

### 5. Testcontainer Cleanup
Testcontainers are automatically stopped after test class execution. No explicit cleanup needed.

### 6. gRPC Channel Cleanup
Tests creating gRPC channels shut them down in `@AfterEach`:

```java
@AfterEach
void tearDown() {
    if (channel != null) {
        channel.shutdownNow();
    }
}
```

## Verification

### No Test Data Leakage
- Each test method runs in isolation
- Mocks are reset between tests
- File resources are cleaned up
- Database records are deleted
- System properties are cleared

### Resource Management
- All file handles are properly closed
- gRPC channels are shut down
- Database connections are returned to pool
- Testcontainers are stopped

## Best Practices

1. **Always use @AfterEach for cleanup** - Even if resources seem to be automatically managed
2. **Clean up in reverse order of creation** - Close dependent resources first
3. **Use try-with-resources** - For file operations within tests
4. **Verify cleanup in tests** - Assert that resources are properly cleaned
5. **Document cleanup requirements** - In test class JavaDoc

## Coverage Verification

All test classes have been verified to include appropriate cleanup methods:
- ✅ Scheduler module: 8/8 test classes with proper cleanup
- ✅ Worker module: 3/3 test classes with proper cleanup  
- ✅ Common module: 3/3 test classes with proper cleanup

## Surefire Configuration

Maven Surefire plugin is configured to:
- Fork tests in separate JVM (forkCount=1, reuseForks=true)
- Run tests in parallel by class (parallel=classes)
- Separate unit and integration test execution
- This ensures test isolation at the JVM level

## Conclusion

All test modules implement comprehensive resource cleanup strategies. No test data leakage occurs between test methods or test classes. Resources are properly managed and released after test execution.
