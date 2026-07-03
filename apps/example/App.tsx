import { StatusBar } from 'expo-status-bar';
import { StyleSheet, View } from 'react-native';
import { FluidGradient, LiquidChrome } from 'react-native-nitro-shaders';

export default function App() {
  return (
    <View style={styles.container}>
      <FluidGradient
        colors={['#4f7cff', '#9b5cff', '#ff5c87']}
        style={styles.surface}
      />
      <LiquidChrome variant="silver" style={{ width: 220, height: 220, marginTop: 24 }} />
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  surface: {
    width: 220,
    height: 220,
  },
});
