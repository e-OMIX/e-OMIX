package com.example.eomix.controller;

import com.example.eomix.repositories.CsvDocumentRepository;
import com.example.eomix.repositories.MetadataFileRepository;
import com.example.eomix.service.FhirServiceImplementation;
import com.example.eomix.service.FileSystemStorageService;
import com.example.eomix.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.eomix.utils.Constants.*;


/**
 * Upload file controller.
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/public")
public class MetadataController {

    /**
     * The class log.
     */
    private static final Logger logger = LoggerFactory.getLogger(MetadataController.class);

    private final StorageService storageService;
    private final MetadataFileRepository fileUploadRepository;
    private final CsvDocumentRepository csvDocumentRepository;
    private final FhirServiceImplementation fhirServiceImplementation;

    /**
     * Instantiates a new Upload file controller.
     *
     * @param storageService            the storage service
     * @param fileUploadRepository      the file upload repository
     * @param csvDocumentRepository     the csv document repository
     * @param fhirServiceImplementation the fhir service implementation
     */
    @Autowired
    public MetadataController(StorageService storageService, MetadataFileRepository fileUploadRepository, CsvDocumentRepository csvDocumentRepository, FhirServiceImplementation fhirServiceImplementation) {
        this.storageService = storageService;
        this.fileUploadRepository = fileUploadRepository;
        this.csvDocumentRepository = csvDocumentRepository;
        this.fhirServiceImplementation = fhirServiceImplementation;
    }

    /**
     * Handle file upload and store it on CouchDB and return a response entity.
     * * This method handles the file upload, detects the delimiter, and stores the file on CouchDB.
     * * It also saves the file metadata in the CSV document repository.
     *
     * @param detectedDelimiter  the detected delimiter
     * @param file               the file "sample metadata.csv"
     * @param redirectAttributes the redirect attributes
     * @return the response entity
     * @throws IOException the io exception
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> handleMetadataFileUpload(@RequestParam("detectedDelimiter") String detectedDelimiter, @RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) throws IOException {
        long startTime = System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        sdf.format(startTime);
        logger.info("start Time {} ms", startTime);
        Map<String, String> mapResponse = new HashMap<>();
        ResponseEntity<String> responseEntity = storageService.storeOnCouchDb(file, detectedDelimiter);
        if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            csvDocumentRepository.addCsvDocument(file);
            redirectAttributes.addFlashAttribute(MESSAGE, "You successfully uploaded " + file.getOriginalFilename() + "!");
            logger.info("You successfully uploaded  {}", file.getOriginalFilename());
            mapResponse.put(MESSAGE, "File uploaded successfully");
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.info("timeTaken : {} ms", duration);
            mapResponse.put("timeTaken : ", duration + " ms");
            return ResponseEntity.ok(mapResponse);
        } else {
            mapResponse.put(MESSAGE, responseEntity.getBody());
            return ResponseEntity.status(responseEntity.getStatusCode()).body(mapResponse);
        }
    }


    /**
     * Export sample metadata as csv and return a response entity.
     * * This method exports the sample metadata as a CSV file.
     * * It retrieves the CSV file from the repository based on the provided filename and returns it as a response entity.
     *
     * @param filename the filename for the CSV file
     * @return the response entity
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSampleMetadataAsCsv(@RequestParam("filename") String filename) {
        try {

            byte[] csvBytes = csvDocumentRepository.getCsv(filename);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "_export.csv\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");
            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(("Error generating CSV: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Save fhir resource on server and return a response entity.
     * * This method saves FHIR resources on the server by storing them in the database.
     * * It takes a filename as a request parameter, retrieves the FHIR resources from the specified file,
     * * and stores them in the database using the FhirServiceImplementation.
     *
     * @param filename the filename
     * @return the response entity
     */
    @PostMapping("/resourceFHIR")
    public ResponseEntity<Object> saveFHIRResourceOnServer(@RequestParam("filename") String filename) {
        try {
            fhirServiceImplementation.storeAllResources(filename);
            return ResponseEntity.status(HttpStatus.CREATED).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

    /**
     * Gets file metadata from the file upload repository and returns it as a response entity.
     * * This method retrieves all file metadata from the file upload repository and formats it into a list of maps.
     * * It adds appropriate headers to the response to prevent caching and ensure that the data is always fresh.
     *
     * @return the file metadata
     * @implNote The method uses the FileSystemStorageService to convert the JsonNode results into a list of maps.
     * @implSpec If an exception occurs while retrieving the file metadata, it returns a response entity with an internal server error status.
     */
    @GetMapping("/files/allMetadataFiles")
    public ResponseEntity<List<Map<String, Object>>> getFileMetadata() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CACHE_CONTROL, NO_CACHE_NO_STORE_MUST_REVALIDATE);
        headers.add(PRAGMA, NO_CACHE);
        headers.add(EXPIRES, "0");
        try {
            List<JsonNode> results = fileUploadRepository.findAllMetadataFiles();
            List<Map<String, Object>> fileMetadataList = FileSystemStorageService.getAllMetadataFromJsonNodes(results);
            return ResponseEntity.ok().headers(headers).body(fileMetadataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Gets file metadata by filename from the file upload repository and returns it as a response entity.
     * * This method retrieves file metadata based on the provided filename and formats it into a list of maps.
     * * It adds appropriate headers to the response to prevent caching and ensure that the data is always fresh.
     *
     * @param filename the filename
     * @return the file metadata by filename
     */
    @GetMapping("/files/metadata")
    public ResponseEntity<List<Map<String, Object>>> getFileMetadataByFilename(@RequestParam("filename") String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CACHE_CONTROL, NO_CACHE_NO_STORE_MUST_REVALIDATE);
        headers.add(PRAGMA, NO_CACHE);
        headers.add(EXPIRES, "0");
        try {
            List<JsonNode> results = fileUploadRepository.findFileMetadataByFilename(filename);
            List<Map<String, Object>> fileMetadataList = FileSystemStorageService.getAllMetadataFromJsonNodes(results);
            return ResponseEntity.ok().headers(headers).body(fileMetadataList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Delete metadata by filename and completely remove the document from the repository.
     * * This method deletes all documents associated with the specified filename from the CSV document repository
     * * and the file upload repository.
     *
     * @param filename the filename
     * @return the response entity
     */
    @DeleteMapping("/delete/metadata")
    public ResponseEntity<String> deleteMetadataByFilename(@RequestParam("filename") String filename) {
        try {
            csvDocumentRepository.completelyRemoveDocument(filename);
            fileUploadRepository.deleteAllByFilename(filename);

            return ResponseEntity.ok("Deleted all documents with filename: " + filename);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting documents: " + e.getMessage());
        }
    }


}
