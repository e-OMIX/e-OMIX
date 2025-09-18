package com.example.eomix.repositories;

import com.example.eomix.entities.MetadataFileUploadEntity;
import com.example.eomix.exception.MetadataFileUploadException;
import com.fasterxml.jackson.databind.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * The type File repository.
 */
@Repository
public class MetadataFileRepository extends CouchDbRepositorySupport<MetadataFileUploadEntity> {

    /**
     * The constant DESIGN_FILE_UPLOAD_ENTITY.
     */
    public static final String DESIGN_FILE_UPLOAD_ENTITY = "_design/MetadataFileUploadEntity";

    /**
     * Instantiates a new File repository.
     * This constructor initializes the repository with the specified CouchDbConnector.
     * It calls the superclass constructor with the MetadataFileUploadEntity class type and the provided CouchDbConnector.
     * It also initializes the standard design document for the repository.
     *
     * @param db the db
     * @implNote The design document is used to define views and indexes for the repository.
     * couchDbConnector1 : file upload database eomix
     */
    public MetadataFileRepository(@Qualifier("couchDbConnector1") CouchDbConnector db) {
        super(MetadataFileUploadEntity.class, db);
        initStandardDesignDocument();
    }

    /**
     * Finds all metadata file uploads matching the given filename.
     *
     * <p>This method queries CouchDB using a view lookup with the filename as the key,
     * returning complete document data for all matching records.</p>
     *
     * <p>Typical use cases include:</p>
     * <ul>
     *   <li>Retrieving all uploads of a specific file</li>
     *   <li>Processing file-related metadata</li>
     * </ul>
     *
     * @param filename the exact filename to search for
     * @return list of matching MetadataFileUploadEntity objects (may be empty)
     * @implNote Uses CouchDB's view mechanism for efficient querying of large datasets
     * @implSpec Returns empty list if no matches found (never null)
     */
    public List<MetadataFileUploadEntity> findByFilename(String filename) {
        return queryView("by_filename", filename);
    }

    /**
     * Retrieves aggregated file metadata from CouchDB without loading full documents.
     *
     * <p>This method queries a CouchDB view to efficiently retrieve file metadata summaries.
     * The results are grouped and returned as JsonNode objects containing only metadata,
     * avoiding the overhead of loading complete documents.</p>
     *
     * @return List of JsonNode objects containing file metadata (maybe empty)
     * @implNote Uses CouchDB's view mechanism for efficient large dataset handling
     * @implSpec Returns empty list if no results found (never null)
     */
    public List<JsonNode> findAllMetadataFiles() {
        ViewQuery query = new ViewQuery().designDocId(DESIGN_FILE_UPLOAD_ENTITY).viewName("file_metadata").group(true).includeDocs(false);
        return db.queryView(query).getRows().stream().map(ViewResult.Row::getValueAsNode).toList();
    }

    /**
     * Deletes all documents associated with the specified filename from CouchDB.
     *
     * <p>This method performs a complete cleanup by:</p>
     * <ul>
     *   <li>Finding all documents matching the filename</li>
     *   <li>Deleting them in bulk operations</li>
     *   <li>Repeating until no matching documents remain</li>
     * </ul>
     *
     * @param filename the filename of documents to delete
     * @implNote Uses a while loop to ensure complete deletion of all matching documents
     * @implSpec Operates as a void method with no return value
     */
    public void deleteAllByFilename(String filename) {
        while (true) {
            List<MetadataFileUploadEntity> docsToDelete = getFileUploadEntitiesByFileName(filename);
            if (docsToDelete.isEmpty()) {
                break;
            }
            db.executeBulk(docsToDelete);
        }
    }

    /**
     * Gets file upload entities by file name.
     * * This method retrieves a list of MetadataFileUploadEntity objects based on the specified filename.
     * * It constructs a ViewQuery to query the CouchDB database using the specified design document and view name.
     * * The key for the query is set to the filename, and the limit is set to 10000 to retrieve a maximum of 10000 results.
     *
     * @param filename the filename
     * @return the list
     * @implNote The method uses the db to query the view and return a list of MetadataFileUploadEntity objects.
     * @implSpec The method is designed to return all documents that match the specified filename.
     * @implNote The method is useful for retrieving all file upload entities associated with a specific filename.
     * * It is typically used in scenarios where you need to fetch all file uploads related to a specific filename
     * * for further processing or analysis.
     */
    private @NotNull List<MetadataFileUploadEntity> getFileUploadEntitiesByFileName(String filename) {
        ViewQuery query = new ViewQuery().designDocId(DESIGN_FILE_UPLOAD_ENTITY).viewName("by_filename").key(filename).limit(10000);

        return db.queryView(query).getRows().stream().map(row -> {
            MetadataFileUploadEntity doc = new MetadataFileUploadEntity();
            doc.setId(row.getId());
            doc.setRevision(row.getValueAsNode().get("_rev").asText());
            return doc;
        }).toList();
    }

    /**
     * Performs bulk insertion of file metadata documents into CouchDB.
     *
     * <p>This method efficiently inserts multiple documents in a single operation,
     * significantly improving performance for batch operations.</p>
     *
     * <p>Typical use cases:</p>
     * <ul>
     *   <li>Initial data loading</li>
     *   <li>Batch file processing</li>
     *   <li>Mass file upload operations</li>
     * </ul>
     *
     * @param documents list of MetadataFileUploadEntity objects to insert
     * @implNote Uses CouchDB's bulk operation API for optimal performance
     * @implSpec This is a void operation with no return value
     */
    public void bulkAdd(List<MetadataFileUploadEntity> documents) {
        try {
            List<Object> docsToSave = new ArrayList<>(documents);
            db.executeBulk(docsToSave);
        } catch (Exception e) {
            throw new MetadataFileUploadException("Failed to add MetadataFile documents as bulk", e);
        }
    }

    /**
     * Retrieves file metadata for documents matching the specified filename.
     *
     * <p>This method queries CouchDB using a view lookup with the filename as key,
     * returning aggregated metadata as JsonNode objects without loading full documents.</p>
     *
     * <p>Typical use cases:</p>
     * <ul>
     *   <li>Displaying file metadata in UI</li>
     *   <li>Processing file-specific metadata</li>
     *   <li>Auditing file uploads</li>
     * </ul>
     *
     * @param filename the exact filename to search for
     * @return list of JsonNode objects containing file metadata (maybe empty)
     * @implNote Uses CouchDB's view mechanism for efficient metadata retrieval
     * @implSpec Returns empty list if no matches found (never null)
     */
    public List<JsonNode> findFileMetadataByFilename(String filename) {
        ViewQuery query = new ViewQuery().designDocId(DESIGN_FILE_UPLOAD_ENTITY).viewName("file_metadata").group(true).key(filename);

        return db.queryView(query).getRows().stream().map(ViewResult.Row::getValueAsNode).toList();
    }
}