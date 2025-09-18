import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, inject, Input, OnDestroy, OnInit, QueryList, ViewChildren, ViewEncapsulation } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../material.module';
import { NgSelectModule } from '@ng-select/ng-select';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { FileManagementService } from '../service/file-management.service';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { Experiment } from '../models/Experiment/Experiment';
import { StatusClass } from '../models/Experiment/ExperimentStatusClass';
import { MatMenu, MatMenuModule } from '@angular/material/menu';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Subject, Subscription, takeUntil } from 'rxjs';
import { ConfirmDeleteDialogComponent } from '../dialog-components/confirm-delete-dialog/confirm-delete-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { ExperimentsService } from '../service/experiments.service';
import { WaitingDialogComponent } from '../dialog-components/waiting-dialog/waiting-dialog.component';
import { VisualizationService } from '../service/visualization.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { saveAs } from 'file-saver';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, FormsModule, ReactiveFormsModule, MaterialModule, NgSelectModule, MatInputModule, MatMenuModule,
    MatButtonModule,
    MatIconModule, MatTableModule,
    MatPaginatorModule,
    MatSortModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  // #region ViewChild
  @ViewChildren(MatPaginator) paginator = new QueryList<MatPaginator>();
  @ViewChildren(MatSort) sort = new QueryList<MatSort>();
  @ViewChildren('menu') menus!: QueryList<MatMenu>;
  // #endregion ViewChild
  @Input() experiments: Experiment[] = [];
  private readonly destroy$ = new Subject<void>();

  loadingFileFetching = false;
  refreshInterval: any;
  files: any[] = [];
  filesDataSource: MatTableDataSource<any> = new MatTableDataSource<any>();
  displayedDataSetsColumns: string[] = ['filename', 'cellularResolution', 'omicsModality', 'organ', 'disorder', 'species', 'actions'];
  pageSize = 6;
  pageIndex = 0;
  experimentsDataSource = new MatTableDataSource(this.experiments);
  searchFilters: { [key in keyof Experiment]?: string } = {};
  filteredExperimentsDataSource = new MatTableDataSource(this.experiments);
  menuTriggerMap: { [key: string]: MatMenu } = {};
  pagedExperiments: Experiment[] = [];
  loadingOnDelete = false;
  loadingOnExport = false;
  tableFilters: { [key: string]: string } = {};
  isDownloading: boolean = false;
  downloadProgress: number = 0;
  // Visualization Service Integration
  private readonly visualizationService = inject(VisualizationService);
  private readonly snackBar = inject(MatSnackBar);
  // Track tabs by containerId
  private readonly monitoredTabs = new Map<string, { tab: Window, intervalId: any }>();
  private readonly visualizationSubscriptions: Subscription[] = [];

  // #region Constructor
  constructor(
    private readonly fileManagementService: FileManagementService,
    private readonly cdr: ChangeDetectorRef,
    private readonly dialog: MatDialog,
    private readonly toastr: ToastrService,
    private readonly experimentsService: ExperimentsService) {
    this.filesDataSource = new MatTableDataSource<any>(this.files);
    this.experimentsDataSource = new MatTableDataSource<Experiment>(this.experiments);
  }
  // #endregion Constructor

  // #region Initialization, Listener and Lifecycle Hooks
  ngAfterViewInit(): void {
    this.filteredExperimentsDataSource.paginator = this.paginator.toArray()[0];
    this.filesDataSource.paginator = this.paginator.toArray()[1];
    this.filesDataSource.sort = this.sort.toArray()[0];
    if (this.paginator) {
      this.paginator.toArray()[1].page.subscribe(() => {
        this.updatePagedExperiments();
      });
    }
    this.updatePagedExperiments();
  }
  ngOnInit(): void {
    this.getFiles();
    this.getAllExperiments();
    this.sortedExperiments();
    this.filteredExperimentsDataSource.paginator = this.paginator.toArray()[0];
    this.filesDataSource.filterPredicate = (data: any, filter: string) => {
      const filters = JSON.parse(filter);
      return Object.keys(filters).every(key => {
        const dataValue = data[key]?.toString().toLowerCase();
        return dataValue.includes(filters[key]);
      });
    };
    this.filesDataSource.paginator = this.paginator.toArray()[1];
    this.filesDataSource.sort = this.sort.toArray()[0];
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    // Clean up all intervals when component is destroyed
    this.monitoredTabs.forEach((value, containerId) => {
      clearInterval(value.intervalId);
      this.monitoredTabs.delete(containerId);
    });
    // Unsubscribe from all observables
    this.visualizationSubscriptions.forEach(sub => sub.unsubscribe());
  }
  // #endregion Initialization, Listener and Lifecycle Hooks

  //#region 📂 Data Fetching Methods
  getFiles(): void {
    this.loadingFileFetching = false;
    this.fileManagementService.getMetadataFiles()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (files) => {
          if (files) {
            this.loadingFileFetching = true;
            this.files = files;
            this.filesDataSource.data = files;
            this.filesDataSource.paginator = this.paginator.toArray()[1];
            this.cdr.detectChanges();
          }
        },
        error: (error) => {
          console.error('Error fetching files:', error);
          this.loadingFileFetching = true;
        }
      });
  }
  getAllExperiments(): void {
    this.experimentsService.getExperiments().subscribe({
      next: (data: Experiment[]) => {
        this.experiments = data;
        this.experimentsDataSource = new MatTableDataSource(this.experiments);
        this.experimentsDataSource.data = this.experiments;
        this.filteredExperimentsDataSource = new MatTableDataSource(this.sortedExperiments());
        this.filteredExperimentsDataSource.paginator = this.paginator.toArray()[0];
        this.updatePagedExperiments();
      },
      error: (error) => {
        console.error('Error fetching experiments:', error);
      }
    });
  }
  // #endregion 📂 Data Fetching Methods

  // #region view result and download
  viewHtml(folderName: string) {
    this.experimentsService.viewMultiQCResult(folderName);
  }
  downloadJson(experiment: Experiment): void {
    this.experimentsService.downloadJson(experiment);
  }
  downloadAllMatrices(folderName: string) {
    this.isDownloading = true;
    this.experimentsService.downloadAllMatrices(folderName).subscribe({
      next: (blob: Blob) => {
        this.toastr.success('Matrices download started', 'Success');
        saveAs(blob, folderName + '_matrices.zip');
        this.isDownloading = false;
      },
      error: (err) => {
        console.error('Download failed:', err);
        this.isDownloading = false;
      }
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
  downloadMatrix(folderName: string) {
    this.experimentsService.downloadProcessedMatrix(folderName);
    this.toastr.success('Matrix download started', 'Success');
  }
  // #endregion view result and download

  //#region 🔍 Filtering & Sorting
  applyFilter(event: Event | string, column: keyof Experiment) {
    this.experimentsService.applyFilter(event, column, this.searchFilters, this.experimentsDataSource);
    this.updatePagedExperiments();
  }
  resetFilters() {
    this.searchFilters = {};
    this.experimentsDataSource.filter = '';
    this.filteredExperimentsDataSource.data = [...this.sortedExperiments()]; // Reset dataset
    this.filteredExperimentsDataSource.paginator = this.paginator.toArray()[0];
    this.filesDataSource.paginator = this.paginator.toArray()[1];
    this.updatePagedExperiments();
    const inputs = document.querySelectorAll<HTMLInputElement>('.filters-container input');
    inputs.forEach(input => (input.value = ''));
    this.searchFilters['experimentType'] = '';
  }
  sortedExperiments(): Experiment[] {
    return this.experimentsService.getDoneExperiments(this.experiments, this.searchFilters, 'None');
  }
  applyTableFilter(event: Event | string, column: string) {
    let filterValue = '';
    if (typeof event === 'string') {
      filterValue = event.toLowerCase();
    } else {
      filterValue = (event.target as HTMLInputElement).value.trim().toLowerCase();
    }
    this.tableFilters[column] = filterValue;
    this.filesDataSource.filter = JSON.stringify(this.tableFilters);
  }
  updatePagedExperiments(): void {
    if (!this.experiments) return;
    this.paginator.toArray()[0].length = this.sortedExperiments().length;
    const startIndex = this.paginator.toArray()[0].pageIndex * this.paginator.toArray()[0].pageSize;
    const endIndex = startIndex + this.paginator.toArray()[0].pageSize;
    this.pagedExperiments = this.sortedExperiments().slice(startIndex, endIndex);
  }
  manualRefresh() {
    this.getFiles();
    console.log("data refresheed")
  }
  //  #endregion 🔍 Filtering & Sorting


  // #region Delete File
  deleteFile(element: any): void {
    this.loadingOnDelete = true;
    this.fileManagementService.deleteDocument(element.filename).subscribe({
      next: () => {
        this.toastr.success('Metadata delete started', 'Success');
        this.manualRefresh();
        this.loadingOnDelete = false;
      },
      error: (err) => {
        console.error('Error deleting file:', err);
        this.loadingOnDelete = false;
      }
    });
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
  // #endregion Delete File

  //#region 🛠 Helpers & Utilities
  getTooltipText(array: any[]): string {
    return this.fileManagementService.getTooltipText(array);
  }
  getStatusClass(status: string): string {
    return StatusClass[status as keyof typeof StatusClass] || '';
  }
  //#endregion 🛠 Helpers & Utilities

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
    console.log(`Calling cleanup service for container ${containerId}...`);
    this.visualizationService.stopVisualization(containerId).subscribe({
      next: () => {
        this.snackBar.open(`Visualization container ${containerId} stopped successfully.`, 'Close', { duration: 4000 });
        console.log(`Container ${containerId} stopped successfully`);
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
        console.log(`Tab for container ${containerId} closed`);
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
