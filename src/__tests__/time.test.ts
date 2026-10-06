import { describe, expect, it } from '@jest/globals';
import { formatVlcTime } from '../time';

describe('formatVlcTime', () => {
  it.each([
    [0, '0:00'],
    [-5000, '0:00'],
    [9_999, '0:09'],
    [65_000, '1:05'],
    [3_725_000, '1:02:05'],
  ])('formats %d ms as %s', (ms, expected) => {
    expect(formatVlcTime(ms)).toBe(expected);
  });
});
