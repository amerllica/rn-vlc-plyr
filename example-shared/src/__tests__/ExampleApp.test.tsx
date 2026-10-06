import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { fireEvent, render, screen } from '@testing-library/react-native';
import type * as NitroModulesMock from '../../../src/__mocks__/react-native-nitro-modules';
import { ExampleApp } from '../ExampleApp';
import { MEDIA } from '../media';

jest.mock('react-native-nitro-modules');
jest.mock('react-native-svg');
jest.mock('@react-native-community/slider');
jest.mock('react-native-safe-area-context');

const { createdPlayers } = jest.requireMock<typeof NitroModulesMock>(
  'react-native-nitro-modules'
);

describe('ExampleApp', () => {
  beforeEach(() => {
    createdPlayers.length = 0;
  });

  it('shows the library with every sample media card', async () => {
    await render(<ExampleApp />);

    expect(screen.getByText('rn-vlc-plyr')).toBeTruthy();
    expect(screen.getByText('VLC for React Native')).toBeTruthy();
    for (const media of MEDIA) {
      expect(screen.getByTestId(`media-${media.id}`)).toBeTruthy();
      expect(screen.getByText(media.title)).toBeTruthy();
    }
    expect(screen.getByText('Custom URL')).toBeTruthy();
    expect(screen.getByText('error demo')).toBeTruthy();
    expect(createdPlayers).toHaveLength(0);
  });

  it('opens the player for a card and goes back to the library', async () => {
    const [media] = MEDIA;
    if (media === undefined) {
      throw new Error('the media catalog is empty');
    }
    await render(<ExampleApp />);

    await fireEvent.press(screen.getByTestId(`media-${media.id}`));

    expect(screen.getByText('Playback')).toBeTruthy();
    expect(createdPlayers.at(-1)?.source).toEqual({
      uri: media.uri,
      title: media.title,
    });

    await fireEvent.press(screen.getByLabelText('Back'));

    expect(screen.getByText('Sample media')).toBeTruthy();
    expect(createdPlayers.at(-1)?.released).toBe(true);
  });

  it('opens a custom URL with HTTP options', async () => {
    await render(<ExampleApp />);

    await fireEvent.changeText(
      screen.getByLabelText('Media URL'),
      'https://example.com/live.m3u8'
    );
    await fireEvent.press(screen.getByLabelText('HTTP options'));
    await fireEvent.changeText(screen.getByLabelText('User agent'), 'Lab/1.0');
    await fireEvent.press(screen.getByLabelText('Open player'));

    expect(createdPlayers.at(-1)?.source).toEqual({
      uri: 'https://example.com/live.m3u8',
      title: 'Custom URL',
      userAgent: 'Lab/1.0',
      referrer: undefined,
    });
  });
});
