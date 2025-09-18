import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Bundle } from '../models/FHIR/patientResource';
import { SpecimenBundle } from '../models/FHIR/specimenResource';
import { environment } from '../../environment';
import { MolecularSequenceBundle } from '../models/FHIR/molecularSequenceResource';
import { GroupBundle } from '../models/FHIR/groupResource';

@Injectable({
  providedIn: 'root'
})
export class FHIRService {

  constructor(private readonly http: HttpClient) { }
  getAllPatientsDetails(identifier: string): Observable<Bundle> {
    const filename = encodeURIComponent(identifier);
    return this.http.get<Bundle>(`${environment.fhirApiUrl}/Patient?identifier=${filename}`);
  }
  getAllSamples(identifier: string): Observable<SpecimenBundle> {
    const filename = encodeURIComponent(identifier);
    return this.http.get<SpecimenBundle>(`${environment.fhirApiUrl}/Specimen?accession=${filename}`);
  }
  saveResourcesOnServer(identifier: string){
    const filename = encodeURIComponent(identifier);
    return this.http.post(`${environment.apiUrl}/resourceFHIR?filename=${filename}`,null);
  }
  getAllMolecularSequences(identifier: string): Observable<MolecularSequenceBundle> {
    const filename = encodeURIComponent(identifier);
    return this.http.get<MolecularSequenceBundle>(`${environment.fhirApiUrl}/MolecularSequence?identifier=${filename}`);
  }
  getAllGroups(identifier:string): Observable<GroupBundle>{
     const filename = encodeURIComponent(identifier);
    return this.http.get<GroupBundle>(`${environment.fhirApiUrl}/Group?identifier=${filename}`);
  }

}
