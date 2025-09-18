package com.example.eomix.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The enum Protocols.
 */
public enum Protocols {

    /**
     * Dropseq protocols.
     */
    DROPSEQ("dropseq"),
    /**
     * Tenxv 1 protocols.
     */
    TENXV1("10XV1"),
    /**
     * Tenxv 2 protocols.
     */
    TENXV2("10XV2"),
    /**
     * Tenxv 3 protocols.
     */
    TENXV3("10XV3"),
    /**
     * Tenxv 4 protocols.
     */
    TENXV4("10XV4");
    private final String value;

    Protocols(String s) {
        this.value = s;
    }

    /**
     * From value protocols.
     * * This method is used to convert a string value to a Protocols enum.
     * * It checks if the provided value matches any of the enum constants,
     * * and returns the corresponding Protocols.
     *
     * @param value the value
     * @return the protocols
     * @implNote If the value is null or empty, it returns null.
     * * If the value does not match any of the enum constants,
     * * it throws an IllegalArgumentException with a message indicating the invalid value.
     */
    @JsonCreator
    public static Protocols fromValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (Protocols protocol : Protocols.values()) {
            if (protocol.value.equalsIgnoreCase(value)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException("Invalid protocol value: " + value);
    }

    /**
     * Returns the string representation of the protocol value.
     *
     * @return the string representation of the protocol value
     */
    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
