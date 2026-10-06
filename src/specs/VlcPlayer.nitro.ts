import type { HybridObject } from 'react-native-nitro-modules';

export type VlcStatus =
  | 'idle'
  | 'opening'
  | 'buffering'
  | 'playing'
  | 'paused'
  | 'stopped'
  | 'ended'
  | 'error';

export type VlcErrorCode = 'invalidSource' | 'network' | 'media' | 'unknown';

export interface VlcSource {
  uri: string;
  userAgent?: string;
  referrer?: string;
  vlcOptions?: string[];
  title?: string;
}

export interface VlcTrack {
  id: number;
  name: string;
}

export interface VlcVideoSize {
  width: number;
  height: number;
}

export interface VlcError {
  code: VlcErrorCode;
  message: string;
}

export interface VlcTimeUpdate {
  currentTime: number;
  duration: number;
}

export interface VlcLoadedInfo {
  duration: number;
  isSeekable: boolean;
  isLive: boolean;
  videoSize: VlcVideoSize;
  audioTracks: VlcTrack[];
  subtitleTracks: VlcTrack[];
}

export interface VlcVolumeInfo {
  volume: number;
  muted: boolean;
}

export interface VlcTracksInfo {
  audioTracks: VlcTrack[];
  subtitleTracks: VlcTrack[];
  selectedAudioTrack: number;
  selectedSubtitleTrack: number;
}

export interface VlcListenerSubscription {
  remove: () => void;
}

export interface VlcPlayer extends HybridObject<{
  ios: 'swift';
  android: 'kotlin';
}> {
  source?: VlcSource;
  volume: number;
  muted: boolean;
  rate: number;
  loop: boolean;
  autoPlay: boolean;
  timeUpdateInterval: number;
  subtitleDelay: number;
  audioDelay: number;

  readonly status: VlcStatus;
  readonly error?: VlcError;
  readonly currentTime: number;
  readonly duration: number;
  readonly isLive: boolean;
  readonly isSeekable: boolean;
  readonly videoSize: VlcVideoSize;
  readonly audioTracks: VlcTrack[];
  readonly subtitleTracks: VlcTrack[];
  readonly selectedAudioTrack: number;
  readonly selectedSubtitleTrack: number;

  play(): void;
  pause(): void;
  stop(): void;
  seek(timeMs: number): void;
  seekBy(deltaMs: number): void;
  setAudioTrack(id: number): void;
  setSubtitleTrack(id: number): void;
  addSubtitle(uri: string, select: boolean): void;
  snapshot(): Promise<string>;
  release(): void;

  addOnStatusChangeListener(
    listener: (status: VlcStatus) => void
  ): VlcListenerSubscription;
  addOnTimeUpdateListener(
    listener: (event: VlcTimeUpdate) => void
  ): VlcListenerSubscription;
  addOnLoadedListener(
    listener: (event: VlcLoadedInfo) => void
  ): VlcListenerSubscription;
  addOnEndedListener(listener: () => void): VlcListenerSubscription;
  addOnErrorListener(
    listener: (error: VlcError) => void
  ): VlcListenerSubscription;
  addOnVolumeChangeListener(
    listener: (event: VlcVolumeInfo) => void
  ): VlcListenerSubscription;
  addOnTracksChangeListener(
    listener: (event: VlcTracksInfo) => void
  ): VlcListenerSubscription;
  addOnVideoSizeChangeListener(
    listener: (size: VlcVideoSize) => void
  ): VlcListenerSubscription;
}
