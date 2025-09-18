import { Routes } from '@angular/router';
import { FullComponent } from './layouts/full/full.component';
import { AnalysisComponent } from './analysis/analysis.component';


export const routes: Routes = [
  {
    path: '',
    component: FullComponent,
    children: [
      {
        path: '',
        redirectTo: '/dashboard',
        pathMatch: 'full',
      },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./pages/pages.routes').then((m) => m.PagesRoutes),
      },
      {
        path: 'fhirData',
        loadChildren: () =>
          import('./pages/fhir-components.routes').then((m) => m.FhirComponentsRoutes),
      },
      { path: 'analysis', component: AnalysisComponent },
    ],
  },
  {
    path: '**',
    redirectTo: 'authentication/error',
  },
];
