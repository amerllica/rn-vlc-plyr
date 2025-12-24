export const clamp = (min: number = 0, value: number, max: number = 100) =>
  Math.min(max, Math.max(min, value));
