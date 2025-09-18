import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-file-error-dialog',
  imports: [MatDialogModule,
    MatButtonModule],
  templateUrl: './file-error-dialog.component.html',
  styleUrl: './file-error-dialog.component.scss'
})
export class FileErrorDialogComponent {
  constructor(@Inject(MAT_DIALOG_DATA) public data: { allowedTypes: string }) { }
}
