import { View } from 'react-native';
import { FakePlayer } from '../__fixtures__/fakePlayer';

export const createdPlayers: FakePlayer[] = [];

export const NitroModules = {
  createHybridObject: () => {
    const player = new FakePlayer();
    createdPlayers.push(player);
    return player;
  },
};

export const getHostComponent = () => View;
