import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { errorMessage } from '../errorMessage';
import { typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { TextField } from '../ui/TextField';

const ADDED_MESSAGE = 'Added. The track appears once VLC parses it.';

interface ExternalTrackFieldProps {
  label: string;
  actionLabel: string;
  placeholder: string;
  initialUri: string;
  onAdd: (uri: string) => void;
}

export function ExternalTrackField({
  label,
  actionLabel,
  placeholder,
  initialUri,
  onAdd,
}: ExternalTrackFieldProps) {
  const [uri, setUri] = useState(initialUri);
  const [feedback, setFeedback] = useState<string>();
  const trimmedUri = uri.trim();

  const add = () => {
    try {
      onAdd(trimmedUri);
      setFeedback(ADDED_MESSAGE);
    } catch (reason) {
      setFeedback(errorMessage(reason));
    }
  };

  return (
    <>
      <TextField
        label={label}
        value={uri}
        onChangeText={setUri}
        placeholder={placeholder}
      />
      <ActionButton
        label={actionLabel}
        disabled={trimmedUri === ''}
        onPress={add}
      />
      {feedback ? (
        <Text style={[typography.caption, styles.feedback]}>{feedback}</Text>
      ) : null}
    </>
  );
}

const styles = StyleSheet.create({
  feedback: {
    marginTop: -4,
  },
});
