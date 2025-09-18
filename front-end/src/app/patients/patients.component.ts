import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { FHIRService } from '../service/fhir.service';
import { Bundle, PatientEntry } from '../models/FHIR/patientResource';
import { MatTableDataSource } from '@angular/material/table';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../material.module';
import { FileManagementService } from '../service/file-management.service';
@Component({
  selector: 'app-patients',
  imports: [CommonModule, MaterialModule],
  templateUrl: './patients.component.html',
  styleUrl: './patients.component.scss'
})
export class PatientsComponent implements OnInit {

  entries: PatientEntry[] = [];
  dataSource = new MatTableDataSource<PatientEntry>();
  loadedData: boolean = false;
  result: boolean = false;
  // displayedPatientColumns: string[] = ['id'];
  showFileDropdown = false;
  selectedMetadataFileDetails: any = null;
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  allFiles: any[] = [];
  isTableVisible: boolean = true;
  loadingOnPatientDetails = false;
  displayedPatientColumns: string[] = ['id', 'age', 'gender', 'species'];
  // #region Constructor
  constructor(
    private readonly patientService: FHIRService,
    private readonly fileManagementService: FileManagementService,
    private readonly cdr: ChangeDetectorRef) {
  }
  // #endregion constructor

  // #region Initialization and Listener 
  ngOnInit(): void {
    this.getAllFiles();
    this.getPatientDetails();
  }
  @HostListener('window:beforeunload', ['$event'])
  preventRefresh(event: Event) {
    if (this.loadingOnPatientDetails) {
      event.preventDefault();
      (event as BeforeUnloadEvent).returnValue = 'Data is still loading. Are you sure you want to leave?';
    }
  }
  // #endregion Initialization and Listener

  // #region Fetch Data
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
  getPatientDetails() {
    if (this.selectedMetadataFileDetails) {
      this.entries = [];
      this.dataSource.data = [];
      this.loadedData = false;
      this.patientService.saveResourcesOnServer(this.selectedMetadataFileDetails).subscribe(() => {
        this.getPatientsDetails();
      },
        (error: any) => {
          this.loadingOnPatientDetails = false;
          this.entries = [];
          this.dataSource.data = [];
          this.cdr.detectChanges();
          console.error('Error:', error);
          alert('An error occurred while geting ids.');
        });
    }
  }
  private getPatientsDetails() {
    this.patientService.getAllPatientsDetails(this.selectedMetadataFileDetails).subscribe(
      data => {
        this.loadingOnPatientDetails = false;
        this.getMetadataData(data);

      });
  }
  private getMetadataData(data: Bundle) {
    if (data?.entry) {
      this.entries = data.entry;
      this.dataSource.data = data.entry;
      this.getTableColumns();
      this.loadedData = true;
      this.cdr.detectChanges();
    } else {
      this.entries = [];
      this.dataSource.data = [];
      this.cdr.detectChanges();
      console.warn('No entries found in the patient data.');
    }
  }
  getPatientSpecies(patient: any): string {
    const speciesExt = patient.extension?.find((ext: any) =>
      ext.url.includes('animal-species') || ext.url.includes('species')
    );
    return speciesExt?.valueCodeableConcept?.coding?.[0]?.display ||
      'Unknown species';
  }
  getPatientAge(patient: any): string {
    const ageExt = patient.extension?.find((ext: any) =>
      ext.url.includes('patient-birthTime') || ext.url.includes('age')
    );

    return ageExt?.valueCodeableConcept?.coding?.[0]?.display ||
      'Unknown age';
  }
  //#endregion Fetch Data

  // #region Set Table Columns
  private getTableColumns() {
    const columnsToDisplay = [
      'id', 'age', 'gender', 'species'
    ];
    this.displayedPatientColumns = columnsToDisplay.filter(column => {
      return this.isColumnDisplayable(column);
    });
  }
  private isColumnVisibleForEntry(column: string, entry: any): boolean {
    const columnChecks: { [key: string]: () => boolean } = {
      id: () => this.hasId(entry),
      gender: () => this.hasGender(entry),
      age: () => this.hasAge(entry),
      species: () => this.hasStandardizedSpecies(entry),
    };
    return columnChecks[column]?.() || false;
  }
  private isColumnDisplayable(column: string): unknown {
    return this.entries.some(entry => {
      return this.isColumnVisibleForEntry(column, entry);
    });
  }
  private hasId(entry: any): boolean {
    return Boolean(entry.resource?.id);
  }

  private hasGender(entry: any): boolean {
    return Boolean(entry.resource?.gender);
  }

  private hasAge(entry: any): boolean {
    const ageExt = entry.resource.extension?.find((ext: any) =>
      ext.url.includes('patient-birthTime') || ext.url.includes('age')
    );
    return Boolean(ageExt);
  }
  private hasStandardizedSpecies(entry: any): boolean {
    const speciesExt = entry.resource.extension?.find((ext: any) =>
      ext.url.includes('animal-species') || ext.url.includes('species')
    );
    return Boolean(speciesExt);
  }
  toggleFileDropdown(): void {
    this.showFileDropdown = !this.showFileDropdown;
  }
  // #endregion
}
