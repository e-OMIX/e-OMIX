package com.example.eomix.service;

import com.example.eomix.utils.Helper;
import org.apache.commons.csv.CSVRecord;
import org.hl7.fhir.r5.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The type Fhir service implementation.
 */
@Service
public class FhirServiceImplementation {

    /**
     * The constant BATCH.
     */
    public static final String BATCH = "batch";
    /**
     * The constant PATIENT_ID.
     */
    public static final String PATIENT_ID = "patient_id";
    /**
     * The constant UNKNOWN.
     */
    public static final String UNKNOWN = "Unknown";
    /**
     * The constant GENDER.
     */
    public static final String GENDER = "gender";
    /**
     * The constant AGE.
     */
    public static final String AGE = "age";
    /**
     * The constant STANDARDIZED_SPECIES.
     */
    public static final String STANDARDIZED_SPECIES = "standardized_species";
    /**
     * The constant SAMPLE_ID.
     */
    public static final String SAMPLE_ID = "sample_id";
    /**
     * The constant PROTOCOL.
     */
    public static final String PROTOCOL = "protocol";
    /**
     * The constant ORGAN.
     */
    public static final String ORGAN = "organ";
    /**
     * The constant DISORDER.
     */
    public static final String DISORDER = "disorder";
    /**
     * The constant SEQUENCE_TYPE.
     */
    public static final String SEQUENCE_TYPE = "sequenceType";
    /**
     * The constant MOLECULAR_SEQUENCE_ID.
     */
    public static final String MOLECULAR_SEQUENCE_ID = "molecularSequence_id";
    /**
     * The constant CELLULAR_RESOLUTION.
     */
    public static final String CELLULAR_RESOLUTION = "cellularResolution";
    /**
     * The log.
     */
    static final Logger ourLog = LoggerFactory.getLogger(FhirServiceImplementation.class);
    private static final Logger logger = LoggerFactory.getLogger(FhirServiceImplementation.class);
    /**
     * The Headers list.
     */
    public static List<String> headersList;
    /**
     * The Molecular sequences list.
     */
    static List<MolecularSequence> molecularSequencesList = new ArrayList<>();
    /**
     * The Tissues list.
     */
    static List<String> tissuesList = new ArrayList<>();

    private final FileSystemStorageService fileSystemStorageService;

    /**
     * Instantiates a new Fhir service implementation.
     * This constructor initializes the FhirServiceImplementation with the provided FileSystemStorageService.
     * * It sets the fileSystemStorageService field to the provided instance.
     *
     * @param fileSystemStorageService the file system storage service
     * @implNote The FileSystemStorageService is used to handle file operations related to FHIR resources.
     */
    public FhirServiceImplementation(FileSystemStorageService fileSystemStorageService) {
        this.fileSystemStorageService = fileSystemStorageService;
    }

    /**
     * Sets patient details.
     * * This method sets the details of a patient based on the provided CSV record and file name.
     * * It creates a new Patient object and populates its fields based on the values in the CSV record.
     *
     * @param nextRecord the next record
     * @param fileName   the file name
     * @return the patient
     * @implNote The method checks if the patient ID is present in the headers list and if it is not equal to "Unknown".
     * * It generates a unique ID for the patient by using the patient ID from the CSV record.
     * * * If the patient with the generated ID does not already exist, it sets the ID, identifiers,
     */
    private static Patient setPatientDetails(CSVRecord nextRecord, String fileName) {
        Patient patient = new Patient();
        if (headersList.contains(PATIENT_ID) && !nextRecord.get(PATIENT_ID).equalsIgnoreCase(UNKNOWN)) {
            patient.addIdentifier(setAccessionId(fileName));
            patient.addIdentifier(setOfficialId(getIdValue(nextRecord.get(PATIENT_ID)), PATIENT_ID));
            patient.setId(getIdValue(nextRecord.get(PATIENT_ID)));
            List<Extension> extensionList = new ArrayList<>();
            if (headersList.contains(GENDER)) {
                patient.setGender(getGender(nextRecord.get(GENDER)));
            }
            if (headersList.contains(AGE)) {
                Extension ageExtension = new Extension();
                ageExtension.setUrl("https://hl7.org/fhir/StructureDefinition/patient-birthTime");
                ageExtension.setValue(setCodeableConceptDetails(AGE, nextRecord));
                extensionList.add(ageExtension);
            }
            if (headersList.contains(STANDARDIZED_SPECIES)) {
                Extension speciesExtension = new Extension();
                speciesExtension.setUrl("https://hl7.org/fhir/ValueSet/animal-species");
                speciesExtension.setValue(setCodeableConceptDetails(STANDARDIZED_SPECIES, nextRecord));
                extensionList.add(speciesExtension);
            }
            patient.setExtension(extensionList);
        }
        return patient;
    }

    /**
     * Sets identifier.
     * * This method sets an identifier for a resource based on the provided parameters.
     * * It creates a new Identifier object and populates its fields based on the provided values.
     *
     * @param id       the id
     * @param value    the value
     * @param useCode  the use code
     * @param typeCode the type code
     * @return the identifier
     * @implNote The method sets the ID, use code, type code, and value for the identifier.
     */
    private static @NotNull Identifier setIdentifier(String id, String value, Identifier.IdentifierUse useCode, String typeCode) {
        Identifier identifier = new Identifier();
        identifier.setId(id);
        identifier.setUse(useCode);
        if (typeCode != null) {
            CodeableConcept type = new CodeableConcept();
            type.addCoding().setCode(typeCode);
            type.setText(value);
            identifier.setType(type);
        }
        identifier.setValue(value);
        return identifier;
    }

    /**
     * Sets specimen details.
     * * This method sets the details of a specimen based on the provided CSV record, file name, patient, and specimen group list.
     * * It creates a new Specimen object and populates its fields based on the values in the CSV record.
     *
     * @param nextRecord        the next record
     * @param fileName          the file name
     * @param patient           the patient
     * @param specimenGroupList the specimen group list
     * @return the specimen
     * @implNote The method checks if the sample ID is present in the headers list and if it is not equal to "Unknown".
     * * It generates a unique ID for the specimen by using the sample ID from the CSV record.
     * * If the specimen with the generated ID does not already exist, it sets the ID, identifiers, collection method,
     * * body site, condition, subject, and batch extension for the specimen.
     */
    private static Specimen setSpecimenDetails(CSVRecord nextRecord, String fileName, Patient patient, List<Group> specimenGroupList, List<MolecularSequence> molecularSequenceList) {

        Specimen specimen = new Specimen();
        Identifier fileIdentifier = new Identifier();
        fileIdentifier.setId(fileName);
        specimen.setAccessionIdentifier(fileIdentifier);
        String specimenId = getIdValue(nextRecord.get(SAMPLE_ID));
        specimen.setId(specimenId);
        specimen.addIdentifier(setOfficialId(specimenId, SAMPLE_ID));
        setSpecimenCollectionForProtocolAndOrgan(nextRecord, specimen);
        if (headersList.contains(DISORDER)) {
            specimen.setCondition(Collections.singletonList(setCodeableConceptDetails(DISORDER, nextRecord)));
        }
        Group specimenGroup = setAndGetSpecimenGroup(fileName,nextRecord, specimenGroupList,  specimen);
        if (specimenGroup == null) return null;
        String groupId = specimenGroup.getId();
        setAndGetMolecularSequencesDetails(nextRecord, fileName, specimenGroup, molecularSequenceList);
        setGroupExtensionForSpecimen(groupId, specimenGroup, specimen);
        specimen.addContained(specimenGroup);
        if (!patient.isEmpty()) {
            specimen.setSubject(setSubjectPatient(patient));
            specimen.addContained(patient);
        }
        return specimen;
    }

    private static void setSpecimenCollectionForProtocolAndOrgan(CSVRecord nextRecord, Specimen specimen) {
        Specimen.SpecimenCollectionComponent collection = new Specimen.SpecimenCollectionComponent();
        if (headersList.contains(PROTOCOL)) {
            collection.setMethod(setCodeableConceptDetails(PROTOCOL, nextRecord));
        }
        if (headersList.contains(ORGAN)) {
            if (!tissuesList.contains(nextRecord.get(ORGAN))) {
                tissuesList.add(nextRecord.get(ORGAN));
            }
            CodeableReference organ = new CodeableReference();
            organ.setConcept(setCodeableConceptDetails(ORGAN, nextRecord));
            collection.setBodySite(organ);
        }
        specimen.setCollection(collection);
    }

    private static @Nullable Group setAndGetSpecimenGroup(String fileName, CSVRecord nextRecord, List<Group> specimenGroupList, Specimen specimen) {
        String specimenGroupId;
        if (headersList.contains(BATCH)) {
            specimenGroupId = nextRecord.get(BATCH);
        } else {
            specimenGroupId = "Default_Batch";
        }
        Group specimenGroup = setSpecimenGroup(specimenGroupId, fileName, specimen, specimenGroupList);
        if (specimenGroup == null || specimenGroup.isEmpty()) {
            logger.info("No Group found!");
            return null;
        }
        specimenGroup.setId(ResourcesFetcher.addOrUpdateGroupTOServerFHIRAndGetID(specimenGroup));
        return specimenGroup;
    }

    private static void setGroupExtensionForSpecimen(String groupId, Group specimenGroup, Specimen specimen) {
        Extension batchExtension = new Extension();
        batchExtension.setUrl("http://localhost:7000/api/fhir/Group/" + groupId);
        batchExtension.setValue(setGroupReference(specimenGroup));
        List<Extension> extensionList = new ArrayList<>();
        extensionList.add(batchExtension);
        specimen.setExtension(extensionList);
    }

    /**
     * Sets specimen group.
     * * This method sets the specimen group based on the provided CSV record, file name, specimen, and specimen group list.
     * * It creates a new Group object and populates its fields based on the values in the CSV record.
     *
     * @param batchId           the batch id
     * @param fileName          the file name
     * @param specimen          the specimen
     * @param specimenGroupList the specimen group list
     * @return the group
     * @implNote The method checks if a specimen group already exists for the given batch identifier.
     * * If it does not exist, it creates a new group with the specified type and adds the specimen as a member.
     * * If the group already exists and the specimen is not already a member, it adds the specimen to the existing group.
     */
    private static Group setSpecimenGroup(String batchId, String fileName, Specimen specimen, List<Group> specimenGroupList) {
        Group.GroupMemberComponent member = new Group.GroupMemberComponent();
        member.setEntity(setSubjectSpecimen(specimen));
        Group isSpecimenGroupFound = getExistingSpecimenGroup(fileName, batchId, specimenGroupList);
        if (isSpecimenGroupFound == null) {
            Group specimenGroup = new Group();
            specimenGroup.addIdentifier(setAccessionId(fileName));
            specimenGroup.addIdentifier(setOfficialId(batchId, BATCH));
            specimenGroup.setType(Group.GroupType.SPECIMEN);
            specimenGroup.addMember(member);
            specimenGroupList.add(specimenGroup);
            return specimenGroup;
        } else if (!isSpecimenGroupFound.getMember().contains(member)) {
            isSpecimenGroupFound.addMember(member);
            return isSpecimenGroupFound;
        }
        return null;
    }

    /**
     * Sets codeable concept details.
     * * This method sets the details of a CodeableConcept based on the provided extension string and CSV record.
     * * It creates a new CodeableConcept object and populates its fields based on the values in the CSV record.
     *
     * @param extensionString the extension string
     * @param nextRecord      the next record
     * @return the codeable concept
     * @implNote The method adds a coding to the CodeableConcept with the code and display set to the value from the CSV record.
     * * It also sets the text of the CodeableConcept to the provided extension string.
     */
    private static CodeableConcept setCodeableConceptDetails(String extensionString, CSVRecord nextRecord) {
        CodeableConcept extensionConcept = new CodeableConcept();
        extensionConcept.addCoding().setCode(nextRecord.get(extensionString)).setDisplay(nextRecord.get(extensionString));
        extensionConcept.setText(extensionString);
        return extensionConcept;
    }

    /**
     * Gets molecular sequence.
     * * This method retrieves a molecular sequence based on the provided molecular sequence id.
     * * It searches through the list of molecular sequences and returns the first one that matches the given id.
     *
     * @param molecularSequenceId the molecular sequence id
     * @return the molecular sequence
     * @implNote The method uses a stream to filter the molecular sequences list and find the matching id. * * If no matching molecular sequence is found, it returns null.
     * @implSpec The method is static and can be called without creating an instance of the class.
     */
    public static MolecularSequence getMolecularSequence(String molecularSequenceId) {
        return molecularSequencesList.stream().filter(molecularSequence -> Objects.equals(molecularSequence.getId(), molecularSequenceId)).findFirst().orElse(null);
    }

    /**
     * Gets id value.
     * * This method retrieves the ID value from the provided string.
     * * It checks if the ID matches a specific pattern and sanitizes it if necessary.
     * * If the ID contains invalid characters, it replaces them with underscores and removes leading or trailing underscores.
     *
     * @param id the id
     * @return the id value
     * @implNote The method uses regular expressions to validate and sanitize the ID.
     */
    private static String getIdValue(String id) {
        if (!id.matches("^[A-Za-z0-9_-]+$")) {
            String sanitizedId = id.replaceAll("[^A-Za-z0-9_-]", "_");
            sanitizedId = sanitizedId.replaceAll("(^_+)|(_+$)", "");
            return sanitizedId;
        }
        return id;
    }

    /**
     * Sets subject specimen.
     * * This method sets the subject specimen for a specimen group.
     * * It creates a new Reference object and sets its ID, type, and resource based on the provided specimen.
     *
     * @param specimen the specimen
     * @return the reference
     * @implNote The method checks if the specimen is not null before creating the reference.
     * * If the specimen is null, it returns null.
     */
    private static Reference setSubjectSpecimen(Specimen specimen) {
        if (specimen != null) {
            Reference reference = new Reference("Specimen/" + specimen.getId());
            reference.setId(specimen.getId());
            reference.setType("Specimen");
            reference.setResource(specimen);
            return reference;
        }
        return null;
    }

    /**
     * Sets subject patient.
     * * This method sets the subject patient for a specimen.
     * * It creates a new Reference object and sets its ID, type, and resource based on the provided patient.
     *
     * @param patient the patient
     * @return the reference
     * @implNote The method checks if the patient is not null before creating the reference.
     * * If the patient is null, it returns null.
     */
    private static Reference setSubjectPatient(Patient patient) {
        if (patient != null) {
            Reference reference = new Reference("http://localhost:7000/api/fhir/Patient/" + patient.getId());
            reference.getExtensionByUrl("http://localhost:7000/api/fhir/Patient/" + patient.getId());
            reference.setId(patient.getId());
            UriType uriType = new UriType("https://hl7.org/fhir/fhir-types");
            reference.setTypeElement(uriType);
            reference.setType("Patient");
            reference.setResource(patient);
            return reference;
        }
        return null;
    }

    /**
     * Sets group reference.
     * * This method sets the specimen group reference for a specimen.
     * * It creates a new Reference object and sets its ID, type, and resource based on the provided group.
     *
     * @param group the group
     * @return the reference
     * @implNote The method checks if the group is not null before creating the reference.
     * * If the group is null, it returns null.
     */
    private static Reference setGroupReference(Group group) {
        if (group != null) {
            Reference reference = new Reference("Group/" + group.getId());
            reference.setId(group.getId());
            reference.setType("Group");
            reference.setResource(group);
            return reference;
        }
        return null;
    }

    /**
     * Checks if the patient is already found in the list.
     * * This method checks if a patient with the same ID already exists in the provided patient list.
     * * It uses a stream to filter the patient list and returns true if a matching patient is found, false otherwise.
     *
     * @param nextRecord  the next record
     * @param patientList the patient list
     * @return true if the patient is already found, false otherwise
     */
    private static boolean isPatientAlreadyFound(CSVRecord nextRecord, List<Patient> patientList) {
        return patientList.stream().filter(pat -> Objects.equals(pat.getId(), getIdValue(nextRecord.get(PATIENT_ID)))).findFirst().orElse(null) == null;
    }

    /**
     * Checks if the specimen is already existing in the list.
     * * This method checks if a specimen with the same ID already exists in the provided specimen list.
     * * It uses a stream to filter the specimen list and returns true if a matching specimen is found, false otherwise.
     *
     * @param nextRecord   the next record
     * @param specimenList the specimen list
     * @return true if the specimen is already existing, false otherwise
     */
    private static boolean isSpecimenAlreadyExisting(CSVRecord nextRecord, List<Specimen> specimenList) {
        String specimenId = getIdValue(nextRecord.get(SAMPLE_ID));
        return specimenList.stream().filter(specimen -> Objects.equals(specimen.getId(), specimenId)).findFirst().orElse(null) == null;
    }

    /**
     * Checks if the specimen group is already found in the list.
     * * This method checks if a specimen group with the same ID already exists in the provided specimen group list.
     * * It uses a stream to filter the specimen group list and returns the first matching group if found, null otherwise.
     *
     * @param fileName          the file name as accession identifier
     * @param batchId           the batch id as official identifier
     * @param specimenGroupList the specimen group list
     * @return the group if found, null otherwise
     */
    private static Group getExistingSpecimenGroup(String fileName, String batchId, List<Group> specimenGroupList) {
        return specimenGroupList.stream().filter(group -> {
            List<Identifier> identifiersList = group.getIdentifier().stream().toList();
            return verifyByAccessionAndOfficialID(fileName, batchId, identifiersList);
        }).findFirst().orElse(null);
    }

    /**
     * Checks if the molecular sequence is already existing in the list and return the molecular sequence if found.
     * * This method checks if a molecular sequence with the same accession and official ID already exists in the provided molecular sequence list.
     * * It uses a stream to filter the molecular sequence list and returns the first matching molecular sequence if found, null otherwise.
     *
     * @param fileName              the file name as accession identifier
     * @param id                    the official id
     * @param molecularSequenceList the molecular sequence list
     * @return the molecular sequence if found, null otherwise
     */
    private static MolecularSequence isMolecularSequenceExisting(String fileName, String id, List<MolecularSequence> molecularSequenceList) {
        return molecularSequenceList.stream().filter(molecularSequence -> {
            List<Identifier> identifiersList = molecularSequence.getIdentifier().stream().toList();
            return verifyByAccessionAndOfficialID(fileName, id, identifiersList);
        }).findFirst().orElse(null);
    }

    /**
     * Verify by accession and official id and return boolean.
     * * This method verifies if the provided file name and ID match the official and accession identifiers in the list.
     * * It retrieves the official and accession identifiers from the list and compares them with the provided values.
     *
     * @param fileName        the file name as accession identifier
     * @param id              the official Identifier
     * @param identifiersList the identifiers list
     * @return true if both identifiers match, false otherwise
     */
    private static boolean verifyByAccessionAndOfficialID(String fileName, String id, List<Identifier> identifiersList) {
        String officialIdentifier = ResourcesFetcher.getOfficialIdentifier(identifiersList);
        String accessionIdentifier = ResourcesFetcher.getAccessionIdentifier(identifiersList);
        return Objects.equals(officialIdentifier, id) && Objects.equals(accessionIdentifier, fileName);
    }

    /**
     * Gets gender.
     * * This method retrieves gender based on the provided code string.
     * * It checks if the code string is not null or empty and compares it with predefined
     *
     * @param codeString the code string
     */
    private static Enumerations.AdministrativeGender getGender(String codeString) {
        if (codeString != null && !codeString.isEmpty()) {
            if ("male".equalsIgnoreCase(codeString) || "m".equalsIgnoreCase(codeString)) {
                return Enumerations.AdministrativeGender.MALE;
            } else if ("female".equalsIgnoreCase(codeString) || "f".equalsIgnoreCase(codeString)) {
                return Enumerations.AdministrativeGender.FEMALE;
            } else if ("other".equalsIgnoreCase(codeString) || "O".equalsIgnoreCase(codeString)) {
                return Enumerations.AdministrativeGender.OTHER;
            } else {
                return Enumerations.AdministrativeGender.UNKNOWN;
            }
        } else {
            return null;
        }
    }

    /**
     * Sets official id.
     * * This method sets the official identifier for a resource based on the provided ID and code.
     * * It creates a new Identifier object and populates its fields with the given values.
     *
     * @param id   the id
     * @param code the code
     * @return the identifier
     * @implNote The method sets the ID, use code, type code, and value for the identifier.
     */
    private static @NotNull Identifier setOfficialId(String id, String code) {
        return setIdentifier(id, code, Identifier.IdentifierUse.OFFICIAL, null);
    }

    /**
     * Sets accession id.
     * * This method sets the accession identifier for a resource based on the provided file name.
     * * It creates a new Identifier object and populates its fields with the given values.
     *
     * @param fileName the file name
     * @return the identifier
     * @implNote The method sets the ID, use code, type code, and value for the identifier.
     */
    private static @NotNull Identifier setAccessionId(String fileName) {
        return setIdentifier(fileName, "Accession ID", Identifier.IdentifierUse.SECONDARY, "ACSN");
    }

    /**
     * Add and get patient.
     * * This method adds a patient to the patient list if it is not already present.
     * * It sets the patient details based on the provided CSV record and file name.
     * * If the patient is not empty and is not already found in the patient list, it adds the patient to the list.
     *
     * @param nextRecord  the next record
     * @param fileName    the file name
     * @param patientList the patient list
     * @return the patient
     * @implNote The method uses the setPatientDetails method to create a new Patient object
     * * and checks if the patient is already found in the patient list using the isPatientAlreadyFound method.
     */
    private static Patient addAndGetPatient(CSVRecord nextRecord, String fileName, List<Patient> patientList) {
        Patient patient = setPatientDetails(nextRecord, fileName);
        if (!patient.isEmpty() && isPatientAlreadyFound(nextRecord, patientList)) patientList.add(patient);
        return patient;

    }

    /**
     * Sets molecular sequences details.
     * * This method sets the details of molecular sequences based on the provided CSV record, file name, group, and molecular sequence list.
     * * It creates a new MolecularSequence object and populates its fields based on the values in the CSV record.
     *
     * @param nextRecord            the next record
     * @param fileName              the file name
     * @param group                 the group
     * @param molecularSequenceList the molecular sequence list
     * @implNote The method checks if the sequence type is present in the headers list and if it is not null. * It generates a unique ID for the molecular sequence by combining the file name and sequence type. * * If the molecular sequence with the generated ID does not already exist, it sets the ID, identifiers, * type, and extensions for the molecular sequence.
     * @implSpec The method also sets the cellular resolution extension with a specific URL and value.
     */
    static void setAndGetMolecularSequencesDetails(CSVRecord nextRecord, String fileName, Group group, List<MolecularSequence> molecularSequenceList) {
        if (headersList.contains(SEQUENCE_TYPE) && nextRecord.get(SEQUENCE_TYPE) != null) {
            List<Identifier> identifiersList = group.getIdentifier().stream().toList();
            String officialIdentifier = ResourcesFetcher.getOfficialIdentifier(identifiersList);
            String id = officialIdentifier + "_" + nextRecord.get(SEQUENCE_TYPE);
            MolecularSequence foundMolecularSequence = isMolecularSequenceExisting(fileName, id, molecularSequenceList);
            if (foundMolecularSequence == null) {
                MolecularSequence molecularSequence = new MolecularSequence();
                if (getMolecularSequence(id) == null) {
                    molecularSequence.setId(id);
                    molecularSequence.addIdentifier(setAccessionId(fileName));
                    molecularSequence.addIdentifier(setOfficialId(id, MOLECULAR_SEQUENCE_ID));
                    molecularSequence.setType(MolecularSequence.SequenceType.fromCode(nextRecord.get(SEQUENCE_TYPE)));
                    molecularSequence.setSubject(setGroupReference(group));
                    Extension cellularResolutionExtension = new Extension();
                    cellularResolutionExtension.setUrl("http://localhost:7000/api/fhir/cellularResolution");
                    cellularResolutionExtension.setValue(setCodeableConceptDetails(CELLULAR_RESOLUTION, nextRecord));
                    List<Extension> extensionList = new ArrayList<>();
                    extensionList.add(cellularResolutionExtension);
                    molecularSequence.setExtension(extensionList);
                    molecularSequenceList.add(molecularSequence);
                }
            }

        }
    }

    /**
     * Store all resources in FHIR server.
     * * This method stores all resources : Patients, Specimens, Specimens Group and Molecular Sequence from a metadata file into the FHIR server.
     * * It first checks if the molecular sequence with the given file name already exists on the server.
     * * If it does not exist, it retrieves the metadata file from CouchDB, processes the CSV records,
     * * * and adds the patients, specimens, specimen groups, and molecular sequence resources to the FHIR server.
     *
     * @param fileName the file name
     * @throws FileNotFoundException the file not found exception
     * @implNote The method uses the ResourcesFetcher class to interact with the FHIR server and perform the necessary operations.
     * @implSpec The method logs the duration of the operation in milliseconds.
     */
    public void storeAllResources(String fileName) throws FileNotFoundException {
        Instant start = Instant.now();
        if (ResourcesFetcher.getMolecularSequenceFromServerByAccessionIdentifier(fileName).isEmpty()) {
            List<Patient> patientList = new ArrayList<>();
            List<Specimen> specimenList = new ArrayList<>();
            List<Group> specimenGroupList = new ArrayList<>();
            List<MolecularSequence> molecularSequenceList = new ArrayList<>();
            File tempFile = fileSystemStorageService.getMetadataFileFromCouchDBByFileName(fileName);
            List<CSVRecord> csvRecordList;
            if (tempFile != null) {
                csvRecordList = Helper.getCSVRecords(tempFile);
                for (CSVRecord nextRecord : csvRecordList) {
                    Patient patient = addAndGetPatient(nextRecord, fileName, patientList);
                    addAndGetSpecimen(nextRecord, fileName, specimenList, patient, specimenGroupList, molecularSequenceList);
                }
                ResourcesFetcher.addSpecimenResourcesToFHIRServer(specimenList);
                ResourcesFetcher.addListOfPatientsResourcesToFHIRServer(patientList);
                ResourcesFetcher.addListOfMolecularSequenceResourcesToFHIRServer(molecularSequenceList);
            }
        } else {
            ourLog.info("The resources on this metadata file are already on the FHIR server");
        }
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        logger.info("{} Milli seconds", durationMs);

    }

    /**
     * Add and get specimen.
     * * This method adds a specimen to the specimen list if it is not already present.
     * * It sets the specimen details based on the provided CSV record, file name, patient, specimen group list and molecular sequence list.
     * * If the sample ID is present in the headers list and is not equal to "Unknown", it adds the specimen to the list
     * * if it is not already existing.
     *
     * @param nextRecord            the next record
     * @param fileName              the file name
     * @param specimenList          the specimen list
     * @param patient               the patient
     * @param specimenGroupList     the specimen group list
     * @param molecularSequenceList the molecular sequence list
     * @implNote The method uses the setSpecimenDetails method to create a new Specimen object
     * * and checks if the specimen is already existing in the specimen list using the isSpecimenAlreadyExisting method.
     */
    private void addAndGetSpecimen(CSVRecord nextRecord, String fileName, List<Specimen> specimenList, Patient patient, List<Group> specimenGroupList, List<MolecularSequence> molecularSequenceList) {
        Specimen specimen;
        if (headersList.contains(SAMPLE_ID) && !nextRecord.get(SAMPLE_ID).equalsIgnoreCase(UNKNOWN)) {
            specimen = setSpecimenDetails(nextRecord, fileName, patient, specimenGroupList, molecularSequenceList);
            if (isSpecimenAlreadyExisting(nextRecord, specimenList)) specimenList.add(specimen);
        }

    }
}
