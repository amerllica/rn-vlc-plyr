import type {
  VlcOutlineThickness,
  VlcResizeMode,
  VlcSurfaceType,
} from 'rn-vlc-plyr';

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

export const REMOTE_SKIP_INTERVALS_MS = [5000, 10000, 15000, 30000] as const;

export const RECONNECT_ON_RESUME_AFTER_MS = [0, 5000, 15000, 30000] as const;

export const RELOAD_ON_STALL_AFTER_MS = [0, 5000, 10000, 20000] as const;

export const LARGE_NETWORK_CACHE_OPTIONS = ['--network-caching=3000'];

export const SUBTITLE_FONT_SCALES = [50, 75, 100, 150, 200] as const;

export const DEFAULT_SUBTITLE_FONT_SCALE = 100;

export const SUBTITLE_COLORS = [
  { label: 'White', value: '#FFFFFF' },
  { label: 'Yellow', value: '#FFFF00' },
  { label: 'Cyan', value: '#00FFFF' },
] as const;

export const DEFAULT_SUBTITLE_COLOR = '#FFFFFF';

export const SUBTITLE_BACKGROUND_COLOR = '#000000B3';

export const OUTLINE_THICKNESSES: readonly VlcOutlineThickness[] = [
  'none',
  'thin',
  'normal',
  'thick',
];

export const DEFAULT_OUTLINE_THICKNESS: VlcOutlineThickness = 'normal';
