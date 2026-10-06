import type { VlcPlayer, VlcStatus, VlcTrack } from 'rn-vlc-plyr';

const ACTIVE_STATUSES: ReadonlySet<VlcStatus> = new Set([
  'opening',
  'buffering',
  'playing',
]);

const BUSY_STATUSES: ReadonlySet<VlcStatus> = new Set(['opening', 'buffering']);

const PINNED_OVERLAY_STATUSES: ReadonlySet<VlcStatus> = new Set([
  'idle',
  'stopped',
  'ended',
  'error',
]);

export const SKIP_MS = 10_000;

export const isActiveStatus = (status: VlcStatus) =>
  ACTIVE_STATUSES.has(status);

export const isBusyStatus = (status: VlcStatus) => BUSY_STATUSES.has(status);

export const pinsOverlay = (status: VlcStatus) =>
  PINNED_OVERLAY_STATUSES.has(status);

export const togglePlayback = (player: VlcPlayer, status: VlcStatus) => {
  if (isActiveStatus(status)) {
    player.pause();
  } else {
    player.play();
  }
};

export const trackName = (tracks: VlcTrack[], id: number): string =>
  tracks.find((track) => track.id === id)?.name ?? 'Off';
