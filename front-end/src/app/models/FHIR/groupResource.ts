import { Identifier, Link, Meta } from "./modelResource";
export interface GroupBundle {
  resourceType: string;
  id: string;
  meta: Meta;
  type: string;
  total: number;
  link: Link[];
  entry: GroupEntry[];
}
export interface GroupEntry {
  fullUrl: string;
  resource: Group;
}
export interface Group {
  resourceType: string;
  id: string;
  meta: Meta;
  identifier: Identifier[];
  type: string;
  member: Member[];
}

export interface Member {
  entity: Entity;
}
export interface Entity {
  id: string;
  reference: string;
  type: string;
}