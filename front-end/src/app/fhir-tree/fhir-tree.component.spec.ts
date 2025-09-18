import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FhirTreeComponent } from './fhir-tree.component';

describe('FhirTreeComponent', () => {
  let component: FhirTreeComponent;
  let fixture: ComponentFixture<FhirTreeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FhirTreeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FhirTreeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
