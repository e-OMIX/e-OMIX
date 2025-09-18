import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, Observable, of, tap } from 'rxjs';
import { environment } from '../../environment';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class FileManagementService {

  constructor(private readonly http: HttpClient) { }
  private readonly apiUrl = `${environment.apiUrl}/`;
  getMetadataFiles(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl + `files/allMetadataFiles`);
  }
  deleteDocument(identifier: string): Observable<any> {
    const filename = encodeURIComponent(identifier);
    return this.http.delete(this.apiUrl + `delete/metadata?filename=${filename}`, { responseType: 'text' });
  }
  getTooltipText(array: any[]): string {
    if (!this.isValidArray(array)) {
      return '';
    }
    return this.getTooltipResult(array);
  }
  prepareUploadFormData(csvContent: string, fileName: string, delimiter: string): FormData {
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const formData = new FormData();
    formData.append('detectedDelimiter', delimiter);
    formData.append('file', blob, `${fileName.trim()}.csv`);
    return formData;
  }
  uploadFile(formData: FormData): Observable<any> {
    return this.http.post(this.apiUrl + `upload`, formData);
  }
  downloadMetadataFile(fileName: string, toastr: ToastrService): Observable<boolean> {
    const filenameEncoded = encodeURIComponent(fileName);
    return this.http.get(`${environment.apiUrl}/export?filename=${filenameEncoded}`, {
      responseType: 'blob'
    }).pipe(
      tap(() => {
        toastr.success('Metadata download started', 'Success');
      }),
      map(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
        return false;
      }),
      catchError(error => {
        console.error('Download error:', error);
        toastr.error('Error downloading file', 'Error');
        return of(false);
      })
    );
  }

  //#region private Methods
  private getTooltipResult(array: any[]): string {
    return this.isObjectArray(array)
      ? this.getObjectArrayTooltip(array)
      : this.getPrimitiveArrayTooltip(array);
  }
  private isValidArray(array: any[]): boolean {
    return Array.isArray(array) && array.length > 0;
  }
  private isObjectArray(array: any[]): boolean {
    return typeof array[0] === 'object';
  }
  private getObjectArrayTooltip(array: any[]): string {
    return array
      .map((item) => this.getTooltipValue(item))
      .filter(Boolean)
      .join(', ');
  }
  private getTooltipValue(item: any): string {
    return item.name || item.value || '';
  }
  private getPrimitiveArrayTooltip(array: any[]): string {
    return array.join(', ');
  }
  //#endregion private methods
}
