import { StatusBar } from 'expo-status-bar';
import { StyleSheet, View } from 'react-native';
import { ShaderSurface } from 'react-native-nitro-shaders';

export default function App() {
  return (
    <View style={styles.container}>
      <ShaderSurface
        color="#4f7cff"
        animated={false}
        paused={false}
        debugTime={0}
        style={styles.surface}
      />
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
