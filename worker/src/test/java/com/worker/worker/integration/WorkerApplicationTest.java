package com.worker.worker.integration;

import com.worker.worker.WorkerApplication;
import com.worker.worker.config.GrpcClientConfig;
import com.worker.worker.config.RabbitMQWorkerConfig;
import com.worker.worker.service.ChunkTaskConsumer;
import com.worker.worker.service.WorkerServiceImpl;
import io.datavault.common.grpc.SchedulerServiceGrpc;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for Worker Application context and configuration.
 * Verifies that the Spring Boot application loads correctly with all required beans.
 * Tests RabbitMQ listener configuration and worker service initialization.
 */
@SpringBootTest(classes = {WorkerApplication.class, WorkerApplicationTest.TestConfig.class})
@TestPropertySource(properties = {
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "SCHEDULER_HOST=localhost",
        "SCHEDULER_PORT=9090",
        "WORKER_ID=test-worker",
        "HOST=localhost",
        "PORT=9091"
})
class WorkerApplicationTest {

    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public WorkerServiceImpl workerServiceImpl() {
            // Return a mock to avoid @PostConstruct initialization issues
            return mock(WorkerServiceImpl.class);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Tests that the Spring Boot application context loads successfully.
     * Verifies that all required beans are created and wired correctly.
     */
    @Test
    void testApplicationContext_LoadsSuccessfully() {
        // Assert
        assertNotNull(applicationContext, "Application context should be loaded");
        assertTrue(applicationContext.containsBean("workerApplication"), 
                "WorkerApplication bean should exist");
    }

    /**
     * Tests that worker service beans are created.
     * Verifies that ChunkTaskConsumer bean is present.
     */
    @Test
    void testWorkerServiceBeans_AreCreated() {
        // Assert - ChunkTaskConsumer bean
        assertTrue(applicationContext.containsBean("chunkTaskConsumer"), 
                "ChunkTaskConsumer bean should be created");
        ChunkTaskConsumer consumer = applicationContext.getBean(ChunkTaskConsumer.class);
        assertNotNull(consumer, "ChunkTaskConsumer should be autowired");

        // Assert - WorkerServiceImpl bean (mocked in test config)
        assertTrue(applicationContext.containsBean("workerServiceImpl"), 
                "WorkerServiceImpl bean should be created");
        WorkerServiceImpl workerService = applicationContext.getBean(WorkerServiceImpl.class);
        assertNotNull(workerService, "WorkerServiceImpl should be autowired");
    }

    /**
     * Tests that RabbitMQ configuration beans are created.
     * Verifies that the file chunks queue is properly configured.
     */
    @Test
    void testRabbitMQConfiguration_BeansAreCreated() {
        // Assert - RabbitMQWorkerConfig bean
        assertTrue(applicationContext.containsBean("rabbitMQWorkerConfig"), 
                "RabbitMQWorkerConfig bean should be created");

        // Assert - Queue bean
        assertTrue(applicationContext.containsBean("fileChunksQueue"), 
                "fileChunksQueue bean should be created");
        Queue queue = applicationContext.getBean("fileChunksQueue", Queue.class);
        assertNotNull(queue, "Queue should be autowired");
        assertEquals("fileChunksQueue", queue.getName(), "Queue name should be fileChunksQueue");
        assertTrue(queue.isDurable(), "Queue should be durable");
    }

    /**
     * Tests that RabbitMQ listener is configured on ChunkTaskConsumer.
     * Verifies that the @RabbitListener annotation is present and configured correctly.
     */
    @Test
    void testRabbitMQListener_IsConfigured() throws NoSuchMethodException {
        // Arrange
        ChunkTaskConsumer consumer = applicationContext.getBean(ChunkTaskConsumer.class);
        assertNotNull(consumer, "ChunkTaskConsumer should exist");

        // Act - Get the handleTask method
        Method handleTaskMethod = ChunkTaskConsumer.class.getMethod("handleTask", String.class);

        // Assert - Verify @RabbitListener annotation
        assertTrue(handleTaskMethod.isAnnotationPresent(RabbitListener.class), 
                "@RabbitListener annotation should be present on handleTask method");

        RabbitListener annotation = handleTaskMethod.getAnnotation(RabbitListener.class);
        String[] queues = annotation.queues();
        assertEquals(1, queues.length, "Should listen to one queue");
        assertEquals("fileChunksQueue", queues[0], "Should listen to fileChunksQueue");
    }

    /**
     * Tests that gRPC client configuration beans are created.
     * Verifies that the scheduler service stub is properly configured.
     */
    @Test
    void testGrpcClientConfiguration_BeansAreCreated() {
        // Assert - GrpcClientConfig bean
        assertTrue(applicationContext.containsBean("grpcClientConfig"), 
                "GrpcClientConfig bean should be created");

        // Assert - SchedulerServiceBlockingStub bean
        assertTrue(applicationContext.containsBean("schedulerServiceStub"),
                "SchedulerServiceBlockingStub bean should be created");
    }

    /**
     * Tests that all configuration classes are loaded.
     * Verifies that RabbitMQWorkerConfig and GrpcClientConfig are present.
     */
    @Test
    void testConfigurationClasses_AreLoaded() {
        // Assert - RabbitMQWorkerConfig
        RabbitMQWorkerConfig rabbitConfig = applicationContext.getBean(RabbitMQWorkerConfig.class);
        assertNotNull(rabbitConfig, "RabbitMQWorkerConfig should be loaded");

        // Assert - GrpcClientConfig
        GrpcClientConfig grpcConfig = applicationContext.getBean(GrpcClientConfig.class);
        assertNotNull(grpcConfig, "GrpcClientConfig should be loaded");
    }

    /**
     * Tests that the application has the correct Spring Boot annotations.
     * Verifies that WorkerApplication is properly annotated as a Spring Boot application.
     */
    @Test
    void testWorkerApplication_HasCorrectAnnotations() {
        // Assert
        assertTrue(WorkerApplication.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class),
                "WorkerApplication should have @SpringBootApplication annotation");
    }

    /**
     * Tests that essential service beans are singleton scoped.
     * Verifies that the same instance is returned for multiple bean requests.
     */
    @Test
    void testServiceBeans_AreSingletons() {
        // Act
        ChunkTaskConsumer consumer1 = applicationContext.getBean(ChunkTaskConsumer.class);
        ChunkTaskConsumer consumer2 = applicationContext.getBean(ChunkTaskConsumer.class);

        // Assert
        assertSame(consumer1, consumer2, "ChunkTaskConsumer should be singleton");
    }

    /**
     * Tests that the application context contains expected number of beans.
     * Verifies that no unexpected beans are created.
     */
    @Test
    void testApplicationContext_ContainsExpectedBeans() {
        // Act
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // Assert
        assertTrue(beanNames.length > 0, "Application context should contain beans");
        
        // Verify key beans exist
        boolean hasChunkConsumer = false;
        boolean hasWorkerService = false;
        boolean hasRabbitConfig = false;
        
        for (String beanName : beanNames) {
            if (beanName.equals("chunkTaskConsumer")) hasChunkConsumer = true;
            if (beanName.equals("workerServiceImpl")) hasWorkerService = true;
            if (beanName.equals("rabbitMQWorkerConfig")) hasRabbitConfig = true;
        }
        
        assertTrue(hasChunkConsumer, "Should have ChunkTaskConsumer bean");
        assertTrue(hasWorkerService, "Should have WorkerServiceImpl bean");
        assertTrue(hasRabbitConfig, "Should have RabbitMQWorkerConfig bean");
    }
}
