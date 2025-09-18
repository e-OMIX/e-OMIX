import { Code, Identifier, Link, Meta, Subject } from "./modelResource";

export interface ObservationBundle {
  resourceType: string;
  id: string;
  meta: Meta;
  type: string;
  total: number;
  link: Link[];
  entry: ObservationEntry[];
}
export interface ObservationEntry {
  fullUrl: string;
  resource: Observation;
}
export interface Observation {
  resourceType: string;
  id: string;
  meta: Meta;
  identifier: Identifier[];
  subject: Subject;
  specimen: Subject;
  component: ObservationDataComponent[];
  code: Code;
  bodySite: Code;
  method: Code;
  derivedFrom: Subject[];
}
export interface ObservationDataComponent {
  code: Code;
  valueInteger?: number;
  valueQuantity?: ValueQuantity;
}
export interface ValueQuantity {
  value: number;
}
