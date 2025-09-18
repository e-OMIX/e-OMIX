import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, OnInit, ViewEncapsulation } from '@angular/core';
import { MaterialModule } from '../material.module';
import { MatTableDataSource } from '@angular/material/table';
import { NestedTreeControl } from '@angular/cdk/tree';
import { MatTreeNestedDataSource } from '@angular/material/tree';
import { FHIRService } from '../service/fhir.service';
import { FileManagementService } from '../service/file-management.service';
import { forkJoin } from 'rxjs';
import { GroupBundle } from '../models/FHIR/groupResource';
import { MolecularSequenceBundle } from '../models/FHIR/molecularSequenceResource';
import { Bundle } from '../models/FHIR/patientResource';
import { SpecimenBundle } from '../models/FHIR/specimenResource';
import { MolecularSequenceTreeNode, BatchTreeNode, SpecimenTreeNode } from '../models/FHIR/treeNode';


@Component({
  selector: 'app-fhir-tree',
  imports: [CommonModule, MaterialModule],
  templateUrl: './fhir-tree.component.html',
  styleUrl: './fhir-tree.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class FhirTreeComponent implements OnInit {
  specimenEntries: any[] = [];
  molecularSequencesEntries: any[] = [];
  batchEntries: any[] = [];
  patientsEntries: any[] = [];
  batchCount: number = 0;
  showFileDropdown = false;
  selectedMetadataFileDetails: any = null;
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  allFiles: any[] = [];
  loadingOnSampleIds = false;
  // Tree node predicates
  isMolecularSequenceNode = (_: number, node: any) => node.level === 0;
  isBatchNode = (_: number, node: any) => node.level === 1;
  isSpecimenNode = (_: number, node: any) => node.level === 2;

  hasChild = (_: number, node: any) =>
    !!node.children && node.children.length > 0 && node.expandable;
  // #region Tree Control Properties
  treeControl = new NestedTreeControl<any>(node => node.children);
  treeDataSource = new MatTreeNestedDataSource<any>();
  treeData: any[] = [];
  // #endregion Tree Control Properties

  // #region Constructor
  constructor(
    private readonly fhirService: FHIRService,
    private readonly fileManagementService: FileManagementService,
    private readonly cdr: ChangeDetectorRef
  ) { }
  // #endregion Constructor

  // #region Initialization, Listener and Lifecycle Hooks
  ngOnInit(): void {
    this.getAllFiles();
  }

  @HostListener('window:beforeunload', ['$event'])
  preventRefresh(event: Event) {
    if (this.loadingOnSampleIds) {
      event.preventDefault();
      (event as BeforeUnloadEvent).returnValue = 'Data is still loading. Are you sure you want to leave?';
    }
  }
  // #endregion Initialization, Listener and Lifecycle Hooks

  //#region 📂 Data Fetching Methods
  getAllFiles(): void {
    this.fileManagementService.getMetadataFiles().subscribe({
      next: (files) => {
        if (files) {
          this.filesDataSource.data = files;
          this.allFiles = files;
        }
      },
      error: (error) => {
        console.error('Error fetching files:', error);
      }
    });
  }
  getSampleIds() {
    if (this.selectedMetadataFileDetails) {
      this.loadingOnSampleIds = true;
      this.fhirService.saveResourcesOnServer(this.selectedMetadataFileDetails).subscribe(() => {
        // Fetch all resources in parallel
        forkJoin({
          molecularSequences: this.fhirService.getAllMolecularSequences(this.selectedMetadataFileDetails),
          groups: this.fhirService.getAllGroups(this.selectedMetadataFileDetails),
          specimens: this.fhirService.getAllSamples(this.selectedMetadataFileDetails),
          patients: this.fhirService.getAllPatientsDetails(this.selectedMetadataFileDetails)
        }).subscribe({
          next: (results) => {
            this.loadingOnSampleIds = false;
            this.getAllEntriesFromResult(results);
            this.transformToTreeData(this.molecularSequencesEntries, this.batchEntries, this.specimenEntries, this.patientsEntries);
            this.cdr.detectChanges();
          },
          error: (error) => {
            this.loadingOnSampleIds = false;
            console.error('Error fetching resources:', error);
          }
        });
      },
        (uploadError) => {
          this.loadingOnSampleIds = false;
          console.error('Error uploading resources:', uploadError);
        });
    }
  }
  // #endregion 📂 Data Fetching Methods

  // #region Set Nodes
  // Transform FHIR data to tree structure
  private transformToTreeData(
    molecularSequenceEntries: any[],
    groupEntries: any[],
    specimenEntries: any[],
    patientEntries: any[]
  ): void {
    const treeData: MolecularSequenceTreeNode[] = [];
    this.setNodes(molecularSequenceEntries, groupEntries, specimenEntries, patientEntries, treeData);
    this.treeData = treeData;
    this.treeDataSource.data = this.treeData;
    this.batchCount = treeData.reduce((total, seqNode) => total + seqNode.batchCount, 0);
    // Expand molecular sequence level by default
    this.treeData.forEach(node => this.treeControl.expand(node));
  }
  private setNodes(molecularSequenceEntries: any[], groupEntries: any[], specimenEntries: any[], patientEntries: any[], treeData: MolecularSequenceTreeNode[]) {
    molecularSequenceEntries.forEach(seqEntry => {
      const molecularSequence = seqEntry.resource;
      // Find the associated group using the subject reference
      this.setMolecularSequenceTreeNode(molecularSequence, groupEntries, specimenEntries, patientEntries, treeData);
    });
  }
  private setMolecularSequenceTreeNode(molecularSequence: any, groupEntries: any[], specimenEntries: any[], patientEntries: any[], treeData: MolecularSequenceTreeNode[]) {
    const groupReference = molecularSequence.subject?.reference;
    let associatedGroup: any = null;
    associatedGroup = this.getAssociatedGroup(groupReference, associatedGroup, groupEntries);
    const molecularSequenceNode: MolecularSequenceTreeNode = this.setMolecularSequenceDetails(molecularSequence, associatedGroup);
    if (associatedGroup) {
      // Create batch node from the associated group
      this.setBatchNode(associatedGroup, specimenEntries, patientEntries, molecularSequenceNode);
      treeData.push(molecularSequenceNode);
    }
  }
  private setBatchNode(associatedGroup: any, specimenEntries: any[], patientEntries: any[], molecularSequenceNode: MolecularSequenceTreeNode) {
    const batchNode: BatchTreeNode = this.setBatchDetails(associatedGroup);
    // Add specimens to batch
    this.addSpecimenRelatedToBatch(associatedGroup, specimenEntries, patientEntries, batchNode);
    molecularSequenceNode.children!.push(batchNode);
  }
  private setSpecimenNode(specimen: any, patientEntries: any[], batchNode: BatchTreeNode) {
    if (specimen) {
      const specimenNode: SpecimenTreeNode = this.setSpecimenDetails(specimen, patientEntries);
      batchNode.children!.push(specimenNode);
    }
  }
  // #endregion Set Nodes

  //  #region Set Reources Details
  private addSpecimenRelatedToBatch(associatedGroup: any, specimenEntries: any[], patientEntries: any[], batchNode: BatchTreeNode) {
    if (associatedGroup.member) {
      associatedGroup.member.forEach((member: any) => {
        const specimenId = this.extractSpecimenIdFromReference(member.entity.reference);
        const specimen = this.getSpecimenById(specimenEntries, specimenId);
        this.setSpecimenNode(specimen, patientEntries, batchNode);
      });
    }
  }
  private setBatchDetails(associatedGroup: any): BatchTreeNode {
    return {
      name: this.getBatchName(associatedGroup),
      id: associatedGroup.id,
      accessionId: this.getAccessionId(associatedGroup),
      specimenCount: associatedGroup.member?.length || 0,
      children: [],
      level: 1,
      expandable: true
    };
  }
  private setMolecularSequenceDetails(molecularSequence: any, associatedGroup: any): MolecularSequenceTreeNode {
    return {
      name: this.getMolecularSequenceName(molecularSequence),
      id: molecularSequence.id,
      type: molecularSequence.type || 'unknown',
      cellularResolution: this.getCellularResolution(molecularSequence),
      accessionId: this.getAccessionId(molecularSequence),
      batchCount: associatedGroup ? 1 : 0,
      children: [],
      level: 0,
      expandable: true
    };
  }
  private setSpecimenDetails(specimen: any, patientEntries: any[]): SpecimenTreeNode {
    return {
      name: this.getSpecimenId(specimen.resource),
      id: specimen.resource.id,
      assay: this.getAssay(specimen.resource),
      organ: this.getOrgan(specimen.resource),
      condition: this.getCondition(specimen.resource),
      patient: this.getPatientInfo(specimen.resource, patientEntries),
      level: 2,
      expandable: true
    };
  }
  private setPatientDetails(patient: any): { id: string; gender: string; age?: string; species?: string } {
    return {
      id: this.getPatientId(patient),
      gender: patient.gender || 'Unknown',
      age: this.getPatientAge(patient),
      species: this.getPatientSpecies(patient)
    };
  }
  // #endregion Reources Details


  //#region 🔍 extract data from FHIR resources
  private getAllEntriesFromResult(results: { molecularSequences: MolecularSequenceBundle; groups: GroupBundle; specimens: SpecimenBundle; patients: Bundle; }) {
    this.molecularSequencesEntries = results.molecularSequences?.entry || [];
    this.batchEntries = results.groups?.entry || [];
    this.specimenEntries = results.specimens?.entry || [];
    this.patientsEntries = results.patients?.entry || [];
  }
  private getSpecimenById(specimenEntries: any[], specimenId: string) {
    return specimenEntries.find(entry => entry.resource.id === specimenId ||
      this.getSpecimenId(entry.resource) === specimenId
    );
  }
  private getAssociatedGroup(groupReference: any, associatedGroup: any, groupEntries: any[]) {
    if (groupReference) {
      const groupId = groupReference.split('/')[1];
      associatedGroup = groupEntries.find(entry => entry.resource.id === groupId
      )?.resource;
    }
    return associatedGroup;
  }
  private getMolecularSequenceName(molecularSequence: any): string {
    return molecularSequence.identifier?.find((id: any) => id.use === 'official')?.id ||
      'Unknown Sequence';
  }
  private getCellularResolution(molecularSequence: any): string {
    const resolutionExt = molecularSequence.extension?.find((ext: any) =>
      ext.url.includes('cellularResolution')
    );

    return resolutionExt?.valueCodeableConcept?.coding?.[0]?.display ||
      resolutionExt?.valueCodeableConcept?.text ||
      'Unknown';
  }
  private getAccessionId(resource: any): string {
    const accessionId = resource.identifier?.find((id: any) =>
      id.type?.text === 'Accession ID' || id.type?.coding?.[0]?.code === 'ACSN'
    );

    return accessionId?.id || 'No Accession ID';
  }
  private getBatchName(group: any): string {
    const officialId = group.identifier?.find((id: any) => id.use === 'official');
    return officialId?.id || 'Unknown Batch';
  }
  private getSpecimenId(specimen: any): string {
    return specimen.identifier?.find((id: any) => id.use === 'official')?.id ||
      'Unknown Specimen';
  }
  private extractSpecimenIdFromReference(reference: string): string {
    if (!reference) return '';
    return reference.split('/')[1] || reference;
  }
  private getAssay(specimen: any): string {
    return specimen.collection?.method?.coding?.[0]?.display ||
      specimen.collection?.method?.text ||
      'Unknown Assay';
  }
  private getOrgan(specimen: any): string {
    return specimen.collection?.bodySite?.concept?.coding?.[0]?.display ||
      specimen.collection?.bodySite?.concept?.text ||
      'Unknown Organ';
  }
  private getCondition(specimen: any): string {
    return specimen.condition?.[0]?.coding?.[0]?.display ||
      specimen.condition?.[0]?.text ||
      'Unknown Condition';
  }
  private getPatientInfo(specimen: any, patientEntries: any[]): { id: string; gender: string; age?: string; species?: string } | undefined {
    // First, check if patient is contained within the specimen
    if (specimen.contained) {
      return this.getPatientFromContained(specimen);
    }
    else {
      return this.getPatientFromReferenceOrExtension(specimen, patientEntries);
    }
  }
  getPatientFromContained(specimen: any): { id: string; gender: string; age?: string; species?: string } | undefined {
    const containedPatient = specimen.contained.find((resource: any) =>
      resource.resourceType === 'Patient'
    );
    if (containedPatient) {
      return this.setPatientDetails(containedPatient);
    }
  }
  getPatientFromReferenceOrExtension(specimen: any, patientEntries: any[]): { id: string; gender: string; age?: string; species?: string } | undefined {
    let patientRef = specimen.subject?.reference;
    let patientId = specimen.subject?.id;
    patientRef = this.getPatientFromExtension(patientRef, specimen);
    if (patientRef) {
      const patient = patientEntries.find(entry =>
        entry.resource.id === patientId
      )?.resource;

      if (patient) {
        return this.setPatientDetails(patient);
      }
    }
  }
  private getPatientFromExtension(patientRef: any, specimen: any) {
    if (!patientRef) {
      // Check extensions for patient reference
      const patientExt = specimen.extension?.find((ext: any) => ext.valueReference?.type === 'Patient'
      );
      patientRef = patientExt?.valueReference?.reference;
    }
    return patientRef;
  }
  private getPatientAge(patient: any): string {
    const ageExt = patient.extension?.find((ext: any) =>
      ext.url.includes('patient-birthTime') || ext.url.includes('age')
    );

    return ageExt?.valueCodeableConcept?.coding?.[0]?.display ||
      'Unknown age';
  }
  private getPatientSpecies(patient: any): string {
    const speciesExt = patient.extension?.find((ext: any) =>
      ext.url.includes('animal-species') || ext.url.includes('species')
    );
    return speciesExt?.valueCodeableConcept?.coding?.[0]?.display ||
      'Unknown species';
  }
  private getPatientId(patient: any): string {
    return patient.identifier?.find((id: any) => id.use === 'official')?.id ||
      'Unknown Patient';
  }
  // #endregion extract data from FHIR resources

}