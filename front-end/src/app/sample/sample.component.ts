import { ChangeDetectorRef, Component, HostListener, OnInit } from '@angular/core';
import { FHIRService } from '../service/fhir.service';
import { SpecimenEntry } from '../models/FHIR/specimenResource';
import { MatTableDataSource } from '@angular/material/table';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../material.module';
import { FileManagementService } from '../service/file-management.service';
@Component({
  selector: 'app-sample',
  imports: [CommonModule, MaterialModule,
  ],
  templateUrl: './sample.component.html',
  styleUrl: './sample.component.scss'
})
export class SampleComponent implements OnInit {

  entries: SpecimenEntry[] = [];
  dataSource = new MatTableDataSource<SpecimenEntry>();
  displayedSpecimenColumns: string[] = ['id', 'samplingMethod', 'condition', 'patient', 'batch'];
  showFileDropdown = false;
  selectedMetadataFileDetails: any = null;
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  allFiles: any[] = [];
  loadingOnSampleIds = false;
  // #region Constructor
  constructor(
    private readonly fhirService: FHIRService,
    private readonly fileManagementService: FileManagementService,
    private readonly cdr: ChangeDetectorRef) { }
  // #endregion constructor

  // #region Initialization and Listener
  ngOnInit(): void {
    this.getAllFiles();
    this.getSampleIds();
  }
  @HostListener('window:beforeunload', ['$event'])
  preventRefresh(event: Event) {
    if (this.loadingOnSampleIds) {
      event.preventDefault();
      (event as BeforeUnloadEvent).returnValue = 'Data is still loading. Are you sure you want to leave?';
    }
  }
  // #endregion Initialization and Listener

  // #region fetching data
  getAllFiles(): void {
    this.fileManagementService.getMetadataFiles().subscribe({
      next: (files) => {
        if (files) {
          this.filesDataSource.data = files;
          this.allFiles = files;
          console.log(this.allFiles);
        }
      },
      error: (error) => {
        console.error('Error fetching files:', error);
      }
    });
  }
  getSampleIds() {
    if (this.selectedMetadataFileDetails) {
      console.log('this.selectedMetadataFileDetails:', this.selectedMetadataFileDetails);
      this.fhirService.saveResourcesOnServer(this.selectedMetadataFileDetails).subscribe(() => {
        console.log("success upload")
        this.fhirService.getAllSamples(this.selectedMetadataFileDetails).subscribe(
          data => {
            this.loadingOnSampleIds = false;
            this.cdr.detectChanges();
            if (data?.entry) {
              this.entries = data.entry;
              this.getTableColumns();
              this.dataSource.data = data.entry;
            } else {
              console.warn('No entries found in the sample data.');
            }
          },
          (error: any) => {
            console.error('Error:', error);
            alert('An error occurred while geting ids.');
          }
        );
      },
        (error: any) => {
          console.error('Error:', error);
          alert('An error occurred while geting ids.');
        });
      this.loadingOnSampleIds = true;
    }
  }
  getOfficialId(containedResource: any): string {
    if (!containedResource?.identifier) return '';
    const officialIdentifier = containedResource.identifier.find(
      (id: any) => id.use === 'official'
    );
    return officialIdentifier?.id || '';
  }
  // #endregion fetching data

  //#region 🛠 Helpers & Utilities
  private getTableColumns() {
    const columnsToDisplay = [
      'id', 'samplingMethod', 'condition', 'patient', 'batch'
    ];
    this.displayedSpecimenColumns = columnsToDisplay.filter(column => {
      return this.isColumnDisplayable(column);
    });
  }
  toggleFileDropdown(): void {
    this.showFileDropdown = !this.showFileDropdown;
  }
  //#endregion 🛠 Helpers & Utilities

  // #region Verification Methods
  private isColumnDisplayable(column: string): unknown {
    return this.entries.some(entry => {
      return this.isColumnVisibleForEntry(column, entry);
    });
  }
  private isColumnVisibleForEntry(column: string, entry: any): boolean {
    const columnChecks: { [key: string]: () => boolean } = {
      id: () => this.hasId(entry),
      patient: () => this.hasPatientReference(entry),
      samplingMethod: () => this.hasSamplingMethod(entry),
      condition: () => this.hasCondition(entry),
      batch: () => this.hasBatch(entry)
    };
    return columnChecks[column]?.() || false;
  }
  private hasId(entry: any): boolean {
    return Boolean(entry.resource?.id);
  }
  private hasPatientReference(entry: any): boolean {
    return Boolean(entry.resource.subject?.reference);
  }
  private hasSamplingMethod(entry: any): boolean {
    const method = entry.resource.collection?.method;
    return Array.isArray(method?.coding) && Boolean(method.coding[0]?.display);
  }
  private hasCondition(entry: any): boolean {
    const condition = entry.resource.condition?.[0];
    return Array.isArray(condition?.coding) && Boolean(condition.coding[0]?.display);
  }
  private hasBatch(entry: any): boolean {
    return Boolean(this.getOfficialId(entry.resource.contained[0]));
  }
  // #endregion Verification Methods
}
