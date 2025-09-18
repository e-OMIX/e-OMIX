import { Group } from "./groupResource";
import { Coding, Link, Meta, Subject } from "./modelResource";

export interface SpecimenBundle {
  resourceType: string;
  id: string;
  meta: Meta;
  type: string;
  total: number;
  link: Link[];
  entry: SpecimenEntry[];
}
export interface SpecimenEntry {
  fullUrl: string;
  resource: Specimen;
}
export interface Specimen {
  resourceType: string;
  id: string;
  identifier: Identifier[]
  meta: SpecimenMeta;
  contained: Group[];
  collection: Collection;
  condition: Condition[];
  subject: Subject;
  extension: Extension[];
}
export interface Identifier {
  id: string;
}
export interface Collection {
  method: Method;
}
export interface Method {
  coding: Coding;
  text: string;
}
export interface Condition {
  coding: Coding;
  text: string;
}
export interface SpecimenMeta {
  versionId: string;
}
export interface Extension {
  url: string;
  valueReference: ValueReference;
}

export interface ValueReference {
  id: string;
  reference: string;
  type: string;
}