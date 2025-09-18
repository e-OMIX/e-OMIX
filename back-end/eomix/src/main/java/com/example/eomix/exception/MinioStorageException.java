package com.example.eomix.exception;

/**
 * The type Minio storage exception.
 */
public class MinioStorageException extends RuntimeException {
    /**
     * Instantiates a new Minio storage exception.
     * * This exception is used to indicate that the application
     * was unable to perform an operation on Minio storage.
     * * <p>
     * * This could be due to various reasons such as network issues,
     * server unavailability,
     * or incorrect configuration.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is trying to access Minio storage
     * but encounters an issue, such as the storage not being available, or the operation failing due to permissions
     * or other errors.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public MinioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
