import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';


import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, HttpClientModule, withFetch } from '@angular/common/http';
import { BrowserAnimationsModule, provideAnimations } from '@angular/platform-browser/animations';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MaterialModule } from './material.module';
import { provideToastr } from 'ngx-toastr';
import { MatDialogModule } from '@angular/material/dialog';
export const appConfig: ApplicationConfig = {
  providers: [provideZoneChangeDetection({ eventCoalescing: true }),
  provideRouter(routes),
  provideClientHydration(),
  importProvidersFrom(HttpClientModule),
  importProvidersFrom(BrowserAnimationsModule, MaterialModule),
  provideAnimationsAsync(),
  provideHttpClient(withFetch()),
  provideAnimations(),
  importProvidersFrom(MatDialogModule),
  provideToastr({
    timeOut: 3000,
    positionClass: 'toast-top-right',
    preventDuplicates: true,
  })]
};