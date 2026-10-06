export const errorMessage = (reason: unknown): string =>
  reason instanceof Error ? reason.message : String(reason);
