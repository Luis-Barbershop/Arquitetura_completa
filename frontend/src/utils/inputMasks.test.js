import { describe, expect, it } from 'vitest';

import { maskCpf, maskPhone, onlyDigits } from './inputMasks';

describe('inputMasks', () => {
  it('onlyDigits removes every non digit character', () => {
    expect(onlyDigits('CPF 123.456.789-01')).toBe('12345678901');
    expect(onlyDigits(undefined)).toBe('');
  });

  it('maskCpf formats and truncates CPF numbers', () => {
    expect(maskCpf('12345678901')).toBe('123.456.789-01');
    expect(maskCpf('1234567890123')).toBe('123.456.789-01');
    expect(maskCpf('abc')).toBe('');
  });

  it('maskPhone formats mobile and fixed phone numbers', () => {
    expect(maskPhone('')).toBe('');
    expect(maskPhone('11')).toBe('(11');
    expect(maskPhone('1198')).toBe('(11) 98');
    expect(maskPhone('1198765')).toBe('(11) 9876-5');
    expect(maskPhone('11987654321')).toBe('(11) 98765-4321');
  });
});