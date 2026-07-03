import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import { LiquidMetal } from 'react-native-nitro-shaders';

export default function App() {
  return (
    <View style={styles.container}>
      <LiquidMetal shape="circle" style={styles.surface} />
      <Text style={styles.label}>LiquidMetal Paper</Text>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f7f8fb',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  surface: {
    width: 220,
    height: 220,
    overflow: 'hidden',
    borderRadius: 220,
  },
  label: {
    color: '#15181f',
    fontSize: 13,
    fontWeight: '600',
  },
});
