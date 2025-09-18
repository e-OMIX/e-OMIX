import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environment';

export interface VisualizationStartResponse {
  containerId: string;
  shinyUrl: string;
}
@Injectable({
  providedIn: 'root'
})
export class VisualizationService {
  
  constructor(private readonly http: HttpClient) { }
  private readonly apiUrl = `${environment.apiUrl}/visualization`;

  startVisualization(experimentName: string): Observable<VisualizationStartResponse> {
    // The backend now returns a JSON object, not plain text
    return this.http.post<VisualizationStartResponse>(`${this.apiUrl}/start`, experimentName);
  }
  // Pass the containerId to be stopped
  stopVisualization(containerId: string): Observable<any> {
    const body = { containerId: containerId };
    return this.http.post(`${this.apiUrl}/stop`, body, { responseType: 'text' });
  }
  // // Pass the dynamic URL to check
  // checkShinyStatus(url: string): Observable<string> {
  //   const params = new HttpParams().set('url', url);
  //   return this.http.get<string>(`${this.apiUrl}/status`, { params, responseType: 'text' as 'json' });
  // }
  // openInNewTab(url: string) {
  //   window.open(url, '_blank');
  // }
}
