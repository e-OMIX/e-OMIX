package com.example.eomix.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.example.eomix.exception.FhirResourceFetchException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for fetching and managing FHIR resources such as Specimen, Patient, Group, and MolecularSequence.
 * <p>
 * This class provides static methods to interact with a FHIR server using the HAPI FHIR client.
 * It supports operations including adding resources to the server, checking for existing resources,
 * and retrieving resources by identifiers.
 * </p>
 * <p>
 * Designed as a utility class with only static methods, it should not be instantiated.
 * Intended for healthcare applications requiring management of patient data, specimens, and molecular sequences.
 * </p>
 * <p>
 * The class assumes a FHIR server running at a specified base URL and handles resource-specific operations
 * using the HAPI FHIR client.
 * </p>
 *
 * @author Molka Anaghim FTOUHI
 * @implNote Uses the HAPI FHIR client to perform create and read operations on FHIR resources.
 * @implSpec Utility class with static methods only; no instance should be created.
 */

public class ResourcesFetcher {

    private static final Logger logger = LoggerFactory.getLogger(ResourcesFetcher.class);
    /**
     * The Ctx
     * <p>
     * This is a static instance of FhirContext for R5 version, cached for performance.<br>
     * It is used to create and manage FHIR resources.
     */
    static FhirContext ctx = FhirContext.forR5Cached();
    /**
     * The Spring boot base url.
     */
    static String springBootBaseUrl = "http://localhost:7000/api/fhir/";
    /**
     * The Client.
     */
    static IGenericClient client = ctx.newRestfulGenericClient(springBootBaseUrl);

    /**
     * Private constructor to prevent instantiation.
     * This class is a utility class and should not be instantiated.
     */
    private ResourcesFetcher() {
    }

    /**
     * Add specimen resources to fhir server.
     * <p>
     * This method adds a list of specimens to the FHIR server.
     * It checks if the list of specimens is not empty and iterates through each specimen,
     * adding it to the server if it does not already exist.
     * If the list is empty, it logs a message indicating that no specimens were found.
     *
     * @param specimens the specimens
     * @implNote The method uses the HAPI FHIR client to create resources on the server.
     * @implSpec Each specimen is checked for existence by its identifier and accession identifier before being added
     * .<br>
     * If a specimen already exists, it logs a message indicating that the specimen already exists.
     */
    public static void addSpecimenResourcesToFHIRServer(List<Specimen> specimens) {

        if (!specimens.isEmpty()) {
            specimens.forEach(ResourcesFetcher::addSpecimenToServerFHIR);
        } else {
            logger.info("No specimens found!");
        }
    }

    /**
     * Add specimen to server fhir.
     * <p>
     * This method adds a single specimen to the FHIR server.
     * <p>
     * It checks if the specimen already exists by its identifier and accession identifier.
     * <ul>
     * <li>    If it does not exist, it creates the specimen resource on the server.</li>
     * <li> If it already exists, it logs a message indicating that the specimen already exists.</li>
     *
     * @param specimen the specimen
     */
    private static void addSpecimenToServerFHIR(Specimen specimen) {
        String identifierValue = specimen.getIdentifierFirstRep().getId();
        String accessionIdentifier = specimen.getAccessionIdentifier().getId();
        if (getSpecimenByIdAndIdentifier(identifierValue, accessionIdentifier) == null) {
            client.create().resource(specimen).execute();
        } else {
            logger.info("Specimen already exist!");
        }
    }

    /**
     * Add list of patients resources to fhir server.
     * <p>
     * This method adds a list of patients to the FHIR server.
     * <p> It checks if the list of patients is not empty and iterates through each patient,
     * adding it to the server if it does not already exist.
     * <ul>
     *     <li>If the list is empty, it logs a message indicating that no patients were found.</li>
     *     <li>Each patient is checked for existence by its official identifier and accession identifier before being
     *     added.</li>
     *     <li>If a patient already exists, it logs a message indicating that the patient already exists.</li>
     *     </ul>
     *
     * @param patients the patients
     * @implNote The method uses the HAPI FHIR client to create resources on the server.
     * @implSpec Each patient is checked for existence by its official identifier and accession identifier before
     * being added.<br>
     * If a patient already exists, it logs a message indicating that the patient already exists.
     */
    public static void addListOfPatientsResourcesToFHIRServer(List<Patient> patients) {

        if (!patients.isEmpty()) {
            patients.forEach(ResourcesFetcher::addPatientTOServerFHIR);
        } else {
            logger.info("No Patient found!");
        }
    }

    /**
     * Add patient to server fhir.
     * This method adds a single patient to the FHIR server.
     * * It checks if the patient already exists by its official identifier and accession identifier.
     * * If it does not exist, it creates the patient resource on the server.
     * * If it already exists, it logs a message indicating that the patient already exists.
     *
     * @param patient the patient
     */
    private static void addPatientTOServerFHIR(Patient patient) {
        List<Identifier> identifiersList = patient.getIdentifier().stream().toList();
        String accessionIdentifier = getAccessionIdentifier(identifiersList);
        String officialIdentifier = getOfficialIdentifier(identifiersList);
        if (getPatientByIdAndIdentifier(accessionIdentifier, officialIdentifier) == null) {
            client.create().resource(patient).execute();
        } else {
            logger.info("Patient already exist!");
        }
    }

    /**
     * Add group to server fhir ang get id string.
     * <p>
     * This method adds a group resource to the FHIR server and returns its ID.
     * It checks if the group already exists by its official identifier and accession identifier.
     *  <ul>
     * <li> If it does not exist, it creates the group resource on the server and returns its ID.</li>
     * <li> If it already exists, it logs a message indicating that the group already exists and returns null.</li>
     * </ul>
     *
     * @param group the group (batch)
     * @return the string
     * @implNote The method uses the HAPI FHIR client to create resources on the server.
     * @implSpec Each group is checked for existence by its official identifier and accession identifier before being
     * added.<br>
     * If a group already exists, it logs a message indicating that the group already exists.
     */
    static String addOrUpdateGroupTOServerFHIRAndGetID(Group group) {
        List<Identifier> identifiersList = group.getIdentifier().stream().toList();
        String officialIdentifier = getOfficialIdentifier(identifiersList);
        String accessionIdentifier = getAccessionIdentifier(identifiersList);
        Group existingGroup = getGroupByIdAndIdentifier(accessionIdentifier, officialIdentifier);
        if (existingGroup == null) {
            MethodOutcome outcome = client.create().resource(group).execute();
            return outcome.getId().getIdPart();
        } else {
            logger.info("Group already exist!");
            group.setId(existingGroup.getIdElement());
            MethodOutcome outcome = client.update().resource(group).execute();
            return outcome.getId().getIdPart();
        }
    }

    /**
     * Add molecular sequence resources to fhir server.
     * This method adds a molecular sequence resource to the FHIR server.
     * It checks if the molecular sequence already exists by its ID.
     *  <ul>
     * <li>  If it does not exist, it creates the molecular sequence resource on the server. </li>
     * <li>  If it already exists, it logs a message indicating that the molecular sequence already exists. </li>
     *  </ul>
     *
     * @param molecularSequenceList the molecular sequence list
     * @implNote The method uses the HAPI FHIR client to create resources on the server.
     * @implSpec Each molecular sequence is checked for existence by its ID before being added.<br>
     * If a molecular sequence already exists, it logs a message indicating that the molecular sequence already exists.
     */
    public static void addListOfMolecularSequenceResourcesToFHIRServer(List<MolecularSequence> molecularSequenceList) {
        if (!molecularSequenceList.isEmpty()) {
            molecularSequenceList.forEach(ResourcesFetcher::addMolecularSequenceToServerFHIR);
        } else {
            logger.info("No Molecular sequence found!");
        }
    }

    private static void addMolecularSequenceToServerFHIR(MolecularSequence molecularSequence) {
        if (molecularSequence != null && !molecularSequence.isEmpty()) {
            String id = molecularSequence.getId();
            if (getMolecularSequenceByIdAndIdentifier(id) == null) {
                client.create().resource(molecularSequence).execute();
            } else {
                logger.info("Molecular sequence already exist!");
            }
        } else {
            logger.info("No molecular sequence found!");
        }
    }

    /**
     * Gets official identifier.
     * <p> This method retrieves the official identifier from a list of identifiers.
     * It filters the list to find the identifier with the use type OFFICIAL and returns its ID.
     *
     * @param identifiersList the identifiers list
     * @return the official identifier
     * @implNote The method uses Java Streams to filter the identifiers list.
     * @implSpec If no official identifier is found, it returns null.
     */
    public static String getOfficialIdentifier(List<Identifier> identifiersList) {
        return getIdentifier(identifiersList, Identifier.IdentifierUse.OFFICIAL);
    }

    /**
     * Gets identifier.
     * <p> This method retrieves an identifier from a list of identifiers based on the specified use type.
     * It filters the list to find the identifier with the given use type and returns its ID.
     *
     * @param identifiersList the identifiers list
     * @param identifierUse   the identifier use
     * @return the identifier
     * @implNote The method uses Java Streams to filter the identifiers list.
     * @implSpec If no identifier with the specified use type is found, it returns null.
     */
    private static @Nullable String getIdentifier(List<Identifier> identifiersList,
                                                  Identifier.IdentifierUse identifierUse) {
        return identifiersList.stream().filter(identifier -> identifier.getUse() == identifierUse).map(Identifier::getId).findFirst().orElse(null);
    }

    /**
     * Gets accession identifier.
     * <p>This method retrieves the accession identifier from a list of identifiers.
     * It filters the list to find the identifier with the use type SECONDARY and returns its ID.
     *
     * @param identifiersList the identifiers list
     * @return the accession identifier
     * @implNote The method uses Java Streams to filter the identifiers list.
     * @implSpec If no accession identifier is found, it returns null.
     */
    public static String getAccessionIdentifier(List<Identifier> identifiersList) {
        return getIdentifier(identifiersList, Identifier.IdentifierUse.SECONDARY);
    }

    /**
     * Gets specimen by id and identifier.
     * <p> This method retrieves a specimen from the server by its ID and identifier.
     * It first fetches a list of specimens from the server using the provided identifier.
     * It then iterates through the list to find a specimen whose official identifier matches the provided specimen ID.
     * <ul>
     * <li>  If a matching specimen is found, it returns that specimen. </li>
     * <li>  If no matching specimen is found, it returns null. </li>
     * </ul>
     *
     * @param specimenId the specimen id
     * @param identifier the identifier
     * @return the specimen by id and identifier
     * @implNote The method uses the HAPI FHIR client to search for specimens on the server.
     * @implSpec If an error occurs during the retrieval process, it throws a RuntimeException with the caught
     * exception.
     */
    public static Specimen getSpecimenByIdAndIdentifier(String specimenId, String identifier) {
        try {
            List<Specimen> specimenList = getSpecimenFromServerByAccessionIdentifier(identifier);
            for (Specimen specimen : specimenList) {
                List<Identifier> identifiersList = specimen.getIdentifier().stream().toList();
                String officialIdentifier = getOfficialIdentifier(identifiersList);
                if (officialIdentifier != null && officialIdentifier.equals(specimenId)) {
                    return specimen;
                }
            }
        } catch (Exception e) {
            throw new FhirResourceFetchException("Failed to fetch Specimen by id and identifier", e);
        }
        return null;
    }

    /**
     * Gets specimen from server by accession identifier.
     * <p> This method retrieves a list of specimens from the server based on the provided accession identifier.<br>
     * It fetches a bundle of specimens from the server and iterates through each entry.<br>
     * For each specimen, it checks if the accession identifier matches the provided identifier.<br>
     * If a match is found, it adds the specimen to the list and returns the list.
     *
     * @param identifier the accession identifier (is the metadata file name)
     * @return the specimen from server by accession identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If no specimens are found with the given accession identifier, it returns an empty list.
     */
    public static List<Specimen> getSpecimenFromServerByAccessionIdentifier(String identifier) {
        List<Specimen> specimenList = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : getBundle(Specimen.class).getEntry()) {
            if (entry.getResource() instanceof Specimen specimen && specimen.hasAccessionIdentifier() && !specimen.getAccessionIdentifier().isEmpty() && specimen.getAccessionIdentifier().getId().equals(identifier)) {
                specimenList.add(specimen);
            }
        }
        return specimenList;
    }

    /**
     * Gets patient by id and identifier.
     * <p> This method retrieves a patient from the server by its ID and identifier.
     * <ul>
     * <li>  It first fetches a list of patients from the server using the provided identifier. </li>
     * <li>  It then iterates through the list to find a patient whose official identifier matches the provided
     * patient ID.  </li>
     *  - If a matching patient is found, it returns that patient. <br>
     *  - If no matching patient is found, it returns null.
     * </ul>
     *
     * @param identifier the identifier
     * @return the patient by id and identifier
     * @implNote The method uses the HAPI FHIR client to search for patients on the server.
     * @implSpec If an error occurs during the retrieval process, it throws a RuntimeException with the caught
     * exception.
     */
    public static Patient getPatientByIdAndIdentifier(String identifier, String officialIdentifier) {
        try {
            List<Patient> patientList = getPatientFromServerByAccessionIdentifier(identifier);
            for (Patient patient : patientList) {
                List<Identifier> identifiersList = patient.getIdentifier().stream().toList();
                String officialId = getOfficialIdentifier(identifiersList);
                if (officialId != null && officialId.equals(officialIdentifier)) {
                    return patient;
                }
            }
        } catch (Exception e) {

            throw new FhirResourceFetchException("Failed to fetch Patient by id and identifier", e);
        }
        return null;
    }

    public static Group getGroupByIdAndIdentifier(String accessionIdentifier, String officialIdentifier) {
        try {
            List<Group> groupList = getGroupFromServerByAccessionIdentifier(accessionIdentifier);
            for (Group group : groupList) {
                List<Identifier> identifiersList = group.getIdentifier().stream().toList();
                String officialId = getOfficialIdentifier(identifiersList);
                if (officialId != null && officialId.equals(officialIdentifier)) {
                    return group;
                }
            }
        } catch (Exception e) {

            throw new FhirResourceFetchException("Failed to fetch Patient by id and identifier", e);
        }
        return null;
    }

    /**
     * Gets patient from server by accession identifier.
     * <p> This method retrieves a list of patients from the server based on the provided accession identifier.<br>
     * It fetches a bundle of patients from the server and iterates through each entry.<br>
     * For each patient, it checks if the accession identifier matches the provided identifier.<br>
     * If a match is found, it adds the patient to the list and returns the list.
     *
     * @param identifier the identifier
     * @return the patient from server by accession identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If no patients are found with the given accession identifier, it returns an empty list.
     */
    public static List<Patient> getPatientFromServerByAccessionIdentifier(String identifier) {
        List<Patient> patientList = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : getBundle(Patient.class).getEntry()) {
            if (entry.getResource() instanceof Patient patient) {
                List<Identifier> identifiersList = patient.getIdentifier().stream().toList();
                String accessionIdentifier = getAccessionIdentifier(identifiersList);
                if (accessionIdentifier != null && accessionIdentifier.equals(identifier)) {
                    patientList.add(patient);
                }
            }
        }
        return patientList;
    }

    /**
     * Gets group from server by accession identifier.
     * <p> This method retrieves a list of groups from the server based on the provided accession identifier.<br>
     * It fetches a bundle of groups from the server and iterates through each entry.<br>
     * For each group, it checks if the accession identifier matches the provided identifier.<br>
     * If a match is found, it adds the group to the list and returns the list.
     *
     * @param identifier the identifier
     * @return the group from server by accession identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If no groups are found with the given accession identifier, it returns an empty list.
     */
    public static List<Group> getGroupFromServerByAccessionIdentifier(String identifier) {
        List<Group> groupList = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : getBundle(Group.class).getEntry()) {
            if (entry.getResource() instanceof Group group) {
                List<Identifier> identifiersList = group.getIdentifier().stream().toList();
                String accessionIdentifier = getAccessionIdentifier(identifiersList);
                if (accessionIdentifier != null && accessionIdentifier.equals(identifier)) {
                    groupList.add(group);
                }
            }
        }
        return groupList;
    }

    /**
     * Gets observation from server by accession identifier.
     * <p> This method retrieves a list of observations from the server based on the provided accession identifier.<br>
     * It fetches a bundle of observations from the server and iterates through each entry.<br>
     * For each observation, it checks if the accession identifier matches the provided identifier.<br>
     * If a match is found, it adds the observation to the list and returns the list.
     *
     * @param identifier the identifier
     * @return the observation from server by accession identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If no observations are found with the given accession identifier, it returns an empty list.
     */
    public static List<Observation> getObservationFromServerByAccessionIdentifier(String identifier) {
        List<Observation> observationList = new ArrayList<>();
        for (Bundle.BundleEntryComponent entry : getBundle(Observation.class).getEntry()) {
            if (entry.getResource() instanceof Observation observation) {
                List<Identifier> identifiersList = observation.getIdentifier().stream().toList();
                String accessionIdentifier = getAccessionIdentifier(identifiersList);
                if (accessionIdentifier != null && accessionIdentifier.equals(identifier)) {
                    observationList.add(observation);
                }
            }
        }
        return observationList;
    }

    /**
     * Gets molecular sequence from server by accession identifier.
     * <p> This method retrieves a molecular sequence from the server based on the provided accession identifier.<br>
     * It fetches a bundle of molecular sequences from the server and iterates through each entry.<br>
     * For each molecular sequence, it checks if the accession identifier matches the provided identifier.
     * <ul>
     * <li> If a match is found, it returns the molecular sequence.</li>
     * <li> If no matching molecular sequence is found, it returns null.</li>
     * </ul>
     *
     * @param identifier the identifier
     * @return the molecular sequence from server by accession identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If an error occurs during the retrieval process, it throws a RuntimeException with the caught
     * exception.
     */
    public static List<MolecularSequence> getMolecularSequenceFromServerByAccessionIdentifier(String identifier) {
        try {
            List<MolecularSequence> molecularSequenceList = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : getBundle(MolecularSequence.class).getEntry()) {
                if (entry.getResource() instanceof MolecularSequence molecularSequence) {
                    List<Identifier> identifiersList = molecularSequence.getIdentifier().stream().toList();
                    String accessionIdentifier = getAccessionIdentifier(identifiersList);
                    if (accessionIdentifier != null && accessionIdentifier.equals(identifier)) {
                        molecularSequenceList.add(molecularSequence);
                    }
                }
            }
            return molecularSequenceList;
        } catch (Exception e) {

            throw new FhirResourceFetchException("Failed to fetch MolecularSequence by accession identifier", e);
        }
    }

    /**
     * Gets molecular sequence by id and identifier.
     * <p> This method retrieves a molecular sequence from the server by its ID.
     * It fetches a bundle of molecular sequences from the server and iterates through each entry. <br>
     * For each molecular sequence, it checks if the ID matches the provided ID.
     * <ul>
     * <li> If a matching molecular sequence is found, it returns that molecular sequence.</li>
     * <li>If no matching molecular sequence is found, it returns null.</li>
     * </ul>
     *
     * @param id the id
     * @return the molecular sequence by id and identifier
     * @implNote The method uses the HAPI FHIR client to search for resources on the server.
     * @implSpec If an error occurs during the retrieval process, it throws a RuntimeException with the caught
     * exception.
     */
    public static MolecularSequence getMolecularSequenceByIdAndIdentifier(String id) {
        try {
            for (Bundle.BundleEntryComponent entry : getBundle(MolecularSequence.class).getEntry()) {
                if (entry.getResource() instanceof MolecularSequence molecularSequence && molecularSequence.getId().equalsIgnoreCase(id)) {
                    return molecularSequence;
                }
            }
        } catch (Exception e) {
            throw new FhirResourceFetchException("Failed to fetch MolecularSequence by ID", e);
        }

        return null;
    }

    private static Bundle getBundle(Class<? extends IBaseResource> var1) {
        return client.search().forResource(var1).returnBundle(Bundle.class).execute();
    }

}
