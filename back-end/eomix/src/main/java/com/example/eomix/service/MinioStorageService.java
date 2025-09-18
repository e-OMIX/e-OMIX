package com.example.eomix.service;

import com.example.eomix.exception.MinioStorageException;
import com.example.eomix.utils.Helper;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The type Minio storage service.
 */
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private JSONStorageService jsonStorageService;
    private StorageService storageService;

    @Value("${minio.bucket.name}")
    private String bucketNameForAlignment;

    @Value("${minio.bucket.post.processing.name}")
    private String bucketNameForPostProcessing;

    /**
     * Instantiates a new Minio storage service.
     *
     * @param minioClient        the minio client
     * @param jsonStorageService the json storage service
     * @param storageService     the storage service
     */
    @Autowired
    public MinioStorageService(MinioClient minioClient, JSONStorageService jsonStorageService,
                               StorageService storageService) {
        this.minioClient = minioClient;
        this.jsonStorageService = jsonStorageService;
        this.storageService = storageService;
    }

    /**
     * Upload json to minio.
     * * This method uploads a JSON file to a specified bucket and folder in MinIO.
     * * It creates a ByteArrayInputStream from the JSON data and uses the MinIO client to put the object in the
     * specified bucket.
     *
     * @param bucketName the bucket name
     * @param folderName the folder name
     * @param fileName   the file name
     * @param jsonData   the json data
     * @throws RuntimeException if there is an error during the upload process
     * @implNote The method constructs the object name by combining the folder name and file name. * It sets the
     * content type to "application/json" and uploads the JSON data as a stream.
     * @implNote This method is typically used to store configuration or metadata in JSON format in MinIO for later
     * retrieval or processing.
     * @implSpec After uploading, it prints a success message to the console.
     * @implSpec It is useful in scenarios where applications need to store structured data in a cloud storage
     * service like MinIO.
     */
    public void uploadJsonToMinio(String bucketName, String folderName, String fileName, String jsonData) {
        try {
            String objectName = folderName + "/" + fileName;
            ByteArrayInputStream jsonStream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
            putObjectInMinioClient(bucketName, objectName, jsonData.length(), "application/json", jsonStream);
            logger.info("JSON file uploaded successfully to MinIO: {}", objectName);

        } catch (Exception e) {
            throw new MinioStorageException("Error uploading JSON to MinIO", e);
        }
    }


    /**
     * Upload alignment file string.
     * * This method uploads alignment files to a specified bucket in MinIO.
     * * It handles both paired-end and single-end sequencing data by accepting two arrays of MultipartFile objects.
     *
     * @param fq1Files       the fq 1 files
     * @param fq2Files       the fq 2 files
     * @param experimentName the experiment name
     * @param jsonData       the json data
     * @return the string
     * @throws FileNotFoundException the file not found exception
     * @implNote The method generates a unique folder name based on the experiment name, aligner type, and current
     * date. * It uploads the fastq files (fq1 and fq2) to the specified folder in the bucket. * It also updates the
     * alignment JSON with the tags of the uploaded files and uploads the updated JSON to MinIO.
     * @implSpec The method retrieves metadata from CouchDB using the experiment name and saves it to the specified
     * bucket.
     */
    public String uploadAlignmentFile(MultipartFile[] fq1Files, MultipartFile[] fq2Files, String experimentName,
                                      String jsonData) throws FileNotFoundException {
        String fileName = "alignment_parameters.json";
        String createdAt = Helper.getDateString();
        String bucketName = bucketNameForAlignment;
        String aligner = Helper.getAlignerFromJson(jsonData);
        String folderName = Helper.generateFolderName(experimentName, aligner, createdAt);
        File metadataFile = storageService.getMetadataFileFromCouchDBByFileNameForMinio(experimentName);
        Map<String, String> fastq1Tags = uploadFastQFiles(fq1Files, folderName);
        if (fq2Files != null) {
            Map<String, String> fastq2Tags = uploadFastQFiles(fq2Files, folderName);
            String jsonUpdated = Helper.updateAlignmentJsonWithTags(jsonData, fastq1Tags, fastq2Tags, folderName);
            updateAndUploadJSON(experimentName, jsonUpdated, folderName, fileName, bucketName, createdAt);
        } else {
            String jsonUpdated = Helper.updateAlignmentJsonWithTags(jsonData, fastq1Tags, null, folderName);
            updateAndUploadJSON(experimentName, jsonUpdated, folderName, fileName, bucketName, createdAt);
        }
        saveFileOnBucket(metadataFile, bucketName, folderName);
        return folderName + "/" + fileName;
    }

    /**
     * Upload process files.
     * * This method uploads post-processing files to a specified bucket in MinIO.
     * * It accepts a MultipartFile for the post-processing file, a JSON string containing process parameters, and a
     * metadata file name.
     *
     * @param jsonData         the json data
     * @param metadataFileName the metadata file name
     * @throws IOException the io exception if there is an error during file upload or JSON processing
     * @implNote The method generates a unique folder name based on the metadata file name and the current date. * It
     * uploads the post-processing file to the specified folder in the bucket and updates the JSON data with the tag
     * of the uploaded file. * * It then uploads the updated JSON to MinIO and saves the metadata file from CouchDB
     * to the specified bucket.
     * @implNote This method is typically used to handle post-processing steps in data analysis pipelines, such as
     * saving parameters for further processing or analysis.
     * @implSpec The method uses the storage service to retrieve the metadata file from CouchDB and save it to MinIO.
     * @implSpec It is useful in scenarios where applications need to store post-processing parameters and results in
     * a cloud storage service like MinIO.
     */
    public String uploadProcessFiles(String jsonData, String metadataFileName) throws IOException {
        String fileName = "post-processing_parameters.json";
        String createdAt = Helper.getDateString();
        String safeFileName = metadataFileName.replace("/", "_");
        String folderName = Helper.generateFolderName(safeFileName, createdAt);
        File file = storageService.getMetadataFileFromCouchDBByFileNameForMinio(metadataFileName);
        String jsonUpdated = Helper.updateProcessJsonWithTags(jsonData, folderName);
        updateAndUploadJSON(metadataFileName, jsonUpdated, folderName, fileName, bucketNameForPostProcessing,
                createdAt);
        saveFileOnBucket(file, bucketNameForPostProcessing, folderName);
        return folderName;
    }

    /**
     * Update and upload json.
     * * This method updates a JSON file with new data and uploads it to a specified bucket in MinIO.
     * * It saves the updated JSON data to CouchDB and then uploads it to the MinIO bucket with the specified folder
     * and file name.
     *
     * @param metadataFileName the metadata file name
     * @param jsonUpdated      the json updated
     * @param folderName       the folder name
     * @param fileName         the file name
     * @param bucketName       the bucket name
     * @param createdAt        the created at timestamp
     * @implNote The method uses the JSONStorageService to save the JSON data to CouchDB and update the JSON string
     * with a unique ID.
     * * It then uploads the updated JSON to MinIO using the uploadJsonToMinio method.
     */
    private void updateAndUploadJSON(String metadataFileName, String jsonUpdated, String folderName, String fileName,
                                     String bucketName, String createdAt) {
        String jsonUpdatedWithID = jsonStorageService.saveJsonDataToCouchDBAndUpdateJSON(jsonUpdated,
                metadataFileName, createdAt);
        uploadJsonToMinio(bucketName, folderName, fileName, jsonUpdatedWithID);

    }


    /**
     * Save file on bucket.
     * * This method saves a file to a specified bucket in MinIO.
     * * It constructs the object name by combining the folder name and the file name,
     * * and uploads the file as a stream to the MinIO bucket.
     *
     * @param file       the file
     * @param bucketName the bucket name
     * @param folderName the folder name
     * @throws MinioStorageException if there is an error during the file upload process
     * @implNote The method uses the MinIO client to put the object in the specified bucket with the content type
     * determined by probing the file. * * If the content type cannot be determined, it defaults to
     * "application/octet-stream". * @implSpec After uploading, it handles any exceptions that may occur during the
     * file upload process.
     */
    public void saveFileOnBucket(File file, String bucketName, String folderName) {
        String objectName = folderName + "/" + file.getName();
        try (InputStream is = new FileInputStream(file)) {
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            putObjectInMinioClient(bucketName, objectName, file.length(), contentType, is);
        } catch (Exception e) {
            throw new MinioStorageException("Failed to store file " + file.getName(), e);
        }
    }

    /**
     * Put object in minio client.
     * * This method uploads an object to a specified bucket in MinIO using the MinIO client.
     * * It constructs the PutObjectArgs with the bucket name, object name, input stream, content type, and object size.
     *
     * @param bucketName  the bucket name
     * @param objectName  the object name
     * @param objectSize  the object size
     * @param contentType the content type
     * @param is          the input stream
     * @throws ErrorResponseException    if there is an error response from MinIO
     * @throws InsufficientDataException if there is insufficient data in the response
     * @throws InternalException         if there is an internal error in the MinIO client
     * @throws InvalidKeyException       if the provided key is invalid
     * @throws InvalidResponseException  if the response from MinIO is invalid
     * @throws IOException               if there is an I/O error during the operation
     * @throws NoSuchAlgorithmException  if the specified algorithm is not available
     * @throws ServerException           if there is a server error in the MinIO client
     * @throws XmlParserException        if there is an error parsing XML in the response
     * @implNote The method uses the MinIO client to put the object in the specified bucket with the provided
     * parameters.
     * @implSpec It handles various exceptions that may occur during the upload process, such as
     * ErrorResponseException, InsufficientDataException, etc.
     */
    private void putObjectInMinioClient(String bucketName, String objectName, long objectSize, String contentType,
                                        InputStream is) throws ErrorResponseException, InsufficientDataException,
            InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException,
            ServerException, XmlParserException {
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(is, objectSize,
                -1).contentType(contentType).build());
    }

    /**
     * Upload fast q files map.
     * * This method uploads multiple FASTQ files to a specified folder in a MinIO bucket.
     * * It iterates over an array of MultipartFile objects, saves each file to the bucket, and collects their tags
     * in a map.
     *
     * @param multipartFilesList the multipart files list
     * @param folderName         the folder name
     * @return the map
     * @throws MinioStorageException if there is an error during the file upload process
     * @implNote The method uses the saveMultipartFileOnBucket method to handle the actual file upload and tag
     * generation. * * It constructs the object name by combining the folder name and the original file name for each
     * file. * @implSpec After uploading all files, it returns a map containing the original file names as keys and
     * their corresponding tags as values.
     * @implNote This method is typically used to handle bulk uploads of sequencing data files, such as FASTQ files,
     * in a cloud storage service like MinIO. * * It is useful in scenarios where applications need to upload
     * multiple files at once, such as in bioinformatics pipelines or data processing workflows.
     */
    public Map<String, String> uploadFastQFiles(MultipartFile[] multipartFilesList, String folderName) {
        Map<String, String> allTags = new HashMap<>();
        for (MultipartFile file : multipartFilesList) {
            Map<String, String> tags = saveMultipartFileOnBucket(file, bucketNameForAlignment, folderName);
            allTags.putAll(tags);
        }
        return allTags;
    }

    /**
     * Save multipart file on bucket map.
     * * This method saves a multipart file to a specified bucket in MinIO and generates a unique tag for the file.
     * * It constructs the object name by combining the folder name and the original file name, and uploads the file
     * as a stream to the MinIO bucket.
     *
     * @param file       the multipart file
     * @param bucketName the bucket name
     * @param folderName the folder name
     * @return the map containing tags for the uploaded file
     * @throws MinioStorageException if there is an error during the file upload process
     * @implNote The method uses the MinIO client to put the object in the specified bucket with the content type of
     * the file.
     * * It sets the object tags in MinIO after uploading.
     * @implSpec If an error occurs during the upload process, it throws a MinioStorageException with an appropriate
     * message.
     */
    private Map<String, String> saveMultipartFileOnBucket(MultipartFile file, String bucketName, String folderName) {
        Map<String, String> tags = generateTag(file.getOriginalFilename());
        String fileName = file.getOriginalFilename();
        String objectName = folderName + "/" + fileName;
        try (InputStream is = file.getInputStream()) {
            putObjectInMinioClient(bucketName, objectName, file.getSize(), file.getContentType(), is);
            minioClient.setObjectTags(SetObjectTagsArgs.builder().bucket(bucketName).object(objectName).tags(tags).build());
            logger.info("File {} uploaded successfully to bucket {}", fileName, bucketName);
        } catch (Exception e) {
            throw new MinioStorageException("Failed to upload file " + fileName, e);
        }
        return tags;
    }


    /**
     * Generate tag map.
     * * This method generates a unique tag for a given file name using a UUID.
     * * It creates a map with the file name as the key and the generated UUID as the value.
     *
     * @param fileName the file name
     * @return the map containing the file name and its corresponding tag
     * @implNote The method uses UUID.randomUUID() to generate a unique identifier for the file.
     * @implSpec This is typically used to create unique identifiers for files in cloud storage systems like MinIO.
     */
    private Map<String, String> generateTag(String fileName) {
        String tag = UUID.randomUUID().toString();
        Map<String, String> tags = new HashMap<>();
        tags.put(fileName, tag);
        return tags;
    }


    /**
     * Gets processed matrix.
     * * This method retrieves a processed matrix file from a specified folder in a MinIO bucket.
     * * It constructs the object name by combining the folder name and the matrix file name,
     * * and uses the MinIO client to get the object as a stream.
     *
     * @param folderName the folder name
     * @return the response entity containing the InputStreamResource of the matrix file
     * @implNote The method sets the content type to "application/octet-stream" for the matrix file. * * It returns a
     * ResponseEntity containing the InputStreamResource of the file, along with appropriate headers for content
     * disposition. * @implSpec If an error occurs during the retrieval process, it returns a 404 Not Found response.
     */
    public ResponseEntity<InputStreamResource> getProcessedMatrix(String folderName) {
        try {
            String matrix = folderName + "_postprocessed.rds";
            String objectName = folderName + "/" + matrix;
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return getFileFromMinio(matrix, objectName, contentType, bucketNameForPostProcessing);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download all matrices as zip response entity.
     * * This method downloads all matrix files from a specified folder in a MinIO bucket,
     * * compresses them into a zip file, and returns the zip file as a ResponseEntity.
     *
     * @param folderName the folder name
     * @return the response entity containing the InputStreamResource of the zip file
     * @implNote The method lists all sample folders in the specified results_alignment directory,
     * * checks for the existence of the matrix file (quants_mat.mtx) in each sample folder,
     * * and adds it to the zip output stream if it exists.
     * * It then prepares the response with appropriate headers for content disposition and content type.
     * @implSpec If no matrix files are found, it returns a 404 Not Found response.
     * @implSpec If an error occurs during the process, it returns a 500 Internal Server Error response.
     */
    public ResponseEntity<InputStreamResource> downloadAllMatricesAsZip(String folderName) {
        try {
            String basePath = folderName + "/results_alignment/";

            // List all sample folders
            ListObjectsArgs listArgs =
                    ListObjectsArgs.builder().bucket(bucketNameForAlignment).prefix(basePath).recursive(false).build();

            // Create zip in memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(baos);

            // Process each sample folder
            for (Result<Item> itemResult : minioClient.listObjects(listArgs)) {
                Item item = itemResult.get();
                if (item.isDir()) {
                    String sampleFolder = item.objectName();
                    String sampleName = sampleFolder.substring(basePath.length(), sampleFolder.length() - 1);
                    String matrixFilePath = sampleFolder + "af_quant/alevin/quants_mat.mtx";
                    // Check if the matrix file exists
                    downloadZipFile(matrixFilePath, sampleName, zipOut);
                }
            }

            zipOut.finish();
            zipOut.close();

            // Check if zip is empty
            if (baos.size() == 0) {
                return ResponseEntity.notFound().build();
            }

            // Prepare response
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            InputStreamResource resource = new InputStreamResource(bais);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"matrices" +
                    ".zip\"").contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(baos.size()).body(resource);

        } catch (Exception e) {
            logger.error("Failed to create matrices zip", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download zip file.
     * * This method checks if a specific matrix file exists in a MinIO bucket and, if it does, downloads the file
     * and adds it to a zip output stream.
     *
     * @param matrixFilePath the matrix file path
     * @param sampleName     the sample name
     * @param zipOut         the zip output stream
     * @throws InsufficientDataException if there is insufficient data in the response
     * @throws InternalException         if there is an internal error in the MinIO client
     * @throws InvalidKeyException       if the provided key is invalid
     * @throws InvalidResponseException  if the response from MinIO is invalid
     * @throws IOException               if there is an I/O error during the operation
     * @throws NoSuchAlgorithmException  if the specified algorithm is not available
     * @throws ServerException           if there is a server error in the MinIO client
     * @throws XmlParserException        if there is an error parsing XML in the response
     * @throws ErrorResponseException    if there is an error response from MinIO
     * @implNote The method first checks for the existence of the matrix file using the statObject method.
     * * If the file exists, it retrieves the file as an input stream and adds it to the provided zip output stream
     * with a structured entry name.
     * * If the file does not exist, it simply skips adding it to the zip.
     * @implSpec This method is useful for creating zip archives of multiple files stored in MinIO, especially when
     * some files may not be present.
     */
    private void downloadZipFile(String matrixFilePath, String sampleName, ZipOutputStream zipOut) throws InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException, ErrorResponseException {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketNameForAlignment).object(matrixFilePath).build());

            // If exists, download and add to zip
            try (InputStream fileStream =
                         minioClient.getObject(GetObjectArgs.builder().bucket(bucketNameForAlignment).object(matrixFilePath).build())) {

                // Create zip entry path: sampleName/quants_mat.mtx
                String entryName = sampleName + "/quants_mat.mtx";
                zipOut.putNextEntry(new ZipEntry(entryName));

                IOUtils.copy(fileStream, zipOut);
                zipOut.closeEntry();
            }
        } catch (ErrorResponseException e) {
            if (!e.errorResponse().code().equals("NoSuchKey")) {
                throw e; // Re-throw if it's not a "file not found" error
            }
        }
    }

    /**
     * Gets multiqc file.
     * * This method retrieves a MultiQC file from a specified folder in a MinIO bucket.
     * * It constructs the object name by combining the folder name and the file name,
     * * and uses the MinIO client to get the object as a stream.
     *
     * @param folderName the folder name
     * @param fileName   the file name
     * @return the multiqc file
     * @implNote The method sets the content type to "text/html" for the MultiQC file. * * It returns a
     * ResponseEntity containing the InputStreamResource of the file, along with appropriate headers for content
     * disposition. * @implSpec If an error occurs during the retrieval process, it returns a 404 Not Found response.
     */
    public ResponseEntity<InputStreamResource> getMultiqcFile(String folderName, String fileName) {
        try {
            String objectName = folderName + "/results_alignment/multiqc/" + fileName;
            String contentType = MediaType.TEXT_HTML_VALUE;
            return getResultFile(fileName, objectName, contentType);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * * Gets result file.
     * * This method retrieves a result file from a specified object in a MinIO bucket.
     * * It constructs the object name using the provided file name and object name,
     * * and uses the MinIO client to get the object as a stream.
     *
     * @param fileName    the file name
     * @param objectName  the object name
     * @param contentType the content type
     * @return the response entity containing the InputStreamResource of the file
     * @throws ErrorResponseException    if there is an error response from MinIO
     * @throws InsufficientDataException if there is insufficient data in the response
     * @throws InternalException         if there is an internal error in the MinIO client
     * @throws InvalidKeyException       if the provided key is invalid
     * @throws InvalidResponseException  if the response from MinIO is invalid
     * @throws IOException               if there is an I/O error during the operation
     * @throws NoSuchAlgorithmException  if the specified algorithm is not available
     * @throws ServerException           if there is a server error in the MinIO client
     * @throws XmlParserException        if there is an error parsing XML in the response
     * @implNote The method sets the content type based on the provided content type parameter.
     * * * It returns a ResponseEntity containing the InputStreamResource of the file, along with appropriate headers
     * for content disposition.
     * @implSpec If an error occurs during the retrieval process, it throws various exceptions such as
     * ErrorResponseException, InsufficientDataException, etc.
     * *
     * @implNote This method is typically used to retrieve files such as CSV or other result files stored in MinIO.
     * @implSpec It is useful in scenarios where applications need to access and download result files from a cloud
     * storage service like MinIO.
     */
    private @NotNull ResponseEntity<InputStreamResource> getResultFile(String fileName, String objectName,
                                                                       String contentType) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        return getFileFromMinio(fileName, objectName, contentType, bucketNameForAlignment);
    }

    @NotNull
    private ResponseEntity<InputStreamResource> getFileFromMinio(String fileName, String objectName,
                                                                 String contentType, String bucketName) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        InputStream stream =
                minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
        HttpHeaders headers = new HttpHeaders();
        if (fileName.endsWith(".csv")) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        } else {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).headers(headers).body(new InputStreamResource(stream));
    }

}
