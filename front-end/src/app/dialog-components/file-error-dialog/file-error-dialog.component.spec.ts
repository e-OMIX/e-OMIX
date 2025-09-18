import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FileErrorDialogComponent } from './file-error-dialog.component';

describe('FileErrorDialogComponent', () => {
  let component: FileErrorDialogComponent;
  let fixture: ComponentFixture<FileErrorDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FileErrorDialogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FileErrorDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
