import type { VlcSource } from './specs/VlcPlayer.nitro';

export const SOURCE_FIELDS = [
  'uri',
  'userAgent',
  'referrer',
  'vlcOptions',
  'title',
] as const satisfies readonly (keyof VlcSource)[];

export const sourceValues = (source: VlcSource) =>
  SOURCE_FIELDS.map((field) => source[field]);
