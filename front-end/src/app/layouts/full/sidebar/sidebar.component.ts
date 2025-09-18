import {
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { BrandingComponent } from './branding.component';
import { MaterialModule } from '../../../material.module';

@Component({
    selector: 'app-sidebar',
    imports: [BrandingComponent,  MaterialModule],
    templateUrl: './sidebar.component.html'
})
export class SidebarComponent  {
  constructor() { }
  @Input() showToggle = true;
  @Output() toggleMobileNav = new EventEmitter<void>();
  @Output() toggleCollapsed = new EventEmitter<void>();

}