package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The enum Clustering.
 */
@Getter
public enum Clustering {
    /**
     * Louvain clustering.
     */
    LOUVAIN("louvain"),
    /**
     * Leiden clustering.
     */
    LEIDEN("leiden");

    private final String value;

    Clustering(String s) {
        this.value = s;
    }

    /**
     * From value clustering.
     * * This method is used to convert a string value to a Clustering enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding Clustering.
     *
     * @param value the value
     * @return the clustering
     * @implNote If the value is null, it returns null.
     * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    public static Clustering fromValue(String value) {
        for (Clustering clustering : Clustering.values()) {
            if (clustering.value.equalsIgnoreCase(value)) {
                return clustering;
            }
        }
        throw new IllegalArgumentException("Invalid clustering value: " + value);
    }

    /**
     * Returns the string representation of the clustering value.
     *
     * @return the string representation of the clustering value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
