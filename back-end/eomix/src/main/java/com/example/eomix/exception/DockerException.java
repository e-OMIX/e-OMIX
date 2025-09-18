package com.example.eomix.exception;

/**
 * The type Docker exception.
 */
public class DockerException extends RuntimeException {


    /**
     * Exception thrown when Docker port mapping cannot be determined.
     * This exception is used to indicate that the application
     * was unable to retrieve or determine the port mapping
     * for a Docker container.
     * * @param message the detail message explaining the reason for the exception
     * * This message should provide context about the failure,
     * such as the specific container or operation that failed.
     *
     * @param message the message
     * @implNote This exception is typically thrown when the application
     * is trying to access a Docker container's port mapping
     * but encounters an issue,
     * such as the container not being found,
     * or the port mapping not being set up correctly.
     * @implSpec The message should be clear and concise,
     * allowing developers to quickly understand the issue.
     * * Example usage:
     * * throw new DockerException("Unable to determine port mapping for container 'my-container'");
     */
    public DockerException(String message) {
        super(message);
    }
}
