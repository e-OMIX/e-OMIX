package com.example.eomix.exception;

/**
 * The type Fhir resource fetch exception.
 */
public class FhirResourceFetchException extends RuntimeException {
    /**
     * Instantiates a new Fhir resource fetch exception.
     * * This exception is used to indicate that the application
     * was unable to fetch a resource from a FHIR server.
     * * <p>
     * * This could be due to various reasons such as network issues,
     * server unavailability,
     * or incorrect resource identifiers.
     * * </p>
     *
     * @param message the message
     * @param cause   the cause
     * @implNote This exception is typically thrown when the application is trying to access a FHIR resource
     * but encounters an issue, such as the resource not being found, or the server returning an error response.
     * @implSpec The message should be clear and concise, allowing developers to quickly understand the issue.
     */
    public FhirResourceFetchException(String message, Throwable cause) {

        super(message, cause);
    }
}
