package com.example.eomix.exception;

/**
 * The type No free port available exception.
 */
public class NoFreePortAvailableException extends RuntimeException {
    /**
     * Instantiates a new No free port available exception.
     * * This exception is used to indicate that the application
     * was unable to find a free port
     * for a service to bind to.
     * * <p>
     * * This could be due to all ports being in use
     * or the system being unable to allocate a port
     * for the service.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is trying to start a service but encounters an issue,
     * such as all ports being occupied or the system being unable to allocate a port.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public NoFreePortAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
