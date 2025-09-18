import { TestBed } from '@angular/core/testing';

import { VisualizationTrackerService } from './visualization-tracker.service';

describe('VisualizationTrackerService', () => {
  let service: VisualizationTrackerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(VisualizationTrackerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
