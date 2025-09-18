package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The enum Experiment type.
 */
@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum ExperimentType {
    /**
     * Alignment experiment type.
     */
    ALIGNMENT("Alignment"),
    /**
     * Post-processing experiment type.
     */
    POST_PROCESSING("Post-processing");
    private final String value;

    ExperimentType(String value) {
        this.value = value;
    }

    /**
     * From value experiment type
     * * This method is used to convert a string value to an ExperimentType enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding ExperimentType.
     *
     * @param value the value
     * @return the experiment type
     * @implNote If the value is null, it throws an IllegalArgumentException.
     * * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    @JsonCreator
    public static ExperimentType fromValue(String value) {
        String normalizedValue = value.replace("_", "-");
        for (ExperimentType experimentType : ExperimentType.values()) {
            if (experimentType.value.equalsIgnoreCase(normalizedValue)) {
                return experimentType;
            }
        }
        throw new IllegalArgumentException("Invalid experimentType value: " + value);
    }

    /**
     * Returns the string representation of the experiment type value.
     *
     * @return the string representation of the experiment type value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }

}
