package com.scheduler.scheduler.controller;

import com.scheduler.scheduler.model.FileMetadata;
import com.scheduler.scheduler.repository.FileMetadataRepository;
import com.scheduler.scheduler.service.CreateMetadataService;
import com.scheduler.scheduler.util.TestDataBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for FileUploadController.
 * Tests REST endpoints for file upload and retrieval operations.
 * Uses @WebMvcTest to test only the web layer with mocked dependencies.
 */
@WebMvcTest(controllers = FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileMetadataRepository metadataRepository;

    @MockBean
    private EntityManager entityManager;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private CreateMetadataService createMetadataService;

    // Mock configuration beans to prevent them from being loaded
    @MockBean(name = "grpcServerConfig")
    private com.scheduler.scheduler.config.GrpcServerConfig grpcServerConfig;

    @MockBean
    private com.scheduler.scheduler.service.SchedulerServiceImpl schedulerServiceImpl;

    /**
     * Cleanup method to reset mocks after each test.
     * Ensures no test data leakage between test methods.
     */
    @AfterEach
    void cleanup() {
        // Reset all mocks to ensure clean state for next test
        reset(metadataRepository, rabbitTemplate, createMetadataService, entityManager);
    }

    /**
     * Tests successful file upload with MockMultipartFile.
     * Verifies that the controller processes the file and returns success message.
     */
    @Test
    void testUploadFile_Success() throws Exception {
        // Arrange
        byte[] content = "Test file content".getBytes();
        MockMultipartFile file = TestDataBuilder.createMockMultipartFile("test.txt", content);

        FileMetadata mockMetadata = TestDataBuilder.createFileMetadata("file-123", "test.txt", 0);
        when(createMetadataService.createMetadata(anyString(), eq("test.txt"), eq((long) content.length), any(LocalDateTime.class)))
                .thenReturn(mockMetadata);

        // Act & Assert
        mockMvc.perform(multipart("/files/uploadFile")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload successful")));

        // Verify interactions
        verify(createMetadataService, times(1)).createMetadata(anyString(), eq("test.txt"), eq((long) content.length), any(LocalDateTime.class));
        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * Tests handling of empty file upload.
     * Verifies that the controller processes empty files without errors.
     */
    @Test
    void testUploadFile_EmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = TestDataBuilder.createEmptyMockMultipartFile("empty.txt");

        FileMetadata mockMetadata = TestDataBuilder.createFileMetadata("file-456", "empty.txt", 0);
        when(createMetadataService.createMetadata(anyString(), eq("empty.txt"), eq(0L), any(LocalDateTime.class)))
                .thenReturn(mockMetadata);

        // Act & Assert
        mockMvc.perform(multipart("/files/uploadFile")
                        .file(emptyFile))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload successful. Total chunks sent: 0")));

        // Verify metadata creation was called but no chunks were sent
        verify(createMetadataService, times(1)).createMetadata(anyString(), eq("empty.txt"), eq(0L), any(LocalDateTime.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * Tests chunking logic for large files.
     * Verifies that files larger than chunk size are split into multiple chunks.
     */
    @Test
    void testUploadFile_LargeFile_ChunksCorrectly() throws Exception {
        // Arrange - Create a file larger than chunk size (128KB)
        int chunkSize = 128 * 1024;
        byte[] largeContent = new byte[chunkSize * 2 + 1000]; // 2.5 chunks
        Arrays.fill(largeContent, (byte) 'A');
        MockMultipartFile largeFile = TestDataBuilder.createMockMultipartFile("large.txt", largeContent);

        FileMetadata mockMetadata = TestDataBuilder.createFileMetadata("file-789", "large.txt", 0);
        when(createMetadataService.createMetadata(anyString(), eq("large.txt"), eq((long) largeContent.length), any(LocalDateTime.class)))
                .thenReturn(mockMetadata);

        // Act & Assert
        mockMvc.perform(multipart("/files/uploadFile")
                        .file(largeFile))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload successful. Total chunks sent: 3")));

        // Verify 3 chunks were sent to RabbitMQ
        verify(rabbitTemplate, times(3)).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * Tests successful file retrieval by name.
     * Verifies that the controller returns the file with correct headers.
     */
    @Test
    void testGetFile_Success() throws Exception {
        // Arrange
        String filename = "test.txt";
        String fileId = "file-123";
        FileMetadata metadata = TestDataBuilder.createFileMetadata(fileId, filename, 0);

        when(metadataRepository.findByFilename(filename)).thenReturn(metadata);
        when(metadataRepository.findAllByFileId(fileId)).thenReturn(List.of(metadata));

        // Act & Assert
        mockMvc.perform(get("/files/getFile")
                        .param("name", filename))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(filename)));

        // Verify repository interactions
        verify(metadataRepository, times(1)).findByFilename(filename);
        verify(metadataRepository, times(1)).findAllByFileId(fileId);
    }

    /**
     * Tests 404 response when file is not found.
     * Verifies that the controller returns NOT_FOUND status for non-existent files.
     */
    @Test
    void testGetFile_NotFound() throws Exception {
        // Arrange
        String filename = "nonexistent.txt";
        when(metadataRepository.findByFilename(filename)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/files/getFile")
                        .param("name", filename))
                .andExpect(status().isNotFound());

        // Verify repository was queried
        verify(metadataRepository, times(1)).findByFilename(filename);
        verify(metadataRepository, never()).findAllByFileId(anyString());
    }

    /**
     * Tests chunk assembly logic for multi-chunk files.
     * Verifies that the controller correctly assembles chunks in order.
     */
    @Test
    void testGetFile_ChunkAssembly_MultipleChunks() throws Exception {
        // Arrange
        String filename = "multipart.txt";
        String fileId = "file-999";
        
        // Create metadata for 3 chunks
        FileMetadata chunk0 = TestDataBuilder.createFileMetadata(fileId, filename, 0, 1024L, "worker-1", "localhost:9090");
        FileMetadata chunk1 = TestDataBuilder.createFileMetadata(fileId, filename, 1, 1024L, "worker-1", "localhost:9090");
        FileMetadata chunk2 = TestDataBuilder.createFileMetadata(fileId, filename, 2, 1024L, "worker-1", "localhost:9090");
        
        List<FileMetadata> chunks = Arrays.asList(chunk0, chunk1, chunk2);

        when(metadataRepository.findByFilename(filename)).thenReturn(chunk0);
        when(metadataRepository.findAllByFileId(fileId)).thenReturn(chunks);

        // Act & Assert
        mockMvc.perform(get("/files/getFile")
                        .param("name", filename))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));

        // Verify chunks were retrieved
        verify(metadataRepository, times(1)).findByFilename(filename);
        verify(metadataRepository, times(1)).findAllByFileId(fileId);
    }
}
