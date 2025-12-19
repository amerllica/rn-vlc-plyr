import { StyleSheet } from 'react-native';

export const createStyle = StyleSheet.create;

export const styleJoiner = (...args: any[]) => StyleSheet.flatten(args);

export const isString = (thing: unknown): boolean => typeof thing === 'string';

export const isNumber = (thing: unknown): boolean => typeof thing === 'number';
