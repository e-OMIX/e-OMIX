package com.example.eomix.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.ektorp.support.CouchDbDocument;
import org.ektorp.support.TypeDiscriminator;

import java.util.Map;

/**
 * The type File upload entity.
 * This class represents a file upload entity in the database.
 * It extends CouchDbDocument to provide the necessary structure
 * for storing sample metadata file uploads.
 * * It includes fields such as meta, sourceColumns, id, and revision.
 * * The meta field holds metadata information for the file upload entity,
 * * including the filename and creation date.
 * * The sourceColumns field is a map that represents the source columns
 * * associated with the file upload.
 * * This class is used to store and retrieve file upload data
 * from a CouchDB database.
 *
 * @implNote The class uses Lombok annotations for getter and setter methods,
 * * which reduces boilerplate code and improves readability.
 * @implSpec The class is designed to be used with a CouchDB database,
 * allowing for easy storage and retrieval of file upload data.
 */
@Getter
@Setter
public class MetadataFileUploadEntity extends CouchDbDocument {
    @TypeDiscriminator
    private transient MetaData meta;
    private Map<String, String> sourceColumns;
    private String id;
    private String revision;


    @JsonProperty("_id")
    @Override
    public String getId() {
        return id;
    }

    @JsonProperty("_rev")
    @Override
    public String getRevision() {
        return revision;
    }

    /**
     * The type Metadata.
     * This class holds metadata information for the file upload entity.
     * It includes fields such as filename and createdAt,
     * which represent the name of the file and the creation date respectively.
     */
    @Getter
    @Setter
    public static class MetaData {
        private String filename;
        private String createdAt;
    }
}
