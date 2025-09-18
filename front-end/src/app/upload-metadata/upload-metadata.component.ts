// #region Imports
import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit, ViewChild, ViewEncapsulation } from '@angular/core';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { FormsModule } from '@angular/forms';
import { MaterialModule } from '../material.module';
import { SharedService } from '../service/shared.service';
import { Papa, ParseResult } from 'ngx-papaparse';
import { FileManagementService } from '../service/file-management.service';
import { Subject, takeUntil, firstValueFrom } from 'rxjs';
import { FHIRService } from '../service/fhir.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDeleteDialogComponent } from '../dialog-components/confirm-delete-dialog/confirm-delete-dialog.component';
import { ProtocolClass } from '../models/protocols';
import { CsvParserService } from '../service/csv-parser.service';
import { DataMappingService } from '../service/data-mapping.service';
import { ToastrService } from 'ngx-toastr';
// #endregion

// #region Types
type ErrorFields = 'sample_id' | 'standardized_species' | 'sequenceType' | 'cellularResolution' | 'protocol' | 'organ' | 'disorder';
type AllFields = 'patient_id' | 'sample_id' | 'age' | 'gender' | 'standardized_species' | 'protocol' | 'organ' | 'disorder' | 'sequenceType' | 'cellularResolution' | 'batch';
// #endregion
@Component({
  selector: 'app-upload-metadata',
  imports: [CommonModule, FormsModule, MaterialModule],
  templateUrl: './upload-metadata.component.html',
  styleUrl: './upload-metadata.component.scss',
  encapsulation: ViewEncapsulation.None,
})
export class UploadMetadataComponent implements OnInit, AfterViewInit, OnDestroy{
  // #region ViewChild
  @ViewChild('fileSummaryPaginator') fileSummaryPaginator: MatPaginator;
  @ViewChild('datasetsPaginator') datasetsPaginator: MatPaginator;
  // #endregion

  // #region File & Table State
  selectedFile: File | null = null;
  fileContent: string;
  isFileSelected: boolean = false;
  fileUploaded: boolean = false;
  isMappingSubmitted: boolean = false;
  newFileName: string = '';
  // #endregion

  // #region Data sources
  newDataSource = new MatTableDataSource<any>();
  filesDataSource = new MatTableDataSource<any>();
  csvData: any[] = [];
  filteredData: any[] = [];
  files: any[] = [];
  // #endregion

  // #region Display and UI properties
  displayedColumns: string[] = [];
  tableHeaders: string[] = [];
  displayedDataSetsColumns: string[] = ['filename', 'cellularResolution', 'omicsModality', 'organ', 'disorder', 'species', 'actions'];
  searchTerms: { [key: string]: string } = {};
  tableFilters: { [key: string]: string } = {};
  isSearchFieldPopulated: boolean = false;
  // #endregion

  // #region Loading and error states
  uploadLoading = false;
  loadingFileFetching = false;
  loadingOnExport = false;
  loadingOnDelete = false;
  errorMessage: string | null = null;
  fileNameExists = false;
  emptyFileName = false;
  errorStates = {
    cellularResolution: false,
    sequenceType: false,
    sample_id: false,
    standardized_species: false,
    protocol: false,
    organ: false,
    disorder: false,
    fileHeadersEmpty: false
  };
  // #endregion

  // #region Mappings
  mappings = {
    patient_id: '', sample_id: '',
    age: '', gender: '', standardized_species: '', protocol: '', organ: '',
    disorder: '', sequenceType: '', cellularResolution: '', batch: ''
  };
  selectedMappings: { [key: string]: string } = {
    patient_id: 'Select Subject ID', sample_id: 'Select Sample ID', age: 'Select Age', gender: 'Select Gender', standardized_species: 'Select Species', protocol: 'Select Protocol',
    organ: 'Select Organ', disorder: 'Select Disorder', sequenceType: 'Select Omics modality', cellularResolution: 'Select Cellular Resolution', batch: 'Select Batch'
  };
  // #endregion

  // #region UI Toggle Sections
  showSections: { [key: string]: boolean } = {
    sequencing: false,
    patient: false,
    sample: false,
  };
  // #endregion

  // #region Cleanup
  private readonly destroy$ = new Subject<void>();
  // #endregion

  // #region Constructor
  constructor(
    private readonly csvParser: CsvParserService,
    private readonly dataMapper: DataMappingService,
    private readonly sharedService: SharedService,
    private readonly fhirService: FHIRService,
    private readonly fileManagementService: FileManagementService,
    private readonly papa: Papa,
    private readonly cdr: ChangeDetectorRef,
    private readonly toastr: ToastrService,
    private readonly dialog: MatDialog
  ) { }
  // #endregion

  // #region Initialization, Listener and Lifecycle Hooks
  ngOnInit(): void {
    if (this.sharedService.hasTableData()) {
      this.newDataSource = this.sharedService.getTableData();
      this.filteredData = this.newDataSource.data;
      this.fileUploaded = this.sharedService.getFileUploadedBooleanValue();
      this.isFileSelected = !this.sharedService.getFileUploadedBooleanValue();
      this.displayedColumns = this.sharedService.getTableHeaders();
    }
    this.getFiles();
    this.filesDataSource.filterPredicate = (data: any, filter: string) => {
      const filters = JSON.parse(filter);
      return Object.keys(filters).every(key => {
        const dataValue = data[key]?.toString().toLowerCase() ?? '';
        const filterValue = filters[key].toLowerCase();
        return dataValue.includes(filterValue);
      });
    };
  }
  ngAfterViewInit() {
    this.setupPaginators();
    this.cdr.detectChanges();
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  private setupPaginators(): void {
    this.filesDataSource.paginator = this.datasetsPaginator;
    if (this.fileSummaryPaginator) {
      this.newDataSource.paginator = this.fileSummaryPaginator;
    }
  }
  @HostListener('window:beforeunload', ['$event'])
  preventRefresh(event: BeforeUnloadEvent) {
    if (this.uploadLoading) {
      event.preventDefault();
      return 'Data is still loading. Are you sure you want to leave?';
    }
  }
  // #endregion

  // #region File Upload Handlers
  onFileSelected(event: Event): void {
    this.resetDataStructures();
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (!this.sharedService.isValidCSVFile(file)) {
        this.sharedService.showErrorDialog(input, ['.csv']);
        return;
      }
      this.selectedFile = file;
      this.isFileSelected = true;
      const reader = new FileReader();
      reader.onload = () => {
        this.fileContent = reader.result as string;
        const { headers, data } = this.csvParser.parseCSV(this.fileContent);
        this.tableHeaders = headers;
        this.csvData = data;
        this.filteredData = [...data];
        this.displayedColumns = this.tableHeaders;
        this.updateDataSource();
        this.cdr.detectChanges();
        this.setupPaginators();
      };
      reader.readAsText(this.selectedFile);
    }
  }
  private async uploadFile(csvContent: string, delimiter: string): Promise<void> {
    this.uploadLoading = true;
    const formData = this.fileManagementService.prepareUploadFormData(
      csvContent,
      this.newFileName,
      delimiter
    );
    try {
      await firstValueFrom(
        this.fileManagementService.uploadFile(formData)
          .pipe(takeUntil(this.destroy$))
      );

      this.handleUploadSuccess();
    } finally {
      this.uploadLoading = false;
    }
  }
  private handleUploadSuccess(): void {
    this.isMappingSubmitted = true;
    this.sharedService.isFileUploadedAlready(true);
    this.sharedService.setTableVisible(true);
    this.sharedService.setTableData(this.newDataSource.data);
    this.sharedService.setTableHeaders(this.displayedColumns);
    this.manualRefresh();
    this.saveResourcesOnServer();
  }
  private handleUploadError(error: any): void {
    if (error.status === 400 && error.error?.message) {
      this.processUploadErrorMessage(error.error.message);
    } else {
      this.errorMessage = 'An error occurred while uploading the file. Please try again.';
    }
  }
  // #endregion

  // #region Mapping Handlers
  async submitMappings(): Promise<void> {
    this.resetSubmissionState();
    if (this.hasBasicErrors()) return;
    try {
      const result = await this.csvParser.parseWithPapa(this.fileContent);
      const { headers, data } = this.processData(result);

      if (this.isEmptyData(headers, data)) {
        this.errorMessage = 'The file appears to be empty or incorrectly formatted.';
        this.errorStates.fileHeadersEmpty = true;
        return;
      }
      const modifiedCsv = this.papa.unparse({ fields: headers, data });
      await this.uploadFile(modifiedCsv, result.meta.delimiter);
    } catch (error) {
      this.handleUploadError(error);
    }
  }
  private processData(result: ParseResult<any>) {
    const headerMap = this.dataMapper.createHeaderMap(this.mappings);
    let data = result.data;
    let headers = result.meta.fields || [];
    headers = this.dataMapper.mapHeaders(headers, headerMap);
    headers = this.dataMapper.addMissingHeaders(headerMap, headers);
    data = this.dataMapper.mapData(data, headerMap, this.mappings);
    return { headers, data };
  }
  selectHeader(event: Event, field: AllFields): void {
    const target = event.target as HTMLSelectElement;
    this.mappings[field] = target.value.trim();
    if (this.isErrorField(field)) {
      this.errorStates[field] = false;
    }
  }
  // #endregion

  // #region CRUD Operations File Management
  getFiles(): void {
    this.loadingFileFetching = true;
    this.fileManagementService.getMetadataFiles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (files) => {
          if (files) {
            this.files = files;
            this.filesDataSource.data = files;
            this.cdr.detectChanges();
          }
        },
        error: (error) => console.error('Error fetching files:', error),
        complete: () => this.loadingFileFetching = false
      });
  }
  deleteFile(element: any): void {
    this.loadingOnDelete = true;
    this.fileManagementService.deleteDocument(element.filename).subscribe({
      next: () => this.manualRefresh(),
      error: (err) => console.error('Error deleting file:', err),
      complete: () => this.loadingOnDelete = false
    });
  }
  downloadMetadataFile(fileName: string): void {
    this.loadingOnExport = true;

    this.fileManagementService.downloadMetadataFile(fileName, this.toastr).subscribe({
      next: (loadingState) => {
        this.loadingOnExport = loadingState;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingOnExport = false;
        this.cdr.detectChanges();
      }
    });
  }
  private saveResourcesOnServer(): void {
    this.fhirService.saveResourcesOnServer(this.newFileName.trim() + '.csv')
      .subscribe({
        next: () => this.cdr.detectChanges(),
        error: (error) => this.errorMessage = `An error occurred while getting IDs: ${error.message}`
      });
  }
  // #endregion

  // #region Search and Filtering
  filterData(): void {
    const activeFilters = Object.entries(this.searchTerms)
      .filter(([_, term]) => term && String(term).trim() !== '');
    this.verifyActiveFilterAndFilterData(activeFilters);
    this.updateDataSource();
    if (this.newDataSource.paginator) {
      this.newDataSource.paginator.firstPage();
    }
  }
  private verifyActiveFilterAndFilterData(activeFilters: [string, string][]) {
    if (activeFilters.length === 0) {
      this.filteredData = [...this.csvData];
      this.isSearchFieldPopulated = false;
    } else {
      this.isSearchFieldPopulated = true;
      this.filteredData = this.csvData.filter(item => {
        return activeFilters.every(([key, searchTerm]) => {
          const itemValue = item[key] ? String(item[key]).toLowerCase().trim() : '';
          const searchValue = String(searchTerm).toLowerCase().trim();
          return itemValue.includes(searchValue);
        });
      });
    }
  }

  private updateDataSource(): void {
    this.newDataSource.data = this.filteredData;
  }
  applyTableFilter(event: Event | string, column: string): void {
    let filterValue = '';

    if (typeof event === 'string') {
      filterValue = event.toLowerCase();
    } else {
      filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    }
    if (filterValue) {
      this.tableFilters[column] = filterValue;
    } else {
      delete this.tableFilters[column];
    }
    this.filesDataSource.filter = JSON.stringify(this.tableFilters);
  }
  // #endregion

  // #region UI Helpers
  manualRefresh(): void {
    this.getFiles();
  }
  getTooltipText(array: any[]): string {
    return this.fileManagementService.getTooltipText(array);
  }
  private processUploadErrorMessage(errorMessage: string): void {
    if (errorMessage.includes('File is missing required parameter(s) :')) {
      const missingHeaders = this.getMissingHeadersFromErrorMessage(errorMessage);
      this.verifyMissingHeaders(missingHeaders);
      this.errorMessage = 'Please fill in all required fields: ' + missingHeaders.join(', ');
    } else if (errorMessage.includes('headers are empty')) {
      this.errorMessage = 'The file appears to be empty or has no headers.';
      this.errorStates.fileHeadersEmpty = true;
    } else {
      this.errorMessage = errorMessage;
    }
  }
  private getMissingHeadersFromErrorMessage(errorMessage: string): string[] {
    return errorMessage.split(':')[1].trim().split(',').map((h: string) => h.trim());
  }
  openDeleteDialog(element: any): void {
    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '400px',
      data: { filename: element.filename }
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.deleteFile(element);
      }
    });
  }
  getProtocolOptions(): string[] {
    return Object.values(ProtocolClass).filter(value => typeof value === 'string');
  }
  // #endregion

  // #region Refresh and Reset Handlers
  refreshPartial(): void {
    this.resetDataStructures();
    this.isFileSelected = false;
    this.fileUploaded = false;
    this.fileContent = '';
    this.selectedFile = null;
    this.displayedColumns = [];
    this.newFileName = '';
    this.isMappingSubmitted = false;
    this.filteredData = [];
    this.mappings = {
      patient_id: '', sample_id: '', age: '', gender: '', standardized_species: '', protocol: '', organ: '',
      disorder: '', sequenceType: '', cellularResolution: '', batch: ''
    };
    this.selectedMappings = {
      patient_id: 'Select Subject ID', sample_id: 'Select Sample ID', age: 'Select Age', gender: 'Select Gender', standardized_species: 'Select Species', protocol: 'Select Protocol',
      organ: 'Select Organ', disorder: 'Select Disorder', sequenceType: 'Select Omics modality', cellularResolution: 'Select Cellular Resolution', batch: 'Select Batch'
    };
    this.updateDataSource();
    this.closeSection('sequencing');
    this.closeSection('patient');
    this.closeSection('sample');

    this.cdr.detectChanges();
  }
  resetSearchFields() {
    Object.keys(this.searchTerms).forEach(key => {
      this.searchTerms[key] = '';
    });
    this.filterData();
  }
  private resetDataStructures(): void {
    this.csvData = [];
    this.filteredData = [];
    this.tableHeaders = [];
    this.newDataSource.data = [];
    this.searchTerms = {};
  }
  private resetSubmissionState(): void {
    this.errorMessage = null;
    this.resetErrorStates();
  }
  resetErrorStates(): void {
    Object.keys(this.errorStates).forEach(key => {
      this.errorStates[key as keyof typeof this.errorStates] = false;
    });
  }
  // #endregion

  // #region Validation
  isFileNameUnique(): boolean {
    if (!this.newFileName || this.newFileName.trim() === '') {
      this.emptyFileName = true;
      this.fileNameExists = false;
      return false;
    }
    this.emptyFileName = false;
    const isUnique = !this.files.some(file => file.filename === `${this.newFileName.trim()}.csv`);
    this.fileNameExists = !isUnique;
    return isUnique;
  }
  private isErrorField(field: AllFields): field is ErrorFields {
    const errorFields: ErrorFields[] = ['sample_id', 'standardized_species', 'sequenceType', 'cellularResolution', 'protocol', 'organ', 'disorder'];
    return errorFields.includes(field as ErrorFields);
  }
  private hasBasicErrors(): boolean {
    if (!this.fileContent) {
      this.errorMessage = 'The file you chose is empty! Please reload and choose another.';
      return true;
    }
    if (!this.isFileNameUnique()) {
      this.errorMessage = this.emptyFileName
        ? 'Please enter a File name.'
        : 'The name you choose already exists! Please choose another.';
      return true;
    }
    return false;
  }
  private isEmptyData(headers: any[], data: any[]): boolean {
    return !headers || headers.length === 0 || data.length === 0;
  }
  private verifyMissingHeaders(missingHeaders: string[]): void {
    missingHeaders.forEach((header: string) => {
      if (Object.keys(this.errorStates).includes(header)) {
        this.errorStates[header as keyof typeof this.errorStates] = true;
      }
    });
  }
  // #endregion

  //#region  Section Handlers
  toggleSection(section: string): void {
    this.showSections[section] = !this.showSections[section];
  }
  openSection(section: string): void {
    this.showSections[section] = true;
  }
  closeSection(section: string): void {
    this.showSections[section] = false;
  }
  // #endregion

}
