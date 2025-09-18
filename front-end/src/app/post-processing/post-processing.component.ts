import { AfterViewInit, ChangeDetectorRef, Component, inject, OnDestroy, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ExperimentsService } from '../service/experiments.service';
import { Experiment } from '../models/Experiment/Experiment';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '../material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatMenu, MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSortModule } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { HttpClient, } from '@angular/common/http';
import { StatusFormatPipe } from '../service/StatusFormatPipe';
import { ToastrService } from 'ngx-toastr';
import { map, Observable, of, Subscription } from 'rxjs';
import { VisualizationService } from '../service/visualization.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WaitingDialogComponent } from '../dialog-components/waiting-dialog/waiting-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { environment } from '../../environment';
@Component({
  selector: 'app-post-processing',
  imports: [CommonModule, MaterialModule, FormsModule, ReactiveFormsModule, MatMenuModule,
    MatButtonModule,
    MatIconModule, MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    StatusFormatPipe],
  templateUrl: './post-processing.component.html',
  styleUrl: './post-processing.component.scss'
})
export class PostProcessingComponent implements OnInit, AfterViewInit, OnDestroy {
  // #region Constructor
  constructor(
    private readonly http: HttpClient,
    private readonly experimentsService: ExperimentsService,
    private readonly cdr: ChangeDetectorRef,
    private readonly toastr: ToastrService,
    private readonly dialog: MatDialog) { }
  // #endregion Constructor

  // #region ViewChild and ViewChildren
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChildren('menu') menus!: QueryList<MatMenu>;
  // #endregion ViewChild and ViewChildren
  // #region Post-Processing Form Data
  formData = {
    alignmentExperiment: null as Experiment | null,
    minGenesByCells: 200,
    minCellsExpressingGene: 3,
    numHighVariableGenes: 2000,
    clustering: ['louvain'],
    dimensionReduction: ['umap'],
    cellularResolution: '',
    omicsModality: '',
  };
  formErrors = {
    minGenesByCells: '',
    minCellsExpressingGene: '',
    numHighVariableGenes: '',
    dimensionReduction: '',
    clustering: ''
  };
  clusteringOptions = ['louvain', 'leiden'];
  dimensionReductionOptions = ['umap', 'tsne'];
  // #endregion Post-Processing Form Data

  // #region Experiments Data
  experiments: Experiment[] = [];
  alignmentExperiments$: Observable<Experiment[]>;
  selectedExperiment: Experiment;
  newPagedExperiments: Experiment[] = [];
  selectedAlignmentExperimentName: string = '';
  searchFilters: { [key in keyof Experiment]?: string } = {};
  experimentsDataSource = new MatTableDataSource(this.experiments);
  filteredExperimentsDataSource = new MatTableDataSource(this.experiments);
  // #endregion Experiments Data

  menuTriggerMap: { [key: string]: MatMenu } = {};
  allFiles: any[] = [];
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  selectedMetadataFileDetails: any = null;
  selectedFileMetadata: File;
  loading = false;
  isParametersSubmited: boolean = false;
  isFileSelected: boolean = false;

  // #region Visualization Service Integration
  private readonly visualizationService = inject(VisualizationService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly monitoredTabs = new Map<string, { tab: Window, intervalId: any }>();
  private readonly visualizationSubscriptions: Subscription[] = [];
  // #endregion Visualization Service Integration

  // #region Initialization and Lifecycle Hooks
  ngOnInit() {
    this.getAllFiles();
    this.getAllExperiments();
    this.updatePagedExperiments();
  }
  ngAfterViewInit() {
    this.menus.forEach((menu, index) => {
      this.menuTriggerMap[index] = menu;
    });
    this.updatePagedExperiments();
    this.paginator.page.subscribe(() => { this.updatePagedExperiments(); });
  }
  ngOnDestroy() {
    // Clean up all intervals when component is destroyed
    this.monitoredTabs.forEach((value, containerId) => {
      clearInterval(value.intervalId);
      this.monitoredTabs.delete(containerId);
    });
    // Unsubscribe from all observables
    this.visualizationSubscriptions.forEach(sub => sub.unsubscribe());
  }
  manualRefresh() {
    this.getAllExperiments();
  }
  updatePagedExperiments(): void {
    if (!this.experiments || !this.paginator) return;
    this.paginator.length = this.sortedExperiments().length;
    const startIndex = this.paginator.pageIndex * this.paginator.pageSize;
    const endIndex = startIndex + this.paginator.pageSize;
    this.newPagedExperiments = this.sortedExperiments().slice(startIndex, endIndex);
    this.cdr.detectChanges();
  }
  refreshPage(): void {
    window.location.reload();
  }
  //#endregion Initialization and Lifecycle Hooks

  //#region 📂 Data Fetching Methods
  getAllExperiments(): void {
    this.experimentsService.getExperiments().pipe(
      map(data => {
        this.experiments = data;
        this.experimentsDataSource = new MatTableDataSource(this.experiments);
        this.experimentsDataSource.data = this.experiments;
        this.filteredExperimentsDataSource = new MatTableDataSource(this.sortedExperiments());
        return this.experimentsService.getDoneExperiments(this.experiments, this.searchFilters, 'Alignment');
      })
    ).subscribe({
      next: alignmentExperiments => {
        this.alignmentExperiments$ = of(alignmentExperiments);
        this.updatePagedExperiments();
      },
      error: error => console.error('Error fetching experiments:', error)
    });
  }
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

  private getSelectedFile() {
    return this.allFiles.find(
      file => file.filename === this.selectedMetadataFileDetails
    );
  }
  // #endregion 📂 Data Fetching Methods

  // #region 🔍 Filtering and Sorting
  applyFilter(event: Event | string, column: keyof Experiment) {
    this.experimentsService.applyFilter(event, column, this.searchFilters, this.experimentsDataSource);
    this.updatePagedExperiments();
  }
  sortedAlignmentExperiments(): Experiment[] {
    return this.experimentsService.getDoneExperiments(this.experiments, this.searchFilters, 'Alignment');
  }
  resetFilters() {
    this.searchFilters = {};
    this.filteredExperimentsDataSource.filter = '';
    this.filteredExperimentsDataSource.data = [...this.experiments];
    this.updatePagedExperiments();
    const inputs = document.querySelectorAll<HTMLInputElement>('input[matInput]');
    inputs.forEach(input => (input.value = ''));
    this.searchFilters['status'] = '';
    this.searchFilters['experimentType'] = '';
  }
  sortedExperiments(): Experiment[] {
    return this.experimentsService.sortedExperimentsByType(this.experiments, this.searchFilters, 'Post-Processing');
  }
  // #endregion 🔍 Filtering and Sorting

  //#region 🛠 Helpers & Utilities
  downloadJson(experiment: Experiment): void {
    this.experimentsService.downloadJson(experiment);
  }
  downloadMatrix(folderName: string) {
    this.experimentsService.downloadProcessedMatrix(folderName);
    this.toastr.success('Matrix download started', 'Success');
  }
  getStatusClass(status: string): string {
    return this.experimentsService.getStatusClass(status);
  }
  async goToNextStep(stepper: any): Promise<void> {
    if (!this.selectedExperiment) {
      return;
    }
    this.selectedMetadataFileDetails = this.selectedExperiment.metadataFileName;
    this.selectedAlignmentExperimentName = this.selectedExperiment.experimentName;
    stepper.next();
  }
  private getErrorMessage(error: any) {
    if (error.status === 400) {
      this.toastr.error(error.error, 'Validation Error');
    } else {
      this.toastr.error('An unexpected error occurred', 'Error');
    }
  }
  // #endregion 🛠 Helpers & Utilities

  // #region Post-Processing Form Submission
  onPostProcessingSubmit(event?: Event) {
    if (event) {
      event.preventDefault();
    }
    if (!this.validateForm()) {
      return;
    }
    if (this.selectedMetadataFileDetails) {
      const selectedFile = this.getSelectedFile();
      this.VerifySelectedFileAndHandlePostProcessing(selectedFile);
    }
  }
  private handlePostProcessing(formData: FormData) {
    this.http.post(`${environment.apiUrl}/postProcessing/save-json`, formData, { responseType: 'text' }).subscribe({
      next: () => {
        this.isParametersSubmited = true;
        this.loading = false;
        this.toastr.success('Post-processing parameters submitted successfully', 'Success');
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.loading = false;
        this.getErrorMessage(error);
      }
    });
  }
  private VerifySelectedFileAndHandlePostProcessing(selectedFile: any) {
    if (selectedFile) {
      this.selectedFileMetadata = selectedFile;
      this.loading = true;
      this.formData.omicsModality = selectedFile.omicsModality;
      this.formData.cellularResolution = selectedFile.cellularResolution;
      this.formData.alignmentExperiment = this.selectedExperiment;
      const jsonData = JSON.stringify(this.formData);
      const formData = new FormData();
      formData.append('jsonData', jsonData);
      formData.append('metadataFile', this.selectedMetadataFileDetails);
      this.handlePostProcessing(formData);
    }
  }
  onCheckboxChange(option: string, type: string, event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    const selectedArray = this.formData[type as keyof typeof this.formData] as string[];

    if (checkbox.checked) {
      selectedArray.push(option);
    } else {
      const index = selectedArray.indexOf(option);
      if (index > -1) {
        selectedArray.splice(index, 1);
      }
    }
  }
  // #endregion Post-Processing Form Submission

  // #region Validation Methods
  validateForm() {
    let isValid = true;
    this.formErrors = {
      minGenesByCells: '',
      minCellsExpressingGene: '',
      numHighVariableGenes: '',
      dimensionReduction: '',
      clustering: ''
    };
    isValid = this.isMinGenesByCellsValid(isValid);
    isValid = this.isMinCellsExpressingGeneValid(isValid);
    isValid = this.isNumHighVariableGenesValid(isValid);
    isValid = this.isDimensionReductionAndClusteringValid(isValid);
    return isValid;
  }
  private isDimensionReductionAndClusteringValid(isValid: boolean) {
    if (this.isDimentionReductionValid() &&
      this.isClustringValid()) {
      this.formErrors.dimensionReduction = 'At least one dimension reduction must be selected';
      this.formErrors.clustering = 'At least one clustering must be selected';
      isValid = false;
    }
    return isValid;
  }
  private isClustringValid() {
    return !this.formData.clustering || this.formData.clustering.length === 0;
  }
  private isDimentionReductionValid() {
    return !this.formData.dimensionReduction || this.formData.dimensionReduction.length === 0;
  }
  private isNumHighVariableGenesValid(isValid: boolean) {
    if (this.formData.numHighVariableGenes == null || this.formData.numHighVariableGenes < 0) {
      this.formErrors.numHighVariableGenes = 'Valid numHighVariableGenes is required';
      isValid = false;
    }
    return isValid;
  }
  private isMinCellsExpressingGeneValid(isValid: boolean) {
    if (this.formData.minCellsExpressingGene == null || this.formData.minCellsExpressingGene < 0) {
      this.formErrors.minCellsExpressingGene = 'Valid minCellsExpressingGene is required';
      isValid = false;
    }
    return isValid;
  }
  private isMinGenesByCellsValid(isValid: boolean) {
    if (this.formData.minGenesByCells == null || this.formData.minGenesByCells < 0) {
      this.formErrors.minGenesByCells = 'Valid minGenesByCells is required';
      isValid = false;
    }
    return isValid;
  }
  // #endregion Validation Methods

  // #region Visualization Service Integration
  openVisualizationN(experimentName: string): void {
    const waitingDialog = this.dialog.open(WaitingDialogComponent, {
      disableClose: true, // Prevent closing by clicking outside
      panelClass: 'transparent-dialog'
    });
    this.snackBar.open('Starting visualization... Please wait.', 'Dismiss', { duration: 6000 });
    this.visualizationService.startVisualization(experimentName).subscribe({
      next: (response) => {
        waitingDialog.close();
        this.snackBar.dismiss();
        this.snackBar.open('Visualization is ready!', 'Close', { duration: 4000 });
        // 1. Open a new tab with a polished loading screen
        const newTab = window.open('', '_blank');
        if (!newTab) {
          this.snackBar.open('Please allow popups for visualization.', 'Close', { duration: 7000 });
          this.cleanupContainer(response.containerId);
          return;
        }
        // 2. Inject a professional-looking loader with auto-redirect
        newTab.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>Loading Visualization</title>
            <style>
              body {
                font-family: 'Arial', sans-serif;
                background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                margin: 0;
                height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
                text-align: center;
              }
              .loader-container {
                background: white;
                padding: 2rem;
                border-radius: 10px;
                box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                max-width: 400px;
              }
              .spinner {
                border: 4px solid rgba(0, 0, 0, 0.1);
                border-radius: 50%;
                border-top: 4px solid #3498db;
                width: 40px;
                height: 40px;
                animation: spin 1s linear infinite;
                margin: 0 auto 20px;
              }
              @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
              }
              h1 {
                color: #2c3e50;
                margin-bottom: 10px;
              }
              p {
                color: #7f8c8d;
              }
            </style>
          </head>
          <body>
            <div class="loader-container">
              <div class="spinner"></div>
              <h1>Launching Visualization</h1>
              <p>This will open automatically in a moment...</p>
            </div>
            <script>
              // Reliable cross-platform redirect (works without refresh)
              setTimeout(() => {
                window.location.replace('${response.shinyUrl}');
              }, 150); // Slightly longer delay for Windows
            </script>
          </body>
        </html>
      `);
        waitingDialog.close();
        // 3. Monitor tab closure
        this.monitorTab(newTab, response.containerId);
      },
      error: (err) => {
        waitingDialog.close();
        console.error('Failed to launch visualization:', err);
        this.snackBar.open(`Error: ${err.error?.error || 'Failed to launch visualization.'}`, 'Close', { duration: 7000 });
      }
    });
  }
  // #endregion Visualization Service Integration
  // #region Cleanup and Monitoring
  private cleanupContainer(containerId: string): void {
    this.visualizationService.stopVisualization(containerId).subscribe({
      next: () => {
        this.snackBar.open(`Visualization container ${containerId} stopped successfully.`, 'Close', { duration: 4000 });
      },
      error: (err) => {
        console.error(`Failed to stop container ${containerId}:`, err);
        this.snackBar.open(`Error stopping container ${containerId}. Please check logs.`, 'Close', { duration: 7000 });
      }
    });
  }
  /**
   * Monitors a tab for closure and cleans up when closed
   */
  private monitorTab(tab: Window, containerId: string): void {
    const intervalId = setInterval(() => {
      if (tab.closed) {
        clearInterval(intervalId);
        this.monitoredTabs.delete(containerId);
        this.cleanupContainer(containerId);
      }
    }, 1000);
    // Store both the tab reference and interval ID
    this.monitoredTabs.set(containerId, { tab, intervalId });
  }
  // #endregion Cleanup and Monitoring
}
