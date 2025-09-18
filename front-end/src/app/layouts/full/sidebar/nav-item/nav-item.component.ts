import {
  Component,
  Input,
  OnChanges,
  Output,
  EventEmitter,
} from '@angular/core';
import { NavItem } from './nav-item';
import { Router } from '@angular/router';
import { NavService } from '../../../../service/nav.service';
import { TranslateModule } from '@ngx-translate/core';
import { MaterialModule } from '../../../../material.module'; 
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-nav-item',
    imports: [TranslateModule, MaterialModule, CommonModule],
    templateUrl: './nav-item.component.html',
    styleUrls: []
})
export class AppNavItemComponent implements OnChanges {
  @Output() toggleMobileLink: any = new EventEmitter<void>();
  @Output() notify: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Input() item: NavItem;
  @Input() depth: any;

  constructor(public navService: NavService, public router: Router) {
    if (this.depth === undefined) {
      this.depth = 0;
    }
  }

  ngOnChanges() {
    this.navService.currentUrl.subscribe(() => {});
  }

  onItemSelected(item: NavItem) {
    this.router.navigate([item.route]);

    //scroll
    window.scroll({
      top: 0,
      left: 0,
      behavior: 'smooth',
    });
  }

  onSubItemSelected() {
    
  }
}
