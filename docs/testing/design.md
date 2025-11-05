# Design Document

## Overview

This document outlines the design for a comprehensive testing suite for the Distributed File Scheduler System. The testing suite will cover three Spring Boot microservices (scheduler, worker, common) with unit tests, integration tests, and configuration tests. The design follows Spring Boot testing best practices, utilizing test slices, Testcontainers, and proper mocking strategies to ensure fast, reliable, and maintainable tests.

## Architecture

### Testing Layers

The testing architecture follows a three-layer approach:

1. **Unit Tests**: Test individual components in isolation using mocks
2. **Integration Tests**: Test component interactions with real infrastructure (database, message broker)
3. **Configuration Tests**: Verify Spring Boot configuration and bean wiring

### Test Organization Structure

```
scheduler/
├── src/
│   ├── main/java/com/scheduler/scheduler/
│   └── test/java/com/scheduler/scheduler/
│       ├── controller/
│       │   └── FileUploadControllerTest.java
│       ├── service/
│       │   ├── SchedulerServiceImplTest.java
│       │   └── CreateMetadataServiceTest.java
│       ├── repository/
│       │   └── FileMetadataRepositoryTest.java
│       ├── config/
│       │   ├── RabbitMQConfigTest.java
│       │   ├── CorsConfigTest.java
│       │   └── GrpcServerConfigTest.java
│       ├── integration/
│       │   ├── FileUploadIntegrationTest.java
│       │   └── SchedulerApplicationTest.java
│       └── util/
│           └── TestDataBuilder.java

worker/
├── src/
│   ├── main/java/com/worker/worker/
│   └── test/java/com/worker/worker/
│       ├── service/
│       │   ├── ChunkTaskConsumerTest.java
│       │   └── WorkerServiceImplTest.java
│       ├── integration/
│       │   ├── ChunkStorageIntegrationTest.java
│       │   └── WorkerApplicationTest.java
│       └── util/
│           └── TestDataBuilder.java

common/
├── src/
│   ├── main/java/com/common/common/
│   └── test/java/com/common/common/
│       ├── grpc/
│       │   ├── ProtobufSerializationTest.java
│       │   └── GrpcServiceStubTest.java
│       └── CommonApplicationTest.java
```

## Components and Interfaces

### Scheduler Service Tests

#### 1. FileUploadController Tests

**Test Class**: `FileUploadControllerTest`

**Testing Strategy**: Use `@WebMvcTest` to test only the web layer

**Key Test Cases**:
- `testUploadFile_Success`: Verify successful file upload with multipart file
- `testUploadFile_EmptyFile`: Verify handling of empty file upload
- `testUploadFile_LargeFile`: Verify chunking logic for large files
- `testGetFile_Success`: Verify file retrieval by name
- `testGetFile_NotFound`: Verify 404 response for non-existent file
- `testGetFile_ChunkAssembly`: Verify correct chunk assembly

**Mocked Dependencies**:
- `FileMetadataRepository`
- `RabbitTemplate`
- `CreateMetadataService`
- `EntityManager`

**Test Utilities**:
```java
MockMultipartFile createTestFile(String name, byte[] content)
FileMetadata createMockMetadata(String fileId, String filename)
```

#### 2. SchedulerServiceImpl Tests

**Test Class**: `SchedulerServiceImplTest`

**Testing Strategy**: Unit test with mocked gRPC StreamObserver

**Key Test Cases**:
- `testSendHeartbeat_NewWorker`: Verify new worker registration
- `testSendHeartbeat_ExistingWorker`: Verify heartbeat update for existing worker
- `testAssignWorkerForChunk_RoundRobin`: Verify round-robin worker assignment
- `testAssignWorkerForChunk_NoActiveWorkers`: Verify error when no workers available
- `testAssignWorkerForChunk_AlreadyExists`: Verify handling of duplicate chunk assignment
- `testCleanInactiveWorkers`: Verify removal of timed-out workers
- `testGetActiveWorkers`: Verify filtering of active workers

**Mocked Dependencies**:
- `FileMetadataRepository`
- `StreamObserver<HeartbeatResponse>`
- `StreamObserver<AssignWorkerResponse>`

#### 3. CreateMetadataService Tests

**Test Class**: `CreateMetadataServiceTest`

**Testing Strategy**: Unit test with mocked repository

**Key Test Cases**:
- `testCreateMetadata_Success`: Verify metadata creation
- `testCreateMetadata_DuplicateFileId`: Verify handling of duplicate entries

**Mocked Dependencies**:
- `FileMetadataRepository`

#### 4. FileMetadataRepository Tests

**Test Class**: `FileMetadataRepositoryTest`

**Testing Strategy**: Use `@DataJpaTest` with embedded H2 database

**Key Test Cases**:
- `testFindByFilename`: Verify query by filename
- `testFindAllByFileId`: Verify query by file ID
- `testFindByFileIdAndChunkId`: Verify composite query
- `testSaveAndRetrieve`: Verify persistence operations
- `testDeleteByFileId`: Verify deletion operations

**Test Configuration**:
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
```

#### 5. Configuration Tests

**Test Classes**: 
- `RabbitMQConfigTest`
- `CorsConfigTest`
- `GrpcServerConfigTest`

**Testing Strategy**: Use `@SpringBootTest` with specific configuration classes

**Key Test Cases**:
- `testRabbitMQBeans_Created`: Verify queue, exchange, and admin beans
- `testCorsConfiguration_AllowedOrigins`: Verify CORS settings
- `testGrpcServerConfiguration_Port`: Verify gRPC server initialization

#### 6. Integration Tests

**Test Class**: `FileUploadIntegrationTest`

**Testing Strategy**: Use `@SpringBootTest` with Testcontainers

**Key Test Cases**:
- `testEndToEndFileUpload`: Test complete upload workflow
- `testFileUploadAndRetrieval`: Test upload and download cycle
- `testConcurrentFileUploads`: Test concurrent upload handling

**Testcontainers Setup**:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("testdb")
    .withUsername("test")
    .withPassword("test");

@Container
static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.rabbitmq.host", rabbitmq::getHost);
    registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
}
```

### Worker Service Tests

#### 1. ChunkTaskConsumer Tests

**Test Class**: `ChunkTaskConsumerTest`

**Testing Strategy**: Unit test with mocked gRPC stubs and file system

**Key Test Cases**:
- `testHandleTask_LocalStorage`: Verify local chunk storage when worker is assigned
- `testHandleTask_ForwardToRemote`: Verify forwarding to remote worker
- `testHandleTask_InvalidMessage`: Verify error handling for malformed messages
- `testStoreChunkLocally_Success`: Verify local file writing
- `testStoreChunkLocally_DirectoryCreation`: Verify directory creation
- `testForwardChunkToRemote_Success`: Verify remote gRPC call

**Mocked Dependencies**:
- `SchedulerServiceGrpc.SchedulerServiceBlockingStub`
- `WorkerServiceGrpc.WorkerServiceBlockingStub`
- `ObjectMapper`

**Test Utilities**:
```java
ChunkTask createTestChunkTask(String fileId, int chunkId, byte[] data)
File createTempStorageDirectory()
void cleanupTestFiles(File directory)
```

#### 2. WorkerServiceImpl Tests

**Test Class**: `WorkerServiceImplTest`

**Testing Strategy**: Unit test with mocked gRPC components

**Key Test Cases**:
- `testStoreChunk_Success`: Verify chunk storage via gRPC
- `testStoreChunk_DirectoryCreation`: Verify storage directory creation
- `testStoreChunk_IOError`: Verify error handling for file write failures
- `testRetrieveChunk_Found`: Verify chunk retrieval when file exists
- `testRetrieveChunk_NotFound`: Verify response when chunk doesn't exist
- `testSendHeartbeat_Success`: Verify heartbeat sending
- `testInit_SchedulerClientInitialization`: Verify PostConstruct initialization

**Mocked Dependencies**:
- `StreamObserver<StoreChunkResponse>`
- `StreamObserver<RetrieveChunkResponse>`
- `SchedulerServiceGrpc.SchedulerServiceBlockingStub`

#### 3. Integration Tests

**Test Class**: `ChunkStorageIntegrationTest`

**Testing Strategy**: Use `@SpringBootTest` with Testcontainers for RabbitMQ

**Key Test Cases**:
- `testMessageConsumption`: Test RabbitMQ message consumption
- `testChunkStorageWorkflow`: Test end-to-end chunk storage
- `testConcurrentChunkProcessing`: Test concurrent message handling

**Testcontainers Setup**:
```java
@Container
static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", rabbitmq::getHost);
    registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
}
```

### Common Module Tests

#### 1. Protobuf Serialization Tests

**Test Class**: `ProtobufSerializationTest`

**Testing Strategy**: Unit test for Protocol Buffer message operations

**Key Test Cases**:
- `testHeartbeatRequest_Serialization`: Verify HeartbeatRequest serialization
- `testHeartbeatRequest_Deserialization`: Verify HeartbeatRequest deserialization
- `testAssignWorkerRequest_Serialization`: Verify AssignWorkerRequest serialization
- `testStoreChunkRequest_WithByteData`: Verify binary data handling
- `testAllMessages_RoundTrip`: Verify all message types serialize/deserialize correctly

**Test Pattern**:
```java
@Test
void testMessageRoundTrip() {
    // Create message
    HeartbeatRequest original = HeartbeatRequest.newBuilder()
        .setWorkerId("worker-1")
        .setAddress("localhost:8080")
        .build();
    
    // Serialize
    byte[] bytes = original.toByteArray();
    
    // Deserialize
    HeartbeatRequest deserialized = HeartbeatRequest.parseFrom(bytes);
    
    // Verify
    assertEquals(original.getWorkerId(), deserialized.getWorkerId());
    assertEquals(original.getAddress(), deserialized.getAddress());
}
```

#### 2. gRPC Service Stub Tests

**Test Class**: `GrpcServiceStubTest`

**Testing Strategy**: Verify stub generation and basic functionality

**Key Test Cases**:
- `testSchedulerServiceStub_Creation`: Verify SchedulerService stub creation
- `testWorkerServiceStub_Creation`: Verify WorkerService stub creation
- `testStubMethods_Available`: Verify all RPC methods are available
- `testBlockingStub_Creation`: Verify blocking stub creation
- `testAsyncStub_Creation`: Verify async stub creation

## Data Models

### Test Data Builders

**Purpose**: Provide reusable test data creation utilities

**Scheduler Test Data Builder**:
```java
public class TestDataBuilder {
    public static FileMetadata createFileMetadata(String fileId, String filename, int chunkId) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setFilename(filename);
        metadata.setChunkId(chunkId);
        metadata.setWorkerId("test-worker");
        metadata.setWorkerAddress("localhost:9090");
        metadata.setUploadTime(LocalDateTime.now());
        return metadata;
    }
    
    public static MockMultipartFile createMockFile(String filename, byte[] content) {
        return new MockMultipartFile(
            "file",
            filename,
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            content
        );
    }
    
    public static ChunkTask createChunkTask(String fileId, int chunkId, byte[] data) {
        String encodedData = Base64.getEncoder().encodeToString(data);
        return new ChunkTask(fileId, chunkId, encodedData);
    }
}
```

**Worker Test Data Builder**:
```java
public class TestDataBuilder {
    public static ChunkTask createChunkTask(String fileId, int chunkId, String encodedData) {
        ChunkTask task = new ChunkTask();
        task.setFileId(fileId);
        task.setChunkId(chunkId);
        task.setEncodedData(encodedData);
        return task;
    }
    
    public static StoreChunkRequest createStoreChunkRequest(
            String workerId, String fileId, int chunkId, byte[] data) {
        return StoreChunkRequest.newBuilder()
            .setWorkerId(workerId)
            .setFileId(fileId)
            .setChunkId(chunkId)
            .setChunkData(ByteString.copyFrom(data))
            .build();
    }
    
    public static File createTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        return tempDir.toFile();
    }
}
```

## Error Handling

### Test Error Scenarios

Each test suite will include error handling tests:

1. **Network Errors**: Simulate gRPC connection failures
2. **IO Errors**: Simulate file system failures
3. **Database Errors**: Simulate constraint violations and connection issues
4. **Message Broker Errors**: Simulate queue unavailability
5. **Validation Errors**: Test input validation and boundary conditions

### Error Testing Pattern

```java
@Test
void testErrorHandling_DatabaseConnectionFailure() {
    // Arrange
    when(repository.save(any())).thenThrow(new DataAccessException("Connection failed") {});
    
    // Act & Assert
    assertThrows(DataAccessException.class, () -> {
        service.saveMetadata(testData);
    });
    
    // Verify error logging or recovery mechanism
    verify(logger).error(contains("Connection failed"));
}
```

## Testing Strategy

### Test Execution Flow

1. **Fast Unit Tests**: Run first, complete in < 30 seconds
2. **Integration Tests**: Run after unit tests, use Testcontainers
3. **Configuration Tests**: Verify Spring context loading

### Test Isolation

- Each test method is independent
- Use `@BeforeEach` and `@AfterEach` for setup/cleanup
- Reset mocks between tests using `@MockitoSettings(strictness = Strictness.STRICT_STUBS)`
- Clean up file system resources in `@AfterEach`

### Test Coverage Goals

- **Minimum**: 70% line coverage per module
- **Target**: 80% line coverage with 90% branch coverage for critical paths
- **Exclusions**: Generated gRPC code, configuration POJOs

### Maven Configuration

Add to each module's `pom.xml`:

```xml
<dependencies>
    <!-- Existing test dependency -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>rabbitmq</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    
    <!-- gRPC Testing -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-testing</artifactId>
        <version>1.69.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Test Naming Conventions

Follow the pattern: `test<MethodName>_<Scenario>_<ExpectedResult>`

Examples:
- `testUploadFile_ValidInput_ReturnsSuccess`
- `testAssignWorker_NoActiveWorkers_ThrowsException`
- `testStoreChunk_IOError_ReturnsFailureResponse`

### Assertion Libraries

Use AssertJ for fluent assertions:

```java
assertThat(result)
    .isNotNull()
    .hasFieldOrPropertyWithValue("success", true)
    .extracting("message")
    .asString()
    .contains("Successfully stored");
```

## Test Environment Configuration

### Application Properties for Tests

Create `src/test/resources/application-test.properties` for each module:

**Scheduler**:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
grpc.server.port=9090
```

**Worker**:
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
worker.storage.path=target/test-storage
grpc.server.port=9091
```

### Test Profiles

Use `@ActiveProfiles("test")` to activate test-specific configurations

## Continuous Integration

### CI Pipeline Integration

```yaml
# Example GitHub Actions workflow
test:
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
    - name: Run Unit Tests
      run: mvn test -Dtest=!*Integration*
    - name: Run Integration Tests
      run: mvn test -Dtest=*Integration*
    - name: Generate Coverage Report
      run: mvn jacoco:report
    - name: Upload Coverage
      uses: codecov/codecov-action@v3
```

## Performance Considerations

### Test Optimization Strategies

1. **Use Test Slices**: `@WebMvcTest`, `@DataJpaTest` instead of full `@SpringBootTest`
2. **Reuse Containers**: Use `@Container` with `static` modifier for Testcontainers
3. **Parallel Execution**: Configure Maven Surefire for parallel test execution
4. **Mock Heavy Dependencies**: Mock gRPC stubs and external services
5. **Limit Integration Tests**: Focus integration tests on critical paths only

### Resource Cleanup

```java
@AfterEach
void cleanup() {
    // Clean up test files
    FileUtils.deleteDirectory(testStorageDir);
    
    // Reset mocks
    Mockito.reset(rabbitTemplate, repository);
    
    // Clear test data
    repository.deleteAll();
}
```

## Documentation

### Test Documentation Standards

Each test class should include:
- Class-level JavaDoc explaining the component under test
- Method-level comments for complex test scenarios
- Inline comments for non-obvious assertions

Example:
```java
/**
 * Unit tests for FileUploadController.
 * Tests REST endpoints for file upload and retrieval operations.
 * Uses @WebMvcTest to test only the web layer with mocked dependencies.
 */
@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {
    
    /**
     * Tests successful file upload with chunking.
     * Verifies that:
     * - File is split into correct number of chunks
     * - Each chunk is sent to RabbitMQ
     * - Metadata is created for the file
     */
    @Test
    void testUploadFile_LargeFile_ChunksCorrectly() {
        // Test implementation
    }
}
```
