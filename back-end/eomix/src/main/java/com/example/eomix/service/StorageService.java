package com.example.eomix.service;


import com.example.eomix.entities.MetadataFileUploadEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * The interface Storage service.
 *
 * @author Molka Anaghim FTOUHI
 */
public interface StorageService {

    /**
     * Store on couch db and return response entity.
     * <p> This method is used to store a file on CouchDB and return a response entity.
     *
     * @param file              the csv file passed as a MultipartFile
     * @param detectedDelimiter the detected delimiter in the csv file
     * @return the response entity
     * @implNote The method handles the storage of a file in CouchDB, allowing for efficient data management and
     * retrieval.
     * @implSpec <ul>
     * <li> The response entity typically contains information about the success or failure of the storage operation,
     * such as status codes and messages.</li>
     * <li> This method is useful for applications that require persistent storage of files, such as CSV files, in a
     * database system like CouchDB.</li>
     * <li> The detected delimiter parameter is particularly useful when dealing with CSV files, as it allows the
     * service to correctly parse the file contents based on the identified delimiter.</li>
     * </ul>
     */
    ResponseEntity<String> storeOnCouchDb(MultipartFile file, String detectedDelimiter);

    /**
     * Convert to csv string.
     * <p>
     * This method is used to convert the document stored as list of MetadataFileUploadEntity objects in the database
     * to a CSV string.
     * <p>
     * It takes a list of MetadataFileUploadEntity objects, which contain metadata about files uploaded to the system,
     * and formats this information into a CSV string representation.
     * This is particularly useful for generating reports or exporting data in a format that can be easily consumed
     * by other applications or tools.
     *
     * @param documents the documents
     * @return the string
     * @implNote This method iterates through the list of MetadataFileUploadEntity objects,
     * formats each entity's attributes into a CSV format, and concatenates them into a single string.
     * It handles the conversion of various data types to their string representations,
     * allowing for a flexible and dynamic generation of CSV content.
     * @implSpec <ul>
     * <li> The method ensures that the resulting CSV string is properly formatted, with each entity's attributes
     * separated by tabulation and each entity on a new line.</li>
     * <li> It can handle different types of metadata, such as file names, upload dates, and other relevant
     * information stored in the MetadataFileUploadEntity objects.</li>
     * <li> This method is particularly useful for applications that need to export metadata for analysis, reporting,
     * or integration with other systems.</li>
     * </ul>
     */
    String convertToCSV(List<MetadataFileUploadEntity> documents);

    /**
     * Gets metadata file from couch db by file name.
     * <p> This method is used to retrieve a metadata file from CouchDB by its filename.
     *
     * @param filename the filename
     * @return File : the metadata file from couch db by file name
     * @throws FileNotFoundException the file not found exception
     */
    File getMetadataFileFromCouchDBByFileName(String filename) throws FileNotFoundException;

    /**
     * Gets metadata file from couch db by file name for minio.
     * <p> This method is used to retrieve a metadata file from CouchDB by its filename specifically for MinIO storage.
     * <p> This method is specifically designed to handle filenames that may contain characters
     * that are not safe for Minio storage.
     *
     * @param filename the filename
     * @return the metadata file from couch db by file name for minio
     * @throws FileNotFoundException the file not found exception
     * @implNote This method is designed to work with files stored in MinIO, allowing retrieval of metadata files
     * that are managed in a CouchDB instance but stored in MinIO.
     * * @implSpec <ul>
     * <li> It ensures that the filename is properly sanitized or encoded to be compatible with MinIO's storage
     * requirements.</li>
     * <li> The method retrieves the file from CouchDB, which may involve querying the database for the specific file
     * entry based on the provided filename.</li>
     * <li> If the file is not found, it throws a FileNotFoundException to indicate that the requested file does not
     * exist in the database.</li>
     * <li> This method is useful for applications that need to access metadata files stored in a CouchDB instance
     * while ensuring compatibility with MinIO's storage system.</li>
     * </ul>
     */
    File getMetadataFileFromCouchDBByFileNameForMinio(String filename) throws FileNotFoundException;
}