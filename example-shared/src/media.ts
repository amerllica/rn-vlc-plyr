import type { VlcSource } from 'rn-vlc-plyr';

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
  badges: MediaBadge[];
}

export const DEFAULT_SUBTITLE_URI =
  'https://raw.githubusercontent.com/andreyvit/subtitle-tools/master/sample.srt';

const container = (label: string): MediaBadge => ({ label, tone: 'neutral' });
const codec = (label: string): MediaBadge => ({ label, tone: 'neutral' });
const MULTI_AUDIO: MediaBadge = { label: 'multi-audio', tone: 'info' };
const SUBTITLES: MediaBadge = { label: 'subtitles', tone: 'info' };

export const LIVE_BADGE: MediaBadge = { label: 'live', tone: 'accent' };

export const MEDIA: MediaItem[] = [
  {
    id: 'bbb-mp4',
    title: 'Big Buck Bunny',
    description: 'Blender open movie, 10 s at 1080p. The quickest smoke test.',
    format: 'MP4',
    uri: 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4',
    badges: [container('MP4'), codec('H.264'), codec('1080p')],
  },
  {
    id: 'matroska-test5',
    title: 'Matroska multi-track',
    description:
      'IETF CELLAR test file with several audio tracks and subtitle languages.',
    format: 'MKV',
    uri: 'https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv',
    badges: [container('MKV'), codec('H.264'), MULTI_AUDIO, SUBTITLES],
  },
  {
    id: 'apple-hls',
    title: 'Apple HLS',
    description: 'Apple bip-bop adaptive HTTP Live Streaming test stream.',
    format: 'HLS',
    uri: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8',
    badges: [container('HLS'), codec('H.264')],
  },
  {
    id: 'bbb-mkv',
    title: 'Big Buck Bunny MKV',
    description: 'The same clip in a Matroska container.',
    format: 'MKV',
    uri: 'https://test-videos.co.uk/vids/bigbuckbunny/mkv/1080/Big_Buck_Bunny_1080_10s_1MB.mkv',
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
}

export const routeForMedia = (media: MediaItem): PlayerRoute => ({
  title: media.title,
  source: { uri: media.uri, title: media.title },
});
