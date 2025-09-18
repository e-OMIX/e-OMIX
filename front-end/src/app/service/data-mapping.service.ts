import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DataMappingService {
  mapHeaders(headers: string[], headerMap: { [key: string]: string }): string[] {
    const reverseHeaderMap = Object.fromEntries(
      Object.entries(headerMap).map(([key, value]) => [value.trim(), key.trim()])
    );
    return headers.map(header => reverseHeaderMap[header.trim()] || header.trim());
  }
  addMissingHeaders(headerMap: { [key: string]: string }, headers: string[]): string[] {
    const newHeaders = [...headers];
    Object.keys(headerMap).forEach((key) => {
      if (!newHeaders.includes(key) && headerMap[key]) {
        newHeaders.push(key);
      }
    });
    return newHeaders;
  }
  mapData(data: any[], headerMap: { [key: string]: string }, defaultMappings: any): any[] {
    return data
      .map(row => this.mapRow(row, headerMap, defaultMappings))
      .filter(row => row !== null);
  }
  createHeaderMap(mappings: any): { [key: string]: string } {
    return Object.fromEntries(
      Object.entries(mappings).filter(([_, value]) => value && value !== '')
    ) as { [key: string]: string };
  }
  //#region private methods
  private mapRow(row: any, headerMap: { [key: string]: string }, defaultMappings: any): any {
    const trimmedOriginalRow = this.trimRowKeys(row);
    if (this.isEmptyRow(trimmedOriginalRow)) return null;
    const updatedRow = { ...row };
    this.applyHeaderMappings(updatedRow, trimmedOriginalRow, headerMap);
    this.applyDefaultMappings(updatedRow, defaultMappings);
    return updatedRow;
  }
  private trimRowKeys(row: any): Record<string, any> {
    return Object.entries(row).reduce<Record<string, any>>((acc, [key, value]) => {
      acc[key.trim()] = value;
      return acc;
    }, {});
  }
  private applyHeaderMappings(updatedRow: any, originalRow: any, headerMap: { [key: string]: string }): void {
    Object.entries(headerMap).forEach(([key, originalHeader]) => {
      if (originalRow[originalHeader]) {
        updatedRow[key] = String(originalRow[originalHeader]).trim();
      }
    });
  }
  private applyDefaultMappings(updatedRow: any, defaultMappings: any): void {
    Object.keys(defaultMappings).forEach((key) => {
      if (!updatedRow[key]) {
        updatedRow[key] = defaultMappings[key] || '';
      }
    });
  }
  private isEmptyRow(row: any): boolean {
    return Object.values(row).every(
      val => val == null || val === undefined || (typeof val === 'string' && val.trim() === '')
    );
  }
  //#endregion private methods
}