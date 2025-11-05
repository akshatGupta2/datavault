# Implementation Plan

- [x] 1. Set up test infrastructure and dependencies





  - Add Testcontainers, JaCoCo, and gRPC testing dependencies to all module pom.xml files
  - Create test resource directories and application-test.properties files for each module
  - Configure JaCoCo Maven plugin for code coverage reporting
  - _Requirements: 1.5, 2.5, 3.5, 7.5_
-

- [x] 2. Create test utility classes and test data builders



  - [x] 2.1 Implement scheduler TestDataBuilder utility class


    - Create methods for generating FileMetadata test objects
    - Create methods for generating MockMultipartFile objects
    - Create methods for generating ChunkTask test objects
    - _Requirements: 6.4_


  - [x] 2.2 Implement worker TestDataBuilder utility class

    - Create methods for generating ChunkTask test objects
    - Create methods for generating StoreChunkRequest protobuf messages
    - Create utility methods for temporary directory creation and cleanup
    - _Requirements: 6.4_

- [x] 3. Implement scheduler service unit tests




  - [x] 3.1 Create FileUploadControllerTest with @WebMvcTest


    - Write test for successful file upload with MockMultipartFile
    - Write test for empty file upload handling
    - Write test for large file chunking logic
    - Write test for successful file retrieval by name
    - Write test for 404 response when file not found
    - Write test for chunk assembly logic
    - Mock FileMetadataRepository, RabbitTemplate, and CreateMetadataService
    - _Requirements: 1.1, 1.5_


  - [x] 3.2 Create SchedulerServiceImplTest for gRPC service

    - Write test for new worker registration via heartbeat
    - Write test for existing worker heartbeat update
    - Write test for round-robin worker assignment algorithm
    - Write test for error when no active workers available
    - Write test for duplicate chunk assignment handling
    - Write test for inactive worker cleanup
    - Write test for active worker filtering
    - Mock FileMetadataRepository and gRPC StreamObserver
    - _Requirements: 1.4, 1.5_

  - [x] 3.3 Create CreateMetadataServiceTest


    - Write test for successful metadata creation
    - Write test for duplicate file ID handling
    - Mock FileMetadataRepository
    - _Requirements: 1.4, 1.5_

  - [x] 3.4 Create FileMetadataRepositoryTest with @DataJpaTest




    - Write test for findByFilename query method
    - Write test for findAllByFileId query method
    - Write test for findByFileIdAndChunkId composite query
    - Write test for save and retrieve operations
    - Write test for delete operations
    - Use H2 in-memory database for testing
    - _Requirements: 1.2, 1.5_

- [x] 4. Implement scheduler service configuration tests





  - [x] 4.1 Create RabbitMQConfigTest


    - Write test to verify Queue bean creation
    - Write test to verify RabbitAdmin bean creation
    - Write test to verify queue properties (durable, name)
    - Use @SpringBootTest with specific configuration class
    - _Requirements: 8.1, 8.5_

  - [x] 4.2 Create CorsConfigTest


    - Write test to verify CORS allowed origins configuration
    - Write test to verify allowed HTTP methods
    - Write test to verify allowed headers
    - _Requirements: 8.2, 8.5_

  - [x] 4.3 Create GrpcServerConfigTest


    - Write test to verify gRPC server bean initialization
    - Write test to verify server port configuration
    - Write test to verify service registration
    - _Requirements: 8.3, 8.5_

- [ ] 5. Implement scheduler service integration tests





  - [x] 5.1 Create FileUploadIntegrationTest with Testcontainers


    - Set up PostgreSQL Testcontainer
    - Set up RabbitMQ Testcontainer
    - Configure dynamic properties for test containers
    - Write test for end-to-end file upload workflow
    - Write test for file upload and retrieval cycle
    - Write test for concurrent file uploads
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_



  - [x] 5.2 Create SchedulerApplicationTest

    - Write test to verify Spring Boot application context loads
    - Write test to verify all required beans are created
    - Write test to verify configuration properties are applied
    - Use @SpringBootTest annotation
    - _Requirements: 4.4, 4.5_

- [x] 6. Implement worker service unit tests






  - [x] 6.1 Create ChunkTaskConsumerTest


    - Write test for local chunk storage when worker is assigned
    - Write test for forwarding chunk to remote worker
    - Write test for invalid message handling
    - Write test for local file writing with directory creation
    - Write test for remote gRPC call forwarding
    - Mock SchedulerServiceBlockingStub and ObjectMapper
    - Use temporary directories for file operations
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

  - [x] 6.2 Create WorkerServiceImplTest for gRPC service




    - Write test for successful chunk storage via gRPC
    - Write test for storage directory creation
    - Write test for IO error handling during file write
    - Write test for chunk retrieval when file exists
    - Write test for chunk retrieval when file not found
    - Write test for heartbeat sending
    - Write test for PostConstruct initialization
    - Mock StreamObserver and SchedulerServiceBlockingStub
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

- [ ] 7. Implement worker service integration tests




  - [x] 7.1 Create ChunkStorageIntegrationTest with Testcontainers


    - Set up RabbitMQ Testcontainer
    - Configure dynamic properties for RabbitMQ
    - Write test for RabbitMQ message consumption
    - Write test for end-to-end chunk storage workflow
    - Write test for concurrent chunk processing
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 7.2 Create WorkerApplicationTest





    - Write test to verify Spring Boot application context loads
    - Write test to verify worker service beans are created
    - Write test to verify RabbitMQ listener configuration
    - _Requirements: 5.4_

- [x] 8. Implement common module tests




  - [x] 8.1 Create ProtobufSerializationTest


    - Write test for HeartbeatRequest serialization
    - Write test for HeartbeatRequest deserialization
    - Write test for AssignWorkerRequest serialization and deserialization
    - Write test for StoreChunkRequest with binary data
    - Write test for RetrieveChunkRequest round-trip
    - Write test for all message types serialization round-trip
    - _Requirements: 3.1, 3.2, 3.5_

  - [x] 8.2 Create GrpcServiceStubTest


    - Write test for SchedulerService stub creation
    - Write test for WorkerService stub creation
    - Write test to verify all RPC methods are available
    - Write test for blocking stub creation
    - Write test for async stub creation
    - _Requirements: 3.2, 3.4, 3.5_

  - [x] 8.3 Create CommonApplicationTest


    - Write test to verify common module application context loads
    - Write test to verify protobuf classes are generated correctly
    - _Requirements: 3.3, 3.5_

- [x] 9. Implement test organization and documentation





  - [x] 9.1 Organize tests in proper package structure


    - Ensure test packages mirror main source code structure
    - Separate unit tests from integration tests
    - Create dedicated integration test packages
    - _Requirements: 6.1, 6.3_

  - [x] 9.2 Add comprehensive test documentation


    - Add class-level JavaDoc to all test classes
    - Add method-level comments for complex test scenarios
    - Document test setup and teardown logic
    - Add inline comments for non-obvious assertions
    - _Requirements: 6.2, 6.5_

- [ ] 10. Configure test execution and reporting






  - [x] 10.1 Configure Maven Surefire plugin for test execution

    - Set up parallel test execution configuration
    - Configure test inclusion/exclusion patterns
    - Set up test execution order (unit tests before integration)
    - _Requirements: 7.1, 7.2, 7.5_



  - [x] 10.2 Verify test coverage meets requirements




    - Run JaCoCo coverage report for all modules
    - Verify minimum 70% line coverage achieved
    - Identify and document any coverage gaps


    - _Requirements: 1.5, 2.5, 3.5_



  - [x] 10.3 Set up test resource cleanup



    - Implement @AfterEach cleanup methods for file resources
    - Implement mock reset logic between tests
    - Verify no test data leakage between test methods
    - _Requirements: 7.4_
