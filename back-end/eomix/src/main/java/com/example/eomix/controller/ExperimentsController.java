package com.example.eomix.controller;

import com.example.eomix.entities.ExperimentFileEntity;
import com.example.eomix.entities.ExperimentResponse;
import com.example.eomix.repositories.ExperimentFileRepository;
import com.example.eomix.resource_provider.SpecimenRP;
import com.example.eomix.service.MinioStorageService;
import com.example.eomix.service.RemoteScriptExecutor;
import com.example.eomix.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import static com.example.eomix.utils.Constants.*;

/**
 * The type Experiments controller.
 */
@RestController
@RequestMapping("/public")
public class ExperimentsController {


    public static final String PATH_TO_POST_PROCESSING_DOCKER_RUN_SCRIPT = "src/main/resources/scRNA-seq/post-processing/post-processing_docker_run_script.sh";
    private static final Logger logger = LoggerFactory.getLogger(ExperimentsController.class);
    /**
     * The Specimen resource provider.
     */
    public final SpecimenRP specimenRP;
    private final MinioStorageService minioStorageService;
    private final RemoteScriptExecutor scriptExecutor;
    private final ExperimentFileRepository experimentFileRepository;


    /**
     * Instantiates a new Experiments controller.
     *
     * @param minioStorageService      the minio storage service
     * @param scriptExecutor           the script executor
     * @param experimentFileRepository the experiment file repository
     * @param specimenRP               the specimen rp
     */
    public ExperimentsController(MinioStorageService minioStorageService, RemoteScriptExecutor scriptExecutor, ExperimentFileRepository experimentFileRepository, SpecimenRP specimenRP) {
        this.minioStorageService = minioStorageService;
        this.scriptExecutor = scriptExecutor;
        this.experimentFileRepository = experimentFileRepository;
        this.specimenRP = specimenRP;
    }

    /**
     * Gets the process ID from a Process instance using reflection.
     * This method is specifically designed for UNIX-like systems where the
     * Process implementation is UNIXProcess. It accesses the private 'pid'
     * field via reflection to extract the process identifier.
     *
     * @param process the Process instance to examine
     * @return the process ID if successful, -1 if the PID cannot be determined
     * @throws SecurityException if reflection access is denied by the security manager
     * @implNote This implementation relies on internal Java implementation details
     * that are not part of the public API. It may break in future Java versions
     * or on non-UNIX platforms. Use primarily for debugging and logging purposes.
     * @apiNote Not recommended for production code or critical functionality due
     * to its fragile nature and dependency on internal implementation details.
     * Consider platform-specific alternatives or Java 9+'s ProcessHandle API
     * for production use cases.
     */
    private static long getProcessId(Process process) {
        try {
            Class<?> unixProcessClass = Class.forName("java.lang.UNIXProcess");
            if (unixProcessClass.isInstance(process)) {
                Field pidField = process.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                return pidField.getLong(process);
            }
        } catch (Exception e) {
            logger.error("Could not get process ID: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Handle alignment, save JSON to Minio and execute the remote Python script to start the alignment and return a response entity.
     * * This method handles the alignment process by uploading the fastq/fasta files and the JSON data containing the parameters for alignment.
     * * It saves the JSON data to Minio and executes a remote Python script to perform the alignment.
     *
     * @param fq1Files       the fastq/fasta 1 files
     * @param fq2Files       the fastq/fasta 2 files
     * @param jsonData       the json data with the parameters for alignment
     * @param experimentName the experiment name
     * @return the response entity
     * @throws FileNotFoundException the file not found exception
     * @implNote The method verifies the JSON data for errors before proceeding with the alignment.
     * @implSpec If the JSON data is valid, it uploads the fastq/fasta files and the JSON data to Minio.
     * * It then executes a remote Python script to perform the alignment and returns a response entity with the result.
     */
    @PostMapping("/alignment/save-json")
    public ResponseEntity<String> handleAlignment(@RequestParam("fq1Files") MultipartFile[] fq1Files, @RequestParam(value = "fq2Files", required = false) MultipartFile[] fq2Files, @RequestParam("jsonData") String jsonData, @RequestParam("experimentName") String experimentName) throws FileNotFoundException {
        ResponseEntity<String> responseEntity = ErrorHandler.verifyErrorsInJSON(jsonData, "Alignment");
        if (responseEntity.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            return responseEntity;
        }
        String pathToJSONInMinio = minioStorageService.uploadAlignmentFile(fq1Files, fq2Files, experimentName, jsonData);
        // Start script execution asynchronously
        try {
            scriptExecutor.executeRemotePythonScript(pathToJSONInMinio);
            logger.info("Python script execution started successfully");
            return ResponseEntity.ok("Alignment process started successfully");
        } catch (Exception e) {
            logger.error("Failed to start alignment process", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start alignment process: " + e.getMessage());
        }
    }

    /**
     * Handle post-processing, save JSON to Minio and execute the post-processing Docker script and return a response entity.
     * * This method handles the post-processing of aligned data by uploading the JSON data containing the parameters for post-processing.
     * * It saves the JSON data to Minio and executes a Docker script to perform the post-processing.
     *
     * @param metadataFileName the metadata file name
     * @param jsonData         the json data with the parameters for post-processing
     * @return the response entity
     * @throws IOException the io exception
     * @implNote The method verifies the JSON data for errors before proceeding with the post-processing.
     * @implSpec If the JSON data is valid, it uploads the JSON data to Minio.
     * * It then executes a Docker script to perform the post-processing and returns a response entity with the result.
     */
    @PostMapping("/postProcessing/save-json")
    public ResponseEntity<String> handlePostProcessing(@RequestParam("metadataFile") String metadataFileName, @RequestParam("jsonData") String jsonData) throws IOException {

        // Input validation
        ResponseEntity<String> responseEntity = ErrorHandler.verifyErrorsInJSON(jsonData, "PostProcessing");
        if (responseEntity.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            return responseEntity;
        }
        if (metadataFileName == null || metadataFileName.isEmpty()) {
            return ResponseEntity.badRequest().body("Metadata file is required");
        }

        // Upload files to Minio
        String experimentName = minioStorageService.uploadProcessFiles(jsonData, metadataFileName);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", PATH_TO_POST_PROCESSING_DOCKER_RUN_SCRIPT, experimentName);
            logger.info("Executing post-processing script for experiment: {}", experimentName);
            logger.info("Script path: {}", PATH_TO_POST_PROCESSING_DOCKER_RUN_SCRIPT);

            File logFile = new File("/tmp/post-processing-" + experimentName + ".log");
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            Process process = processBuilder.start();

            logger.info("Process started with PID: {}", getProcessId(process));
            logger.info("Process output will be logged to: {}", logFile.getAbsolutePath());

            // Check if process started (but this doesn't guarantee it will complete successfully)

            if (process.isAlive()) {
                // Start a thread to monitor the process completion
                new Thread(() -> {
                    try {
                        int exitCode = process.waitFor();
                        logger.info("Post-processing script completed for {} with exit code: {}", experimentName, exitCode);
                        if (exitCode != 0) {
                            logger.error("Post-processing script failed for {}. Check log file: {}", experimentName, logFile.getAbsolutePath());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("Process monitoring interrupted: {}", e.getMessage());
                    }
                }).start();

                return ResponseEntity.ok("JSON saved successfully to Minio and post-processing started in background");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start post-processing - process not alive");
            }
        } catch (IOException e) {
            logger.error("Failed to start post-processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start post-processing: " + e.getMessage());
        }
    }

    /**
     * Gets experiments list by experiment type from the experiment file repository and returns it as a response entity.
     * * This method retrieves a list of experiments based on the specified experiment type.
     * * It adds appropriate headers to the response to prevent caching and ensure that the data is always fresh.
     *
     * @param experimentType the experiment type
     * @return the experiments list by experiment type
     */
    @GetMapping("/experiment")
    public ResponseEntity<List<ExperimentResponse>> getExperimentsListByExperimentType(@RequestParam("experimentType") String experimentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CACHE_CONTROL, NO_CACHE_NO_STORE_MUST_REVALIDATE);
        headers.add(PRAGMA, NO_CACHE);
        headers.add(EXPIRES, "0");
        try {
            List<ExperimentFileEntity> results = experimentFileRepository.findByExperimentNameAndType(experimentType);
            return Helper.getExperimentsListResponseEntity(results, headers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Gets all experiments from the experiment file repository and returns it as a response entity.
     * * This method retrieves all experiments stored in the repository and formats them into a list of ExperimentResponse objects.
     * * It adds appropriate headers to the response to prevent caching and ensure that the data is always fresh.
     *
     * @return the all experiments
     */
    @GetMapping("/experiment/allExperiments")
    public ResponseEntity<List<ExperimentResponse>> getAllExperiments() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CACHE_CONTROL, NO_CACHE_NO_STORE_MUST_REVALIDATE);
        headers.add(PRAGMA, NO_CACHE);
        headers.add(EXPIRES, "0");
        try {
            List<ExperimentFileEntity> results = experimentFileRepository.getAll();

            return Helper.getExperimentsListResponseEntity(results, headers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Gets multiqc file from Minio storage service and returns it as a response entity.
     * * This method retrieves a MultiQC file based on the provided folder name and file name.
     * * It uses the MinioStorageService to access the file and returns it as an InputStreamResource.
     *
     * @param folderName the folder name
     * @param fileName   the file name
     * @return the multiqc file
     */
    @GetMapping("/experiment/result/multiqc/{folderName}/{fileName}")
    public ResponseEntity<InputStreamResource> getMultiqcFile(@PathVariable String folderName, @PathVariable String fileName) {
        return minioStorageService.getMultiqcFile(folderName, fileName);
    }

    /**
     * Gets raw matrix file from Minio storage service and returns it as a response entity.
     * * This method retrieves a raw matrix file based on the provided folder name and file name.
     * * It uses the MinioStorageService to access the file and returns it as an InputStreamResource.
     *
     * @param folderName the folder name
     * @return the raw matrix file
     */
    @GetMapping("/experiment/result/rawMatrix/{folderName}")
    public ResponseEntity<InputStreamResource> getRawMatrixFile(@PathVariable String folderName) {
        return minioStorageService.downloadAllMatricesAsZip(folderName);

    }

    /**
     * Gets result matrix for post-processing
     *
     * @param experimentName the experiment name
     * @return the processed matrix
     */
    @GetMapping("/experiment/result/postProcessing/matrix/{experimentName}")
    public ResponseEntity<InputStreamResource> getProcessedMatrix(@PathVariable String experimentName) {
        return minioStorageService.getProcessedMatrix(experimentName);
    }

    /**
     * Gets sample IDs by metadata file name from the Specimen resource provider and returns it as a response entity.
     * <p> This method retrieves sample IDs based on the provided metadata file name. <br>
     * It uses the Specimen resource provider to search for specimens and extract their official identifiers.
     *
     * @param metadataFileName the metadata file name
     * @return the sample IDs by metadata file name
     */
    @GetMapping("/experiment/Alignment/sampleIds")
    public ResponseEntity<List<String>> getSampleIdsByMetadataFileName(@RequestParam("metadataFileName") String metadataFileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CACHE_CONTROL, NO_CACHE_NO_STORE_MUST_REVALIDATE);
        headers.add(PRAGMA, NO_CACHE);
        headers.add(EXPIRES, "0");
        try {
            List<String> sampleIds = experimentFileRepository.getSampleIds(metadataFileName, this);
            return ResponseEntity.ok().headers(headers).body(sampleIds);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
