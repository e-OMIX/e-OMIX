import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class VisualizationTrackerService {
  
  // constructor(private readonly visualizationService: VisualizationService) { }
  // private readonly activeVisualizations: Map<string, string> = new Map(); // experimentName -> containerId

  // addVisualization(experimentName: string, containerId: string): void {
  //   this.activeVisualizations.set(experimentName, containerId);
  // }
  // stopVisualization(experimentName: string): void {
  //   const containerId = this.activeVisualizations.get(experimentName);
  //   if (containerId) {
  //     this.visualizationService.stopVisualization(containerId).subscribe();
  //     this.activeVisualizations.delete(experimentName);
  //   }
  // }
  // stopAllVisualizations(): void {
  //   this.activeVisualizations.forEach((containerId) => {
  //     this.visualizationService.stopVisualization(containerId).subscribe();
  //   });
  //   this.activeVisualizations.clear();
  // }
}
