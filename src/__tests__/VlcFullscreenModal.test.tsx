import { describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';
import { VlcFullscreenModal } from '../VlcFullscreenModal';
import { FakePlayer } from '../__fixtures__/fakePlayer';

jest.mock('react-native-nitro-modules');

type RenderedTree = Awaited<ReturnType<typeof render>>['container'];

const videoViews = (container: RenderedTree) =>
  container.queryAll((instance) => instance.props.player !== undefined);

describe('VlcFullscreenModal', () => {
  it('passes the player, resize mode and surface type to the video view', async () => {
    const player = new FakePlayer();
    const { container } = await render(
      <VlcFullscreenModal
        player={player}
        visible
        onClose={() => {}}
        resizeMode="cover"
        surfaceType="surface"
      />
    );

    const [view] = videoViews(container);
    expect(view?.props).toMatchObject({
      player,
      resizeMode: 'cover',
      surfaceType: 'surface',
    });
  });

  it('uses the contain resize mode by default', async () => {
    const { container } = await render(
      <VlcFullscreenModal
        player={new FakePlayer()}
        visible
        onClose={() => {}}
      />
    );

    expect(videoViews(container)[0]?.props.resizeMode).toBe('contain');
  });

  it('mounts no video view while hidden', async () => {
    const { container } = await render(
      <VlcFullscreenModal
        player={new FakePlayer()}
        visible={false}
        onClose={() => {}}
      />
    );

    expect(videoViews(container)).toEqual([]);
  });

  it('renders overlay children when visible', async () => {
    await render(
      <VlcFullscreenModal player={new FakePlayer()} visible onClose={() => {}}>
        <Text>controls</Text>
      </VlcFullscreenModal>
    );

    expect(screen.getByText('controls')).toBeTruthy();
  });

  it('closes on the hardware back request', async () => {
    const closed: boolean[] = [];
    const { container } = await render(
      <VlcFullscreenModal
        player={new FakePlayer()}
        visible
        onClose={() => closed.push(true)}
      />
    );

    const [modal] = container.queryAll(
      (instance) => typeof instance.props.onRequestClose === 'function'
    );
    if (modal === undefined) {
      throw new Error('modal host not rendered');
    }
    await fireEvent(modal, 'requestClose');

    expect(closed).toEqual([true]);
  });
});
