package com.scheduler.scheduler.util;

import com.scheduler.scheduler.model.ChunkTask;
import com.scheduler.scheduler.model.FileMetadata;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Utility class for building test data objects.
 * Provides factory methods for creating FileMetadata, MockMultipartFile, and ChunkTask test objects.
 */
public class TestDataBuilder {

    /**
     * Creates a FileMetadata object with default test values.
     *
     * @param fileId   the unique file identifier
     * @param filename the name of the file
     * @param chunkId  the chunk identifier
     * @return a FileMetadata object populated with test data
     */
    public static FileMetadata createFileMetadata(String fileId, String filename, int chunkId) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setFilename(filename);
        metadata.setChunkId(chunkId);
        metadata.setSize(1024L);
        metadata.setWorkerId("test-worker-1");
        metadata.setWorkerAddress("localhost:9090");
        metadata.setUploadTime(LocalDateTime.now());
        return metadata;
    }

    /**
     * Creates a FileMetadata object with custom size.
     *
     * @param fileId   the unique file identifier
     * @param filename the name of the file
     * @param chunkId  the chunk identifier
     * @param size     the size of the file in bytes
     * @return a FileMetadata object populated with test data
     */
    public static FileMetadata createFileMetadata(String fileId, String filename, int chunkId, long size) {
        FileMetadata metadata = createFileMetadata(fileId, filename, chunkId);
        metadata.setSize(size);
        return metadata;
    }

    /**
     * Creates a FileMetadata object with all custom values.
     *
     * @param fileId        the unique file identifier
     * @param filename      the name of the file
     * @param chunkId       the chunk identifier
     * @param size          the size of the file in bytes
     * @param workerId      the worker identifier
     * @param workerAddress the worker address
     * @return a FileMetadata object populated with test data
     */
    public static FileMetadata createFileMetadata(String fileId, String filename, int chunkId, 
                                                   long size, String workerId, String workerAddress) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setFilename(filename);
        metadata.setChunkId(chunkId);
        metadata.setSize(size);
        metadata.setWorkerId(workerId);
        metadata.setWorkerAddress(workerAddress);
        metadata.setUploadTime(LocalDateTime.now());
        return metadata;
    }

    /**
     * Creates a MockMultipartFile with the specified filename and content.
     *
     * @param filename the name of the file
     * @param content  the file content as byte array
     * @return a MockMultipartFile object for testing file uploads
     */
    public static MockMultipartFile createMockMultipartFile(String filename, byte[] content) {
        return new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                content
        );
    }

    /**
     * Creates a MockMultipartFile with the specified filename, content type, and content.
     *
     * @param filename    the name of the file
     * @param contentType the MIME type of the file
     * @param content     the file content as byte array
     * @return a MockMultipartFile object for testing file uploads
     */
    public static MockMultipartFile createMockMultipartFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                content
        );
    }

    /**
     * Creates a MockMultipartFile with default test content.
     *
     * @param filename the name of the file
     * @return a MockMultipartFile object with default test content
     */
    public static MockMultipartFile createMockMultipartFile(String filename) {
        byte[] defaultContent = "Test file content".getBytes();
        return createMockMultipartFile(filename, defaultContent);
    }

    /**
     * Creates an empty MockMultipartFile for testing empty file scenarios.
     *
     * @param filename the name of the file
     * @return an empty MockMultipartFile object
     */
    public static MockMultipartFile createEmptyMockMultipartFile(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                new byte[0]
        );
    }

    /**
     * Creates a ChunkTask object with the specified parameters.
     *
     * @param fileId   the unique file identifier
     * @param chunkId  the chunk identifier
     * @param data     the chunk data as byte array
     * @return a ChunkTask object with Base64 encoded data
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId, byte[] data) {
        String encodedData = Base64.getEncoder().encodeToString(data);
        return new ChunkTask(fileId, chunkId, encodedData);
    }

    /**
     * Creates a ChunkTask object with pre-encoded data.
     *
     * @param fileId      the unique file identifier
     * @param chunkId     the chunk identifier
     * @param encodedData the Base64 encoded chunk data
     * @return a ChunkTask object
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId, String encodedData) {
        return new ChunkTask(fileId, chunkId, encodedData);
    }

    /**
     * Creates a ChunkTask object with default test data.
     *
     * @param fileId  the unique file identifier
     * @param chunkId the chunk identifier
     * @return a ChunkTask object with default test content
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId) {
        byte[] defaultData = "Test chunk data".getBytes();
        return createChunkTask(fileId, chunkId, defaultData);
    }
}
