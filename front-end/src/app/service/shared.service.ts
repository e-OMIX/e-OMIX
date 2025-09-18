import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MatTableDataSource } from '@angular/material/table';
import { FileErrorDialogComponent } from '../dialog-components/file-error-dialog/file-error-dialog.component';
import { MatDialog } from '@angular/material/dialog';

@Injectable({
  providedIn: 'root'
})
export class SharedService {
  constructor(private readonly dialog: MatDialog) { }
  private readonly isDataTableVisibleSource = new BehaviorSubject<boolean>(false);
  private readonly isTableVisibleSource = new BehaviorSubject<boolean>(false);
  private tableDisplayedColumns: string[];
  private readonly tableData: MatTableDataSource<any> = new MatTableDataSource<any>();
  private isFileUploaded: boolean = false;
  hasTableDataInfo$ = this.isDataTableVisibleSource.asObservable(); 
  isTableVisible$ = this.isTableVisibleSource.asObservable();

  setTableVisible(isVisible: boolean) {
    this.isTableVisibleSource.next(isVisible);
  }
  setTableData(data: any[]) {
    this.tableData.data = data;
  }
  getTableData(): MatTableDataSource<any> {
    return this.tableData;
  }
  setTableHeaders(displayedColumns: string[]) {
    this.tableDisplayedColumns = displayedColumns;
  }
  getTableHeaders(): string[] {
    return this.tableDisplayedColumns;
  }
  hasTableData(): boolean {
    if (this.tableData.data.length > 0) {
      return true;
    }
    return false;
  }
  isFileUploadedAlready(isUploaded: boolean) {
    this.isFileUploaded = isUploaded;
  }
  getFileUploadedBooleanValue(): boolean {
    return this.isFileUploaded;
  }
  isValidCSVFile(file: File): boolean {
    const validExtensions = ['.csv'];
    return this.isValidExtention(file, validExtensions);
  }
  isValidExtention(file: File, validExtensions: string[]): boolean {
    const fileName = file.name.toLowerCase();
    return validExtensions.some(ext => fileName.endsWith(ext));
  }
  showErrorDialog(input: HTMLInputElement, allowedTypes: string[]): void {
    const dialogRef = this.dialog.open(FileErrorDialogComponent, {
      width: '400px',
      disableClose: true,
      data: { allowedTypes } // Pass dynamic data here
    });

    dialogRef.afterClosed().subscribe(() => {
      input.value = ''; // Reset file input
    });
  }
}
