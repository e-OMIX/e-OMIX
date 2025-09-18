export interface Experiment {
  experimentName: string;
  experimentType: string;
  status: string;
  metadataFileName: string;
  samples: any;
  selectedOrganism: string;
  selectedProtocol: string;
  annotation: string;
  genome: string;
  createdAt: string;
  omicsModality: string;
  cellularResolution: string;
}