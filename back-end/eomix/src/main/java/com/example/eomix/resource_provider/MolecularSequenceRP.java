package com.example.eomix.resource_provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.example.eomix.service.ResourcesFetcher;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.MolecularSequence;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Molecular sequence resource provider.
 */
@Component
public class MolecularSequenceRP implements IResourceProvider {

    /**
     * Search all molecular sequence and return a molecular sequence.
     * This method is used to search for a molecular sequence based on the provided identifier.
     * It retrieves the molecular sequence from the server using the identifier.
     *
     * @param theProvider the provider containing the identifier
     * @return the molecular sequence
     */
    @Search
    public List<MolecularSequence> searchAllMolecularSequence(@RequiredParam(name = MolecularSequence.SP_IDENTIFIER) TokenParam theProvider) {
        String identifier = theProvider.getValue();
        return ResourcesFetcher.getMolecularSequenceFromServerByAccessionIdentifier(identifier);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return MolecularSequence.class;
    }
}
