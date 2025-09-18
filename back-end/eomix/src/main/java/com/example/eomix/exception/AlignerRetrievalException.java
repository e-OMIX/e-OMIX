package com.example.eomix.exception;

/**
 * The type Aligner retrieval exception.
 */
public class AlignerRetrievalException extends RuntimeException {


    /**
     * Instantiates a new Aligner retrieval exception.
     * * This constructor is used to create an exception with a specific message.
     * * This message should provide context about the failure,
     * such as the specific operation that failed or the expected outcome that was not met.
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is unable to retrieve aligner data due to various reasons, such as network issues, data not found, or unexpected data formats.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public AlignerRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

}
