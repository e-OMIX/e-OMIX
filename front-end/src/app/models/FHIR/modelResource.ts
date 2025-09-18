export interface Coding {
  code: string;
  display: string;
}
export interface Subject {
  reference: string;
}
export interface Meta {
  lastUpdated: string;
  versionId: string;
}
export interface Link {
  relation: string;
  url: string;
}
export interface Code {
  coding: Coding
  text: string;
}
export interface UploadResponse {
  message: string;
}
export interface Identifier {
  id: string;
  use: string;
  type: Code
  value: string;
}
