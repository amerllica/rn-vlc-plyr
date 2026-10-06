import type {
  VlcError,
  VlcListenerSubscription,
  VlcLoadedInfo,
  VlcPlayer,
  VlcSource,
  VlcStatus,
  VlcTimeUpdate,
  VlcTracksInfo,
  VlcVideoSize,
  VlcVolumeInfo,
} from '../specs/VlcPlayer.nitro';

type Listener<T> = (value: T) => void;

class Channel<T> {
  readonly listeners = new Set<Listener<T>>();

  add(listener: Listener<T>): VlcListenerSubscription {
    this.listeners.add(listener);
    return { remove: () => this.listeners.delete(listener) };
  }

  emit(value: T) {
    this.listeners.forEach((listener) => listener(value));
  }
}

export class FakePlayer implements VlcPlayer {
  readonly name = 'VlcPlayer';
  readonly calls: string[] = [];
  readonly channels = {
    statusChange: new Channel<VlcStatus>(),
    timeUpdate: new Channel<VlcTimeUpdate>(),
    loaded: new Channel<VlcLoadedInfo>(),
    ended: new Channel<void>(),
    error: new Channel<VlcError>(),
    volumeChange: new Channel<VlcVolumeInfo>(),
    tracksChange: new Channel<VlcTracksInfo>(),
    videoSizeChange: new Channel<VlcVideoSize>(),
  };

  private currentSource: VlcSource | undefined;
  released = false;
  volume = 100;
  muted = false;
  rate = 1;
  loop = false;
  autoPlay = true;
  timeUpdateInterval = 250;
  subtitleDelay = 0;
  audioDelay = 0;
  status: VlcStatus = 'idle';
  error: VlcError | undefined = undefined;
  currentTime = 0;
  duration = 0;
  isLive = false;
  isSeekable = false;
  videoSize: VlcVideoSize = { width: 0, height: 0 };
  audioTracks = [];
  subtitleTracks = [];
  selectedAudioTrack = -1;
  selectedSubtitleTrack = -1;

  get source(): VlcSource | undefined {
    return this.currentSource;
  }

  set source(value: VlcSource | undefined) {
    this.calls.push(`source:${value?.uri ?? 'none'}`);
    this.currentSource = value;
  }

  setStatus(status: VlcStatus) {
    this.status = status;
    this.channels.statusChange.emit(status);
  }

  play() {}
  pause() {}
  stop() {}
  seek() {}
  seekBy() {}
  setAudioTrack() {}
  setSubtitleTrack() {}
  addSubtitle() {}
  snapshot() {
    return Promise.resolve('/tmp/snapshot.png');
  }
  release() {
    this.released = true;
    this.calls.push('release');
  }
  dispose() {}
  equals(other: VlcPlayer): boolean {
    return other === this;
  }
  toString() {
    return 'FakePlayer';
  }

  addOnStatusChangeListener(listener: Listener<VlcStatus>) {
    return this.channels.statusChange.add(listener);
  }
  addOnTimeUpdateListener(listener: Listener<VlcTimeUpdate>) {
    return this.channels.timeUpdate.add(listener);
  }
  addOnLoadedListener(listener: Listener<VlcLoadedInfo>) {
    return this.channels.loaded.add(listener);
  }
  addOnEndedListener(listener: () => void) {
    return this.channels.ended.add(listener);
  }
  addOnErrorListener(listener: Listener<VlcError>) {
    return this.channels.error.add(listener);
  }
  addOnVolumeChangeListener(listener: Listener<VlcVolumeInfo>) {
    return this.channels.volumeChange.add(listener);
  }
  addOnTracksChangeListener(listener: Listener<VlcTracksInfo>) {
    return this.channels.tracksChange.add(listener);
  }
  addOnVideoSizeChangeListener(listener: Listener<VlcVideoSize>) {
    return this.channels.videoSizeChange.add(listener);
  }
}
