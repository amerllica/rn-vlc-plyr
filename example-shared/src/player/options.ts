import type { VlcResizeMode, VlcSurfaceType } from 'rn-vlc-plyr';

export const PLAYBACK_RATES = [0.5, 0.75, 1, 1.25, 1.5, 2] as const;

export const RESIZE_MODES: readonly VlcResizeMode[] = [
  'contain',
  'cover',
  'stretch',
  'original',
];

export const SURFACE_TYPES: readonly VlcSurfaceType[] = ['texture', 'surface'];

export const DELAY_STEP_MS = 100;

export const TIME_UPDATE_INTERVAL_MS = 250;
