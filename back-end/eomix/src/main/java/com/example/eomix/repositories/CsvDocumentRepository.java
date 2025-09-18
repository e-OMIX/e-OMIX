package com.example.eomix.repositories;

import com.example.eomix.entities.CsvDocument;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The type Csv document repository.
 */
@Repository
public class CsvDocumentRepository extends CouchDbRepositorySupport<CsvDocument> {

    private static final Logger logger = LoggerFactory.getLogger(CsvDocumentRepository.class);

    /**
     * Instantiates a new Csv document repository.
     * * This constructor initializes the repository with the specified CouchDbConnector.
     * * It calls the superclass constructor with the CsvDocument class type and the provided CouchDbConnector.
     * * * It also initializes the standard design document for the repository.
     *
     * @param db the db
     * @implNote The design document is used to define views and indexes for the repository.
     * couchDbConnector3 : attachment database
     */
    public CsvDocumentRepository(@Qualifier("couchDbConnector3") CouchDbConnector db) {
        super(CsvDocument.class, db);
        initStandardDesignDocument();
    }

    /**
     * Add csv document.
     * * This method adds a CSV document to the CouchDB database.
     * * It checks if the file is not empty, retrieves the input stream and content type,
     * * and creates an attachment input stream with the file name, input stream, and content type.
     * * * The method then stores the attachment in the database using the file's original filename.
     *
     * @param file the file
     * @throws IOException the io exception
     * @implNote The method logs the process of storing the document and the attachment ID.
     */
    public void addCsvDocument(MultipartFile file) throws IOException {

        if (!file.isEmpty()) {
            logger.info("Storing document in CouchDB: {}", file.getOriginalFilename());
            InputStream inputStream = file.getInputStream();
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();
            AttachmentInputStream attachment = new AttachmentInputStream(
                    fileName, inputStream, contentType);
            db.createAttachment(file.getOriginalFilename(), attachment);
            logger.info("Attachment added to document with ID: {}", file.getOriginalFilename());
        }
    }

    /**
     * Get csv byte [ ].
     * * This method retrieves a CSV file from the CouchDB database by its filename.
     * * It queries the database for a document with the specified filename,
     * * retrieves the attachment associated with that document,
     * * and returns the content of the attachment as a byte array.
     * * The method uses a view query to find the document by its filename,
     * * includes the document in the result, and limits the result to one document.
     *
     * @param filename the filename
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    @SuppressWarnings("unchecked")
    public byte[] getCsv(String filename) throws IOException {

        ViewQuery query = new ViewQuery()
                .designDocId("_design/csv_docs")
                .viewName("by_filename")
                .key(filename)
                .includeDocs(true)
                .limit(1);

        List<Map<String, Object>> result = (List<Map<String, Object>>) (List<?>) db.queryView(query, Map.class);
        String attachmentId = (String) result.get(0).get("_id");
        AttachmentInputStream attachmentStream = db.getAttachment(attachmentId, filename);
        return attachmentStream.readAllBytes();
    }

    /**
     * Completely remove document.
     * * This method deletes a document and all its attachments from the CouchDB database.
     *
     * @param documentId the document id
     */
    @SuppressWarnings("unchecked")
    public void completelyRemoveDocument(String documentId) {
        try {
            Map<String, Object> doc = db.find(Map.class, documentId);
            db.delete(doc);
            logger.info("Document and all attachments completely removed: {}", documentId);
        } catch (DocumentNotFoundException e) {
            logger.warn("Document not found for complete removal: {}", documentId);
        } catch (Exception e) {
            logger.error("Error during complete removal of document {}: {}", documentId, e.getMessage());
        }
    }
}
