
import { Routes } from '@angular/router';
import { SampleComponent } from '../sample/sample.component';
import { PatientsComponent } from '../patients/patients.component';
import { AnalysisComponent } from '../analysis/analysis.component';
import { AlignmentComponent } from '../alignment/alignment.component';
import { PostProcessingComponent } from '../post-processing/post-processing.component';
import { FhirTreeComponent } from '../fhir-tree/fhir-tree.component';

export const FhirComponentsRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: 'analysis',
        component: AnalysisComponent,
      },
      {
        path: 'alignment',
        component: AlignmentComponent,
      },
      {
        path: 'postProcessing',
        component: PostProcessingComponent,
      },
      {
        path: 'sample',
        component: SampleComponent,
      },
      {
        path: 'patients',
        component: PatientsComponent,
      },
      {
        path:'fhirTree',
        component: FhirTreeComponent,
      }

    ]
  }
]