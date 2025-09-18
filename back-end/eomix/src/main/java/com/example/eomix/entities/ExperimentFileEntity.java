package com.example.eomix.entities;

import com.example.eomix.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.ektorp.support.CouchDbDocument;

/**
 * The type Experiment file entity.
 * This class represents an experiment file entity in the database.
 * It extends CouchDbDocument to provide the necessary structure
 * for storing experiment-related data.
 * * It includes fields such as experiment name, type, status,
 * * metadata file name, samples, selected organism,
 * * selected protocol, aligner, annotation, genome,
 * * fastq tags, minimum genes by cells,
 * * minimum cells expressing gene,
 * * number of high variable genes,
 * * clustering information,
 * * dimension reduction information,
 * * creation date,
 * * omics modality,
 * * and cellular resolution.
 * * This class is used to store and retrieve experiment data
 * from a CouchDB database.
 * * @see CouchDbDocument
 * * @see ExperimentType
 * * @see ExperimentStatus
 * * @see Protocols
 * * @see Aligner
 * * @see Clustering
 * * @see DimensionReduction
 * * @see JsonNode
 *
 * @implNote The class uses Lombok annotations for getter and setter methods,
 * * which reduces boilerplate code and improves readability.
 * @implSpec The class is designed to be used with a CouchDB database,
 * * allowing for easy storage and retrieval of experiment data.
 * * @author Molka Anaghim FTOUHI
 */
@Getter
@Setter
public class ExperimentFileEntity extends CouchDbDocument {
    private String experimentName;
    private ExperimentType experimentType;
    private ExperimentStatus status;
    private String metadataFileName;
    private transient JsonNode samples;
    private String selectedOrganism;
    private Protocols selectedProtocol;
    private Aligner selectedAligner;
    private String annotation;
    private String genome;
    private transient JsonNode fastq1Tags;
    private transient JsonNode fastq2Tags;
    private Integer minGenesByCells;
    private Integer minCellsExpressingGene;
    private Integer numHighVariableGenes;
    private Clustering[] clustering;
    private DimensionReduction[] dimensionReduction;
    private String createdAt;
    private String omicsModality;
    private String cellularResolution;
}
