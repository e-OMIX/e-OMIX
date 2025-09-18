package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The enum Experiment status.
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ExperimentStatus {
    /**
     * In progress experiment status.
     */
    IN_PROGRESS("In_progress"),
    /**
     * In evaluation experiment status.
     */
    IN_EVALUATION("In_evaluation"),
    /**
     * Done experiment status.
     */
    DONE("Done"),
    /**
     * Error experiment status.
     */
    ERROR("Error");

    private final String value;

    ExperimentStatus(String value) {
        this.value = value;
    }

    /**
     * From value experiment status.
     * * This method is used to convert a string value to an ExperimentStatus enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding ExperimentStatus.
     *
     * @param value the value
     * @return the experiment status
     * @implNote If the value is null, it throws an IllegalArgumentException.
     * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    @JsonCreator
    public static ExperimentStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("experimentStatus cannot be null");
        }
        for (ExperimentStatus status : ExperimentStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid experimentStatus value: " + value);
    }

    /**
     * Returns the string representation of the experiment status value.
     *
     * @return the string representation of the experiment status value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
