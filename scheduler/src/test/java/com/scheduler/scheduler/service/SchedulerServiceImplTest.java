package com.scheduler.scheduler.service;

import com.scheduler.scheduler.model.FileMetadata;
import com.scheduler.scheduler.repository.FileMetadataRepository;
import io.datavault.common.grpc.AssignWorkerRequest;
import io.datavault.common.grpc.AssignWorkerResponse;
import io.datavault.common.grpc.HeartbeatRequest;
import io.datavault.common.grpc.HeartbeatResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SchedulerServiceImpl gRPC service.
 * Tests worker registration, heartbeat handling, and worker assignment logic.
 * Uses mocked FileMetadataRepository and gRPC StreamObserver.
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceImplTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private StreamObserver<HeartbeatResponse> heartbeatResponseObserver;

    @Mock
    private StreamObserver<AssignWorkerResponse> assignWorkerResponseObserver;

    @InjectMocks
    private SchedulerServiceImpl schedulerService;

    @BeforeEach
    void setUp() {
        // Reset the service state before each test
        schedulerService = new SchedulerServiceImpl();
        schedulerService.fileMetadataRepository = fileMetadataRepository;
    }

    @AfterEach
    void cleanup() {
        // Reset all mocks to ensure clean state for next test
        reset(fileMetadataRepository, heartbeatResponseObserver, assignWorkerResponseObserver);
    }

    /**
     * Tests new worker registration via heartbeat.
     * Verifies that a new worker is added to the worker pool.
     */
    @Test
    void testSendHeartbeat_NewWorker() {
        // Arrange
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setWorkerId("worker-1")
                .setAddress("localhost:9090")
                .build();

        // Act
        schedulerService.sendHeartbeat(request, heartbeatResponseObserver);

        // Assert
        ArgumentCaptor<HeartbeatResponse> responseCaptor = ArgumentCaptor.forClass(HeartbeatResponse.class);
        verify(heartbeatResponseObserver).onNext(responseCaptor.capture());
        verify(heartbeatResponseObserver).onCompleted();

        HeartbeatResponse response = responseCaptor.getValue();
        assertTrue(response.getAcknowledged());
        assertTrue(response.getMessage().contains("worker-1"));

        // Verify worker is in active workers
        Map<String, String> activeWorkers = schedulerService.getActiveWorkers();
        assertEquals(1, activeWorkers.size());
        assertEquals("localhost:9090", activeWorkers.get("worker-1"));
    }

    /**
     * Tests existing worker heartbeat update.
     * Verifies that heartbeat updates the last heartbeat timestamp for existing workers.
     */
    @Test
    void testSendHeartbeat_ExistingWorker() {
        // Arrange - Register worker first
        HeartbeatRequest initialRequest = HeartbeatRequest.newBuilder()
                .setWorkerId("worker-1")
                .setAddress("localhost:9090")
                .build();
        schedulerService.sendHeartbeat(initialRequest, heartbeatResponseObserver);

        // Act - Send another heartbeat
        HeartbeatRequest updateRequest = HeartbeatRequest.newBuilder()
                .setWorkerId("worker-1")
                .setAddress("localhost:9090")
                .build();
        schedulerService.sendHeartbeat(updateRequest, heartbeatResponseObserver);

        // Assert
        verify(heartbeatResponseObserver, times(2)).onNext(any(HeartbeatResponse.class));
        verify(heartbeatResponseObserver, times(2)).onCompleted();

        // Verify worker is still active
        Map<String, String> activeWorkers = schedulerService.getActiveWorkers();
        assertEquals(1, activeWorkers.size());
        assertEquals("localhost:9090", activeWorkers.get("worker-1"));
    }

    /**
     * Tests round-robin worker assignment algorithm.
     * Verifies that workers are assigned in round-robin fashion.
     */
    @Test
    void testAssignWorkerForChunk_RoundRobin() {
        // Arrange - Register multiple workers
        registerWorker("worker-1", "localhost:9090");
        registerWorker("worker-2", "localhost:9091");
        registerWorker("worker-3", "localhost:9092");

        when(fileMetadataRepository.findByFileIdAndChunkId(anyString(), anyInt()))
                .thenReturn(Optional.empty());

        // Act - Assign workers for 3 chunks
        String fileId = "file-123";
        String[] assignedWorkers = new String[3];

        for (int i = 0; i < 3; i++) {
            AssignWorkerRequest request = AssignWorkerRequest.newBuilder()
                    .setFileId(fileId)
                    .setChunkId(i)
                    .build();

            StreamObserver<AssignWorkerResponse> observer = mock(StreamObserver.class);
            schedulerService.assignWorkerForChunk(request, observer);

            ArgumentCaptor<AssignWorkerResponse> responseCaptor = ArgumentCaptor.forClass(AssignWorkerResponse.class);
            verify(observer).onNext(responseCaptor.capture());
            assignedWorkers[i] = responseCaptor.getValue().getAssignedWorkerId();
        }

        // Assert - Verify round-robin assignment (workers are assigned in round-robin order)
        // The order depends on the internal counter state, so we verify all workers are used
        assertNotNull(assignedWorkers[0]);
        assertNotNull(assignedWorkers[1]);
        assertNotNull(assignedWorkers[2]);
        
        // Verify all three workers were assigned (each worker appears once)
        assertTrue(assignedWorkers[0].startsWith("worker-"));
        assertTrue(assignedWorkers[1].startsWith("worker-"));
        assertTrue(assignedWorkers[2].startsWith("worker-"));
        
        // Verify they are different workers
        assertNotEquals(assignedWorkers[0], assignedWorkers[1]);
        assertNotEquals(assignedWorkers[1], assignedWorkers[2]);
        assertNotEquals(assignedWorkers[0], assignedWorkers[2]);
    }

    /**
     * Tests error when no active workers are available.
     * Verifies that UNAVAILABLE error is returned when worker pool is empty.
     */
    @Test
    void testAssignWorkerForChunk_NoActiveWorkers() {
        // Arrange
        AssignWorkerRequest request = AssignWorkerRequest.newBuilder()
                .setFileId("file-123")
                .setChunkId(0)
                .build();

        // Act
        schedulerService.assignWorkerForChunk(request, assignWorkerResponseObserver);

        // Assert
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(assignWorkerResponseObserver).onError(errorCaptor.capture());
        verify(assignWorkerResponseObserver, never()).onNext(any());
        verify(assignWorkerResponseObserver, never()).onCompleted();

        Throwable error = errorCaptor.getValue();
        assertTrue(error instanceof StatusRuntimeException);
        StatusRuntimeException statusException = (StatusRuntimeException) error;
        assertEquals(Status.Code.UNAVAILABLE, statusException.getStatus().getCode());
        assertTrue(statusException.getStatus().getDescription().contains("No active workers"));
    }

    /**
     * Tests duplicate chunk assignment handling.
     * Verifies that ALREADY_EXISTS error is returned when chunk is already assigned.
     */
    @Test
    void testAssignWorkerForChunk_AlreadyExists() {
        // Arrange
        registerWorker("worker-1", "localhost:9090");

        FileMetadata existingMetadata = new FileMetadata();
        existingMetadata.setFileId("file-123");
        existingMetadata.setChunkId(0);
        existingMetadata.setWorkerId("worker-1");
        existingMetadata.setWorkerAddress("localhost:9090");

        when(fileMetadataRepository.findByFileIdAndChunkId("file-123", 0))
                .thenReturn(Optional.of(existingMetadata));
        when(fileMetadataRepository.save(any(FileMetadata.class)))
                .thenReturn(existingMetadata);

        AssignWorkerRequest request = AssignWorkerRequest.newBuilder()
                .setFileId("file-123")
                .setChunkId(0)
                .build();

        // Act
        schedulerService.assignWorkerForChunk(request, assignWorkerResponseObserver);

        // Assert
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(assignWorkerResponseObserver).onError(errorCaptor.capture());
        verify(assignWorkerResponseObserver, never()).onNext(any());
        verify(assignWorkerResponseObserver, never()).onCompleted();

        Throwable error = errorCaptor.getValue();
        assertTrue(error instanceof StatusRuntimeException);
        StatusRuntimeException statusException = (StatusRuntimeException) error;
        assertEquals(Status.Code.ALREADY_EXISTS, statusException.getStatus().getCode());
        assertTrue(statusException.getStatus().getDescription().contains("Worker already assigned"));
    }

    /**
     * Tests inactive worker cleanup.
     * Verifies that workers with expired heartbeats are removed from the pool.
     */
    @Test
    void testCleanInactiveWorkers() throws InterruptedException {
        // Arrange - Register a worker
        registerWorker("worker-1", "localhost:9090");
        
        // Verify worker is active
        Map<String, String> activeWorkersBefore = schedulerService.getActiveWorkers();
        assertEquals(1, activeWorkersBefore.size());

        // Wait for timeout (5 seconds + buffer)
        Thread.sleep(6000);

        // Act
        schedulerService.cleanInactiveWorkers();

        // Assert - Worker should be removed
        Map<String, String> activeWorkersAfter = schedulerService.getActiveWorkers();
        assertEquals(0, activeWorkersAfter.size());
    }

    /**
     * Tests active worker filtering.
     * Verifies that only workers with recent heartbeats are returned as active.
     */
    @Test
    void testGetActiveWorkers() {
        // Arrange - Register multiple workers
        registerWorker("worker-1", "localhost:9090");
        registerWorker("worker-2", "localhost:9091");
        registerWorker("worker-3", "localhost:9092");

        // Act
        Map<String, String> activeWorkers = schedulerService.getActiveWorkers();

        // Assert
        assertEquals(3, activeWorkers.size());
        assertEquals("localhost:9090", activeWorkers.get("worker-1"));
        assertEquals("localhost:9091", activeWorkers.get("worker-2"));
        assertEquals("localhost:9092", activeWorkers.get("worker-3"));
    }

    /**
     * Helper method to register a worker via heartbeat.
     */
    private void registerWorker(String workerId, String address) {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setWorkerId(workerId)
                .setAddress(address)
                .build();
        StreamObserver<HeartbeatResponse> observer = mock(StreamObserver.class);
        schedulerService.sendHeartbeat(request, observer);
    }
}
