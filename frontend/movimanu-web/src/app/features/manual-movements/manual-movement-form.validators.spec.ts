import { FormControl, Validators } from '@angular/forms';
import {
  formatMonetaryTextForDisplay,
  integerRangeValidator,
  monetaryTextValidator,
  nonWhitespaceValidator,
  toApiMonetaryText,
} from './manual-movement-form.validators';

describe('manual movement form validators', () => {
  it('accepts month and year boundaries and rejects invalid integer text', () => {
    const month = new FormControl('', integerRangeValidator(1, 12, 2));
    const year = new FormControl('', integerRangeValidator(1, 9999, 4));

    for (const value of ['1', '12', '01']) {
      month.setValue(value);
      expect(month.valid).toBe(true);
    }
    for (const value of ['0', '13', '123', '1.0', '-1']) {
      month.setValue(value);
      expect(month.invalid).toBe(true);
    }

    for (const value of ['1', '0001', '9999']) {
      year.setValue(value);
      expect(year.valid).toBe(true);
    }
    for (const value of ['0', '0000', '10000', '20.26', '-1']) {
      year.setValue(value);
      expect(year.invalid).toBe(true);
    }
  });

  it('requires description content without trimming valid text and enforces its full length', () => {
    const description = new FormControl('', [
      Validators.required,
      nonWhitespaceValidator,
      Validators.maxLength(50),
    ]);

    description.setValue('');
    expect(description.hasError('required')).toBe(true);
    description.setValue('   ');
    expect(description.hasError('whitespaceOnly')).toBe(true);

    const paddedDescription = '  Ajuste manual  ';
    description.setValue(paddedDescription);
    expect(description.valid).toBe(true);
    expect(description.value).toBe(paddedDescription);

    description.setValue(`${'x'.repeat(49)} `);
    expect(description.valid).toBe(true);
    description.setValue(`${'x'.repeat(50)} `);
    expect(description.hasError('maxlength')).toBe(true);
  });

  it('accepts supported monetary text and rejects zero, grouping and invalid precision', () => {
    const value = new FormControl('', [Validators.required, monetaryTextValidator]);

    for (const accepted of [
      '150',
      '150,5',
      '150.50',
      '-150,00',
      '1234,56',
      '1,23',
      '1.23',
      '9999999999999999,99',
    ]) {
      value.setValue(accepted);
      expect(value.valid).toBe(true);
    }

    for (const rejected of [
      '0',
      '000',
      '0.00',
      '0,00',
      '-0.0',
      '-0,00',
      '-000,00',
      '1.234',
      '1,234',
      '1.234,56',
      '1,234.56',
      '1 234,56',
      '1e3',
      '1E3',
      '150,001',
      '10000000000000000,00',
      '+150,00',
    ]) {
      value.setValue(rejected);
      expect(value.invalid).toBe(true);
    }
  });

  it('converts valid monetary input by text and formats it without grouping', () => {
    expect(toApiMonetaryText('150')).toBe('150.00');
    expect(toApiMonetaryText('150,5')).toBe('150.50');
    expect(toApiMonetaryText('150.50')).toBe('150.50');
    expect(toApiMonetaryText('-150,00')).toBe('-150.00');
    expect(toApiMonetaryText('1,23')).toBe(toApiMonetaryText('1.23'));
    expect(toApiMonetaryText('1.23')).toBe('1.23');
    expect(toApiMonetaryText('9999999999999999,99')).toBe('9999999999999999.99');
    expect(toApiMonetaryText('1,234')).toBeNull();

    expect(formatMonetaryTextForDisplay('150')).toBe('150,00');
    expect(formatMonetaryTextForDisplay('150.5')).toBe('150,50');
    expect(formatMonetaryTextForDisplay('1234.56')).toBe('1234,56');
    expect(formatMonetaryTextForDisplay('9999999999999999,99')).toBe(
      '9999999999999999,99',
    );
    expect(formatMonetaryTextForDisplay('1,234')).toBe('1,234');
  });
});
