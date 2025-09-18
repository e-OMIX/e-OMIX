package com.example.eomix.resource_provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.example.eomix.service.ResourcesFetcher;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Group;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Group resource provider.
 */
@Component
public class GroupRP implements IResourceProvider {

    /**
     * Search by accession identifier and return a list of groups.
     * This method is used to search for groups based on the provided accession identifier.
     *
     * @param theProvider the provider containing the accession identifier
     * @return the list
     */
    @Search
    public List<Group> searchByAccessionIdentifier(
            @RequiredParam(name = Group.SP_IDENTIFIER) TokenParam theProvider) {
        String identifier = theProvider.getValue();
        return ResourcesFetcher.getGroupFromServerByAccessionIdentifier(identifier);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Group.class;
    }
}
