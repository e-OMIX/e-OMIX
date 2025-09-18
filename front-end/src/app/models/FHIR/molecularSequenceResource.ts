import { Identifier, Link, Meta } from "./modelResource";
import { Extension } from "./specimenResource";
export interface MolecularSequenceBundle {
  resourceType: string;
  id: string;
  meta: Meta;
  type: string;
  total: number;
  link: Link[];
  entry: MolecularSequenceEntry[];
}
export interface MolecularSequenceEntry {
    fullUrl: string;
    resource: MolecularSequence;
}
export interface MolecularSequence {

    resourceType: string;
    id: string;
    meta: Meta;
    identifier: Identifier[];
    extension: Extension[];
    type: string;
}