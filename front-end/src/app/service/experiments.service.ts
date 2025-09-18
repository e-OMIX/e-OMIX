import { Injectable } from '@angular/core';
import { Experiment } from '../models/Experiment/Experiment';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { MatTableDataSource } from '@angular/material/table';
import { StatusClass } from '../models/Experiment/ExperimentStatusClass';
import { environment } from '../../environment';

@Injectable({
  providedIn: 'root'
})
export class ExperimentsService {

  constructor(private readonly http: HttpClient) { }
  private readonly apiUrl = `${environment.apiUrl}/`;
  getExperiments(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl + 'experiment/allExperiments');
  }
  downloadJson(experiment: Experiment): void {
    const jsonData = JSON.stringify(experiment, null, 2);
    const blob = new Blob([jsonData], { type: 'application/json' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${experiment.experimentName.replace(/\s+/g, '_')}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
  sortedExperimentsByType(experiments: Experiment[], searchFilters: { [key in keyof Experiment]?: string }, experimentType: string): Experiment[] {
    return experiments
      .filter(exp =>
        this.verifyExperimentDetails(exp, searchFilters, experimentType)
      )
      .sort((a, b) =>
        this.compareByStatusThenName(a, b));
  }
  getDoneExperiments(experiments: Experiment[], searchFilters: { [key in keyof Experiment]?: string }, experimentType: string): Experiment[] {
    return this.sortedExperimentsByType(experiments, searchFilters, experimentType).filter(exp => exp.status.toLowerCase() === 'Done'.toLowerCase());
  }
  getStatusClass(status: string): string {
    return StatusClass[status as keyof typeof StatusClass] || '';
  }
  applyFilter(event: Event | string, column: keyof Experiment, searchFilters: { [key in keyof Experiment]?: string }, experimentsDataSource: MatTableDataSource<Experiment>) {
    const filterValue = this.normalizeFilterValue(event);
    this.updateSearchFilters(filterValue, column, searchFilters);
    this.getFilteredExperiment(searchFilters, experimentsDataSource);
  }
  downloadAllMatrices(folderName: string): Observable<Blob> {
    return this.http.get(this.apiUrl + `experiment/result/rawMatrix/${folderName}`, {
      responseType: 'blob'
    });
  }
  downloadProcessedMatrix(folderName: string) {
    const matrixFileUrl = this.apiUrl + `experiment/result/postProcessing/matrix/${folderName}`;
    const link = document.createElement('a');
    link.href = matrixFileUrl;
    link.setAttribute('download', 'result_matrix.sce.rds');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }
  viewMultiQCResult(folderName: string) {
    const htmlFileUrl = this.apiUrl + `experiment/result/multiqc/${folderName}/multiqc_report.html`;
    window.open(htmlFileUrl, '_blank');
  }
   //#region private Methods
  private verifyExperimentDetails(exp: Experiment, searchFilters: { [key in keyof Experiment]?: string }, experimentType: string): unknown {
    return (this.verifyExperimentName(exp, searchFilters)) &&
      (this.verifyExperimentType(exp, searchFilters, experimentType)) &&
      this.verifyExperimentStatus(exp, searchFilters)
  }
  private verifyExperimentStatus(exp: Experiment, searchFilters: { [key in keyof Experiment]?: string }): unknown {
    return !searchFilters['status'] || exp.status.toLowerCase() === searchFilters['status'].toLowerCase();
  }
  private verifyExperimentType(exp: Experiment, searchFilters: { [key in keyof Experiment]?: string }, experimentType: string) {
    if (experimentType !== null && experimentType !== 'None') {
      return exp.experimentType.toLowerCase() === experimentType.toLowerCase();
    }
    else if (experimentType === 'None') {
      return !searchFilters['experimentType'] || exp.experimentType.toLowerCase().includes(searchFilters['experimentType'].toLowerCase());
    }
  }
  private verifyExperimentName(exp: Experiment, searchFilters: { [key in keyof Experiment]?: string }) {
    return !searchFilters['experimentName'] || exp.experimentName.toLowerCase().includes(searchFilters['experimentName'].toLowerCase());
  }
  private compareByStatusThenName(a: Experiment, b: Experiment): number {
    const aDone = a.status === 'Done' ? 0 : 1;
    const bDone = b.status === 'Done' ? 0 : 1;
    if (aDone !== bDone) return aDone - bDone;
    return a.experimentName.localeCompare(b.experimentName);
  }
  private  getFilteredExperiment(searchFilters: { [key in keyof Experiment]?: string }, experimentsDataSource: MatTableDataSource<Experiment>) {
    experimentsDataSource.filterPredicate = (data: Experiment) => {
      return Object.entries(searchFilters).every(([key, value]) => {
        return (data[key as keyof Experiment] as string).toLowerCase().includes(value);
      });
    };
    experimentsDataSource.filter = JSON.stringify(searchFilters);
  }
   private updateSearchFilters(filterValue: string, column: keyof Experiment, searchFilters: { [key in keyof Experiment]?: string }) {
    if (filterValue) {
      searchFilters[column] = filterValue;
    } else {
      delete searchFilters[column];
    }
  }
  private  normalizeFilterValue(event: string | Event): string {
    if (typeof event === 'string') {
      return event.toLowerCase();
    } else {
      return (event.target as HTMLInputElement).value.trim().toLowerCase();
    }
  }
  //  #endregion private Methods
}
