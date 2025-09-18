package com.example.eomix.exception;

/**
 * The type Python runner exception.
 */
public class PythonRunnerException extends RuntimeException {
    /**
     * Instantiates a new Python runner exception.
     * * This exception is used to indicate that the application
     * was unable to run a Python script
     * * or encountered an error while executing a Python script.
     * * <p>
     * * This could be due to various reasons such as
     * syntax errors in the script,
     * missing dependencies,
     * or runtime errors during execution.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is trying to execute a Python script but encounters an issue,
     * such as the script failing to run, or an error occurring during execution.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public PythonRunnerException(String message, Throwable cause) {
        super(message, cause);
    }
}
