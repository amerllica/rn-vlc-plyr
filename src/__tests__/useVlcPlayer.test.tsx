import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { renderHook } from '@testing-library/react-native';
import { useVlcPlayer } from '../useVlcPlayer';
import { createdPlayers } from '../__mocks__/react-native-nitro-modules';
import type { FakePlayer } from '../__fixtures__/fakePlayer';

jest.mock('react-native-nitro-modules');

const lastPlayer = (): FakePlayer => {
  const player = createdPlayers.at(-1);
  if (player === undefined) {
    throw new Error('no player was created');
  }
  return player;
};

describe('useVlcPlayer', () => {
  beforeEach(() => {
    createdPlayers.length = 0;
  });

  it('runs setup before the source is assigned', async () => {
    await renderHook(() =>
      useVlcPlayer('https://example.com/a.mp4', (player) => {
        player.autoPlay = false;
        (player as FakePlayer).calls.push('setup');
      })
    );

    const player = lastPlayer();
    expect(player.calls.slice(0, 2)).toEqual([
      'setup',
      'source:https://example.com/a.mp4',
    ]);
    expect(player.autoPlay).toBe(false);
  });

  it('keeps one player across renders and swaps the source in place', async () => {
    const { result, rerender } = await renderHook(
      ({ uri }: { uri: string }) => useVlcPlayer(uri),
      { initialProps: { uri: 'https://example.com/a.mp4' } }
    );
    const first = result.current;

    await rerender({ uri: 'https://example.com/b.mp4' });

    expect(result.current).toBe(first);
    expect(first.source).toEqual({ uri: 'https://example.com/b.mp4' });
  });

  it('does not reassign an unchanged source on rerender', async () => {
    const { rerender } = await renderHook(() =>
      useVlcPlayer({ uri: 'https://example.com/a.mp4', userAgent: 'test' })
    );
    await rerender({});

    const sourceAssignments = lastPlayer().calls.filter((call) =>
      call.startsWith('source:')
    );
    expect(sourceAssignments).toHaveLength(1);
  });

  it('clears the source when it becomes null', async () => {
    const { result, rerender } = await renderHook(
      ({ uri }: { uri: string | null }) => useVlcPlayer(uri),
      { initialProps: { uri: 'https://example.com/a.mp4' as string | null } }
    );

    await rerender({ uri: null });

    expect(result.current.source).toBeUndefined();
  });

  it('releases the player on unmount', async () => {
    const { result, unmount } = await renderHook(() =>
      useVlcPlayer('https://example.com/a.mp4')
    );
    const player = result.current as FakePlayer;

    await unmount();

    expect(player.released).toBe(true);
  });
});
