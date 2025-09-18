import { TestBed } from '@angular/core/testing';

import { FHIRService } from './fhir.service';

describe('FHIRService', () => {
  let service: FHIRService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FHIRService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
