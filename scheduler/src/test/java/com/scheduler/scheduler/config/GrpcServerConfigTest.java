package com.scheduler.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

import com.scheduler.scheduler.repository.FileMetadataRepository;
import com.scheduler.scheduler.service.CreateMetadataService;
import com.scheduler.scheduler.service.SchedulerServiceImpl;

/**
 * Unit tests for GrpcServerConfig.
 * Tests gRPC server configuration including bean initialization, port configuration,
 * and service registration.
 * Uses @SpringBootTest with test profile to use port 0 (random port) to avoid conflicts.
 */
@SpringBootTest(properties = {"grpc.server.port=0"})
class GrpcServerConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GrpcServerConfig grpcServerConfig;

    @MockBean
    private FileMetadataRepository fileMetadataRepository;

    @MockBean
    private CreateMetadataService createMetadataService;

    @MockBean
    private ConnectionFactory connectionFactory;

    /**
     * Cleanup method to reset mocks after each test.
     * Ensures no test data leakage between test methods.
     */
    @AfterEach
    void cleanup() {
        // Reset all mocks to ensure clean state for next test
        reset(fileMetadataRepository, createMetadataService, connectionFactory);
    }

    /**
     * Tests that the GrpcServerConfig bean is initialized correctly.
     * Verifies that the configuration bean exists in the application context.
     */
    @Test
    void testGrpcServerConfig_BeanInitialization() {
        GrpcServerConfig config = applicationContext.getBean(GrpcServerConfig.class);
        
        assertThat(config).isNotNull();
    }

    /**
     * Tests that the SchedulerServiceImpl is registered as a service.
     * Verifies that the gRPC service implementation bean exists.
     */
    @Test
    void testGrpcServer_ServiceRegistration() {
        SchedulerServiceImpl service = applicationContext.getBean(SchedulerServiceImpl.class);
        
        assertThat(service).isNotNull();
    }

    /**
     * Tests that the gRPC server configuration is properly set up.
     * Verifies that the GrpcServerConfig bean is autowired and accessible.
     */
    @Test
    void testGrpcServer_ConfigurationSetup() {
        assertThat(grpcServerConfig).isNotNull();
    }
}
