package com.scheduler.scheduler.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for CorsConfig.
 * Tests CORS configuration including allowed origins, methods, and headers.
 * Uses MockMvc to verify CORS headers in HTTP responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Tests that CORS allowed origins are configured correctly.
     * Verifies that the frontend origin (http://localhost:5173) is allowed.
     */
    @Test
    void testCorsConfiguration_AllowedOrigins() throws Exception {
        mockMvc.perform(options("/api/files/upload")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    /**
     * Tests that CORS allowed HTTP methods are configured correctly.
     * Verifies that GET, POST, PUT, DELETE, and OPTIONS methods are allowed.
     */
    @Test
    void testCorsConfiguration_AllowedMethods() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/files/upload")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andReturn();

        String allowedMethods = result.getResponse().getHeader("Access-Control-Allow-Methods");
        
        assertThat(allowedMethods).isNotNull();
        assertThat(allowedMethods).contains("GET");
        assertThat(allowedMethods).contains("POST");
        assertThat(allowedMethods).contains("PUT");
        assertThat(allowedMethods).contains("DELETE");
        assertThat(allowedMethods).contains("OPTIONS");
    }

    /**
     * Tests that CORS allowed headers are configured correctly.
     * Verifies that all headers (*) are allowed.
     */
    @Test
    void testCorsConfiguration_AllowedHeaders() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/files/upload")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andReturn();

        String allowedHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
        
        assertThat(allowedHeaders).isNotNull();
        assertThat(allowedHeaders).contains("Content-Type");
    }
}
