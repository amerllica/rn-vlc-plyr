import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { act, fireEvent, render, screen } from '@testing-library/react-native';
import type { FakePlayer } from '../../../src/__fixtures__/fakePlayer';
import type * as NitroModulesMock from '../../../src/__mocks__/react-native-nitro-modules';
import { PlayerScreen } from '../player/PlayerScreen';

jest.mock('react-native-nitro-modules');
jest.mock('react-native-svg');
jest.mock('@react-native-community/slider');
jest.mock('react-native-safe-area-context');

const { createdPlayers } = jest.requireMock<typeof NitroModulesMock>(
  'react-native-nitro-modules'
);

const ROUTE = {
  title: 'Sample',
  source: { uri: 'https://example.com/sample.mkv', title: 'Sample' },
};

const lastPlayer = (): FakePlayer => {
  const player = createdPlayers.at(-1);
  if (player === undefined) {
    throw new Error('no player was created');
  }
  return player;
};

const renderPlayer = async () => {
  await render(<PlayerScreen route={ROUTE} onBack={() => {}} />);
  return lastPlayer();
};

const apiValue = (name: string) =>
  screen.getByTestId(`api-${name}`).props.children;

describe('PlayerScreen', () => {
  beforeEach(() => {
    createdPlayers.length = 0;
  });

  it('shows live player values in the API lab', async () => {
    const player = await renderPlayer();
    await fireEvent.press(screen.getByLabelText('Expand API lab'));

    await act(() => {
      player.setStatus('playing');
      player.channels.loaded.emit({
        duration: 120_000,
        isSeekable: true,
        isLive: false,
        videoSize: { width: 1920, height: 1080 },
        audioTracks: [{ id: 1, name: 'English' }],
        subtitleTracks: [{ id: 3, name: 'French' }],
      });
      player.channels.timeUpdate.emit({
        currentTime: 65_000,
        duration: 120_000,
      });
      player.channels.volumeChange.emit({ volume: 40, muted: true });
      player.channels.tracksChange.emit({
        audioTracks: [{ id: 1, name: 'English' }],
        subtitleTracks: [{ id: 3, name: 'French' }],
        selectedAudioTrack: 1,
        selectedSubtitleTrack: 3,
      });
    });

    expect(apiValue('status')).toBe('playing');
    expect(apiValue('currentTime')).toBe('1:05 (65000 ms)');
    expect(apiValue('duration')).toBe('2:00 (120000 ms)');
    expect(apiValue('volume')).toBe('40');
    expect(apiValue('muted')).toBe('true');
    expect(apiValue('isSeekable')).toBe('true');
    expect(apiValue('videoSize')).toBe('1920×1080');
    expect(apiValue('selectedAudioTrack')).toBe('1 (English)');
    expect(apiValue('selectedSubtitleTrack')).toBe('3 (French)');
    expect(apiValue('error')).toBe('—');
    expect(screen.getAllByTestId('event-log-entry').length).toBeGreaterThan(0);
  });

  it('writes speed, loop and delays to the player', async () => {
    const player = await renderPlayer();
    await fireEvent.press(screen.getByLabelText('Expand API lab'));

    await fireEvent.press(screen.getByLabelText('1.5×'));
    await fireEvent(screen.getByLabelText('Loop'), 'valueChange', true);
    await fireEvent.press(screen.getByLabelText('Increase Audio delay'));
    await fireEvent.press(screen.getByLabelText('Decrease Subtitle delay'));

    expect(player.rate).toBe(1.5);
    expect(player.loop).toBe(true);
    expect(player.audioDelay).toBe(100);
    expect(player.subtitleDelay).toBe(-100);
    expect(apiValue('rate')).toBe('1.5');
    expect(apiValue('loop')).toBe('true');
  });

  it('shows the error panel and retries with play', async () => {
    const player = await renderPlayer();
    const play = jest.spyOn(player, 'play');

    await act(() => {
      player.setStatus('error');
      player.channels.error.emit({
        code: 'network',
        message: 'Could not open the media over the network',
      });
    });

    expect(screen.getByText('network')).toBeTruthy();
    expect(
      screen.getByText('Could not open the media over the network')
    ).toBeTruthy();

    await fireEvent.press(screen.getByLabelText('Retry'));

    expect(play).toHaveBeenCalledTimes(1);
  });

  it('seeks when the seek bar is released', async () => {
    const player = await renderPlayer();
    const seek = jest.spyOn(player, 'seek');

    await fireEvent(screen.getByLabelText('Seek'), 'slidingComplete', 30_000);

    expect(seek).toHaveBeenCalledWith(30_000);
  });
});
