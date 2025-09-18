import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-error',
  imports: [CommonModule],
  templateUrl: './error.component.html',
  styleUrl: './error.component.scss'
})
export class ErrorComponent {
  @Input() errors: string[] = [];
  @Input() dismissible = false;
  @Input() type: 'alert' | 'toast' = 'alert';

  dismissError(index: number): void {
    this.errors.splice(index, 1);
  }
}
