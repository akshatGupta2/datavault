package com.scheduler.scheduler.service;

import com.scheduler.scheduler.model.FileMetadata;
import com.scheduler.scheduler.repository.FileMetadataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateMetadataService.
 * Tests metadata creation and duplicate handling.
 * Uses mocked FileMetadataRepository.
 */
@ExtendWith(MockitoExtension.class)
class CreateMetadataServiceTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @InjectMocks
    private CreateMetadataService createMetadataService;

    /**
     * Cleanup method to reset mocks after each test.
     * Ensures no test data leakage between test methods.
     */
    @AfterEach
    void cleanup() {
        // Reset all mocks to ensure clean state for next test
        reset(fileMetadataRepository);
    }

    /**
     * Tests successful metadata creation.
     * Verifies that metadata is created with correct values and saved to repository.
     */
    @Test
    void testCreateMetadata_Success() {
        // Arrange
        String fileId = "file-123";
        String filename = "test.txt";
        long size = 1024L;
        LocalDateTime uploadTime = LocalDateTime.now();

        FileMetadata expectedMetadata = new FileMetadata(fileId, filename, size);
        expectedMetadata.setUploadTime(uploadTime);

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(expectedMetadata);

        // Act
        FileMetadata result = createMetadataService.createMetadata(fileId, filename, size, uploadTime);

        // Assert
        assertNotNull(result);
        assertEquals(fileId, result.getFileId());
        assertEquals(filename, result.getFilename());
        assertEquals(size, result.getSize());
        assertEquals(uploadTime, result.getUploadTime());

        // Verify repository interaction
        ArgumentCaptor<FileMetadata> metadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
        verify(fileMetadataRepository, times(1)).save(metadataCaptor.capture());

        FileMetadata savedMetadata = metadataCaptor.getValue();
        assertEquals(fileId, savedMetadata.getFileId());
        assertEquals(filename, savedMetadata.getFilename());
        assertEquals(size, savedMetadata.getSize());
        assertEquals(uploadTime, savedMetadata.getUploadTime());
    }

    /**
     * Tests metadata creation with null fileId.
     * Verifies that NullPointerException is thrown for null fileId.
     */
    @Test
    void testCreateMetadata_NullFileId_ThrowsException() {
        // Arrange
        String filename = "test.txt";
        long size = 1024L;
        LocalDateTime uploadTime = LocalDateTime.now();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            createMetadataService.createMetadata(null, filename, size, uploadTime);
        });

        // Verify repository was never called
        verify(fileMetadataRepository, never()).save(any());
    }

    /**
     * Tests metadata creation with null filename.
     * Verifies that NullPointerException is thrown for null filename.
     */
    @Test
    void testCreateMetadata_NullFilename_ThrowsException() {
        // Arrange
        String fileId = "file-123";
        long size = 1024L;
        LocalDateTime uploadTime = LocalDateTime.now();

        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            createMetadataService.createMetadata(fileId, null, size, uploadTime);
        });

        // Verify repository was never called
        verify(fileMetadataRepository, never()).save(any());
    }

    /**
     * Tests duplicate file ID handling.
     * Verifies that DataIntegrityViolationException is thrown when duplicate fileId is saved.
     */
    @Test
    void testCreateMetadata_DuplicateFileId_ThrowsException() {
        // Arrange
        String fileId = "file-123";
        String filename = "test.txt";
        long size = 1024L;
        LocalDateTime uploadTime = LocalDateTime.now();

        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () -> {
            createMetadataService.createMetadata(fileId, filename, size, uploadTime);
        });

        // Verify repository was called
        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }

    /**
     * Tests metadata creation with zero size.
     * Verifies that metadata can be created for empty files.
     */
    @Test
    void testCreateMetadata_ZeroSize_Success() {
        // Arrange
        String fileId = "file-456";
        String filename = "empty.txt";
        long size = 0L;
        LocalDateTime uploadTime = LocalDateTime.now();

        FileMetadata expectedMetadata = new FileMetadata(fileId, filename, size);
        expectedMetadata.setUploadTime(uploadTime);

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(expectedMetadata);

        // Act
        FileMetadata result = createMetadataService.createMetadata(fileId, filename, size, uploadTime);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getSize());
        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }

    /**
     * Tests metadata creation with large file size.
     * Verifies that metadata can be created for large files.
     */
    @Test
    void testCreateMetadata_LargeSize_Success() {
        // Arrange
        String fileId = "file-789";
        String filename = "large.bin";
        long size = 10L * 1024 * 1024 * 1024; // 10 GB
        LocalDateTime uploadTime = LocalDateTime.now();

        FileMetadata expectedMetadata = new FileMetadata(fileId, filename, size);
        expectedMetadata.setUploadTime(uploadTime);

        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(expectedMetadata);

        // Act
        FileMetadata result = createMetadataService.createMetadata(fileId, filename, size, uploadTime);

        // Assert
        assertNotNull(result);
        assertEquals(size, result.getSize());
        verify(fileMetadataRepository, times(1)).save(any(FileMetadata.class));
    }
}
