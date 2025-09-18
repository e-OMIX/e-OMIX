package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The enum Aligner.
 */
public enum Aligner {
    /**
     * Simpleaf aligner.
     */
    SIMPLEAF("simpleaf"),
    /**
     * Cellranger aligner.
     */
    CELLRANGER("cellranger");

    private final String value;

    Aligner(String s) {
        this.value = s;
    }

    /**
     * From value aligner.
     * * This method is used to convert a string value to an Aligner enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding Aligner.
     *
     * @param value the value
     * @return the aligner
     * @implNote If the value is null, it returns null.
     * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    @JsonCreator
    public static Aligner fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Aligner aligner : Aligner.values()) {
            if (aligner.value.equalsIgnoreCase(value)) {
                return aligner;
            }
        }
        throw new IllegalArgumentException("Invalid aligner value: " + value);
    }

    /**
     * Returns the string representation of the aligner value.
     *
     * @return the string representation of the aligner value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
