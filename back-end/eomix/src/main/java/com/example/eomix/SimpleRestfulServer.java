package com.example.eomix;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.provider.HashMapResourceProvider;
import com.example.eomix.resource_provider.*;
import jakarta.servlet.annotation.WebServlet;
import org.hl7.fhir.r5.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.cors.CorsConfiguration;


/**
 * The type Simple restful server.
 *
 * @author Molka Anaghim FTOUHI
 */
@WebServlet("/*")
public class SimpleRestfulServer extends RestfulServer {


    /**
     * The Our fhir context.
     */
    static FhirContext ourFhirContext = FhirContext.forR5Cached();
    private final transient SpecimenRP specimenResourceProvider;
    private final transient ObservationRP observationResourceProvider;
    private final transient PatientRP patientResourceProvider;
    private final transient MolecularSequenceRP molecularSequenceResourceProvider;
    private final transient GroupRP groupResourceProvider;

    /**
     * Initializes a new FHIR R5 RESTful server with specified resource providers.
     *
     * <p>This constructor configures a FHIR server to handle standard resource types including:
     * <ul>
     *   <li>Specimen</li>
     *   <li>Patient</li>
     *   <li>Observation</li>
     *   <li>MolecularSequence</li>
     *   <li>Group</li>
     * </ul>
     *
     * <p>Server characteristics:
     * <ul>
     *   <li>Designed for Spring Boot integration</li>
     *   <li>Registers as a servlet for HTTP request handling</li>
     *   <li>Supports standard FHIR CRUD operations</li>
     *   <li>Extensible for additional resource types</li>
     * </ul>
     *
     * @param specimenResourceProvider          specimen resource provider
     * @param patientResourceProvider           patient resource provider
     * @param observationResourceProvider       observation resource provider
     * @param molecularSequenceResourceProvider molecular sequence resource provider
     * @param groupResourceProvider             group resource provider
     * @implSpec Uses FHIR R5 (latest stable version)
     * @implNote Primarily intended for development environments
     */
    public SimpleRestfulServer(SpecimenRP specimenResourceProvider, PatientRP patientResourceProvider, ObservationRP observationResourceProvider, MolecularSequenceRP molecularSequenceResourceProvider, GroupRP groupResourceProvider) {
        this.specimenResourceProvider = specimenResourceProvider;
        this.observationResourceProvider = observationResourceProvider;
        this.patientResourceProvider = patientResourceProvider;
        this.molecularSequenceResourceProvider = molecularSequenceResourceProvider;
        this.groupResourceProvider = groupResourceProvider;
    }

    /**
     * Configures and returns a CORS interceptor.
     * <p>
     * This method sets up a {@link org.springframework.web.cors.CorsConfiguration} to allow
     * cross-origin requests from specific origins. It defines the allowed headers, methods, and
     * credentials necessary for enabling CORS in the application.
     *
     * @return the configured CORS interceptor
     * @implSpec This method leverages {@code CorsConfiguration} to define CORS policies.
     * It permits requests from trusted origins such as:
     * <ul>
     *     <li>{@code http://localhost:4200} (typically used for local development)</li>
     *     <li>Production server URL (to be specified)</li>
     * </ul>
     * It also defines the allowed headers and HTTP methods.
     * @implNote Use this configuration in environments where a frontend application
     * (e.g., Angular) interacts with a backend FHIR server hosted on a different domain or port.
     * <p>
     * The interceptor is registered to handle CORS preflight (OPTIONS) requests and to allow
     * cross-origin access to FHIR resources.
     */

    private static @NotNull CorsInterceptor getCorsInterceptor() {
        CorsConfiguration corsConfig = new CorsConfiguration();
//        corsConfig.addAllowedOrigin("http://localhost");
        corsConfig.addAllowedOrigin("http://localhost:4200");
        corsConfig.addAllowedHeader("Content-Type");
        corsConfig.addAllowedHeader("Authorization");
        corsConfig.addAllowedMethod("GET");
        corsConfig.addAllowedMethod("POST");
        corsConfig.addAllowedMethod("PUT");
        corsConfig.addAllowedMethod("DELETE");
        corsConfig.addAllowedMethod("OPTIONS");
        corsConfig.setAllowCredentials(true);
        return new CorsInterceptor(corsConfig);
    }

    /**
     * Initializes the FHIR server with the appropriate context and resource providers.
     * <p>
     * This method sets up the FHIR context for the R5 version and registers resource providers
     * responsible for handling various FHIR resource types such as {@code Specimen}, {@code Patient},
     * {@code Observation}, {@code MolecularSequence}, and {@code Group}.
     * It also registers a CORS interceptor to support cross-origin requests.
     *
     * @implSpec This method uses {@link ca.uhn.fhir.context.FhirContext#forR5()} to create the context
     * for R5 FHIR resources. It then registers both custom and in-memory (HashMap-based) resource providers
     * for the supported resource types.
     * @implNote This method is typically called during server startup or configuration
     * to prepare the FHIR environment for processing incoming requests.
     * It ensures that all necessary resource providers and interceptors are registered before handling requests.
     */
    @Override
    protected void initialize() {
        setFhirContext(FhirContext.forR5());
        registerProvider(specimenResourceProvider);
        registerProvider(patientResourceProvider);
        registerProvider(observationResourceProvider);
        registerProvider(molecularSequenceResourceProvider);
        registerProvider(groupResourceProvider);
        registerProvider(new HashMapResourceProvider<>(ourFhirContext, Specimen.class));
        registerProvider(new HashMapResourceProvider<>(ourFhirContext, Patient.class));
        registerProvider(new HashMapResourceProvider<>(ourFhirContext, Observation.class));
        registerProvider(new HashMapResourceProvider<>(ourFhirContext, MolecularSequence.class));
        registerProvider(new HashMapResourceProvider<>(ourFhirContext, Group.class));
        CorsInterceptor corsInterceptor = getCorsInterceptor();
        registerInterceptor(corsInterceptor);

    }


}
