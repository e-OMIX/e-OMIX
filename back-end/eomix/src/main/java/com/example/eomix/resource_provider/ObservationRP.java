package com.example.eomix.resource_provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.example.eomix.service.ResourcesFetcher;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Observation;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * The type Observation resource provider.
 */
@Component
public class ObservationRP implements IResourceProvider {
    /**
     * Search by identifier and return a list of Observations.
     * This method is used to search for Observations based on the provided identifier.
     * It retrieves the Observations from the server using the identifier.
     *
     * @param theProvider the provider containing the identifier
     * @return the list
     * @throws FileNotFoundException the file not found exception
     */
    @Search
    public List<Observation> searchByIdentifier(
            @RequiredParam(name = Observation.SP_IDENTIFIER) TokenParam theProvider) throws FileNotFoundException {
        String identifier = theProvider.getValue();
        return ResourcesFetcher.getObservationFromServerByAccessionIdentifier(identifier);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Observation.class;
    }
}
