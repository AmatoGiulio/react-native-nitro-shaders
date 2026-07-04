import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import { MaterialOrb } from 'react-native-nitro-shaders';

export default function App() {
  return (
    <View style={styles.container}>
      <View style={styles.item}>
        <MaterialOrb material="metal" style={styles.surface} />
        <Text style={styles.label}>metal</Text>
      </View>
      <View style={styles.item}>
        <MaterialOrb material="water" style={styles.surface} />
        <Text style={styles.label}>water</Text>
      </View>
      <View style={styles.item}>
        <MaterialOrb material="iridescent" style={styles.surface} />
        <Text style={styles.label}>iridescent</Text>
      </View>
      <View style={styles.item}>
        <MaterialOrb material="aura" style={styles.surface} />
        <Text style={styles.label}>aura</Text>
      </View>
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
    gap: 24,
  },
  item: {
    alignItems: 'center',
    gap: 8,
  },
  surface: {
    width: 220,
    height: 220,
  },
  label: {
    color: '#15181f',
    fontSize: 13,
    fontWeight: '600',
  },
});
