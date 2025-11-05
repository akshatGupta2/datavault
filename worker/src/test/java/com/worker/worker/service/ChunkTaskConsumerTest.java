package com.worker.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worker.worker.model.ChunkTask;
import com.worker.worker.util.TestDataBuilder;
import io.datavault.common.grpc.AssignWorkerRequest;
import io.datavault.common.grpc.AssignWorkerResponse;
import io.datavault.common.grpc.SchedulerServiceGrpc;
import io.datavault.common.grpc.StoreChunkRequest;
import io.datavault.common.grpc.StoreChunkResponse;
import io.datavault.common.grpc.WorkerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChunkTaskConsumer.
 * Tests RabbitMQ message consumption and chunk storage/forwarding logic.
 * Uses mocked dependencies to test the consumer in isolation.
 */
@ExtendWith(MockitoExtension.class)
class ChunkTaskConsumerTest {

    @Mock
    private SchedulerServiceGrpc.SchedulerServiceBlockingStub schedulerStub;

    private ChunkTaskConsumer chunkTaskConsumer;
    private ObjectMapper objectMapper;

    @TempDir
    File tempDir;

    private static final String TEST_WORKER_ID = "test-worker-1";
    private static final String TEST_FILE_ID = "test-file-123";
    private static final int TEST_CHUNK_ID = 1;
    private static final byte[] TEST_DATA = "Test chunk data content".getBytes();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Set environment variable for current worker ID
        System.setProperty("WORKER_ID", TEST_WORKER_ID);
        
        chunkTaskConsumer = new ChunkTaskConsumer(schedulerStub);
    }

    @AfterEach
    void tearDown() {
        // Clean up system property
        System.clearProperty("WORKER_ID");
        
        // Clean up any test files
        TestDataBuilder.cleanupDirectoryQuietly(new File("app/storage/" + TEST_WORKER_ID));
    }

    /**
     * Tests local chunk storage when the current worker is assigned to handle the chunk.
     * Verifies that the chunk is stored locally and not forwarded to another worker.
     */
    @Test
    void testHandleTask_LocalStorage_WhenWorkerIsAssigned() throws Exception {
        // Arrange
        ChunkTask task = TestDataBuilder.createChunkTask(TEST_FILE_ID, TEST_CHUNK_ID, TEST_DATA);
        String message = objectMapper.writeValueAsString(task);

        AssignWorkerResponse response = AssignWorkerResponse.newBuilder()
                .setAssignedWorkerId(TEST_WORKER_ID)
                .setAssignedWorkerAddress("localhost:9091")
                .build();

        when(schedulerStub.assignWorkerForChunk(any(AssignWorkerRequest.class)))
                .thenReturn(response);

        // Act
        chunkTaskConsumer.handleTask(message);

        // Assert
        verify(schedulerStub, times(1)).assignWorkerForChunk(any(AssignWorkerRequest.class));
        
        // Verify file was created locally
        File expectedFile = new File("app/storage/" + TEST_WORKER_ID + "/chunk_" + TEST_CHUNK_ID + ".chunk");
        assertTrue(expectedFile.exists(), "Chunk file should be created locally");
        
        byte[] storedData = Files.readAllBytes(expectedFile.toPath());
        assertArrayEquals(TEST_DATA, storedData, "Stored data should match original data");
    }

    /**
     * Tests chunk forwarding to a remote worker when a different worker is assigned.
     * Verifies that the chunk is not stored locally but forwarded via gRPC.
     */
    @Test
    void testHandleTask_ForwardToRemote_WhenDifferentWorkerAssigned() throws Exception {
        // Arrange
        String remoteWorkerId = "remote-worker-2";
        String remoteWorkerAddress = "localhost:9092";
        
        ChunkTask task = TestDataBuilder.createChunkTask(TEST_FILE_ID, TEST_CHUNK_ID, TEST_DATA);
        String message = objectMapper.writeValueAsString(task);

        AssignWorkerResponse response = AssignWorkerResponse.newBuilder()
                .setAssignedWorkerId(remoteWorkerId)
                .setAssignedWorkerAddress(remoteWorkerAddress)
                .build();

        when(schedulerStub.assignWorkerForChunk(any(AssignWorkerRequest.class)))
                .thenReturn(response);

        // Act
        chunkTaskConsumer.handleTask(message);

        // Assert
        verify(schedulerStub, times(1)).assignWorkerForChunk(any(AssignWorkerRequest.class));
        
        // Verify file was NOT created locally
        File localFile = new File("app/storage/" + TEST_WORKER_ID + "/chunk_" + TEST_CHUNK_ID + ".chunk");
        assertFalse(localFile.exists(), "Chunk file should not be created locally when forwarding");
        
        // Note: Actual gRPC forwarding is tested separately as it creates a new channel
    }

    /**
     * Tests handling of invalid JSON message.
     * Verifies that the consumer handles malformed messages gracefully without crashing.
     */
    @Test
    void testHandleTask_InvalidMessage_HandlesGracefully() {
        // Arrange
        String invalidMessage = "{ invalid json }";

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> chunkTaskConsumer.handleTask(invalidMessage));
        
        // Verify scheduler was not called
        verify(schedulerStub, never()).assignWorkerForChunk(any(AssignWorkerRequest.class));
    }

    /**
     * Tests local file writing with directory creation.
     * Verifies that the storage directory is created if it doesn't exist.
     */
    @Test
    void testStoreChunkLocally_CreatesDirectory_WhenNotExists() throws IOException {
        // Arrange
        String workerId = "new-worker-123";
        File storageDir = new File("app/storage/" + workerId);
        
        // Ensure directory doesn't exist
        if (storageDir.exists()) {
            TestDataBuilder.cleanupDirectory(storageDir);
        }
        assertFalse(storageDir.exists(), "Storage directory should not exist initially");

        // Act
        chunkTaskConsumer.storeChunkLocally(TEST_FILE_ID, TEST_CHUNK_ID, TEST_DATA, workerId);

        // Assert
        assertTrue(storageDir.exists(), "Storage directory should be created");
        assertTrue(storageDir.isDirectory(), "Storage path should be a directory");
        
        File chunkFile = new File(storageDir, "chunk_" + TEST_CHUNK_ID + ".chunk");
        assertTrue(chunkFile.exists(), "Chunk file should be created");
        
        byte[] storedData = Files.readAllBytes(chunkFile.toPath());
        assertArrayEquals(TEST_DATA, storedData, "Stored data should match original data");
        
        // Cleanup
        TestDataBuilder.cleanupDirectory(storageDir);
    }

    /**
     * Tests successful local chunk storage.
     * Verifies that chunk data is correctly written to the file system.
     */
    @Test
    void testStoreChunkLocally_Success_WritesDataCorrectly() throws IOException {
        // Arrange
        String workerId = "test-worker-456";
        byte[] chunkData = "Sample chunk content for testing".getBytes();

        // Act
        chunkTaskConsumer.storeChunkLocally(TEST_FILE_ID, TEST_CHUNK_ID, chunkData, workerId);

        // Assert
        File chunkFile = new File("app/storage/" + workerId + "/chunk_" + TEST_CHUNK_ID + ".chunk");
        assertTrue(chunkFile.exists(), "Chunk file should exist");
        
        byte[] storedData = Files.readAllBytes(chunkFile.toPath());
        assertArrayEquals(chunkData, storedData, "Stored data should match input data");
        
        // Cleanup
        TestDataBuilder.cleanupDirectory(new File("app/storage/" + workerId));
    }

    /**
     * Tests that AssignWorkerRequest is created with correct parameters.
     * Verifies the request sent to the scheduler contains the right worker ID, file ID, and chunk ID.
     */
    @Test
    void testHandleTask_CreatesCorrectAssignWorkerRequest() throws Exception {
        // Arrange
        ChunkTask task = TestDataBuilder.createChunkTask(TEST_FILE_ID, TEST_CHUNK_ID, TEST_DATA);
        String message = objectMapper.writeValueAsString(task);

        AssignWorkerResponse response = AssignWorkerResponse.newBuilder()
                .setAssignedWorkerId(TEST_WORKER_ID)
                .setAssignedWorkerAddress("localhost:9091")
                .build();

        ArgumentCaptor<AssignWorkerRequest> requestCaptor = ArgumentCaptor.forClass(AssignWorkerRequest.class);
        when(schedulerStub.assignWorkerForChunk(requestCaptor.capture()))
                .thenReturn(response);

        // Act
        chunkTaskConsumer.handleTask(message);

        // Assert
        AssignWorkerRequest capturedRequest = requestCaptor.getValue();
        assertEquals(TEST_WORKER_ID, capturedRequest.getRequesterWorkerId(), 
                "Request should contain correct worker ID");
        assertEquals(TEST_FILE_ID, capturedRequest.getFileId(), 
                "Request should contain correct file ID");
        assertEquals(TEST_CHUNK_ID, capturedRequest.getChunkId(), 
                "Request should contain correct chunk ID");
    }

    /**
     * Tests handling of empty chunk data.
     * Verifies that empty data is handled correctly without errors.
     */
    @Test
    void testStoreChunkLocally_EmptyData_HandlesCorrectly() throws IOException {
        // Arrange
        byte[] emptyData = new byte[0];
        String workerId = "test-worker-empty";

        // Act
        chunkTaskConsumer.storeChunkLocally(TEST_FILE_ID, TEST_CHUNK_ID, emptyData, workerId);

        // Assert
        File chunkFile = new File("app/storage/" + workerId + "/chunk_" + TEST_CHUNK_ID + ".chunk");
        assertTrue(chunkFile.exists(), "Chunk file should exist even with empty data");
        assertEquals(0, chunkFile.length(), "File should be empty");
        
        // Cleanup
        TestDataBuilder.cleanupDirectory(new File("app/storage/" + workerId));
    }

    /**
     * Tests Base64 decoding of chunk data from the message.
     * Verifies that encoded data is correctly decoded before storage.
     */
    @Test
    void testHandleTask_DecodesBase64Data_Correctly() throws Exception {
        // Arrange
        byte[] originalData = "Original binary data".getBytes();
        String encodedData = Base64.getEncoder().encodeToString(originalData);
        ChunkTask task = TestDataBuilder.createChunkTask(TEST_FILE_ID, TEST_CHUNK_ID, encodedData);
        String message = objectMapper.writeValueAsString(task);

        AssignWorkerResponse response = AssignWorkerResponse.newBuilder()
                .setAssignedWorkerId(TEST_WORKER_ID)
                .setAssignedWorkerAddress("localhost:9091")
                .build();

        when(schedulerStub.assignWorkerForChunk(any(AssignWorkerRequest.class)))
                .thenReturn(response);

        // Act
        chunkTaskConsumer.handleTask(message);

        // Assert
        File chunkFile = new File("app/storage/" + TEST_WORKER_ID + "/chunk_" + TEST_CHUNK_ID + ".chunk");
        assertTrue(chunkFile.exists(), "Chunk file should be created");
        
        byte[] storedData = Files.readAllBytes(chunkFile.toPath());
        assertArrayEquals(originalData, storedData, "Decoded data should match original data");
    }
}
