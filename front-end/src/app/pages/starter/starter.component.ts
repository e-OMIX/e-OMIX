import { Component, ViewEncapsulation } from '@angular/core';
import { MaterialModule } from '../../material.module';
import { DashboardComponent } from '../../dashboard/dashboard.component';

@Component({
    selector: 'app-starter',
    imports: [
        MaterialModule,
        DashboardComponent
    ],
    templateUrl: './starter.component.html',
    styleUrls: ['./starter.component.scss'],
    encapsulation: ViewEncapsulation.None
})
export class StarterComponent { }
