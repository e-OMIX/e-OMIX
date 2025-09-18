package com.example.eomix.resource_provider;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import com.example.eomix.service.ResourcesFetcher;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Patient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The type Patient resource provider.
 */
@Component
public class PatientRP implements IResourceProvider {


    /**
     * Search patients by accession identifier and return a list of Patients.
     * This method is used to search for patients based on the provided accession identifier.
     * This method is annotated with @Search to indicate that it is a search operation
     * it uses the @RequiredParam annotation to specify that the identifier parameter is required.
     * This method retrieves a list of patients from the server based on the provided identifier.
     *
     * @param theProvider the provider containing the identifier
     * @return the list
     */
    @Search
    public List<Patient> searchByAccessionIdentifier(
            @RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam theProvider) {
        String identifier = theProvider.getValue();
        return ResourcesFetcher.getPatientFromServerByAccessionIdentifier(identifier);
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }
}
