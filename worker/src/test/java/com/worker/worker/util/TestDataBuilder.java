package com.worker.worker.util;

import com.worker.worker.model.ChunkTask;
import com.google.protobuf.ByteString;
import io.datavault.common.grpc.StoreChunkRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;

/**
 * Utility class for building test data objects for worker service tests.
 * Provides factory methods for creating ChunkTask, StoreChunkRequest, and temporary directory utilities.
 */
public class TestDataBuilder {

    /**
     * Creates a ChunkTask object with the specified parameters.
     *
     * @param fileId      the unique file identifier
     * @param chunkId     the chunk identifier
     * @param encodedData the Base64 encoded chunk data
     * @return a ChunkTask object
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId, String encodedData) {
        ChunkTask task = new ChunkTask();
        task.setFileId(fileId);
        task.setChunkId(chunkId);
        task.setEncodedData(encodedData);
        return task;
    }

    /**
     * Creates a ChunkTask object with byte array data that will be Base64 encoded.
     *
     * @param fileId  the unique file identifier
     * @param chunkId the chunk identifier
     * @param data    the chunk data as byte array
     * @return a ChunkTask object with Base64 encoded data
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId, byte[] data) {
        String encodedData = Base64.getEncoder().encodeToString(data);
        return createChunkTask(fileId, chunkId, encodedData);
    }

    /**
     * Creates a ChunkTask object with default test data.
     *
     * @param fileId  the unique file identifier
     * @param chunkId the chunk identifier
     * @return a ChunkTask object with default test content
     */
    public static ChunkTask createChunkTask(String fileId, int chunkId) {
        byte[] defaultData = "Test chunk data for worker".getBytes();
        return createChunkTask(fileId, chunkId, defaultData);
    }

    /**
     * Creates a StoreChunkRequest protobuf message with the specified parameters.
     *
     * @param workerId the worker identifier
     * @param fileId   the unique file identifier
     * @param chunkId  the chunk identifier
     * @param data     the chunk data as byte array
     * @return a StoreChunkRequest protobuf message
     */
    public static StoreChunkRequest createStoreChunkRequest(String workerId, String fileId, 
                                                             int chunkId, byte[] data) {
        return StoreChunkRequest.newBuilder()
                .setWorkerId(workerId)
                .setFileId(fileId)
                .setChunkId(chunkId)
                .setChunkData(ByteString.copyFrom(data))
                .build();
    }

    /**
     * Creates a StoreChunkRequest protobuf message with default test data.
     *
     * @param workerId the worker identifier
     * @param fileId   the unique file identifier
     * @param chunkId  the chunk identifier
     * @return a StoreChunkRequest protobuf message with default test content
     */
    public static StoreChunkRequest createStoreChunkRequest(String workerId, String fileId, int chunkId) {
        byte[] defaultData = "Test chunk data for protobuf".getBytes();
        return createStoreChunkRequest(workerId, fileId, chunkId, defaultData);
    }

    /**
     * Creates a temporary directory for testing file operations.
     *
     * @param prefix the prefix string to be used in generating the directory name
     * @return a File object representing the created temporary directory
     * @throws IOException if the directory cannot be created
     */
    public static File createTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        return tempDir.toFile();
    }

    /**
     * Creates a temporary directory with a default prefix.
     *
     * @return a File object representing the created temporary directory
     * @throws IOException if the directory cannot be created
     */
    public static File createTempDirectory() throws IOException {
        return createTempDirectory("worker-test-");
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Safe to call even if the directory doesn't exist.
     *
     * @param directory the directory to delete
     * @throws IOException if deletion fails
     */
    public static void cleanupDirectory(File directory) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }

        Path dirPath = directory.toPath();
        Files.walk(dirPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Recursively deletes a directory and all its contents.
     * Suppresses IOExceptions for use in cleanup methods where exceptions cannot be thrown.
     *
     * @param directory the directory to delete
     */
    public static void cleanupDirectoryQuietly(File directory) {
        try {
            cleanupDirectory(directory);
        } catch (IOException e) {
            // Suppress exception for cleanup operations
            System.err.println("Warning: Failed to cleanup directory: " + directory);
        }
    }

    /**
     * Creates a test file within the specified directory.
     *
     * @param directory the parent directory
     * @param filename  the name of the file to create
     * @param content   the content to write to the file
     * @return a File object representing the created file
     * @throws IOException if the file cannot be created
     */
    public static File createTestFile(File directory, String filename, byte[] content) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        Path filePath = directory.toPath().resolve(filename);
        Files.write(filePath, content);
        return filePath.toFile();
    }

    /**
     * Creates a test file with default content.
     *
     * @param directory the parent directory
     * @param filename  the name of the file to create
     * @return a File object representing the created file
     * @throws IOException if the file cannot be created
     */
    public static File createTestFile(File directory, String filename) throws IOException {
        byte[] defaultContent = "Test file content".getBytes();
        return createTestFile(directory, filename, defaultContent);
    }
}
