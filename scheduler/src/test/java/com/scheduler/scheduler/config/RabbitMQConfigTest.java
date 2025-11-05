package com.scheduler.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Unit tests for RabbitMQConfig.
 * Tests RabbitMQ configuration beans including Queue and RabbitAdmin.
 * Uses @SpringBootTest to verify bean creation and configuration.
 */
@SpringBootTest(classes = RabbitMQConfig.class)
@TestPropertySource(properties = {
    "spring.rabbitmq.host=localhost",
    "spring.rabbitmq.port=5672"
})
class RabbitMQConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private ConnectionFactory connectionFactory;

    /**
     * Cleanup method to reset mocks after each test.
     * Ensures no test data leakage between test methods.
     */
    @AfterEach
    void cleanup() {
        // Reset all mocks to ensure clean state for next test
        reset(connectionFactory);
    }

    /**
     * Tests that the Queue bean is created correctly.
     * Verifies that the fileChunksQueue bean exists in the application context.
     */
    @Test
    void testQueueBean_Created() {
        Queue queue = applicationContext.getBean("fileChunksQueue", Queue.class);
        
        assertThat(queue).isNotNull();
    }

    /**
     * Tests that the RabbitAdmin bean is created correctly.
     * Verifies that the rabbitAdmin bean exists in the application context.
     */
    @Test
    void testRabbitAdminBean_Created() {
        RabbitAdmin rabbitAdmin = applicationContext.getBean("rabbitAdmin", RabbitAdmin.class);
        
        assertThat(rabbitAdmin).isNotNull();
    }

    /**
     * Tests that the queue has correct properties.
     * Verifies:
     * - Queue name matches expected value
     * - Queue is durable (survives broker restart)
     */
    @Test
    void testQueueProperties_CorrectConfiguration() {
        Queue queue = applicationContext.getBean("fileChunksQueue", Queue.class);
        
        assertThat(queue.getName()).isEqualTo(RabbitMQConfig.CHUNK_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }
}
