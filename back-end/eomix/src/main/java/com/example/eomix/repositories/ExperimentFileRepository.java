package com.example.eomix.repositories;

import ca.uhn.fhir.rest.param.TokenParam;
import com.example.eomix.controller.ExperimentsController;
import com.example.eomix.entities.ExperimentFileEntity;
import com.example.eomix.exception.ExperimentException;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;
import org.hl7.fhir.r5.model.Identifier;
import org.hl7.fhir.r5.model.Specimen;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.example.eomix.service.ResourcesFetcher.getOfficialIdentifier;

/**
 * The type Experiment file repository.
 */
@Repository
public class ExperimentFileRepository extends CouchDbRepositorySupport<ExperimentFileEntity> {

    private static final Logger logger = LoggerFactory.getLogger(ExperimentFileRepository.class);
    private final CouchDbConnector couchDbConnector2;

    /**
     * Instantiates a new Experiment file repository.
     * <p> This constructor initializes the repository with the specified CouchDbConnector.<br>
     * It calls the superclass constructor
     * with the ExperimentFileEntity class type and the provided CouchDbConnector.<br>
     * It also initializes the standard design document for the repository.
     *
     * @param db the db
     * @implNote The design document is used to define views and indexes for the repository.
     * couchDbConnector2 : experiment database
     */
    public ExperimentFileRepository(@Qualifier("couchDbConnector2") CouchDbConnector db) {
        super(ExperimentFileEntity.class, db);
        initStandardDesignDocument();
        this.couchDbConnector2 = db;
    }

    /**
     * Retrieves a list of {@code ExperimentFileEntity} objects based on the given experiment type.
     * <p>
     * This method queries the CouchDB database using a specific view defined in a design document.
     * It sets the key to the specified experiment type and includes the full documents in the results.
     *
     * @param experimentType the experiment type used as the key in the CouchDB view
     * @return a list of matching {@code ExperimentFileEntity} objects
     * @implSpec This method uses the {@code couchDbConnector2} to access a view and fetch documents that match
     * the given experiment type. It returns complete documents by setting {@code includeDocs} to true.
     * @implNote Typically used in scenarios where all experiment files related to a specific type
     * are needed for processing or analysis. This method is part of the {@code ExperimentFileRepository}
     * class, which supports CRUD operations for CouchDB entities in a Spring context.
     */
    public List<ExperimentFileEntity> findByExperimentNameAndType(String experimentType) {
        ViewQuery query = new ViewQuery()
                .designDocId("_design/experiments")
                .viewName("by_experimentNameAndType")
                .key(experimentType)
                .includeDocs(true);
        return couchDbConnector2.queryView(query, ExperimentFileEntity.class);
    }

    /**
     * Gets sample ids from metadata file name.
     * <p> This method retrieves a list of sample IDs based on the metadata file name. <br>
     * It uses the ExperimentsController to search for specimens by their identifier,
     * which is set to the metadata file name.<br>
     * For each specimen found, it checks if the specimen has an ID and retrieves its official identifier.
     *
     * @param metadataFileName      the metadata file name
     * @param experimentsController the experiments controller
     * @return the list of sample IDs
     * @throws ExperimentException if a specimen has no ID
     */
    public @NotNull List<String> getSampleIds(String metadataFileName, ExperimentsController experimentsController) {
        List<Specimen> specimens = experimentsController.specimenRP.searchByIdentifier(new TokenParam().setValue(metadataFileName));
        List<String> sampleIds = new ArrayList<>();
        for (Specimen specimen : specimens) {
            if (specimen.getId() == null || specimen.getId().isEmpty()) {
                logger.warn("Specimen with metadata file name {} has no ID", metadataFileName);
                throw new ExperimentException("Specimen with metadata file name " + metadataFileName + " has no ID");
            }
            List<Identifier> identifiersList = specimen.getIdentifier().stream().toList();
            String officialIdentifier = getOfficialIdentifier(identifiersList);
            sampleIds.add(officialIdentifier);
        }
        return sampleIds;
    }
}
