package com.example.eomix.service;


import com.example.eomix.entities.MetadataFileUploadEntity;
import com.example.eomix.model.Protocols;
import com.example.eomix.repositories.MetadataFileRepository;
import com.example.eomix.utils.Helper;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.CouchDbConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.eomix.controller.ErrorHandler.handleMissingDataInMetadataFile;
import static com.example.eomix.utils.Constants.*;

/**
 * The type File system storage service.
 */
@Service
public class FileSystemStorageService implements StorageService {


    private static final String SAMPLE_ID = "sample_id";
    /**
     * The Metadata file repository.
     */
    public final MetadataFileRepository metadataFileRepository;

    /**
     * Instantiates a new File system storage service.
     *
     * @param db the db
     */
    @Autowired
    public FileSystemStorageService(@Qualifier("couchDbConnector1") CouchDbConnector db) {
        this.metadataFileRepository = new MetadataFileRepository(db);
    }

    /**
     * Gets all metadata from json nodes.
     * This method extracts metadata from a list of JsonNode objects, which are typically the results of a database query.
     * It retrieves specific fields such as filename, species, omics modality, cellular resolution, protocol, organ, and disorder from each JsonNode.
     * It constructs a list of maps, where each map represents the metadata for a file.
     *
     * @param results the results
     * @return the all metadata from json nodes
     */
    public static @NotNull List<Map<String, Object>> getAllMetadataFromJsonNodes(List<JsonNode> results) {
        List<Map<String, Object>> fileMetadataList = new ArrayList<>();
        for (JsonNode row : results) {
            if (row == null) {
                continue;
            }
            Map<String, Object> fileMetadata = new HashMap<>();
            fileMetadata.put("filename", row.get("filename").asText());
            fileMetadata.put("species", row.get("species").asText());
            fileMetadata.put("omicsModality", row.get(SEQUENCE_TYPE).asText());
            fileMetadata.put(CELLULAR_RESOLUTION, row.get(CELLULAR_RESOLUTION).asText());
            if (row.get(PROTOCOL) != null)
                fileMetadata.put(PROTOCOL, Protocols.fromValue(row.get(PROTOCOL).asText()));
            addListOfDisordersOrOrgansToFileMetadataMap(row, ORGAN, fileMetadata);
            addListOfDisordersOrOrgansToFileMetadataMap(row, DISORDER, fileMetadata);
            fileMetadataList.add(fileMetadata);
        }
        return fileMetadataList;
    }

    /**
     * Adds a list of disorders or organs to the file metadata map.
     * This method extracts a list of disorders or organs from a JsonNode and adds it to the provided file metadata map.
     * It checks if the specified key exists in the JsonNode and if it is an array, then iterates through the array to collect the values.
     *
     * @param row           the JsonNode containing the data
     * @param keyToBeListed the key to be listed (either "disorder" or "organ")
     * @param fileMetadata  the map where the list will be added
     */
    private static void addListOfDisordersOrOrgansToFileMetadataMap(JsonNode row, String keyToBeListed, Map<String, Object> fileMetadata) {
        List<String> disorderList = new ArrayList<>();
        JsonNode node = row.get(keyToBeListed);
        if (node.isArray()) {
            for (JsonNode disorderNode : node) {
                disorderList.add(disorderNode.asText());
            }
        }
        fileMetadata.put(keyToBeListed, disorderList);
    }

    /**
     * Verifies the file type of the uploaded file.
     * This method checks if the uploaded file is a CSV file by examining its name and extension.
     * If the file is not a CSV or has no name, it returns a bad request response with an appropriate error message.
     *
     * @param file the uploaded file
     * @return a ResponseEntity indicating success or failure
     */
    private static @Nullable ResponseEntity<String> verifyFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: File has no name");
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body("Error: Only CSV files are allowed");
        }
        return null;
    }

    /**
     * Processes CSV lines to extract and group sample data by sample_id.
     * * The method:
     * * - Expects the first line to contain headers including a 'sample_id' column
     * * - Groups data by sample_id into a map of {sample_id → list of sample records}
     * * - Each sample record is a map of {header → value} pairs
     * * - Filters out invalid lines (wrong column count or empty sample_id)
     * * Processing details:
     * * - Uses Java Streams for efficient processing
     * * - Pre-computes column positions for optimal performance
     * * - Returns empty map if no valid samples found
     *
     * @param batchLines List of CSV lines (first line should be headers)
     * @param headers    Expected column headers
     * @param separator  CSV field delimiter
     * @return Non-null map grouping samples by sample_id (empty if no valid data)
     * @throws NullPointerException if any parameter is null
     * @implNote The SAMPLE_ID constant defines the key column name
     * @implSpec This implementation is optimized for large files by:
     * *           - Using stream processing
     * *           - Avoiding repeated column lookups
     * *           - Filtering early in the pipeline
     */
    private static @NotNull Map<String, List<Map<String, String>>> getSamplesBatchFromLines(List<String> batchLines, String[] headers, String separator) {
        // Find the index of the sample_id column once, before processing the stream.
        // This is much more efficient than searching for it in every single line.
        int sampleIdIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            if (SAMPLE_ID.equalsIgnoreCase(headers[i])) {
                sampleIdIndex = i;
                break;
            }
        }

        // If there's no "sample_id" column in the headers at all, we can't proceed.
        if (sampleIdIndex == -1) {
            // Return an empty map as no grouping is possible.
            return Collections.emptyMap();
        }

        // A final variable is required for use in a lambda expression.
        final int finalSampleIdIndex = sampleIdIndex;

        return batchLines.stream()
                .map(line -> line.split(separator))
                .filter(values -> values.length == headers.length)
                // Filter for records where the sample_id is present and not empty.
                .filter(values -> !values[finalSampleIdIndex].isEmpty())
                .collect(Collectors.groupingBy(
                        // Classifier: Directly access the sample_id using the pre-calculated index.
                        // No need for a loop or null check here, as the stream is already filtered.
                        values -> values[finalSampleIdIndex],

                        // Downstream Collector: Map headers to values.
                        Collectors.mapping(
                                values ->
                                        // Using IntStream is a concise way to create the map.
                                        IntStream.range(0, headers.length)
                                                .boxed()
                                                .collect(Collectors.toMap(i -> headers[i], i -> values[i])),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * Stores the uploaded file on CouchDB after processing it.
     * This method reads the file, validates its headers, and processes its lines to extract metadata.
     * It then stores the processed metadata in CouchDB in batches.
     *
     * @param file              the uploaded file
     * @param detectedDelimiter the delimiter used in the CSV file
     * @return a ResponseEntity indicating success or failure
     * @implNote The method handles file reading, header validation, and metadata extraction.
     * * It also manages the storage of metadata in CouchDB, ensuring that the file is processed in manageable batches.
     */
    @Override
    public ResponseEntity<String> storeOnCouchDb(MultipartFile file, String detectedDelimiter) {

        ResponseEntity<String> body = verifyFileType(file);
        if (body != null) return body;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            // Read headers and validate
            String[] headers = reader.readLine().split(detectedDelimiter);
            ResponseEntity<String> response = handleMissingDataInMetadataFile(headers);
            if (response != null) return response;
            // Process file lines
            List<String> fileLines = reader.lines()
                    .filter(line -> !line.toLowerCase().contains("type") &&
                            !line.toLowerCase().contains("group"))
                    .toList();

            Map<String, Integer> sampleCounts = new HashMap<>();
            ResponseEntity<String> body1 = parseFileLinesAndGetCellsCountNumber(detectedDelimiter, fileLines, headers, sampleCounts);
            if (body1 != null) return body1;
            // Process batch and upload to CouchDB
            List<MetadataFileUploadEntity> fileList = processBatch(fileLines, headers, file, detectedDelimiter, sampleCounts);
            // Process in batches
            int batchSize = 1000;
            for (int i = 0; i < fileList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, fileList.size());
                List<MetadataFileUploadEntity> batch = fileList.subList(i, end);
                if (batch.isEmpty()) {
                    return ResponseEntity.badRequest().body("No valid records found to process");
                }
                metadataFileRepository.bulkAdd(batch);

            }

            return ResponseEntity.ok("File processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    /**
     * Parses file lines and counts the number of cells for each sample.
     * This method iterates through each line of the file, splits it by the detected delimiter,
     * and counts the number of cells for each sample based on the sample_id.
     * It also checks for required parameters and returns an error response if any are missing.
     *
     * @param detectedDelimiter the delimiter used in the CSV file
     * @param fileLines         the lines of the file
     * @param headers           the headers of the file
     * @param sampleCounts      a map to store sample counts
     * @return a ResponseEntity indicating success or failure
     */
    private @Nullable ResponseEntity<String> parseFileLinesAndGetCellsCountNumber(String detectedDelimiter, List<String> fileLines, String[] headers, Map<String, Integer> sampleCounts) {
        for (String line : fileLines) {
            List<String> values = Arrays.stream(line.split(detectedDelimiter)).toList();
            String sampleId = null;
            if (values.size() == headers.length) {
                sampleId = findSampleId(values, headers);
                sampleCounts.put(sampleId, sampleCounts.getOrDefault(sampleId, 0) + 1);
            } else if (parseCSVLine(line).size() == headers.length) {
                List<String> newValues = parseCSVLine(line);
                sampleId = findSampleId(newValues, headers);
                sampleCounts.put(sampleId, sampleCounts.getOrDefault(sampleId, 0) + 1);
            }
            if (sampleId == null || sampleId.isEmpty()) {
                return ResponseEntity.badRequest().body("Missing sample id in the records");
            }
            if (findCellularResolution(values, headers).equals("Single Cell") && !Arrays.stream(headers).toList().contains(PROTOCOL)) {
                return ResponseEntity.badRequest().body("File is missing required parameter(s) : " + PROTOCOL +
                        ". It is required for Single Cell data.");
            }
        }
        return null;
    }

    /**
     * Parses a CSV line and returns a list of values.
     * This method handles quoted values and commas within quotes correctly.
     * It splits the line by commas, taking care to handle cases where commas are part of quoted strings.
     *
     * @param line the CSV line to parse
     * @return a list of values extracted from the line
     */
    private List<String> parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());
        return values;
    }

    /**
     * Processes a batch of lines from the file and creates MetadataFileUploadEntity objects.
     * This method groups the lines by sample_id, extracts relevant metadata, and creates entities for each sample.
     * It also sets the createdAt timestamp and other metadata fields.
     *
     * @param batchLines   the lines of the file to process
     * @param headers      the headers of the file
     * @param file         the original file
     * @param separator    the field separator used in the file
     * @param sampleCounts a map containing counts of cells for each sample_id
     * @return a list of MetadataFileUploadEntity objects created from the batch lines
     * @implNote This method is used to process a batch of lines from a file and create MetadataFileUploadEntity objects.
     * * It is typically called after reading the file and validating its headers.
     * @implSpec The method expects the batchLines to be a list of strings, where each string represents a line from the file.
     */
    private List<MetadataFileUploadEntity> processBatch(List<String> batchLines, String[] headers,
                                                        MultipartFile file, String separator, Map<String, Integer> sampleCounts) {

        String createdAt = Instant.now().toString();
        List<MetadataFileUploadEntity> uploadEntities = new ArrayList<>();
        // Group by sample_id and create MetadataFileUploadEntity objects
        Map<String, List<Map<String, String>>> samples = getSamplesBatchFromLines(batchLines, headers, separator);

        samples.forEach((sampleId, records) -> {
            if (!records.isEmpty()) {
                MetadataFileUploadEntity.MetaData metaData = new MetadataFileUploadEntity.MetaData();
                metaData.setFilename(file.getOriginalFilename());
                metaData.setCreatedAt(createdAt);
                Map<String, String> sourceColumns = new HashMap<>();
                Map<String, String> firstRecord = records.get(0);
                sourceColumns.put(ORGAN, firstRecord.get(ORGAN));
                sourceColumns.put(GENDER, firstRecord.get(GENDER));
                sourceColumns.put(SAMPLE_ID, sampleId);
                sourceColumns.put(PATIENT_ID, firstRecord.get(PATIENT_ID));
                if (firstRecord.get(PROTOCOL) != null)
                    sourceColumns.put(PROTOCOL, Protocols.fromValue(firstRecord.get(PROTOCOL)).toString());
                sourceColumns.put(STANDARDIZED_SPECIES, firstRecord.get(STANDARDIZED_SPECIES));
                sourceColumns.put(CELLULAR_RESOLUTION, firstRecord.get(CELLULAR_RESOLUTION));
                sourceColumns.put(DISORDER, firstRecord.get(DISORDER));
                sourceColumns.put(AGE, firstRecord.get(AGE));
                sourceColumns.put(SEQUENCE_TYPE, firstRecord.get(SEQUENCE_TYPE));
                sourceColumns.put(BATCH, firstRecord.get(BATCH));
                sourceColumns.put(CELL_NUMBER, sampleCounts.get(sampleId).toString());
                MetadataFileUploadEntity uploadedFile = new MetadataFileUploadEntity();
                uploadedFile.setMeta(metaData);
                uploadedFile.setSourceColumns(sourceColumns);
                uploadEntities.add(uploadedFile);
            }
        });
        return uploadEntities;
    }

    /**
     * Finds the sample_id in the list of values based on the headers.
     * This method iterates through the headers and returns the corresponding value for sample_id.
     * * If sample_id is not found, it returns null.
     *
     * @param values  the list of values from a CSV line
     * @param headers the headers of the CSV file
     * @return the sample_id if found, otherwise null
     * @implNote This method is used to extract the sample_id from a list of values based on the headers.
     * @implSpec It is typically called after parsing a CSV line to find the sample_id value.
     */
    private String findSampleId(List<String> values, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            if (SAMPLE_ID.equalsIgnoreCase(headers[i])) {
                return values.get(i);
            }
        }
        return null;
    }

    /**
     * Finds the cellular resolution in the list of values based on the headers.
     * This method iterates through the headers and returns the corresponding value for cellular resolution.
     * If cellular resolution is not found, it returns an empty string.
     *
     * @param values  the list of values from a CSV line
     * @param headers the headers of the CSV file
     * @return the cellular resolution if found, otherwise an empty string
     * @implNote This method is used to extract the cellular resolution from a list of values based on the headers.
     * @implSpec It is typically called after parsing a CSV line to find the cellular resolution value.
     */
    private String findCellularResolution(List<String> values, String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            if (CELLULAR_RESOLUTION.equalsIgnoreCase(headers[i])) {
                return values.get(i);
            }
        }
        return "";
    }

    /**
     * Converts a list of MetadataFileUploadEntity objects to a CSV string.
     * This method extracts the source columns from each entity and formats them into a tab-separated string.
     * If the list is empty, it returns an empty string.
     *
     * @param documents the list of MetadataFileUploadEntity objects
     * @return a string representing the CSV content
     */
    @Override
    public String convertToCSV(List<MetadataFileUploadEntity> documents) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        if (!documents.isEmpty()) {
            MetadataFileUploadEntity firstDoc = documents.get(0);
            writer.println(String.join("\t", firstDoc.getSourceColumns().keySet()));
            for (MetadataFileUploadEntity doc : documents) {
                Map<String, String> sourceColumns = doc.getSourceColumns();
                writer.println(String.join("\t", sourceColumns.values()));
            }
        }
        return stringWriter.toString();
    }

    /**
     * Retrieves a metadata file from CouchDB by its filename and converts it to a File object.
     * This method fetches the metadata file as bytes, converts it to a File object, and returns it.
     * If the file is not found, it throws a FileNotFoundException.
     *
     * @param filename the name of the file to retrieve
     * @return a File object containing the metadata
     * @throws FileNotFoundException if the file is not found in CouchDB
     */
    @Override
    public File getMetadataFileFromCouchDBByFileName(String filename) throws FileNotFoundException {
        byte[] csvBytes = getMetadataFileAsBytesFromFileName(filename);
        return Helper.convertBytesToFile(csvBytes, "newFileName");
    }

    /**
     * Retrieves a metadata file from CouchDB by its filename and converts it to a File object for Minio.
     * <p> This method fetches the metadata file as bytes, converts it to a File object with a safe filename,
     * and returns it. If the file is not found, it throws a FileNotFoundException.
     * <p> This method is specifically designed to handle filenames that may contain characters
     * that are not safe for Minio storage.
     *
     * @param filename the name of the file to retrieve
     * @return a File object containing the metadata
     * @throws FileNotFoundException if the file is not found in CouchDB
     * @implNote This method is specifically designed to handle filenames that may contain characters
     * * that are not safe for Minio storage.
     * @implSpec It replaces any forward slashes in the filename with underscores to ensure compatibility with Minio.
     */
    @Override
    public File getMetadataFileFromCouchDBByFileNameForMinio(String filename) throws FileNotFoundException {
        byte[] csvBytes = getMetadataFileAsBytesFromFileName(filename);
        String safeFileName = filename.replace("/", "_");
        return Helper.convertBytesToFile(csvBytes, safeFileName);
    }

    /**
     * Retrieves a metadata file from CouchDB by its filename and converts it to a byte array.
     * This method fetches the metadata file as bytes and returns it. If the file is not found,
     * it throws a FileNotFoundException.
     * * This method is used to retrieve the metadata file content as a byte array,
     * * which can be useful for further processing or storage.
     *
     * @param filename the name of the file to retrieve
     * @return a byte array containing the metadata file content
     * @throws FileNotFoundException if the file is not found in CouchDB
     * @implNote It uses the metadataFileRepository to find the file by its filename.
     * @implSpec The method converts the retrieved documents to a CSV string and then to a byte array.
     */
    private byte @NotNull [] getMetadataFileAsBytesFromFileName(String filename) throws FileNotFoundException {
        List<MetadataFileUploadEntity> documents = metadataFileRepository.findByFilename(filename);
        if (documents.isEmpty()) {
            throw new FileNotFoundException(filename + " not found");
        }
        String csvContent = convertToCSV(documents);
        return csvContent.getBytes(StandardCharsets.UTF_8);
    }


}
