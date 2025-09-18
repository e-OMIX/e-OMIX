package com.example.eomix.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.ektorp.support.CouchDbDocument;


/**
 * The type Csv document.
 * This class represents a document in a CouchDB database
 * that contains the CSV file of the sample metadata as the user uploaded it.
 * It is typically used in conjunction with the CouchDB database
 * to store and retrieve the sample metadata in CSV format.
 * It extends the CouchDbDocument class, which provides basic functionality
 * for CouchDB documents.
 * <p>
 * The CsvDocument class has two fields:
 * - id: a string representing the unique identifier of the document in the database.
 * - filename: a string representing the name of the CSV file.
 * </p>
 * The class uses Lombok annotations to generate getters and setters for the fields,
 * and the @JsonProperty annotation to specify the JSON property name for the id field.
 * <p>
 * This class is used to store the CSV file of the sample metadata
 * as the user uploaded it in the CouchDB database.
 */
@Setter
@Getter
public class CsvDocument extends CouchDbDocument {
    @JsonProperty("_id")
    private String id;
    private String filename;
}