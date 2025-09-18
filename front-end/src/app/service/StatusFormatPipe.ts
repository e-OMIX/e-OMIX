import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'statusFormat' })
export class StatusFormatPipe implements PipeTransform {
  transform(value: string): string {
    if (!value) return '';
    // Replace underscores only if they exist
    return value.includes('_')
      ? value.replace(/_/g, ' ')
      : value;
  }
}