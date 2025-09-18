import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, ViewChild, ViewEncapsulation } from '@angular/core';
import { Subscription } from 'rxjs';
import { MatSidenav } from '@angular/material/sidenav';
import { navItems } from './sidebar/sidebar-data';
import { AppNavItemComponent } from './sidebar/nav-item/nav-item.component';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../../material.module';
import { CommonModule } from '@angular/common';
import { SidebarComponent } from './sidebar/sidebar.component';
import { NgScrollbarModule } from 'ngx-scrollbar';
import { HeaderComponent } from './header/header.component';

const MOBILE_VIEW = 'screen and (max-width: 768px)';
const TABLET_VIEW = 'screen and (min-width: 769px) and (max-width: 1024px)';
const MONITOR_VIEW = 'screen and (min-width: 1024px)';


@Component({
  selector: 'app-full',
  imports: [
    RouterModule,
    AppNavItemComponent,
    MaterialModule,
    CommonModule,
    SidebarComponent,
    NgScrollbarModule,
    HeaderComponent,
  ],
  templateUrl: './full.component.html',
  styleUrls: [],
  encapsulation: ViewEncapsulation.None
})

export class FullComponent {

  navItems = navItems;
  imageUrl: string = "src/assets/images/logo_UEWallonieFWB.jpg";
  @ViewChild('leftsidenav')
  public sidenav: MatSidenav;

  //get options from service
  private readonly layoutChangesSubscription = Subscription.EMPTY;
  private isMobileScreen = false;
  isContentWidthFixed = true;
  isCollapsedWidthFixed = false;
  private readonly htmlElement!: HTMLHtmlElement;

  get isOver(): boolean {
    return this.isMobileScreen;
  }

  constructor(private readonly breakpointObserver: BreakpointObserver) {
    if (typeof document !== 'undefined') {
      this.htmlElement = document.querySelector('html')!;
      this.htmlElement.classList.add('light-theme');
      this.layoutChangesSubscription = this.breakpointObserver
        .observe([MOBILE_VIEW, TABLET_VIEW, MONITOR_VIEW])
        .subscribe((state) => {
          // SidenavOpened must be reset true when layout changes
          this.isMobileScreen = state.breakpoints[MOBILE_VIEW];
          this.isContentWidthFixed = state.breakpoints[MONITOR_VIEW];
        });
    }
  }

  // ngOnInit(): void {}

  ngOnDestroy() {
    this.layoutChangesSubscription.unsubscribe();
  }

  toggleCollapsed() {
    this.isContentWidthFixed = false;
  }

  onSidenavClosedStart() {
    this.isContentWidthFixed = false;
  }

  onSidenavOpenedChange() {
    this.isCollapsedWidthFixed = !this.isOver;
  }
}
