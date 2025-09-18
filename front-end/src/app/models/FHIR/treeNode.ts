export interface MolecularSequenceTreeNode {
  name: string;
  id: string;
  type: string;
  cellularResolution: string;
  accessionId: string;
  batchCount: number;
  children?: BatchTreeNode[];
  level: number;
  expandable: boolean;
  specimenReferences?: string[];
}
export interface BatchTreeNode {
  name: string;
  id: string;
  accessionId: string;
  specimenCount: number;
  children?: SpecimenTreeNode[];
  level: number;
  expandable: boolean;
}
export interface SpecimenTreeNode {
  name: string;
  id: string;
  assay: string;
  organ: string;
  condition: string;
  patient?: {
    id: string;
    gender: string;
    age?: string;
    species?: string;
  };
  level: number;
  expandable: boolean;
}