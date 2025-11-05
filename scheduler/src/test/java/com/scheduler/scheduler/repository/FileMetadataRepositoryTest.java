package com.scheduler.scheduler.repository;

import com.scheduler.scheduler.model.FileMetadata;
import com.scheduler.scheduler.util.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileMetadataRepository.
 * Tests repository query methods using H2 in-memory database.
 * Uses @DataJpaTest to test only the persistence layer.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class FileMetadataRepositoryTest {

    @Autowired
    private FileMetadataRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * Cleanup method to clear test data after each test.
     * Ensures no test data leakage between test methods.
     */
    @AfterEach
    void cleanup() {
        // Clear all test data from repository
        repository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Tests findByFilename query method.
     * Verifies that metadata can be retrieved by filename.
     */
    @Test
    void testFindByFilename() {
        // Arrange
        String fileId = "file-123";
        String filename = "test.txt";
        FileMetadata metadata = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        entityManager.persist(metadata);
        entityManager.flush();

        // Act
        FileMetadata result = repository.findByFilename(filename);

        // Assert
        assertNotNull(result);
        assertEquals(fileId, result.getFileId());
        assertEquals(filename, result.getFilename());
        assertEquals(0, result.getChunkId());
    }

    /**
     * Tests findByFilename when file doesn't exist.
     * Verifies that null is returned for non-existent filename.
     */
    @Test
    void testFindByFilename_NotFound() {
        // Act
        FileMetadata result = repository.findByFilename("nonexistent.txt");

        // Assert
        assertNull(result);
    }

    /**
     * Tests findAllByFileId query method.
     * Verifies that all chunks for a file can be retrieved.
     */
    @Test
    void testFindAllByFileId() {
        // Arrange
        String fileId = "file-456";
        String filename = "multipart.txt";
        
        FileMetadata chunk0 = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        FileMetadata chunk1 = TestDataBuilder.createFileMetadata(fileId, filename, 1);
        FileMetadata chunk2 = TestDataBuilder.createFileMetadata(fileId, filename, 2);
        
        entityManager.persist(chunk0);
        entityManager.persist(chunk1);
        entityManager.persist(chunk2);
        entityManager.flush();

        // Act
        List<FileMetadata> results = repository.findAllByFileId(fileId);

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Verify all chunks belong to the same file
        results.forEach(metadata -> assertEquals(fileId, metadata.getFileId()));
    }

    /**
     * Tests findAllByFileId when no chunks exist.
     * Verifies that empty list is returned for non-existent fileId.
     */
    @Test
    void testFindAllByFileId_EmptyList() {
        // Act
        List<FileMetadata> results = repository.findAllByFileId("nonexistent-file");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Tests findByFileIdAndChunkId composite query.
     * Verifies that specific chunk can be retrieved by fileId and chunkId.
     */
    @Test
    void testFindByFileIdAndChunkId() {
        // Arrange
        String fileId = "file-789";
        String filename = "test.bin";
        int chunkId = 5;
        
        FileMetadata metadata = TestDataBuilder.createFileMetadata(fileId, filename, chunkId);
        entityManager.persist(metadata);
        entityManager.flush();

        // Act
        Optional<FileMetadata> result = repository.findByFileIdAndChunkId(fileId, chunkId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(fileId, result.get().getFileId());
        assertEquals(chunkId, result.get().getChunkId());
        assertEquals(filename, result.get().getFilename());
    }

    /**
     * Tests findByFileIdAndChunkId when chunk doesn't exist.
     * Verifies that empty Optional is returned for non-existent chunk.
     */
    @Test
    void testFindByFileIdAndChunkId_NotFound() {
        // Act
        Optional<FileMetadata> result = repository.findByFileIdAndChunkId("file-999", 99);

        // Assert
        assertFalse(result.isPresent());
    }

    /**
     * Tests save and retrieve operations.
     * Verifies that metadata can be saved and retrieved correctly.
     */
    @Test
    void testSaveAndRetrieve() {
        // Arrange
        String fileId = "file-111";
        String filename = "save-test.txt";
        int chunkId = 0;
        long size = 2048L;
        String workerId = "worker-1";
        String workerAddress = "localhost:9090";
        LocalDateTime uploadTime = LocalDateTime.now();

        FileMetadata metadata = new FileMetadata(fileId, filename, size);
        metadata.setChunkId(chunkId);
        metadata.setWorkerId(workerId);
        metadata.setWorkerAddress(workerAddress);
        metadata.setUploadTime(uploadTime);

        // Act
        FileMetadata saved = repository.save(metadata);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context to force database read

        Optional<FileMetadata> retrieved = repository.findByFileIdAndChunkId(fileId, chunkId);

        // Assert
        assertNotNull(saved);
        assertTrue(retrieved.isPresent());
        
        FileMetadata result = retrieved.get();
        assertEquals(fileId, result.getFileId());
        assertEquals(filename, result.getFilename());
        assertEquals(chunkId, result.getChunkId());
        assertEquals(size, result.getSize());
        assertEquals(workerId, result.getWorkerId());
        assertEquals(workerAddress, result.getWorkerAddress());
        assertNotNull(result.getUploadTime());
    }

    /**
     * Tests update operation on existing metadata.
     * Verifies that metadata can be updated and changes are persisted.
     */
    @Test
    void testUpdate() {
        // Arrange
        String fileId = "file-222";
        String filename = "update-test.txt";
        FileMetadata metadata = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        entityManager.persist(metadata);
        entityManager.flush();

        // Act - Update worker information
        metadata.setWorkerId("worker-2");
        metadata.setWorkerAddress("localhost:9091");
        repository.save(metadata);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<FileMetadata> updated = repository.findByFileIdAndChunkId(fileId, 0);
        assertTrue(updated.isPresent());
        assertEquals("worker-2", updated.get().getWorkerId());
        assertEquals("localhost:9091", updated.get().getWorkerAddress());
    }

    /**
     * Tests delete operations.
     * Verifies that metadata can be deleted from the repository.
     */
    @Test
    void testDelete() {
        // Arrange
        String fileId = "file-333";
        String filename = "delete-test.txt";
        FileMetadata metadata = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        entityManager.persist(metadata);
        entityManager.flush();

        // Verify it exists
        Optional<FileMetadata> beforeDelete = repository.findByFileIdAndChunkId(fileId, 0);
        assertTrue(beforeDelete.isPresent());

        // Act
        repository.delete(metadata);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<FileMetadata> afterDelete = repository.findByFileIdAndChunkId(fileId, 0);
        assertFalse(afterDelete.isPresent());
    }

    /**
     * Tests deletion of multiple chunks for the same file.
     * Verifies that all chunks can be deleted independently.
     */
    @Test
    void testDeleteMultipleChunks() {
        // Arrange
        String fileId = "file-444";
        String filename = "multi-delete.txt";
        
        FileMetadata chunk0 = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        FileMetadata chunk1 = TestDataBuilder.createFileMetadata(fileId, filename, 1);
        FileMetadata chunk2 = TestDataBuilder.createFileMetadata(fileId, filename, 2);
        
        entityManager.persist(chunk0);
        entityManager.persist(chunk1);
        entityManager.persist(chunk2);
        entityManager.flush();

        // Verify all exist
        List<FileMetadata> beforeDelete = repository.findAllByFileId(fileId);
        assertEquals(3, beforeDelete.size());

        // Act - Delete all chunks
        repository.deleteAll(beforeDelete);
        entityManager.flush();
        entityManager.clear();

        // Assert
        List<FileMetadata> afterDelete = repository.findAllByFileId(fileId);
        assertTrue(afterDelete.isEmpty());
    }

    /**
     * Tests composite primary key uniqueness.
     * Verifies that same fileId with different chunkIds can coexist.
     */
    @Test
    void testCompositePrimaryKey_Uniqueness() {
        // Arrange
        String fileId = "file-555";
        String filename = "composite-test.txt";
        
        FileMetadata chunk0 = TestDataBuilder.createFileMetadata(fileId, filename, 0);
        FileMetadata chunk1 = TestDataBuilder.createFileMetadata(fileId, filename, 1);
        
        // Act
        repository.save(chunk0);
        repository.save(chunk1);
        entityManager.flush();

        // Assert
        List<FileMetadata> results = repository.findAllByFileId(fileId);
        assertEquals(2, results.size());
        
        // Verify both chunks exist independently
        Optional<FileMetadata> retrievedChunk0 = repository.findByFileIdAndChunkId(fileId, 0);
        Optional<FileMetadata> retrievedChunk1 = repository.findByFileIdAndChunkId(fileId, 1);
        
        assertTrue(retrievedChunk0.isPresent());
        assertTrue(retrievedChunk1.isPresent());
    }
}
