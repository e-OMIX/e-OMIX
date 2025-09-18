package com.example.eomix.exception;

/**
 * The type Helper exception.
 */
public class HelperException extends RuntimeException {
    /**
     * Instantiates a new Helper exception.
     *
     * @param message the message
     * @param cause   the cause
     * @implSpec This constructor initializes the exception with a message.
     * * This message should provide context about the failure, such as the specific operation that failed
     * or the expected outcome that was not met.
     */
    public HelperException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new Helper exception.
     *
     * @param message the message
     * @implSpec This constructor initializes the exception with a message.
     * * This message should provide context about the failure, such as the specific operation that failed
     * or the expected outcome that was not met.
     */
    public HelperException(String message) {
        super(message);
    }
}
