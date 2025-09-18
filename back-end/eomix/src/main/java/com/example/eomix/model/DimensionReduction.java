package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * The enum Dimension reduction.
 */
@Getter
public enum DimensionReduction {
    /**
     * Umap dimension reduction.
     */
    UMAP("umap"),
    /**
     * Tsne dimension reduction.
     */
    TSNE("tsne");

    private final String value;

    DimensionReduction(String s) {
        this.value = s;
    }

    /**
     * Dimension reduction from value dimension reduction.
     * * This method is used to convert a string value to a DimensionReduction enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding DimensionReduction.
     *
     * @param value the value
     * @return the dimension reduction
     * @implNote If the value is null, it returns null.
     * * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    public static DimensionReduction dimensionReductionFromValue(String value) {
        for (DimensionReduction dimensionReduction : DimensionReduction.values()) {
            if (dimensionReduction.value.equalsIgnoreCase(value)) {
                return dimensionReduction;
            }
        }
        throw new IllegalArgumentException("Invalid dimensionReduction value: " + value);
    }

    /**
     * Returns the string representation of the dimension reduction value.
     *
     * @return the string representation of the dimension reduction value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
