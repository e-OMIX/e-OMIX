package com.example.eomix;

import com.example.eomix.resource_provider.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * The type e-omix application.
 *
 * @author Molka Anaghim FTOUHI
// */
@SpringBootApplication
public class EomixApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EomixApplication.class, args);
    }

    /**
     * Registers a servlet for handling FHIR resource requests.
     * <p>
     * This method creates and configures a {@link org.springframework.boot.web.servlet.ServletRegistrationBean}
     * that maps a FHIR servlet to the URL pattern {@code "/fhir/*"}. The servlet is initialized with multiple
     * resource providers responsible for handling different FHIR resource types.
     *
     * @param specimenRP          the resource provider for {@code Specimen} resources
     * @param patientRP           the resource provider for {@code Patient} resources
     * @param observationRP       the resource provider for {@code Observation} resources
     * @param molecularSequenceRP the resource provider for {@code MolecularSequence} resources
     * @param groupRP             the resource provider for {@code Group} resources
     * @return a configured {@code ServletRegistrationBean} that registers the FHIR servlet
     * @implSpec This method uses {@code ServletRegistrationBean} to register and map the servlet.
     * The servlet is initialized with multiple resource providers, enabling it to support a broad
     * set of FHIR resource types.
     * @implNote Typically used in Spring Boot applications to expose a FHIR server through a RESTful API.
     * The servlet is named {@code "FhirServlet"} and mapped to the path {@code "/fhir/*"},
     * making it accessible for handling incoming FHIR requests.
     */
    @Bean
    public ServletRegistrationBean ServletRegistrationBean(SpecimenRP specimenRP, PatientRP patientRP, ObservationRP observationRP, MolecularSequenceRP molecularSequenceRP, GroupRP groupRP) {
        ServletRegistrationBean registration = new ServletRegistrationBean(new SimpleRestfulServer(specimenRP, patientRP, observationRP, molecularSequenceRP, groupRP), "/fhir/*");
        registration.setName("FhirServlet");
        return registration;
    }
}
