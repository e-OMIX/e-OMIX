package com.example.eomix.controller;

import com.example.eomix.utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Error handler.
 */
public class  ErrorHandler {

    /**
     * The constant POST_PROCESSING.
     */
    public static final String POST_PROCESSING = "PostProcessing";
    /**
     * The constant ALIGNMENT.
     */
    public static final String ALIGNMENT = "Alignment";

    private ErrorHandler() {
    }

    /**
     * Verify post-processing json is not missing any parameters and returns a response entity.
     * * Required parameters are:
     * * - minGenesByCells
     * * - minCellsExpressingGene
     * * - numHighVariableGenes
     * * - clustering
     * * - dimensionReduction
     *
     * @param jsonNode the json node
     * @return the response entity
     * @implNote This method checks if the required parameters are present in the JSON node.
     */
    protected static @Nullable ResponseEntity<String> verifyPostProcessingJson(JsonNode jsonNode) {
        List<String> errors = new ArrayList<>();
        if (jsonNode.get("minGenesByCells").asText().equals("null")) {
            errors.add("Minimum Genes By Cells is empty");
        }
        if (jsonNode.get("minCellsExpressingGene").asText().equals("null")) {
            errors.add("Minimum Cells Expressing a Gene is empty");
        }
        if (jsonNode.get("numHighVariableGenes").asText().equals("null")) {
            errors.add("Number of High Variable Genes is empty");
        }
        if (jsonNode.get("clustering").isEmpty()) {
            errors.add("Clustering is empty.");
        }
        if (jsonNode.get("dimensionReduction").isEmpty()) {
            errors.add("Dimension Reduction is empty");
        }
        // Return all errors if any
        if (!errors.isEmpty()) {
            String errorMessage = "Error : " + String.join(", ", errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }
        return null;
    }

    /**
     * Verify alignment json is not missing any parameters and returns a response entity.
     * * Required parameters are:
     * * - selectedOrganism
     * * - selectedProtocol
     * * - annotation
     * * - genome
     * * - selectedAligner
     * * - samples
     *
     * @param jsonNode the json node
     * @return the response entity
     */
    protected static @Nullable ResponseEntity<String> verifyAlignmentJson(JsonNode jsonNode) {
        if (jsonNode.get("selectedOrganism").asText().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Organism is empty");
        } else if (jsonNode.get("selectedProtocol").asText().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Protocol is empty");
        } else if (jsonNode.get("annotation").asText().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Annotation is empty");
        } else if (jsonNode.get("genome").asText().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Genome is empty");
        } else if (jsonNode.get("selectedAligner").asText().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Aligner is empty");
        } else if (jsonNode.get("samples").isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Samples is empty");
        }
        return null;
    }


    /**
     * Verify errors in json and verify if the required parameters are present and returns a response entity.
     * Required parameters are:
     * * - cellularResolution
     * * - omicsModality
     *
     * @param jsonData       the json data
     * @param experimentType the experiment type
     * @return the response entity
     * @implNote This method checks if the JSON data is valid and contains the required parameters.
     * * If the JSON data is null or empty, it returns a bad request response with an error message.
     * * If the JSON data is valid, it checks for the presence of required parameters based on the experiment type.
     * * If the required parameters are missing, it returns a bad request response with an error message.
     * @implSpec If the experiment type is "Alignment", it calls the verifyAlignmentJson method to check for alignment-specific parameters.
     * * If the experiment type is "PostProcessing", it calls the verifyPostProcessingJson method to check for post-processing-specific parameters.
     * * If the JSON data contains an "error" field, it returns a bad request response with the error message.
     * * If all checks pass, it returns an OK response with a message indicating that the JSON is valid.
     * @implNote This method is used to validate the JSON data before processing it further.
     */
    protected static ResponseEntity<String> verifyErrorsInJSON(String jsonData, String experimentType) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonData == null || jsonData.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error in JSON");
        }
        try {

            JsonNode jsonNode = objectMapper.readTree(jsonData);
            if (jsonNode.get("cellularResolution").asText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("CellularResolution is empty");
            } else if (jsonNode.get("omicsModality").asText().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OmicsModality is empty");
            }
            if (Objects.equals(experimentType, ALIGNMENT)) {
                ResponseEntity<String> badRequest = verifyAlignmentJson(jsonNode);
                if (badRequest != null) return badRequest;

            } else if (POST_PROCESSING.equals(experimentType)) {
                ResponseEntity<String> badRequest = verifyPostProcessingJson(jsonNode);
                if (badRequest != null) return badRequest;
            }
            if (jsonNode.has("error")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(jsonNode.get("error").asText());
            }
            return ResponseEntity.ok("JSON is valid");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


    /**
     * Handle missing data in metadata file and returns a response entity.
     * Required parameters are:
     * * - cellularResolution
     * * - sequenceType
     * * - standardizedSpecies
     * * - sampleId
     * * - organ
     * * - disorder
     * * This method checks if the headers are present in the metadata file.
     * * @throws IllegalArgumentException if the headers are empty or missing required parameters
     * * This method returns a ResponseEntity with a bad request status if any required headers are missing.
     * * If all required headers are present, it returns null.
     *
     * @param headers the headers
     * @return the response entity
     * @implNote This method is used to validate the headers of a metadata file before processing it.
     * @implSpec It checks if the headers array is empty and if any required headers are missing.
     * @implNote If the headers are empty, it returns a bad request response with an error message.
     * * * If any required headers are missing, it returns a bad request response with an error message listing the missing headers.
     */
    public static @Nullable ResponseEntity<String> handleMissingDataInMetadataFile(String[] headers) {
        if (headers.length == 0) {
            return ResponseEntity.badRequest().body("File headers are empty");
        }
        List<String> requiredHeaders = Arrays.asList(
                Constants.CELLULAR_RESOLUTION,
                Constants.SEQUENCE_TYPE,
                Constants.STANDARDIZED_SPECIES,
                Constants.SAMPLE_ID,
                Constants.ORGAN,
                Constants.DISORDER
        );

        List<String> headerList = Arrays.asList(headers);
        List<String> missingHeaders = requiredHeaders.stream()
                .filter(header -> !headerList.contains(header))
                .toList();

        if (!missingHeaders.isEmpty()) {
            String errorMessage = "File is missing required parameter(s) :" +
                    String.join(", ", missingHeaders);
            return ResponseEntity.badRequest().body(errorMessage);
        }
        return null;
    }
}
