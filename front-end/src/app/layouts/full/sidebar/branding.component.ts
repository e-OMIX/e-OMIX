import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'app-branding',
    imports: [RouterModule],
    template: `
    <div class="branding">
      <a [routerLink]="['/']">
        <img
          src="assets/newLogo/smallLogo.jpg"
          class="align-middle m-2"
          alt="eomix logo"
        />
      </a>
    </div>
  `
})
export class BrandingComponent {
  constructor() {}
}
