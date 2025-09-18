package com.example.eomix.entities;

import com.example.eomix.model.Clustering;
import com.example.eomix.model.DimensionReduction;
import com.example.eomix.model.Protocols;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Objects;

/**
 * The type Experiment response.
 * This class represents the response structure for an experiment,
 * including details such as experiment name, type, status,
 * metadata file name, samples, selected organism,
 * selected protocol, annotation, genome,
 * minimum genes by cells,
 * minimum cells expressing gene,
 * number of high variable genes,
 * clustering information,
 * dimension reduction information,
 * creation date,
 * omics modality,
 * and cellular resolution.
 * * It implements the equals, hashCode, and toString methods
 * to provide a complete representation of the experiment response.
 *
 * @param experimentName         the name of the experiment
 * @param experimentType         the type of the experiment
 * @param status                 the status of the experiment
 * @param metadataFileName       the name of the metadata file
 * @param samples                the samples associated with the experiment
 * @param selectedOrganism       the selected organism for the experiment
 * @param selectedProtocol       the protocol selected for the experiment
 * @param annotation             the annotation for the experiment
 * @param genome                 the genome used in the experiment
 * @param minGenesByCells        the minimum number of genes per cell
 * @param minCellsExpressingGene the minimum number of cells expressing a gene
 * @param numHighVariableGenes   the number of high variable genes
 * @param clustering             the clustering information for the experiment
 * @param dimensionReduction     the dimension reduction information for the experiment
 * @param createdAt              the creation date of the experiment
 * @param omicsModality          the omics modality of the experiment
 * @param cellularResolution     the cellular resolution of the experiment
 * * This class is immutable and uses a record to encapsulate the data.
 * * It provides a concise way to represent the experiment response
 * * and ensures that all fields are included in the response.
 */
public record ExperimentResponse(String experimentName, com.example.eomix.model.ExperimentType experimentType,
                                 String status, String metadataFileName, JsonNode samples, String selectedOrganism,
                                 Protocols selectedProtocol, String annotation, String genome, Integer minGenesByCells,
                                 Integer minCellsExpressingGene, Integer numHighVariableGenes, Clustering[] clustering,
                                 DimensionReduction[] dimensionReduction, String createdAt, String omicsModality,
                                 String cellularResolution) {

    /**
     * Checks if two ExperimentResponse objects are equal.
     * * This method compares the current object with another object to determine if they are equal.
     * * It checks if the other object is an instance of ExperimentResponse and compares all fields for equality.
     *
     * @param o the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExperimentResponse that = (ExperimentResponse) o;
        return Objects.equals(status, that.status) && Objects.equals(genome, that.genome)
                && Objects.equals(samples, that.samples) && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(annotation, that.annotation) && Objects.equals(omicsModality, that.omicsModality)
                && Objects.equals(experimentName, that.experimentName) && Objects.equals(metadataFileName, that.metadataFileName)
                && Objects.equals(selectedOrganism, that.selectedOrganism) && Objects.equals(minGenesByCells, that.minGenesByCells)
                && Objects.deepEquals(clustering, that.clustering) && Objects.equals(cellularResolution, that.cellularResolution)
                && selectedProtocol == that.selectedProtocol && Objects.equals(numHighVariableGenes, that.numHighVariableGenes)
                && Objects.equals(minCellsExpressingGene, that.minCellsExpressingGene)
                && Objects.deepEquals(dimensionReduction, that.dimensionReduction) && experimentType == that.experimentType;
    }

    /**
     * Returns the hash code value for the ExperimentResponse object.
     * * This method computes a hash code based on all fields of the ExperimentResponse object.
     * * It uses the "Objects.hash" method to generate a hash code that is consistent with the "equals" method.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(experimentName, experimentType, status,
                metadataFileName, samples, selectedOrganism, selectedProtocol,
                annotation, genome, minGenesByCells, minCellsExpressingGene,
                numHighVariableGenes, Arrays.hashCode(clustering),
                Arrays.hashCode(dimensionReduction), createdAt, omicsModality, cellularResolution);
    }

    /**
     * Returns a string representation of the ExperimentResponse object.
     * * This method provides a string representation of the ExperimentResponse object,
     * * including all fields and their values.
     *
     * @return a string representation of the ExperimentResponse object.
     */
    @Override
    public String toString() {
        return "ExperimentResponse{" +
                "experimentName='" + experimentName + '\'' +
                ", experimentType=" + experimentType +
                ", status='" + status + '\'' +
                ", metadataFileName='" + metadataFileName + '\'' +
                ", samples=" + samples +
                ", selectedOrganism='" + selectedOrganism + '\'' +
                ", selectedProtocol=" + selectedProtocol +
                ", annotation='" + annotation + '\'' +
                ", genome='" + genome + '\'' +
                ", minGenesByCells=" + minGenesByCells +
                ", minCellsExpressingGene=" + minCellsExpressingGene +
                ", numHighVariableGenes=" + numHighVariableGenes +
                ", clustering=" + Arrays.toString(clustering) +
                ", dimensionReduction=" + Arrays.toString(dimensionReduction) +
                ", createdAt='" + createdAt + '\'' +
                ", omicsModality='" + omicsModality + '\'' +
                ", cellularResolution='" + cellularResolution + '\'' +
                '}';
    }
}
