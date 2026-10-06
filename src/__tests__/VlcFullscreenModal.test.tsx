import { describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';
import { VlcFullscreenModal } from '../VlcFullscreenModal';
import { FakePlayer } from '../__fixtures__/fakePlayer';

jest.mock('react-native-nitro-modules');

describe('VlcFullscreenModal', () => {
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
