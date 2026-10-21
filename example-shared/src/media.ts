import type { VlcNowPlaying, VlcSource } from 'rn-vlc-plyr';

export type BadgeTone = 'neutral' | 'accent' | 'info' | 'danger';

export interface MediaBadge {
  label: string;
  tone: BadgeTone;
}

export interface MediaItem {
  id: string;
  title: string;
  description: string;
  format: string;
  uri: string;
  artist?: string;
  artworkUri?: string;
  badges: MediaBadge[];
}

export const DEFAULT_AUDIO_URI = '/data/data/rnvlcplyr.example/cache/audio.m4a';

export const DEFAULT_SUBTITLE_URI =
  'https://raw.githubusercontent.com/andreyvit/subtitle-tools/master/sample.srt';

const container = (label: string): MediaBadge => ({ label, tone: 'neutral' });
const codec = (label: string): MediaBadge => ({ label, tone: 'neutral' });
const MULTI_AUDIO: MediaBadge = { label: 'multi-audio', tone: 'info' };
const SUBTITLES: MediaBadge = { label: 'subtitles', tone: 'info' };
const BLENDER = 'Blender Foundation';
const BIG_BUCK_BUNNY_POSTER =
  'https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/500px-Big_buck_bunny_poster_big.jpg';
const MATROSKA_LOGO =
  'https://upload.wikimedia.org/wikipedia/commons/thumb/3/39/Matroska.svg/500px-Matroska.svg.png';
const APPLE_LOGO =
  'https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Apple_logo_black.svg/500px-Apple_logo_black.svg.png';

export const LIVE_BADGE: MediaBadge = { label: 'live', tone: 'accent' };

export const MEDIA: MediaItem[] = [
  {
    id: 'bbb-mp4',
    title: 'Big Buck Bunny',
    description: 'Blender open movie, 10 s at 1080p. The quickest smoke test.',
    format: 'MP4',
    uri: 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4',
    artist: BLENDER,
    artworkUri: BIG_BUCK_BUNNY_POSTER,
    badges: [container('MP4'), codec('H.264'), codec('1080p')],
  },
  {
    id: 'matroska-test5',
    title: 'Matroska multi-track',
    description:
      'IETF CELLAR test file with several audio tracks and subtitle languages.',
    format: 'MKV',
    uri: 'https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv',
    artist: 'IETF CELLAR',
    artworkUri: MATROSKA_LOGO,
    badges: [container('MKV'), codec('H.264'), MULTI_AUDIO, SUBTITLES],
  },
  {
    id: 'apple-hls',
    title: 'Apple HLS',
    description: 'Apple bip-bop adaptive HTTP Live Streaming test stream.',
    format: 'HLS',
    uri: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8',
    artist: 'Apple',
    artworkUri: APPLE_LOGO,
    badges: [container('HLS'), codec('H.264')],
  },
  {
    id: 'bbb-mkv',
    title: 'Big Buck Bunny MKV',
    description: 'The same clip in a Matroska container.',
    format: 'MKV',
    uri: 'https://test-videos.co.uk/vids/bigbuckbunny/mkv/1080/Big_Buck_Bunny_1080_10s_1MB.mkv',
    artist: BLENDER,
    artworkUri: BIG_BUCK_BUNNY_POSTER,
    badges: [container('MKV'), codec('H.264'), codec('1080p')],
  },
  {
    id: 'broken',
    title: 'Broken URL',
    description:
      'A host that never resolves. Exercises the error status and retry.',
    format: 'ERR',
    uri: 'https://example.invalid/missing.mp4',
    badges: [container('MP4'), { label: 'error demo', tone: 'danger' }],
  },
];

export interface PlayerRoute {
  title: string;
  source: VlcSource;
  nowPlaying: VlcNowPlaying;
}

export const routeForMedia = (media: MediaItem): PlayerRoute => ({
  title: media.title,
  source: { uri: media.uri, title: media.title },
  nowPlaying: {
    title: media.title,
    artist: media.artist,
    album: media.format,
    artworkUri: media.artworkUri,
  },
});
