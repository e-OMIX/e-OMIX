package com.example.eomix.resource_provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.example.eomix.service.ResourcesFetcher;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Specimen;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Specimen resource provider.
 */
@Component
public class SpecimenRP implements IResourceProvider {

    /**
     * Search for specimens by identifier and return a list of Specimens.
     * This method is used to search for specimens based on the provided accession identifier.
     *
     * @param theProvider the provider containing the accession identifier
     * @return the list
     */
    @Search
    public List<Specimen> searchByIdentifier(
            @RequiredParam(name = Specimen.SP_ACCESSION) TokenParam theProvider) {
        String identifier = theProvider.getValue();
        return ResourcesFetcher.getSpecimenFromServerByAccessionIdentifier(identifier);
    }


    /**
     * Search by id specimen.
     * This method is used to search for a specimen by its identifier and accession identifier.
     * It retrieves a single specimen resource based on the provided parameters.
     *
     * @param theProvider   the provider containing the identifier
     * @param theIdentifier the identifier containing the accession identifier
     * @return the specimen
     */
    @Search
    public Specimen searchById(@RequiredParam(name = Specimen.SP_IDENTIFIER) TokenParam theProvider, @RequiredParam(name = Specimen.SP_ACCESSION) TokenParam theIdentifier) {
        String id = theProvider.getValue();
        String identifier = theIdentifier.getValue();
        return ResourcesFetcher.getSpecimenByIdAndIdentifier(id, identifier);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Specimen.class;
    }
}
