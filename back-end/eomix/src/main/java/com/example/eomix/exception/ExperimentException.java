package com.example.eomix.exception;

/**
 * The type Experiment exception.
 */
public class ExperimentException extends RuntimeException {
    /**
     * Instantiates a new Experiment exception.
     * This constructor is used to create an exception with a specific message.
     * This message should provide context about the failure,
     * such as the specific operation that failed or the expected outcome that was not met.
     *
     * @param message the message
     * @implNote This exception is typically thrown when the application encounters an issue related to experiments,
     * such as invalid experiment configurations, failed experiment executions, or other experiment-related errors.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public ExperimentException(String message) {
        super(message);
    }
}
