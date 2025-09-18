package com.example.eomix.exception;

/**
 * The type Process interrupted exception.
 */
public class ProcessInterruptedException extends RuntimeException {
    /**
     * Instantiates a new Process interrupted exception.
     * * This exception is used to indicate that a process was interrupted
     * while it was running.
     * * <p>
     * * This could be due to various reasons such as a user interrupting the process,
     * or the system shutting down unexpectedly.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when a process is running and is interrupted,
     * * such as when a user cancels an operation or when the system is shutting down.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public ProcessInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
