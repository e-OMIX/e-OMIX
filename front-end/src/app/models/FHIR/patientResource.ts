import { Identifier, Link, Meta, Subject } from "./modelResource";

export interface Bundle {
  resourceType: string;
  id: string;
  meta: Meta;
  type: string;
  total: number;
  link: Link[];
  entry: PatientEntry[];
}
export interface PatientEntry {
  fullUrl: string;
  resource: Patient;
}
export interface Patient {
  resourceType: string;
  id: string;
  meta: Meta;
  identifier: Identifier[];
  status: string;
  category: Category[];
  code: Code;
  subject: Subject;
  encounter: Subject;
  effectiveDateTime: string;
  issued: string;
  valueCodeableConcept: Code;
  extension: Extension[];
  gender: string;
}
export interface Code {
  coding: Coding[];
  text: string;
}
export interface Category {
  coding: Coding[];
}
export interface Coding {
  system: string;
  code: string;
  display: string;
}
export interface Tag {
  system: string;
  code: string;
}

export interface Extension {
  url: string;
  valueCodeableConcept: ValueCodeableConcept;
}

export interface ValueCodeableConcept {
  coding: Coding;
  text: string;
}