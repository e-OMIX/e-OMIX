package com.example.eomix.exception;

/**
 * The type Metadata file upload exception.
 */
public class MetadataFileUploadException extends RuntimeException {
    /**
     * Instantiates a new Metadata file upload exception.
     * * This exception is used to indicate that the application
     * was unable to upload a metadata file.
     * * <p>
     * * This could be due to various reasons such as network issues,
     * server unavailability,
     * or incorrect file format.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is trying to upload a metadata file
     * but encounters an issue,
     * such as the file not being found, or the server returning an error response.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public MetadataFileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
