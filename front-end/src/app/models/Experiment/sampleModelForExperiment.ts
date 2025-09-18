export interface Sample {
  sampleName: string;
  fq1Files: File[];
  fq2Files: File[];
  selectedSequencingType: string;
}

export interface AllSamplesData {
  samples: Sample[] | any[];
  selectedOrganism: string;
  selectedProtocol: string;
  annotation: string;
  genome: string;
  omicsModality: string;
  cellularResolution: string;
  selectedAligner: string;
}