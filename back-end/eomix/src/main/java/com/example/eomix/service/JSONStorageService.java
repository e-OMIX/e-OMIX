package com.example.eomix.service;

import com.example.eomix.entities.ExperimentFileEntity;
import com.example.eomix.model.*;
import com.example.eomix.repositories.ExperimentFileRepository;
import com.example.eomix.utils.Helper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.CouchDbConnector;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The type Json storage service.
 */
@Service
public class JSONStorageService {
    private static final Logger logger = LoggerFactory.getLogger(JSONStorageService.class);
    /**
     * The Experiment file repository.
     * This repository is used to interact with the CouchDB database for storing and retrieving experiment file entities.
     */
    private final ExperimentFileRepository experimentFileRepository;

    /**
     * Instantiates a new Json storage service.
     * * This constructor initializes the JSONStorageService with a CouchDbConnector : experiment.
     * * It creates an instance of ExperimentFileRepository using the provided CouchDbConnector.
     *
     * @param db the db
     */
    @Autowired
    public JSONStorageService(@Qualifier("couchDbConnector2") CouchDbConnector db) {
        this.experimentFileRepository = new ExperimentFileRepository(db);
    }

    /**
     * Sets clustering and dimension reduction in the ExperimentFileEntity based on the provided JSON nodes.
     * This method checks if the JSON nodes are arrays or single values and sets the corresponding fields in the entity.
     *
     * @param clusteringNode       the JSON node for clustering
     * @param experimentFileEntity the ExperimentFileEntity to update
     */
    private static void setClustering(JsonNode clusteringNode, ExperimentFileEntity experimentFileEntity) {
        if (clusteringNode != null) {
            if (clusteringNode.isArray()) {
                List<Clustering> clusteringList = new ArrayList<>();
                for (JsonNode node : clusteringNode) {
                    clusteringList.add(Clustering.fromValue(node.asText()));
                }
                experimentFileEntity.setClustering(clusteringList.toArray(new Clustering[0]));
            } else {
                experimentFileEntity.setClustering(new Clustering[]{Clustering.fromValue(clusteringNode.asText())});
            }
        }
    }

    /**
     * Sets dimension reduction in the ExperimentFileEntity based on the provided JSON node.
     * This method checks if the JSON node is an array or a single value and sets the corresponding field in the entity.
     *
     * @param clusteringNode       the JSON node for dimension reduction
     * @param experimentFileEntity the ExperimentFileEntity to update
     */
    private static void setDimensionReduction(JsonNode clusteringNode, ExperimentFileEntity experimentFileEntity) {
        if (clusteringNode != null) {
            if (clusteringNode.isArray()) {
                List<DimensionReduction> dimensionReductions = new ArrayList<>();
                for (JsonNode node : clusteringNode) {
                    dimensionReductions.add(DimensionReduction.dimensionReductionFromValue(node.asText()));
                }
                experimentFileEntity.setDimensionReduction(dimensionReductions.toArray(new DimensionReduction[0]));
            } else {
                experimentFileEntity.setDimensionReduction(new DimensionReduction[]{DimensionReduction.dimensionReductionFromValue(clusteringNode.asText())});
            }
        }
    }

    /**
     * Gets the integer value from the JSON node for the specified key.
     * If the key does not exist or its value is null, it returns null.
     *
     * @param jsonNode the JSON node
     * @param key      the key to look for
     * @return the integer value or null if not found
     * @implNote This method is used to safely extract integer values from JSON nodes,
     * * ensuring that it handles cases where the key might not be present or the value is null.
     * @implSpec It checks if the JSON node has the specified key and if the value is not null before returning the integer value.
     */
    private static @Nullable Integer getIntegerValue(JsonNode jsonNode, String key) {
        return jsonNode.has(key) && !jsonNode.get(key).isNull()
                ? jsonNode.get(key).asInt()
                : null;
    }

    /**
     * Save json data to couch db and update json string.
     * * This method takes a JSON string, metadata file name, and created at timestamp,
     * * converts the JSON string to a JsonNode,
     * * creates a new ExperimentFileEntity,
     * * populates it with data from the JSON node,
     * * and saves it to the CouchDB database.
     * * It also generates a unique ID for the entity and updates the JSON string with this ID.
     * * Typical use cases:
     * * * - Storing experiment metadata in CouchDB
     * * * - Updating JSON data with a unique identifier
     *
     * @param jsonString       the json string
     * @param metadataFileName the metadata file name
     * @param createdAt        the created at
     * @return the string
     * @throws RuntimeException if there is an error during JSON conversion or saving to CouchDB
     * @implNote This method uses Jackson's ObjectMapper to parse the JSON string and convert it into a JsonNode.
     * @implSpec It handles different experiment types (ALIGNMENT and POST_PROCESSING) by checking the experimentTypeNode * and setting the appropriate fields in the ExperimentFileEntity.
     */
    public String saveJsonDataToCouchDBAndUpdateJSON(String jsonString, String metadataFileName, String createdAt) {
        ObjectMapper objectMapper = new ObjectMapper();
        String id;
        String jsonUpdatedWithID;
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            ExperimentFileEntity experimentFileEntity = new ExperimentFileEntity();
            id = UUID.randomUUID().toString();
            experimentFileEntity.setId(id);
            experimentFileEntity.setCreatedAt(createdAt);
            JsonNode experimentNameNode = jsonNode.get("experimentName");
            JsonNode experimentTypeNode = jsonNode.get("experimentType");
            experimentFileEntity.setExperimentName(
                    experimentNameNode != null ? experimentNameNode.asText() : "Unknown"
            );
            experimentFileEntity.setExperimentType(ExperimentType.fromValue(experimentTypeNode.asText()));
            experimentFileEntity.setStatus(ExperimentStatus.fromValue(ExperimentStatus.IN_EVALUATION.getValue()));
            experimentFileEntity.setMetadataFileName(metadataFileName);
            experimentFileEntity.setOmicsModality(jsonNode.get("omicsModality").asText());
            experimentFileEntity.setCellularResolution(jsonNode.get("cellularResolution").asText());
            if (experimentTypeNode.asText().equalsIgnoreCase(ExperimentType.ALIGNMENT.getValue())) {
                experimentFileEntity.setSamples(jsonNode.get("samples"));
                experimentFileEntity.setSelectedOrganism(jsonNode.get("selectedOrganism").asText());
                experimentFileEntity.setSelectedProtocol(Protocols.fromValue(jsonNode.get("selectedProtocol").asText()));
                experimentFileEntity.setAnnotation(jsonNode.get("annotation").asText());
                experimentFileEntity.setGenome(jsonNode.get("genome").asText());
                experimentFileEntity.setFastq1Tags(jsonNode.get("fastq1Tags"));
                experimentFileEntity.setSelectedAligner(Aligner.fromValue(jsonNode.get("selectedAligner").asText()));
                experimentFileEntity.setFastq2Tags(jsonNode.get("fastq2Tags"));
            } else if (experimentTypeNode.asText().equalsIgnoreCase(ExperimentType.POST_PROCESSING.getValue())) {
                experimentFileEntity.setMinGenesByCells(getIntegerValue(jsonNode, "minGenesByCells"));
                experimentFileEntity.setMinCellsExpressingGene(getIntegerValue(jsonNode, "minCellsExpressingGene"));
                experimentFileEntity.setNumHighVariableGenes(getIntegerValue(jsonNode, "numHighVariableGenes"));
                JsonNode clusteringNode = jsonNode.get("clustering");
                setClustering(clusteringNode, experimentFileEntity);
                JsonNode dimensionReduction = jsonNode.get("dimensionReduction");
                setDimensionReduction(dimensionReduction, experimentFileEntity);
            }
            experimentFileRepository.add(experimentFileEntity);
            logger.info("Document with ID {} successfully saved to CouchDB", id);
            jsonUpdatedWithID = Helper.addCouchDBIDToJSON(jsonString, id);
            logger.info("json data : {}" ,jsonUpdatedWithID);
        } catch (Exception e) {
            throw new JSONStorageException("Failed to save JSON data to CouchDB", e);
        }
        return jsonUpdatedWithID;
    }

    /**
     * Custom exception class for handling JSON storage errors.
     * This exception is thrown when there is an issue with saving or processing JSON data in the storage service.
     */
    public static class JSONStorageException extends RuntimeException {
        /**
         * Instantiates a new Json storage exception.
         *
         * @param message the message
         * @param cause   the cause
         */
        public JSONStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}