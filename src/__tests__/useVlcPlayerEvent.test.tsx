import { describe, expect, it } from '@jest/globals';
import { act, renderHook } from '@testing-library/react-native';
import { useVlcPlayerEvent, useVlcPlayerStatus } from '../useVlcPlayerEvent';
import { addVlcPlayerListener } from '../events';
import { FakePlayer } from '../__fixtures__/fakePlayer';

describe('useVlcPlayerEvent', () => {
  it('calls the latest listener without resubscribing', async () => {
    const player = new FakePlayer();
    const received: string[] = [];
    const { rerender } = await renderHook(
      ({ tag }: { tag: string }) =>
        useVlcPlayerEvent(player, 'statusChange', (status) =>
          received.push(`${tag}:${status}`)
        ),
      { initialProps: { tag: 'first' } }
    );

    await rerender({ tag: 'second' });
    await act(() => player.setStatus('playing'));

    expect(received).toEqual(['second:playing']);
    expect(player.channels.statusChange.listeners.size).toBe(1);
  });

  it('removes the native listener on unmount', async () => {
    const player = new FakePlayer();
    const { unmount } = await renderHook(() =>
      useVlcPlayerEvent(player, 'timeUpdate', () => {})
    );

    await unmount();

    expect(player.channels.timeUpdate.listeners.size).toBe(0);
  });

  it('maps the ended event to a payload-less callback', async () => {
    const player = new FakePlayer();
    const received: unknown[] = [];
    addVlcPlayerListener(player, 'ended', (payload) => received.push(payload));

    player.channels.ended.emit();

    expect(received).toEqual([undefined]);
  });
});

describe('useVlcPlayerStatus', () => {
  it('follows native status changes', async () => {
    const player = new FakePlayer();
    const { result } = await renderHook(() => useVlcPlayerStatus(player));

    expect(result.current).toBe('idle');
    await act(() => player.setStatus('buffering'));
    expect(result.current).toBe('buffering');
  });
});
