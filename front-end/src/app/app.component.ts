import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppIconsModule } from './app-icons.module';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppIconsModule,],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent  {
  constructor(
    // private readonly tracker: VisualizationTrackerService
  ) { }
  title = 'e-OMIX';
}

