// #region Imports
import { AfterViewInit, ChangeDetectorRef, Component, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatMenu, MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Experiment } from '../models/Experiment/Experiment';
import { ExperimentsService } from '../service/experiments.service';
import { SpecimenBundle, SpecimenEntry } from '../models/FHIR/specimenResource';
import { AllSamplesData } from '../models/Experiment/sampleModelForExperiment';
import { Annotation } from '../models/Experiment/annotation';
import { Genome } from '../models/Experiment/genome';
import { FHIRService } from '../service/fhir.service';
import { HttpClient } from '@angular/common/http';
import { StatusFormatPipe } from '../service/StatusFormatPipe';
import { SharedService } from '../service/shared.service';
import { ConfirmationDialogComponent } from '../dialog-components/confirmation-dialog/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { AlignerClass } from '../models/Experiment/aligners';
import { ProtocolClass } from '../models/protocols';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { ToastrService } from 'ngx-toastr';
import { ErrorComponent } from '../dialog-components/error/error.component';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { saveAs } from 'file-saver';
import { environment } from '../../environment';
// #endregion

@Component({
  selector: 'app-alignment',
  imports: [CommonModule, MaterialModule, FormsModule, ReactiveFormsModule, MatMenuModule,
    MatButtonModule,
    MatIconModule, MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    StatusFormatPipe, FormsModule, MatStepperModule, ErrorComponent, MatProgressSpinnerModule],
  templateUrl: './alignment.component.html',
  styleUrl: './alignment.component.scss'
})
export class AlignmentComponent implements AfterViewInit, OnInit {
  //#region 🔹 Constructor & Dependency Injection
  constructor(
    private readonly http: HttpClient,
    private readonly cdr: ChangeDetectorRef,
    private readonly experimentsService: ExperimentsService,
    private readonly fhirService: FHIRService,
    private readonly sharedService: SharedService,
    private readonly dialog: MatDialog,
    private readonly toastr: ToastrService) {
    this.experimentsDataSource = new MatTableDataSource(this.experiments);
    this.filteredExperimentsDataSource = new MatTableDataSource(this.experiments);
  }
  //#endregion
  //#region 🔹 ViewChild / ViewChildren References
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild('stepper') stepper: MatStepper;
  @ViewChildren('menu') menus!: QueryList<MatMenu>;
  //#endregion

  //#region 🔹 Component State Variables
  experiments: Experiment[] = [];
  searchFilters: { [key in keyof Experiment]?: string } = {};
  experimentsDataSource = new MatTableDataSource(this.experiments);
  filteredExperimentsDataSource = new MatTableDataSource(this.experiments);
  menuTriggerMap: { [key: string]: MatMenu } = {};
  selectedMetadataFileDetails: any = null;
  allFiles: any[] = [];
  allSamplesData: AllSamplesData = {
    samples: [
      { sampleName: '', fq1Files: [], fq2Files: [], selectedSequencingType: '' }
    ],
    selectedOrganism: '',
    selectedProtocol: '',
    annotation: '',
    genome: '',
    cellularResolution: '',
    omicsModality: '',
    selectedAligner: ''
  };
  loadingOnSampleIds = false;
  remainingIds: string[] = [];
  isParametersSubmited: boolean = false;
  loadingOnSavingAlignment = false;
  sampleFq1FilesList: File[] = [];
  sampleFq2FilesList: File[] = [];
  allFq1FilesList: File[] = [];
  allFq2FilesList: File[] = [];
  showFileDropdown = false;
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  pageSize: number = 6;
  pageIndex: number = 0;
  selectedAligner: string;
  isDownloading: boolean = false;
  downloadProgress: number = 0;
  errorMessage: string | null = null;
  newPagedExperiments: Experiment[] = [];
  errors: string[] = [];
  masterSequencingType: string = '';
  previousSelections: string[] = [];
  //#endregion

  //#region 🔹 Lifecycle Hooks
  ngOnInit() {
    this.getAllFiles();
    this.getAllExperiments();
    this.filteredExperimentsDataSource = new MatTableDataSource(this.sortedExperiments());
    this.previousSelections = new Array(this.allSamplesData.samples.length).fill('');
    this.cdr.detectChanges();
  }
  ngAfterViewInit() {
    this.menus.forEach((menu, index) => {
      this.menuTriggerMap[index] = menu;
    });
    if (this.paginator) {
      this.paginator.page.subscribe(() => {
        this.updatePagedExperiments();
      });
    }
    this.updatePagedExperiments();
  }
  //#endregion

  //#region 📜 Navigation & Stepper Control
  async goToNextStep(stepper: any): Promise<void> {
    if (this.selectedMetadataFileDetails) {
      try {
        await this.getSampleIds();
        if (this.remainingIds.length > 0) {
          stepper.next();
          console.log("selectedMetadataFileDetails &&  allSamplesData &&  allSamplesData.selectedOrganism", this.selectedMetadataFileDetails, this.allSamplesData, this.allSamplesData.selectedOrganism);
          this.errorMessage = '';
        } else {
          this.errorMessage = 'The dataset you have selected does not contain any samples! Please choose another one.';
        }
      } catch (error) {
        console.error('Error in goToNextStep:', error);
        this.errorMessage = 'An error occurred while processing the dataset.';
      }
    }
  }
  goBack() {
    if (this.stepper?.selected) {
      this.stepper.selected.completed = false;
      this.stepper.previous();
    }
  }
  goToSummary(stepper: any) {
    if (this.remainingIds.length > 0 && this.allSamplesData.samples.length > 0) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '350px',
        data: { message: 'There are still ' + this.remainingIds.length + ' sample/s left! Are you sure you want to submit?' }
      });
      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          stepper.next();
          this.errorMessage = '';
        }
      });
    }
    else {
      stepper.next();
    }
  }
  //endregion

  //#region 📂 Data Fetching Methods
  /** Fetch all metadata files */
  getAllFiles(): void {
    this.http.get<any[]>(`${environment.apiUrl}/files/allMetadataFiles`).subscribe({
      next: (files) => {
        if (files) {
          this.filesDataSource.data = files;
          this.allFiles = this.filesDataSource.data;
        }
      },
      error: (error) => {
        console.error('Error fetching files:', error);
      }
    });
  }

  /** Fetch all experiments */
  getAllExperiments(): void {
    this.experimentsService.getExperiments().subscribe(
      {
        next: (data: Experiment[]) => {
          this.experiments = data;
          this.experimentsDataSource = new MatTableDataSource(this.experiments);
          this.experimentsDataSource.data = this.experiments;
          this.filteredExperimentsDataSource = new MatTableDataSource(this.sortedExperiments());
          this.updatePagedExperiments();
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Error fetching experiments:', error);
        }
      });
  }
  /** Fetch samples from the selected metadata file */
  async getSampleIds(): Promise<void> {
    if (!this.selectedMetadataFileDetails) return;
    const selectedFile = this.allFiles.find(
      file => file.filename === this.selectedMetadataFileDetails
    );
    console.log('selectedFile : ', selectedFile);
    if (selectedFile) {
      this.allSamplesData.omicsModality = selectedFile.omicsModality;
      this.allSamplesData.cellularResolution = selectedFile.cellularResolution;
      this.allSamplesData.selectedOrganism = selectedFile.species;
      this.allSamplesData.selectedProtocol = selectedFile.protocol;
      this.selectedAligner = this.getDefaultAligner();
      this.allSamplesData.annotation = Annotation[this.allSamplesData.selectedOrganism as keyof typeof Annotation];
      this.allSamplesData.genome = Genome[this.allSamplesData.selectedOrganism as keyof typeof Genome];
      this.loadingOnSampleIds = true;
      return new Promise((resolve, reject) => {
        this.fhirService.saveResourcesOnServer(this.selectedMetadataFileDetails).subscribe(() => {
          this.getAllSamplesOfMetadataFile(resolve, reject);
        });
      });
    }
  }
  private getAllSamplesOfMetadataFile(resolve: (value: void | PromiseLike<void>) => void, reject: (reason?: any) => void) {
    this.fhirService.getAllSamples(this.selectedMetadataFileDetails).subscribe({
      next: (data) => {
        this.loadingOnSampleIds = false;
        this.cdr.detectChanges();
        this.getRemainingIds(data, resolve);
      },
      error: (error: any) => {
        console.error('Error:', error);
        this.loadingOnSampleIds = false;
        alert('An error occurred while getting ids.');
        reject(error);
      }
    });
  }
  //#endregion

  //#region 🔍 Filtering & Sorting
  applyFilter(event: Event | string, column: keyof Experiment) {
    this.experimentsService.applyFilter(event, column, this.searchFilters, this.experimentsDataSource);
    this.updatePagedExperiments();
  }
  resetFilters() {
    this.searchFilters = {};
    this.experimentsDataSource.filter = '';
    this.filteredExperimentsDataSource.data = [...this.experiments];
    this.updatePagedExperiments();
    const inputs = document.querySelectorAll<HTMLInputElement>('input[matInput]');
    inputs.forEach(input => (input.value = ''));
    this.searchFilters['status'] = '';
    this.searchFilters['experimentType'] = '';
  }
  sortedExperiments(): Experiment[] {
    return this.experimentsService.sortedExperimentsByType(this.experiments, this.searchFilters, 'Alignment');
  }
  updatePagedExperiments(): void {
    if (!this.experiments) return;
    this.paginator.length = this.sortedExperiments().length;
    const startIndex = this.paginator.pageIndex * this.paginator.pageSize;
    const endIndex = startIndex + this.paginator.pageSize;
    this.newPagedExperiments = this.sortedExperiments().slice(startIndex, endIndex);
  }
  //#endregion

  //#region 📁 FastQ File Management
  private addFastqFiles(formData: FormData) {
    this.allFq1FilesList.forEach(file => {
      formData.append('fq1Files', file);
    });
    this.allFq2FilesList.forEach(file => {
      formData.append('fq2Files', file);
    });
  }
  onFqFileSelected(event: Event, sampleIndex: number, fileType: 'fq1Name' | 'fq2Name') {
    const input = event.target as HTMLInputElement;
    const files = input.files;
    if (!files || files.length === 0) return;
    const validExtensions = ['.fastq.gz', '.fasta.gz', '.fastq', '.fasta'];
    if (this.areAllFilesExtensionsValid(files, validExtensions)) {
      this.sharedService.showErrorDialog(input, validExtensions);
      return;
    }
    this.manageFastqFiles(files, sampleIndex, fileType);
  }
  private areAllFilesExtensionsValid(files: FileList, validExtensions: string[]) {
    return !Array.from(files).every(file => this.sharedService.isValidExtention(file, validExtensions));
  }
  private manageFastqFiles(files: FileList, sampleIndex: number, fileType: 'fq1Name' | 'fq2Name') {
    if (files) {
      const sampleName = this.allSamplesData.samples[sampleIndex].sampleName;
      const renamedFiles = this.renameFiles(files, sampleName);
      if (fileType === 'fq1Name') {
        this.manageFastq1Files(renamedFiles, sampleIndex);
      } else if (fileType === 'fq2Name') {
        this.manageFastq2Files(renamedFiles, sampleIndex);
      }
      this.showFileDropdown = false;
    }
  }
  private manageFastq2Files(renamedFiles: File[], sampleIndex: number) {
    const uniqueFiles = this.filterUniqueFiles(renamedFiles, this.sampleFq2FilesList);
    this.sampleFq2FilesList = this.sampleFq2FilesList.concat(uniqueFiles);
    this.allFq2FilesList = this.allFq2FilesList.concat(uniqueFiles);
    this.allSamplesData.samples[sampleIndex].fq2Files = this.allSamplesData.samples[sampleIndex].fq2Files.concat(uniqueFiles.map(file => file.name))
      .filter((name: '') => name);
  }
  private manageFastq1Files(renamedFiles: File[], sampleIndex: number) {
    const uniqueFiles = this.filterUniqueFiles(renamedFiles, this.sampleFq1FilesList);
    this.sampleFq1FilesList = this.sampleFq1FilesList.concat(uniqueFiles);
    this.allFq1FilesList = this.allFq1FilesList.concat(uniqueFiles);
    this.allSamplesData.samples[sampleIndex].fq1Files = this.allSamplesData.samples[sampleIndex].fq1Files.concat(uniqueFiles.map(file => file.name))
      .filter((name: '') => name);
  }
  private filterUniqueFiles(renamedFiles: File[], fastqFilesList: File[]) {
    return renamedFiles.filter(
      (file) => !fastqFilesList.some((f) => f.name === file.name)
    );
  }
  private renameFiles(files: FileList, sampleName: any) {
    return Array.from(files).map(file => {
      const sanitizedFileName = file.name.replace(/[^a-zA-Z0-9._-]/g, "_");
      const newFileName = `${sampleName}_${sanitizedFileName}`;
      return new File([file], newFileName, { type: file.type });
    });
  }
  removeFile(sampleIndex: number, fileType: 'fq1Name' | 'fq2Name', fileIndex: number) {
    const fileName = this.getFileType(fileType, sampleIndex, fileIndex);
    if (fileType === 'fq1Name') {
      this.removeFastq1File(sampleIndex, fileIndex, fileName);
    } else if (fileType === 'fq2Name') {
      this.removeFastq2File(sampleIndex, fileIndex, fileName);
    }
  }
  private removeFastq2File(sampleIndex: number, fileIndex: number, fileName: any) {
    this.allSamplesData.samples[sampleIndex].fq2Files.splice(fileIndex, 1);
    this.allFq2FilesList = this.allFq2FilesList.filter(file => file.name !== fileName);
    this.sampleFq2FilesList = this.sampleFq2FilesList.filter(file => file.name !== fileName);
  }
  private removeFastq1File(sampleIndex: number, fileIndex: number, fileName: any) {
    this.allSamplesData.samples[sampleIndex].fq1Files.splice(fileIndex, 1);
    this.allFq1FilesList = this.allFq1FilesList.filter(file => file.name !== fileName);
    this.sampleFq1FilesList = this.sampleFq1FilesList.filter(file => file.name !== fileName);
  }
  private getFileType(fileType: string, sampleIndex: number, fileIndex: number) {
    return fileType === 'fq1Name'
      ? this.allSamplesData.samples[sampleIndex].fq1Files[fileIndex]
      : this.allSamplesData.samples[sampleIndex].fq2Files[fileIndex];
  }
  // #endregion
  //#region 🧪 Sample Management
  private getRemainingIds(data: SpecimenBundle, resolve: (value: void | PromiseLike<void>) => void) {
    if (data?.entry) {
      this.remainingIds = [...this.getSamplesIds(data.entry)];
      resolve();
    } else {
      console.warn('No samples entries found in the metadata.');
      resolve();
    }
  }
  private getSamplesIds(specimenEntries: SpecimenEntry[]) {
    return specimenEntries
      .filter(entry => entry.resource.identifier[0]?.id)
      .map(entry => entry.resource.identifier[0].id);
  }
  getDefaultAligner(): string {
    if (this.allSamplesData.selectedProtocol === ProtocolClass.DROPSEQ) {
      return AlignerClass.SIMPLEAF;
    }
    return AlignerClass.CELLRANGER;
  }
  addSample() {
    if (this.remainingIds.length > 0) {
      const newSample = {
        sampleName: '',
        fq1Files: [],
        fq2Files: [],
        selectedSequencingType: this.masterSequencingType || ''
      };
      this.allSamplesData.samples.push(newSample);
      this.sampleFq1FilesList = [];
      this.sampleFq2FilesList = [];
    }
  }
  setMasterSequencingType(type: string, index: number) {
    if (index === 0) {
      this.masterSequencingType = type;
      for (let i = 1; i < this.allSamplesData.samples.length; i++) {
        this.allSamplesData.samples[i].selectedSequencingType = type;
      }
    }
  }
  onSampleIdSelected(sampleIndex: number) {
    const sample = this.allSamplesData.samples[sampleIndex];
    const previousId = this.previousSelections[sampleIndex];
    if (previousId && previousId !== sample.sampleName) {
      this.remainingIds.push(previousId);
      this.remainingIds.sort((a, b) => a.localeCompare(b));
    }
    if (sample.sampleName) {
      const idIndex = this.remainingIds.indexOf(sample.sampleName);
      if (idIndex > -1) {
        this.remainingIds.splice(idIndex, 1);
      }
    }
    this.previousSelections[sampleIndex] = sample.sampleName;
  }
  removeSample(index: number) {
    const removedSample = this.allSamplesData.samples.splice(index, 1)[0];
    if (removedSample?.sampleName) {
      this.remainingIds.push(removedSample.sampleName);
    }
    if (index === 0 && this.allSamplesData.samples.length > 0) {
      this.masterSequencingType = this.allSamplesData.samples[0].selectedSequencingType;
    }
  }
  // #endregion

  //#region 📦 Alignment Submission
  onAlignmentSubmit() {
    this.loadingOnSavingAlignment = true;
    this.allSamplesData.selectedAligner = this.selectedAligner;
    this.cdr.detectChanges();
    const jsonNewData = JSON.stringify(this.allSamplesData);
    const formData = new FormData();
    formData.append('jsonData', jsonNewData);
    formData.append('experimentName', this.selectedMetadataFileDetails)
    this.addFastqFiles(formData);
    this.saveAlignmentParametersAsJSON(formData);
  }
  private saveAlignmentParametersAsJSON(formData: FormData) {
    this.http.post(`${environment.apiUrl}/alignment/save-json`, formData, { responseType: 'text' }).subscribe(
      {
        next: () => {
          this.isParametersSubmited = true;
          this.loadingOnSavingAlignment = false;
          this.toastr.success('Alignment parameters submitted successfully', 'Success');
          this.cdr.detectChanges();
        },
        error: (error: any) => {
          console.error('Error:', error);
          this.loadingOnSavingAlignment = false;
          this.handleAlignmentSubmissionError(error);
          this.cdr.detectChanges();
        }
      });
  }
  //#endregion

  //#region 🛠 Helpers & Utilities
  downloadJson(experiment: Experiment): void {
    this.experimentsService.downloadJson(experiment);
  }
  viewHtml(folderName: string) {
    this.experimentsService.viewMultiQCResult(folderName);
  }
  downloadAllMatrices(folderName: string) {
    this.isDownloading = true;
    this.downloadProgress = 0;
    this.experimentsService.downloadAllMatrices(folderName).subscribe({
      next: (blob: Blob) => {
        this.toastr.success('Matrices download started', 'Success');
        saveAs(blob, folderName + '_matrices.zip');
        this.isDownloading = false;
        this.downloadProgress = 100;
      },
      error: (err) => {
        console.error('Download failed:', err);
        this.isDownloading = false;
      }
    });
  }
  getStatusClass(status: string): string {
    return this.experimentsService.getStatusClass(status);
  }
  onSelectOpened(isOpened: boolean) {
    if (isOpened) {
      this.errorMessage = '';
    }
  }
  allFormsValid(): boolean {
    return (
      this.allSamplesData.samples.every(sample => {
        return (
          sample.sampleName &&
          sample.fq1Files.length > 0
        );
      }));
  }
  // #endregion

  //#region 🔹 error handling
  private handleAlignmentSubmissionError(error: any) {
    if (error.status === 400) {
      this.handleBadRequetErrors(error);
    } else if (error.status === 0) {
      this.errors = ['Unable to connect to the server. Please check your network connection.'];
    } else if (error.status >= 500) {
      this.errors = ['A server error occurred. Please try again later.'];
    } else {
      this.errors = ['An unexpected error occurred.'];
    }
  }
  private handleBadRequetErrors(error: any) {
    if (error.error.includes('Required part')) {
      const errorMessages = this.parseBackendErrors(error.error);
      this.errors = errorMessages;
    } else {
      this.errors = ['Invalid request. Please check your input.'];
    }
  }
  private parseBackendErrors(errorText: string): string[] {
    const errors: string[] = [];
    this.parseAndRegisterMissingPart(errorText, errors);
    this.parseJsonErrorsFromText(errorText, errors);
    return errors.length > 0 ? errors : [errorText];
  }
  private parseJsonErrorsFromText(errorText: string, errors: string[]) {
    if (errorText.includes('Error in JSON')) {
      const jsonErrors = errorText.split('Error in JSON:');
      jsonErrors.shift();
      jsonErrors.forEach(err => {
        const cleanError = err.trim();
        if (cleanError) {
          errors.push(cleanError);
        }
      });
    }
  }
  private parseAndRegisterMissingPart(errorText: string, errors: string[]) {
    if (errorText.includes('Required part')) {
      const missingPart = RegExp(/Required part '(.+?)'/).exec(errorText);
      if (missingPart?.[1]) {
        errors.push(`Required file is missing: ${missingPart[1]}`);
      }
    }
  }
  // #endregion

  //#region refreshing
  manualRefresh() {
    this.getAllExperiments();
  }
  refreshPage(): void {
    window.location.reload();
  }
  //#endregion
}
