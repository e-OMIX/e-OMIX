import { Routes } from '@angular/router';
import { StarterComponent } from './starter/starter.component';
import { AboutUsComponent } from '../about-us/about-us.component';
import { UploadMetadataComponent } from '../upload-metadata/upload-metadata.component';

export const PagesRoutes: Routes = [
  {
    path: '',
    component: StarterComponent,
    data: {
      title: 'Starter',
      urls: [
        { title: 'Dashboard', url: '/dashboard' },
        { title: 'Starter' },
      ],
    },
  },
  {
    path: 'aboutUs',
    component: AboutUsComponent,
  },
  {
    path: 'upload',
    component: UploadMetadataComponent
  }
];
