import { Injectable } from '@angular/core';
import { Papa, ParseResult } from 'ngx-papaparse';

@Injectable({ providedIn: 'root' })
export class CsvParserService {

  constructor(private readonly papa: Papa) { }
  parseCSV(csvContent: string): { headers: string[]; data: any[] } {
    const rows = csvContent.split(this.getRowSeparator(csvContent));
    const separator = this.getHeaderSeparator(rows);
    const headers = rows[0].split(separator).map(header => header.trim().replace(/(^"|"$)/g, ''));
    const data = rows.slice(1)
      .map(row => this.createRowObject(headers, row, separator))
      .filter(obj => obj !== null);
    return { headers: headers.filter(h => h.trim() !== ''), data };
  }
  parseWithPapa(csvContent: string): Promise<ParseResult<any>> {
    return new Promise((resolve, reject) => {
      this.papa.parse(csvContent, {
        header: true,
        skipEmptyLines: true,
        complete: (result) => resolve(result),
        error: (error) => reject(new Error(typeof error === 'string' ? error : JSON.stringify(error)))
      });
    });
  }
  //#region private methods
  private createRowObject(headers: string[], row: string, separator: string): any {
    const values = row.replace(/(^"|"$)/g, '').split(separator);
    if (values.every(val => val.trim() === '')) return null;
    const obj: any = {};
    this.trimHeaders(headers, obj, values);
    return obj;
  }
  private trimHeaders(headers: string[], obj: any, values: string[]) {
    headers.forEach((header, index) => {
      obj[header.trim()] = values[index]?.trim().replace(/(^"|"$)/g, '') || '';
    });
  }
  private getRowSeparator(csv: string): string {
    if (csv.includes('\r\n')) return '\r\n';
    if (csv.includes('\n')) return '\n';
    return '\r';
  }
  private getHeaderSeparator(rows: string[]): string {
    return ['\t', ';', ',', ' ', '|'].find(sep => rows[0]?.includes(sep)) || '';
  }
  //#endregion private methods
}