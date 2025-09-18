package com.example.eomix.utils;

import com.example.eomix.entities.ExperimentFileEntity;
import com.example.eomix.entities.ExperimentResponse;
import com.example.eomix.exception.AlignerRetrievalException;
import com.example.eomix.exception.HelperException;
import com.example.eomix.model.Aligner;
import com.example.eomix.model.ExperimentType;
import com.example.eomix.service.FhirServiceImplementation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The type Helper.
 *
 * @author Molka Anaghim FTOUHI
 */
public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    /**
     * Private constructor to prevent instantiation.
     * This class contains static utility methods and should not be instantiated.
     */
    private Helper() {
    }

    /**
     * Converts a byte array into a file and stores it in the system's temporary directory.
     * <p>
     * This method creates a file with the specified name in the system's default temporary directory
     * (e.g., {@code /tmp} on Linux or {@code %TEMP%} on Windows) and writes the provided byte array to it.
     * <p>
     * If a file with the same name already exists, it will be overwritten without prompting.
     *
     * @param fileBytes the byte array containing the file's content
     * @param fileName  the name of the file to be created (e.g., {@code "example.txt"})
     * @return the {@link File} object representing the created file, or {@code null} if an I/O error occurs
     * @throws NullPointerException if {@code fileName} is {@code null}
     * @implNote <ul>
     * <li>The file is stored in the system's temporary directory, retrieved using {@code java.io.tmpdir}.</li>
     * <li>If a file with the same name exists, it is overwritten and a warning is logged.</li>
     * <li>Any I/O error (e.g., permission issues) results in a {@code null} return, and the error is logged.</li>
     * </ul>
     */
    public static File convertBytesToFile(byte[] fileBytes, String fileName) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File convFile = new File(tempDir, fileName);
        try {
            boolean newFile = convFile.createNewFile();
            if (newFile) {
                logger.info("File created successfully: {}", convFile.getAbsolutePath());
            } else {
                logger.warn("File already exists, overwriting: {}", convFile.getAbsolutePath());
            }
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(fileBytes);
            }
        } catch (IOException e) {
            logger.error("Failed to create/write file: {}", e.getMessage());
            convFile = null;
        }
        return convFile;

    }

    /**
     * Reads a CSV file and returns a list of parsed CSV records.
     * <p>
     * This method uses the Apache Commons CSV library to parse a tab-delimited CSV file.
     * It treats the first row as a header and skips any empty rows. If the first header contains
     * the word {@code "TYPE"}, it is removed from the records.
     *
     * @param fileEntry the uploaded file to read (typically a tab-delimited CSV file)
     * @return a list of {@link org.apache.commons.csv.CSVRecord} objects, each representing a row in the file
     * @throws HelperException if an error occurs while reading or parsing the CSV file
     * @implSpec The method uses a {@link java.io.BufferedReader} for efficient file reading
     * and constructs a {@link org.apache.commons.csv.CSVParser} to parse the content.
     * @implNote This method is commonly used in scenarios such as importing structured data from CSV files
     * or preprocessing data for further analysis.
     * <ul>
     *   <li>Tab-delimited format is assumed (i.e., TSV)</li>
     *   <li>Empty rows are skipped during parsing</li>
     *   <li>The first row is removed if it contains a header with the word {@code "TYPE"}</li>
     * </ul>
     */

    public static List<CSVRecord> getCSVRecords(File fileEntry) {

        List<CSVRecord> csvRecordList;
        try {
            CSVParser csvParser = getCSVParser(fileEntry);
            FhirServiceImplementation.headersList = csvParser.getHeaderNames();
            csvRecordList = csvParser.getRecords();
            for (CSVRecord csvRecord : csvParser) {
                if (!isRowEmpty(csvRecord)) {
                    csvRecordList.add(csvRecord);
                }
            }
            if (csvRecordList.get(0).get(0).equalsIgnoreCase("TYPE")) {
                csvRecordList.remove(0);
            }

        } catch (IOException e) {
            throw new HelperException("Error reading CSV file: " + fileEntry.getName(), e);
        }
        return csvRecordList;
    }

    /**
     * Creates and returns a CSV parser for the specified file entry.
     * <p>
     * This method reads a CSV file using a tab delimiter and treats the first row as the header.
     * It utilizes a {@link java.io.BufferedReader} for efficient reading and
     * constructs a {@link org.apache.commons.csv.CSVParser} to parse the file content.
     *
     * @param fileEntry the uploaded file to parse
     * @return a configured {@link org.apache.commons.csv.CSVParser} instance for the file
     * @throws IOException if an I/O error occurs while reading the file
     * @implSpec The parser is configured with:
     * <ul>
     *   <li>Tab ({@code \t}) as the delimiter</li>
     *   <li>The first row treated as the header</li>
     * </ul>
     * @implNote This method reads the entire file content and builds the parser accordingly.
     * It is intended for use cases that require structured tabular data extraction from tab-delimited files.
     */

    private static CSVParser getCSVParser(File fileEntry) throws IOException {
        InputStream inputStream = new FileInputStream(fileEntry);
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String firstLine = bufferedReader.readLine();
            reader = new StringReader(firstLine + "\n" + bufferedReader.lines().collect(Collectors.joining("\n")));
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter('\t')
                .setHeader() // Or .setHeader() depending on your exact need
                .build();
        return format.parse(reader);
    }

    /**
     * Checks whether a given CSV record (row) is empty.
     * <p>
     * A row is considered empty if all its cells are either {@code null} or contain only whitespace.
     *
     * @param row the CSV record to check
     * @return {@code true} if the row is empty; {@code false} otherwise
     * @implNote The method iterates over each cell in the row, trimming whitespace and verifying
     * if any cell contains non-empty content. If any cell has meaningful content, the row is not empty.
     */

    private static boolean isRowEmpty(CSVRecord row) {
        for (String recordCell : row) {
            if (recordCell != null && !recordCell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates alignment JSON data by adding Fastq file tags and experiment information.
     * <p>
     * This method parses the input JSON string, inserts the provided Fastq file tags as new fields,
     * and adds experiment-related metadata such as experiment name, type, and status.
     *
     * @param jsonData       the original JSON data as a string
     * @param fastq1FileTags the tags for the first Fastq file
     * @param fastq2FileTags the tags for the second Fastq file (optional; may be {@code null})
     * @param experimentName the name of the experiment
     * @return the updated JSON data as a string
     * @throws HelperException if an error occurs during JSON parsing or modification
     * @implSpec Uses Jackson's {@code ObjectMapper} to parse the JSON string into an {@code ObjectNode} for editing.
     * The method sets the Fastq file tags and experiment metadata as new fields in the JSON object.
     * If the second Fastq file tags are provided, they are also added.
     * @implNote In case of any JSON processing errors, the method throws a {@code HelperException}
     * with a descriptive message.
     */
    public static String updateAlignmentJsonWithTags(String jsonData, Map<String, String> fastq1FileTags, Map<String, String> fastq2FileTags, String experimentName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode alignmentObjectNode = getJsonNodes(jsonData, objectMapper);
            ObjectNode fastq1Tags = objectMapper.valueToTree(fastq1FileTags);
            alignmentObjectNode.set("fastq1Tags", fastq1Tags);
            if (fastq2FileTags != null) {
                ObjectNode fastq2Tags = objectMapper.valueToTree(fastq2FileTags);
                alignmentObjectNode.set("fastq2Tags", fastq2Tags);
            }
            addExperimentTypeAndStatusToJSON(alignmentObjectNode, experimentName, ExperimentType.ALIGNMENT);
            return objectMapper.writeValueAsString(alignmentObjectNode);
        } catch (Exception e) {
            throw new HelperException("Failed to update JSON with tags", e);
        }
    }

    /**
     * Adds a CouchDB ID to the given JSON data.
     * <p>
     * This method parses the input JSON string, inserts a new field named {@code "couchDBId"}
     * with the specified ID value, and returns the updated JSON string.
     *
     * @param jsonData the original JSON data as a string
     * @param id       the CouchDB ID to add
     * @return the updated JSON data as a string
     * @throws HelperException if an error occurs during JSON parsing or modification
     * @implNote The method uses Jackson's {@code ObjectMapper} to parse the JSON string
     * into an {@code ObjectNode} for editing. The {@code "couchDBId"} field is then set
     * with the provided ID before returning the updated JSON string.
     */
    public static String addCouchDBIDToJSON(String jsonData, String id) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonData);

            if (jsonNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                objectNode.put("couchDBId", id);
                return objectMapper.writeValueAsString(objectNode);
            } else {
                throw new HelperException("JSON data is not an object");
            }
        } catch (Exception e) {
            throw new HelperException("Failed to add CouchDB ID to JSON", e);
        }
    }

    /**
     * Updates process JSON data by adding an experiment name and a tag.
     * <p>
     * This method parses the input JSON string
     * and adds experiment-related metadata including experiment name, type, and status.
     *
     * @param jsonData       the original JSON data as a string
     * @param experimentName the name of the experiment
     * @return the updated JSON data as a string
     * @throws HelperException if an error occurs during JSON parsing or modification
     * @implNote The method uses Jackson's {@code ObjectMapper} to parse the JSON string
     * into an {@code ObjectNode} for editing. The tag and experiment metadata
     * are added as new fields. In case of errors, a {@code HelperException} is thrown with details.
     */
    public static String updateProcessJsonWithTags(String jsonData, String experimentName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNode = getJsonNodes(jsonData, objectMapper);
            addExperimentTypeAndStatusToJSON(objectNode, experimentName, ExperimentType.POST_PROCESSING);
            return objectMapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            throw new HelperException("Failed to update JSON with tags", e);
        }
    }

    /**
     * Parses the provided JSON data and returns it as an {@link com.fasterxml.jackson.databind.node.ObjectNode}.
     * <p>
     * This method uses Jackson's {@code ObjectMapper} to read the JSON string into a tree model.
     * It verifies that the root node is an {@code ObjectNode}; if not, it throws an exception.
     *
     * @param jsonData     the JSON data as a string
     * @param objectMapper the Jackson {@code ObjectMapper} instance used for parsing
     * @return the parsed {@code ObjectNode}
     * @throws com.fasterxml.jackson.core.JsonProcessingException if an error occurs during JSON processing
     * @throws IllegalArgumentException                           if the root JSON node is not an {@code ObjectNode}
     * @implNote The method calls {@code readTree} on the {@code ObjectMapper} to parse the JSON.
     * It checks the root node’s type and ensures it is an {@code ObjectNode}, otherwise throws {@code IllegalArgumentException}.
     */
    private static @NotNull ObjectNode getJsonNodes(String jsonData, ObjectMapper objectMapper) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(jsonData);
        if (!(rootNode instanceof ObjectNode objectNode)) {
            throw new IllegalArgumentException("Invalid JSON format");
        }
        return objectNode;
    }

    /**
     * Adds experiment details to the given JSON object.
     * <p>
     * This method sets the fields {@code "experimentName"}, {@code "experimentType"}, and {@code "status"}
     * in the provided {@link com.fasterxml.jackson.databind.node.ObjectNode} with the specified values.
     *
     * @param objectNode     the JSON object to update
     * @param experimentName the name of the experiment
     * @param experimentType the type of the experiment
     * @implNote This method modifies the {@code ObjectNode} directly using Jackson's API,
     * setting the specified fields with the given values.
     */
    private static void addExperimentTypeAndStatusToJSON(ObjectNode objectNode, String experimentName, ExperimentType experimentType) {
        objectNode.put("experimentName", experimentName);
        objectNode.put("experimentType", experimentType.getValue());
        objectNode.put("status", "In_evaluation");

    }


    /**
     * Generates a folder name by sanitizing the input and appending a formatted date.
     * <p>
     * This method removes the ".csv" extension from the provided folder name (if present),
     * replaces all spaces with underscores, and appends the specified formatted date.
     *
     * @param folderName    the original folder name, expected to include ".csv"
     * @param formattedDate the formatted date string to append
     * @return the generated folder name string with underscores and appended date
     * @implNote The method uses {@code Objects.requireNonNull} to validate the folder name is not null.
     * It then removes the ".csv" extension, replaces spaces with underscores,
     * and appends the formatted date to form the final folder name.
     */
    public static String generateFolderName(String folderName, String formattedDate) {

        return Objects.requireNonNull(folderName.replace(".csv", "")).replace(" ", "_") + "-" + formattedDate;
    }

    /**
     * Generates a folder name by sanitizing the input, appending an aligner type and a formatted date.
     * <p>
     * This method removes the ".csv" extension from the given folder name (if present),
     * replaces spaces with underscores, then appends the specified aligner type
     * followed by the formatted date to create the final folder name.
     *
     * @param folderName    the original folder name, expected to include ".csv"
     * @param aligner       the aligner type to append
     * @param formattedDate the formatted date string to append
     * @return the generated folder name string with underscores, aligner, and appended date
     * @implNote The method uses {@code Objects.requireNonNull} to ensure the folder name is not null.
     * It then sanitizes the folder name by replacing spaces with underscores,
     * removes the ".csv" extension, and appends the aligner and formatted date.
     */
    public static String generateFolderName(String folderName, String aligner, String formattedDate) {

        return Objects.requireNonNull(folderName.replace(".csv", "")).replace(" ", "_") + "-" + aligner + "-" + formattedDate;
    }

    /**
     * Generates the current date and time as a formatted string.
     * <p>
     * The format used is {@code "dd-MM-yyyy_HH-mm-ss"}, representing day, month, year,
     * followed by hours, minutes, and seconds separated by underscores and hyphens.
     *
     * @return the current date and time formatted as a string
     * @implNote This method creates a new {@link java.text.SimpleDateFormat} instance with the specified pattern
     * and formats the current date and time accordingly.
     */
    public static @NotNull String getDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        return sdf.format(new Date());
    }

    /**
     * Retrieves the selected aligner value from the provided JSON string.
     * <p>
     * This method parses the JSON data to extract the value associated with the "selectedAligner" field
     * and returns it as a string.
     *
     * @param jsonString the JSON string containing the aligner information
     * @return the value of the "selectedAligner" field as a string
     * @throws AlignerRetrievalException if an error occurs during JSON parsing or if the field is missing
     * @implNote The method uses Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper} to parse the JSON
     * and extract the "selectedAligner" field. Errors in parsing or field retrieval result in an exception.
     */
    public static String getAlignerFromJson(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return Aligner.fromValue(jsonNode.get("selectedAligner").asText()).toString();
        } catch (Exception e) {
            throw new AlignerRetrievalException("Error retrieving aligner from JSON", e);
        }
    }

    /**
     * Converts a list of {@code ExperimentFileEntity} objects into a list of {@code ExperimentResponse} objects,
     * and returns them wrapped in a {@link org.springframework.http.ResponseEntity} with the specified HTTP headers.
     *
     * @param results the list of experiment file entities to convert
     * @param header  the HTTP headers to include in the response
     * @return a {@code ResponseEntity} containing the list of experiment responses and the provided headers
     * @implNote This method maps each {@code ExperimentFileEntity} to an {@code ExperimentResponse}
     * and constructs a {@code ResponseEntity} containing the mapped list along with the given headers.
     */
    @NotNull
    public static ResponseEntity<List<ExperimentResponse>> getExperimentsListResponseEntity(List<ExperimentFileEntity> results, HttpHeaders header) {
        List<ExperimentResponse> responseList = results.stream().map(exp -> new ExperimentResponse(exp.getExperimentName(), exp.getExperimentType(), exp.getStatus().toString(), exp.getMetadataFileName(), exp.getSamples(), exp.getSelectedOrganism(), exp.getSelectedProtocol(), exp.getAnnotation(), exp.getGenome(), exp.getMinGenesByCells(), exp.getMinCellsExpressingGene(), exp.getNumHighVariableGenes(), exp.getClustering(), exp.getDimensionReduction(), exp.getCreatedAt(), exp.getOmicsModality(), exp.getCellularResolution())).toList();

        return ResponseEntity.ok().headers(header).body(responseList);
    }


}
