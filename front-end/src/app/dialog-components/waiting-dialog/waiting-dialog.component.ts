import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-waiting-dialog',
  imports: [CommonModule, MatProgressSpinnerModule],
  templateUrl: './waiting-dialog.component.html',
  styleUrl: './waiting-dialog.component.scss'
})
export class WaitingDialogComponent {

}
