// #region Imports
import { CommonModule } from '@angular/common';
import { Component, ViewChildren, QueryList, ViewChild, inject, OnInit, OnDestroy, AfterViewInit } from '@angular/core';
import { MaterialModule } from '../material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatMenu, MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortable, MatSortModule } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { Experiment } from '../models/Experiment/Experiment';
import { StatusClass } from '../models/Experiment/ExperimentStatusClass';
import { StatusFormatPipe } from '../service/StatusFormatPipe';
import { VisualizationService } from '../service/visualization.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { WaitingDialogComponent } from '../dialog-components/waiting-dialog/waiting-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { ExperimentsService } from '../service/experiments.service';
import { saveAs } from 'file-saver';
import { ToastrService } from 'ngx-toastr';
// #endregion Imports

@Component({
  selector: 'app-analysis',
  imports: [CommonModule, MaterialModule, FormsModule, ReactiveFormsModule, MatMenuModule,
    MatButtonModule,
    MatIconModule, MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    StatusFormatPipe],
  templateUrl: './analysis.component.html',
  styleUrl: './analysis.component.scss'
})
export class AnalysisComponent implements OnInit, AfterViewInit, OnDestroy {
  // #region ViewChild
  @ViewChildren('menu') menus!: QueryList<MatMenu>;
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: false }) sort!: MatSort;
  // #endregion ViewChild

  experiments: Experiment[] = [];
  displayedColumns: string[] = ['experimentName', 'omicsModality', 'cellularResolution', 'experimentType', 'status', 'createdAt', 'Action', 'download'];
  experimentsDataSource = new MatTableDataSource(this.experiments);
  menuTriggerMap: { [key: string]: MatMenu } = {};
  refreshInterval: any;
  searchFilters: { [key in keyof Experiment]?: string } = {};
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
    private readonly dialog: MatDialog,
    private readonly toastr: ToastrService,
    private readonly experimentsService: ExperimentsService) { }
  // #endregion Constructor

  // #region Initialization, Listener and Lifecycle Hooks
  ngOnInit(): void {
    this.getAllExperiments();
  }
  ngAfterViewInit() {
    this.menus.forEach((menu, index) => {
      this.menuTriggerMap[index] = menu;
    });
    setTimeout(() => {
      if (this.experimentsDataSource) {
        this.experimentsDataSource.paginator = this.paginator;
        this.experimentsDataSource.sort = this.sort;
        this.setupMultiSort();
      } else {
        console.error('experimentsDataSource is still undefined');
      }
    }, 500);
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
  // #endregion Initialization, Listener and Lifecycle Hooks

  // #region Experiment Table Methods







  // #region CRUD Operations Experiments
  getAllExperiments(): void {
    this.experimentsService.getExperiments().subscribe({
      next: (data: Experiment[]) => {
        this.experiments = data;
        this.experimentsDataSource = new MatTableDataSource(this.experiments);
        this.experimentsDataSource.data = this.experiments;
        this.experimentsDataSource.paginator = this.paginator;
        this.experimentsDataSource.sort = this.sort;
      },
      error: (error) => {
        console.error('Error fetching experiments:', error);
      }
    });
  }
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
  downloadProcessedMatrix(folderName: string) {
    this.experimentsService.downloadProcessedMatrix(folderName);
    this.toastr.success('Matrix download started', 'Success');
  }
  // #endregion CRUD Operations Experiments

  // #region 🔍 Filtering and Searching
  applyFilter(event: Event | string, column: keyof Experiment) {
    this.experimentsService.applyFilter(event, column, this.searchFilters, this.experimentsDataSource);
  }
  private getActiveSorts(sort: MatSort) {
    const activeSorts: { column: string; direction: number; }[] = [];
    sort.sortables.forEach((_sortable: MatSortable, column: string) => {
      if (sort.active === column && sort.direction) {
        activeSorts.push({
          column,
          direction: sort.direction === 'asc' ? 1 : -1,
        });
      }
    });
    return activeSorts;
  }
  // #endregion Filtering and Searching

  // #region Sorting and Comparison
  setupMultiSort() {
    this.experimentsDataSource.sortData = (data, sort) => {
      const activeSorts: { column: string; direction: number; }[] = this.getActiveSorts(sort);
      if (activeSorts.length === 0) return data;
      return this.compareByMultipleColumns(data, activeSorts);
    };
  }
  private multiColumnComparator(a: Experiment, b: Experiment, activeSorts: { column: string; direction: number }[]): number {
    for (const { column, direction } of activeSorts) {
      const comparisonResult = this.compareSingleColumn(a, b, column, direction);
      if (comparisonResult !== 0) {
        return comparisonResult;
      }
    }
    return 0;
  }
  private compareSingleColumn(a: Experiment, b: Experiment, column: string, direction: number): number {
    let valueA = a[column as keyof Experiment];
    let valueB = b[column as keyof Experiment];
    ({ valueA, valueB } = this.getLowerCaseValues(valueA, valueB));
    if (valueA < valueB) return -1 * direction;
    if (valueA > valueB) return 1 * direction;
    return 0;
  }
  private compareByMultipleColumns(data: Experiment[], activeSorts: { column: string; direction: number; }[]): Experiment[] {
    return data.sort((a, b) => this.multiColumnComparator(a, b, activeSorts));
  }
  // #endregion Sorting and Comparison

  // #region UI Helpers  
  private getLowerCaseValues(valueA: any, valueB: any) {
    if (this.isString(valueA, valueB)) {
      valueA = valueA.toLowerCase();
      valueB = valueB.toLowerCase();
    }
    return { valueA, valueB };
  }
  private isString(valueA: any, valueB: any) {
    return typeof valueA === 'string' && typeof valueB === 'string';
  }
  getStatusClass(status: string): string {
    return StatusClass[status as keyof typeof StatusClass] || '';
  }
  // #endregion UI Helpers

  // #region Refresh and Reset Handlers
  manualRefresh() {
    this.getAllExperiments();
    console.log("data refresheed")
  }
  resetFilters() {
    this.searchFilters = {};
    this.experimentsDataSource.filter = '';
    const inputs = document.querySelectorAll<HTMLInputElement>('.search-container input');
    inputs.forEach(input => (input.value = ''));
    this.searchFilters['status'] = '';
    this.searchFilters['experimentType'] = '';
  }
  // #endregion Refresh and Reset Handlers

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