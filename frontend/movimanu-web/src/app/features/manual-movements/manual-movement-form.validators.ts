import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function integerRangeValidator(
  minimum: number,
  maximum: number,
  maximumDigits: number,
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === undefined || value === '') {
      return null;
    }

    if (
      typeof value !== 'string' ||
      !new RegExp(`^\\d{1,${maximumDigits}}$`).test(value)
    ) {
      return { integerFormat: true };
    }

    const number = Number(value);
    return Number.isInteger(number) && number >= minimum && number <= maximum
      ? null
      : { integerRange: true };
  };
}

export function nonWhitespaceValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (value === null || value === undefined || value === '') {
    return null;
  }

  return typeof value === 'string' && /\S/.test(value) ? null : { whitespaceOnly: true };
}

export function monetaryTextValidator(control: AbstractControl): ValidationErrors | null {
  return monetaryValueError(control.value);
}

function monetaryValueError(value: unknown): ValidationErrors | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }

  if (typeof value !== 'string' || !/^-?\d{1,16}(?:[.,]\d{1,2})?$/.test(value)) {
    return { monetaryFormat: true };
  }

  const digits = value.replace(/^-/, '').replace(/[.,]/, '');
  return /[1-9]/.test(digits) ? null : { monetaryZero: true };
}

export function toApiMonetaryText(value: string): string | null {
  if (monetaryValueError(value) !== null) {
    return null;
  }

  const [integerPart, fraction = ''] = value.replace(',', '.').split('.');
  return `${integerPart}.${fraction.padEnd(2, '0')}`;
}

export function formatMonetaryTextForDisplay(value: string): string {
  if (monetaryValueError(value) !== null) {
    return value;
  }

  const [integerPart, fraction = ''] = value.replace(',', '.').split('.');
  return `${integerPart},${fraction.padEnd(2, '0')}`;
}
